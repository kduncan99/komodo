/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.InvalidBlockSizeException;
import com.kadware.komodo.hardwarelib.exceptions.InvalidTrackCountException;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *  Class for emulating a tape device in a system file
 *
 * Tape device which uses host system files as media.
 * We emulate any tape device which does not do compression or block sequence numbering,
 * storing blocks of sequential 8-bit frames.
 * We do not actually do any checksumming or other parity or CRC checks - we assume
 * no media errors, since that is quite beyond our control.
 * We restrict the virtual length of the media to around 240 MBytes.
 * Maximum data block is 131 KBytes, while minimum data block is 1 Byte.
 *
 * We allow various mode changes:
 *      Data Converter is not allowed to be enabled
 *      Density changes are accepted and ignored in practice
 *      Noise Constant may be set to any value, but we ignore it in practice
 *      Parity can be any valid setting - we ignore it in practice
 *
 * The host system file name is not of concern to us, except that we need it
 * in order to 'mount' the media - i.e., we do an OpenFile type of command,
 * and then issue reads/writes as appropriate.
 *
 * The mount command is not in the IO chain - it is conceptually an out-of-band control
 * invoked by a controller which is aware of, and manages, a virtual tape library.
 * Of course, the application may also connect the user to the mount command,
 * allowing him to manually load a virtual tape as well.
 *
 * In-band data is stored as a series of sequential bytes, parenthesized at the beginning
 * and end with a 4-byte dword indicating the length of the data.  We can therefore support
 * data blocks as small as 0 bytes and as large as 2^32 - 2 bytes (see tape mark), but
 * see previous for actual enforced limits.
 *
 * A tape mark is stored as a single 4-byte dword containing 0xFFFFFFFF (this means that this
 * value cannot be used to describe the length of a data block).
 *
 * When two consecutive file marks are written, the host file is truncated at the point
 * immediately following the second file mark.  Note that this does not mean files are always
 * truncated after two file marks - for example, Write mark, Move file back, Move file forward,
 * Write mark will produce two consecutive file marks, but will not truncate the host file.
 *
 * If a read encounters an invalid block size, or exceeds the physical length of the host
 * system file, the device goes into loss-of-position state.  At this point, the only IO
 * functions which will succeed are rewind and unload - all other functions will receive
 * loss-of-position return status.  This is more restrictive than a true tape drive.
 *
 * An attempt to read after a previous write or write mark will result in invalid function.
 *
 * Attempts to move or read backwards from the load point result in end-of-tape status.
 * A read backwards function which reads valid data and stops at load point will receive a
 * normal successful IO completion, as will a move backward block which stops at load point.
 * A move file backward function (find previous tape mark) which ends at load point will
 * return successful if the first block on the medium is in fact a tape mark; if no tape mark
 * is found, and load point is reached, the IO will receive end-of-tape status.  A rewind
 * will position the tape to load point; since this is expected, even from load-point,
 * a rewind always returns successful status.
 *
 * Any write or write mark operation which causes the host system file pointer to reference
 * a location exceeding the built-in limit, will result in an end-of-tape status, with the
 * funcion completing otherwise successfully (it is therefore possible - and likely - that
 * the program or user will overwrite our maximum file size).
 *
 * Every file begins with a scratch pad which identifies the file as a virtual tape volume
 * and indicates the file size limit (if one exists) for the particular volume.
 */
@SuppressWarnings("Duplicates")
public class FileSystemTapeDevice extends TapeDevice {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    //TODO move this elsewhere
    private static class OutOfDataException extends Exception {}


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final int FILE_MARK_CONTROL_WORD = 0xffffffff;
    private static final int MAX_BLOCK_SIZE = 128 * 1024;
    private static final int MAX_FILE_SIZE = 240 * 1024 * 1024;
    private static final Logger LOGGER = LogManager.getLogger(FileSystemTapeDevice.class);

    /**
     * Backing file for the currently-mounted volume (if any)
     */
    private RandomAccessFile _file;

    /**
     * Indicates we have reached or exceeded max file size
     */
    private boolean _endOfTapeFlag;

    /**
     * Indicates the volume is positioned at load point
     */
    private boolean _loadPointFlag;

    /**
     * Currently-mounted volume name (if any)
     */
    private String _volumeName;

    /**
     * Last operation was a write
     */
    private boolean _writeFlag;

    /**
     * Last operation was a write mark
     */
    private boolean _writeMarkFlag;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     */
    private FileSystemTapeDevice(
        final String name
    ) {
        super(DeviceModel.FileSystemTape, name);
        _endOfTapeFlag = false;
        _loadPointFlag = false;
        _writeFlag = false;
        _writeMarkFlag = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     */
    @Override
    public boolean canConnect(
        final Node candidateAncestor
    ) {
        //  We can connect only to ByteDisk controllers
        return candidateAncestor instanceof ByteChannelModule;
    }

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Volume Name:     %s\n", _volumeName));
            //???? many other things to write
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    @Override
    public int getMaxBlockSize() { return MAX_BLOCK_SIZE; }

    @Override
    public boolean hasByteInterface() { return true; }

    @Override
    public boolean hasWordInterface() { return false; }

    @Override
    public void initialize() {}

    /**
     * Produces a byte stream describing this disk device.
     * This is an immediate IO, no waiting.
     */
    @Override
    protected void ioGetInfo(
        final DeviceIOInfo ioInfo
    ) {
//        //  Create a TapeDeviceInfo object and then serialize it to the IOInfo's buffer
//        TapeDeviceInfo devInfo = new TapeDeviceInfo(getDeviceType(),
//                                                    getDeviceModel(),
//                                                    _readyFlag,
//                                                    getSubsystemIdentifier(),
//                                                    _unitAttentionFlag,
//                                                    isMounted(),
//                                                    isWriteProtected(),
//                                                    getEndOfTapeFlag(),
//                                                    getLoadPointFlag());
//
//        ByteBuffer byteBuffer = ByteBuffer.wrap(ioInfo._byteBuffer);
//        devInfo.serialize(byteBuffer);
//        if (ioInfo._transferCount < byteBuffer.position()) {
//            ioInfo._status = DeviceStatus.InvalidBlockSize);
//        } else {
//            //  Clear UA flag - user is asking for the info pending for him.
//            setUnitAttention(false);
//
//            //  Complete the IOInfo object
//            ioInfo._transferredCount = (byteBuffer.position());
//            ioInfo._status = DeviceStatus.Successful);
//        }
    }

    /**
     * Handles the MoveBlock command
     */
    @Override
    protected void ioMoveBlock(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            int controlWord = readControlWord();
            if (controlWord == FILE_MARK_CONTROL_WORD) {
                ioInfo._status = DeviceStatus.FileMark;
            } else {
                //  We have a valid block, but we don't want to read it, just skip it, and the terminating control word.
                _file.seek(_file.getFilePointer() + controlWord + 4);
                readControlWord();
                ioInfo._transferredCount = (0);

                //  Are we past the end of tape?
                if (_file.getFilePointer() > MAX_FILE_SIZE) {
                    ioInfo._status = DeviceStatus.EndOfTape;
                } else {
                    ioInfo._status = DeviceStatus.Successful;
                }
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", _name));
            ioInfo._status = DeviceStatus.LostPosition;
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
            _loadPointFlag = false;
        }
    }

    /**
     * Handles the move block backward command
     */
    @Override
    protected void ioMoveBlockBackward(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_loadPointFlag) {
            ioInfo._status = DeviceStatus.EndOfTape;
            ioInfo._transferredCount = (0);
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            int controlWord = readPreceedingControlWord();
            if (controlWord == FILE_MARK_CONTROL_WORD) {
                if (_file.getFilePointer() == 0) {
                    ioInfo._status = DeviceStatus.EndOfTape;
                    _loadPointFlag = true;
                } else {
                    ioInfo._status = DeviceStatus.FileMark;
                }
            } else {
                _file.seek(_file.getFilePointer() - controlWord);
                readPreceedingControlWord();
                if (_file.getFilePointer() == 0) {
                    ioInfo._status = DeviceStatus.EndOfTape;
                    _loadPointFlag = true;
                } else {
                    ioInfo._status = DeviceStatus.Successful;
                }
            }

            ioInfo._transferredCount = (0);

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", _name));
            ioInfo._status = DeviceStatus.LostPosition;
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Handles the MoveFile command
     */
    @Override
    protected void ioMoveFile(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            _writeFlag = false;
            _writeMarkFlag = false;
            _loadPointFlag = false;

            //  Loop until we get a file mark, or lose position, or something
            int controlWord = readControlWord();
            while (controlWord != FILE_MARK_CONTROL_WORD) {
                //  Skip this block, and the following control word, then read the next control word.
                _file.seek(controlWord);
                readControlWord();
                controlWord = readControlWord();
            }

            ioInfo._transferredCount = (0);

            //  Are we past the end of tape?
            if (_file.getFilePointer() > MAX_FILE_SIZE) {
                ioInfo._status = DeviceStatus.EndOfTape;
            } else {
                ioInfo._status = DeviceStatus.Successful;
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", _name));
            ioInfo._status = DeviceStatus.LostPosition;
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Handles MoveFileBackward command
     */
    @Override
    protected void ioMoveFileBackward(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_loadPointFlag) {
            ioInfo._status = DeviceStatus.EndOfTape;
            ioInfo._transferredCount = (0);
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            //  Loop until we get a file mark, or lose position, or something
            int controlWord = readPreceedingControlWord();
            while (controlWord != FILE_MARK_CONTROL_WORD) {
                //  Skip this block, and the following control word, then read the next control word.
                _file.seek(_file.getFilePointer() - controlWord);
                readPreceedingControlWord();
                controlWord = readPreceedingControlWord();
            }

            ioInfo._transferredCount = (0);

            if (_file.getFilePointer() == 0) {
                ioInfo._status = DeviceStatus.EndOfTape;
                _loadPointFlag = true;
            } else {
                ioInfo._status = DeviceStatus.FileMark;
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", _name));
            ioInfo._status = DeviceStatus.LostPosition;
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Reads logical records from the underlying data store.
     */
    @Override
    protected void ioRead(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (ioInfo._byteBuffer.length < ioInfo._transferCount) {
            ioInfo._status = DeviceStatus.BufferTooSmall;
            ioInfo._source.signal();
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            //  Loop until we get a valid block, or lose position, or something
            int controlWord = readControlWord();
            if (controlWord == FILE_MARK_CONTROL_WORD) {
                ioInfo._status = DeviceStatus.FileMark;
            } else {
                //  We have a valid block to read.  Read it.
                int bytes = _file.read(ioInfo._byteBuffer, 0, ioInfo._transferCount);
                if (bytes != ioInfo._transferCount) {
                    ioInfo._status = DeviceStatus.LostPosition;
                } else {
                    //  Skip the terminating control word, and we're done.
                    readControlWord();
                    ioInfo._transferredCount = (bytes);

                    //  Are we past the end of tape?
                    if (_file.getFilePointer() > MAX_FILE_SIZE) {
                        ioInfo._status = DeviceStatus.EndOfTape;
                    } else {
                        ioInfo._status = DeviceStatus.Successful;
                    }
                }
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", _name));
            ioInfo._status = DeviceStatus.LostPosition;
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Handles (or rather, doesn't handle) the ReadBackward command
     */
    @Override
    protected void ioReadBackward(
        final DeviceIOInfo ioInfo
    ) {
        ioInfo._status = DeviceStatus.InvalidFunction;
    }

    /**
     * Resets the device - unmount any volume
     */
    @Override
    protected void ioReset(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
        } else {
            unmount();
            ioInfo._transferredCount = (0);
            ioInfo._status = DeviceStatus.Successful;
        }
    }

    /**
     * Handles the Rewind command
     */
    @Override
    protected void ioRewind(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        try {
            _file.seek(0);
            _endOfTapeFlag = false;
            _writeFlag = false;
            _writeMarkFlag = false;
            _loadPointFlag = true;

            ioInfo._transferredCount = (0);
            ioInfo._status = DeviceStatus.Successful;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Unmounts the currently-mounted media, leaving the device not-ready.
     */
    @Override
    protected void ioUnload(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
        } else {
            unmount();
            ioInfo._status = DeviceStatus.Successful;
        }

        ioInfo._transferredCount = (0);
    }

    /**
     * Writes a data block to the media
     */
    @Override
    protected void ioWrite(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (isWriteProtected()) {
            ioInfo._status = DeviceStatus.WriteProtected;
            return;
        }

        if (ioInfo._byteBuffer.length < ioInfo._transferCount) {
            ioInfo._status = DeviceStatus.BufferTooSmall;
            return;
        }

        if (ioInfo._transferCount > getMaxBlockSize()) {
            ioInfo._status = DeviceStatus.InvalidBlockSize;
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            _file.write(ioInfo._byteBuffer, 0, ioInfo._transferCount);

            _writeFlag = true;
            _writeMarkFlag = false;
            ioInfo._transferredCount = (ioInfo._transferCount);

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;

            if (_endOfTapeFlag) {
                ioInfo._status = DeviceStatus.EndOfTape;
            } else {
                ioInfo._status = DeviceStatus.Successful;
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Handles the WriteEndOfFile command
     */
    @Override
    protected void ioWriteEndOfFile(
        final DeviceIOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            ioInfo._source.signal();
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            ioInfo._source.signal();
            return;
        }

        if (isWriteProtected()) {
            ioInfo._status = DeviceStatus.WriteProtected;
            ioInfo._source.signal();
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;

        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
            bb.putInt(FILE_MARK_CONTROL_WORD);
            _file.write(bb.array());

            _writeFlag = true;
            _writeMarkFlag = true;
            ioInfo._transferredCount = (0);

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;

            if (_endOfTapeFlag) {
                ioInfo._status = DeviceStatus.EndOfTape;
            } else {
                ioInfo._status = DeviceStatus.Successful;
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo._status = DeviceStatus.SystemException;
        }
    }

    /**
     * Mounts the media for this device.
     * For a FileSystemTapeDevice, this entails opening a filesystem file.
     * @param mediaName full path and file name for the volume to be mounted
     * @return true if successful, else false
     */
    public boolean mount(
        final String mediaName
    ) {
        if (isMounted()) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Already mounted", _name, mediaName));
            return false;
        }

        try {
            //  Open the file
            _file = new RandomAccessFile(mediaName, "rw");

            //  Read and verify the scratch pad area
            int bufferSize = 128;
            byte[] buffer = new byte[bufferSize];
            _file.seek(0);
            int bytes = _file.read(buffer);
            if (bytes < bufferSize) {
                LOGGER.error(String.format("Device %s Cannot mount %s:Failed to read %d bytes of scratch pad",
                                           _name,
                                           mediaName,
                                           bufferSize));
                _file.close();
                _file = null;
                return false;
            }

            //  Things are good - set up our local state and we're done.
            _volumeName = mediaName;
            setIsMounted(true);
            _loadPointFlag = true;
            _writeFlag = false;
            _writeMarkFlag = false;
            LOGGER.info(String.format("Device %s Mount %s successful", _name, mediaName));
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Caught %s", _name, mediaName, ex.getMessage()));
            return false;
        }

        return true;
    }

    /**
     * Reads a 32-bit signed integer from the mounted file.
     * Does not update any state information.
     * @return control word
     * @throws IOException if we cannot read from the file
     * @throws OutOfDataException if we are apparently at the end of the file and cannot read a control word
     */
    int readControlWord(
    ) throws IOException,
             OutOfDataException {
        //  Read 4 bytes - if there aren't 4 bytes left, set lost position flag.
        //  If something worse happens, we propogate the exception to the caller,
        //  who may choose to catch it or propogate it, as appropriate.
        byte[] buffer = new byte[4];
        int bytes = _file.read(buffer);
        if (bytes != 4) {
            throw new OutOfDataException();
        }

        // Convert the bytes to the 32-bit control word value
        return ByteBuffer.wrap(buffer).getInt();
    }

    /**
     * Reads the previous control word, leaving the file pointer at the position of the control word.
     * @return control word
     * @throws IOException if we cannot read from the file
     * @throws OutOfDataException if we are apparently at the end of the file and cannot read a control word
     */
    int readPreceedingControlWord(
    ) throws IOException,
             OutOfDataException {
        _file.seek(_file.getFilePointer() - 4);
        int result = readControlWord();
        _file.seek(_file.getFilePointer() - 4);
        return result;
    }

    /**
     * Overrides the call to set the ready flag, preventing setting true if we're not mounted.
     */
    @Override
    public boolean setReady(
        final boolean readyFlag
    ) {
        if (readyFlag && !isMounted()) {
            return false;
        }

        return super.setReady(readyFlag);
    }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}

    /**
     * Unmounts the currently-mounted media
     * @return true if successful, else false
     */
    boolean unmount(
    ) {
        if (!isMounted()) {
            LOGGER.error(String.format("Device %s Cannot unmount pack - no pack mounted", _name));
            return false;
        }

        //  Clear ready flag to prevent any more IOs from coming in.
        setReady(false);

        // Release the host system file
        try {
            _file.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
            return false;
        }

        _file = null;
        _loadPointFlag = false;
        _volumeName = null;
        _writeFlag = false;
        _writeMarkFlag = false;

        setIsMounted(false);
        setIsWriteProtected(true);

        return true;
    }

    /**
     * Writes a control word to the underlying file
     * <p>
     * @param controlWord control word to be written
     * <p>
     * @throws IOException if we cannot write to the file
     */
    public void writeControlWord(
        final int controlWord
    ) throws IOException {
        //  Encode the value into an array of 4 bytes
        ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
        bb.putInt(controlWord);

        //  Write it to the file
        _file.write(bb.array());
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a host system file which can be mounted on this type of device as a volume
     * @param fileName (including full path name)
     */
    public static void createVolume(
        final String fileName
    ) throws FileNotFoundException,
             InternalError,
             IOException {
        //  Create system file to contain the pack image
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        //  Write two file marks, just because we can.
        ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.putInt(FILE_MARK_CONTROL_WORD);
        bb.putInt(FILE_MARK_CONTROL_WORD);

        //  Write the serialized stream to the underlying file and close it
        file.seek(0);
        file.write(bb.array());
        file.close();
    }
}
