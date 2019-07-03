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

public class FileSystemTapeDevice extends TapeDevice {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class OutOfDataException extends Exception {
    }


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
     * @param candidateAncestor
     * @return
     */
    @Override
    public boolean canConnect(
        final Node candidateAncestor
    ) {
        //  We can connect only to ByteDisk controllers
        return candidateAncestor instanceof TapeController;
    }

    /**
     * For debugging purposes
     * <p>
     * @param writer
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Volume Name:     %s\n", getVolumeName()));
            //???? many other things to write
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getMaxBlockSize(
    ) {
        return MAX_BLOCK_SIZE;
    }

    /**
     * We are a byte interface device
     * <p>
     * @return
     */
    @Override
    public boolean hasByteInterface(
    ) {
        return true;
    }

    /**
     * We are NOT a word interface device
     * <p>
     * @return
     */
    @Override
    public boolean hasWordInterface(
    ) {
        return false;
    }

    /**
     * Nothing to be done here
     */
    @Override
    public void initialize() {}

    /**
     * Produces a byte stream describing this disk device.
     * This is an immediate IO, no waiting.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioGetInfo(
        final IOInfo ioInfo
    ) {
        //  Create a TapeDeviceInfo object and then serialize it to the IOInfo's buffer
        TapeDeviceInfo devInfo = new TapeDeviceInfo(getDeviceType(),
                                                    getDeviceModel(),
                                                    isReady(),
                                                    getSubsystemIdentifier(),
                                                    getUnitAttentionFlag(),
                                                    isMounted(),
                                                    isWriteProtected(),
                                                    getEndOfTapeFlag(),
                                                    getLoadPointFlag());

        ByteBuffer byteBuffer = ByteBuffer.wrap(ioInfo.getByteBuffer());
        devInfo.serialize(byteBuffer);
        if (ioInfo.getTransferCount() < byteBuffer.position()) {
            ioInfo.setStatus(IOStatus.InvalidBlockSize);
        } else {
            //  Clear UA flag - user is asking for the info pending for him.
            setUnitAttention(false);

            //  Complete the IOInfo object
            ioInfo.setTransferredCount(byteBuffer.position());
            ioInfo.setStatus(IOStatus.Successful);
        }
    }

    /**
     * Handles the MoveBlock command
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioMoveBlock(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            int controlWord = readControlWord();
            if (controlWord == FILE_MARK_CONTROL_WORD) {
                ioInfo.setStatus(IOStatus.FileMark);
            } else {
                //  We have a valid block, but we don't want to read it, just skip it, and the terminating control word.
                _file.seek(_file.getFilePointer() + controlWord + 4);
                readControlWord();
                ioInfo.setTransferredCount(0);

                //  Are we past the end of tape?
                if (_file.getFilePointer() > MAX_FILE_SIZE) {
                    ioInfo.setStatus(IOStatus.EndOfTape);
                } else {
                    ioInfo.setStatus(IOStatus.Successful);
                }
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", getName()));
            ioInfo.setStatus(IOStatus.LostPosition);
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
            _loadPointFlag = false;
        }
    }

    /**
     * Handles the move block backward command
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioMoveBlockBackward(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        if (_loadPointFlag) {
            ioInfo.setStatus(IOStatus.EndOfTape);
            ioInfo.setTransferredCount(0);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            int controlWord = readPreceedingControlWord();
            if (controlWord == FILE_MARK_CONTROL_WORD) {
                if (_file.getFilePointer() == 0) {
                    ioInfo.setStatus(IOStatus.EndOfTape);
                    _loadPointFlag = true;
                } else {
                    ioInfo.setStatus(IOStatus.FileMark);
                }
            } else {
                _file.seek(_file.getFilePointer() - controlWord);
                readPreceedingControlWord();
                if (_file.getFilePointer() == 0) {
                    ioInfo.setStatus(IOStatus.EndOfTape);
                    _loadPointFlag = true;
                } else {
                    ioInfo.setStatus(IOStatus.Successful);
                }
            }

            ioInfo.setTransferredCount(0);

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", getName()));
            ioInfo.setStatus(IOStatus.LostPosition);
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Handles the MoveFile command
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioMoveFile(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

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

            ioInfo.setTransferredCount(0);

            //  Are we past the end of tape?
            if (_file.getFilePointer() > MAX_FILE_SIZE) {
                ioInfo.setStatus(IOStatus.EndOfTape);
            } else {
                ioInfo.setStatus(IOStatus.Successful);
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", getName()));
            ioInfo.setStatus(IOStatus.LostPosition);
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    @Override
    protected void ioMoveFileBackward(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        if (_loadPointFlag) {
            ioInfo.setStatus(IOStatus.EndOfTape);
            ioInfo.setTransferredCount(0);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

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

            ioInfo.setTransferredCount(0);

            if (_file.getFilePointer() == 0) {
                ioInfo.setStatus(IOStatus.EndOfTape);
                _loadPointFlag = true;
            } else {
                ioInfo.setStatus(IOStatus.FileMark);
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", getName()));
            ioInfo.setStatus(IOStatus.LostPosition);
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Reads logical records from the underlying data store.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioRead(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        if (ioInfo.getByteBuffer().length < ioInfo.getTransferCount()) {
            ioInfo.setStatus(IOStatus.BufferTooSmall);
            if (ioInfo.getSource() != null) {
                ioInfo.getSource().signal(this);
            }
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            //  Loop until we get a valid block, or lose position, or something
            int controlWord = readControlWord();
            if (controlWord == FILE_MARK_CONTROL_WORD) {
                ioInfo.setStatus(IOStatus.FileMark);
            } else {
                //  We have a valid block to read.  Read it.
                int bytes = _file.read(ioInfo.getByteBuffer(), 0, ioInfo.getTransferCount());
                if (bytes != ioInfo.getTransferCount()) {
                    ioInfo.setStatus(IOStatus.LostPosition);
                } else {
                    //  Skip the terminating control word, and we're done.
                    readControlWord();
                    ioInfo.setTransferredCount(bytes);

                    //  Are we past the end of tape?
                    if (_file.getFilePointer() > MAX_FILE_SIZE) {
                        ioInfo.setStatus(IOStatus.EndOfTape);
                    } else {
                        ioInfo.setStatus(IOStatus.Successful);
                    }
                }
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;
        } catch (OutOfDataException ex) {
            LOGGER.error(String.format("Device %s Out of data on read", getName()));
            ioInfo.setStatus(IOStatus.LostPosition);
            _loadPointFlag = false;
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Handles (or rather, doesn't handle) the ReadBackward command
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioReadBackward(
        final IOInfo ioInfo
    ) {
        ioInfo.setStatus(IOStatus.InvalidFunction);
    }

    /**
     * Resets the device - unmount any volume
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioReset(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
        } else {
            unmount();
            ioInfo.setTransferredCount(0);
            ioInfo.setStatus(IOStatus.Successful);
        }
    }

    /**
     * Handles the Rewind command
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioRewind(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        try {
            _file.seek(0);
            _endOfTapeFlag = false;
            _writeFlag = false;
            _writeMarkFlag = false;
            _loadPointFlag = true;

            ioInfo.setTransferredCount(0);
            ioInfo.setStatus(IOStatus.Successful);
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Unmounts the currently-mounted media, leaving the device not-ready.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioUnload(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
        } else {
            unmount();
            ioInfo.setStatus(IOStatus.Successful);
        }

        ioInfo.setTransferredCount(0);
    }

    /**
     * Writes a data block to the media
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioWrite(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        if (isWriteProtected()) {
            ioInfo.setStatus(IOStatus.WriteProtected);
            return;
        }

        if (ioInfo.getByteBuffer().length < ioInfo.getTransferCount()) {
            ioInfo.setStatus(IOStatus.BufferTooSmall);
            return;
        }

        if (ioInfo.getTransferCount() > getMaxBlockSize()) {
            ioInfo.setStatus(IOStatus.InvalidBlockSize);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

        try {
            _file.write(ioInfo.getByteBuffer(), 0, (int)ioInfo.getTransferCount());

            _writeFlag = true;
            _writeMarkFlag = false;
            ioInfo.setTransferredCount(ioInfo.getTransferCount());

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;

            if (_endOfTapeFlag) {
                ioInfo.setStatus(IOStatus.EndOfTape);
            } else {
                ioInfo.setStatus(IOStatus.Successful);
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Handles the WriteEndOfFile command
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioWriteEndOfFile(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            if (ioInfo.getSource() != null) {
                ioInfo.getSource().signal(this);
            }
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            if (ioInfo.getSource() != null) {
                ioInfo.getSource().signal(this);
            }
            return;
        }

        if (isWriteProtected()) {
            ioInfo.setStatus(IOStatus.WriteProtected);
            if (ioInfo.getSource() != null) {
                ioInfo.getSource().signal(this);
            }
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);

        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
            bb.putInt(FILE_MARK_CONTROL_WORD);
            _file.write(bb.array());

            _writeFlag = true;
            _writeMarkFlag = true;
            ioInfo.setTransferredCount(0);

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == 0;

            if (_endOfTapeFlag) {
                ioInfo.setStatus(IOStatus.EndOfTape);
            } else {
                ioInfo.setStatus(IOStatus.Successful);
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Mounts the media for this device.
     * For a FileSystemTapeDevice, this entails opening a filesystem file.
     * <p>
     * @param mediaName full path and file name for the volume to be mounted
     * <p>
     * @return true if successful, else false
     */
    public boolean mount(
        final String mediaName
    ) {
        if (isMounted()) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Already mounted", getName(), mediaName));
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
                                           getName(),
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
            LOGGER.info(String.format("Device %s Mount %s successful", getName(), mediaName));
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Caught %s", getName(), mediaName, ex.getMessage()));
            return false;
        }

        return true;
    }

    /**
     * Reads a 32-bit signed integer from the mounted file.
     * Does not update any state information.
     * <p>
     * @return control word
     * <p>
     * @throws IOException if we cannot read from the file
     * @throws OutOfDataException if we are apparently at the end of the file and cannot read a control word
     */
    public int readControlWord(
    ) throws IOException,
             OutOfDataException {
        //  Read 4 bytes - if there aren't 4 bytes left, set lost position flag.
        //  If something worse happens, we propogate the exception to the caller,
        //  who may choose to catch it or propogate it, as appropriate.
        byte buffer[] = new byte[4];
        int bytes = _file.read(buffer);
        if (bytes != 4) {
            throw new OutOfDataException();
        }

        // Convert the bytes to the 32-bit control word value
        return ByteBuffer.wrap(buffer).getInt();
    }

    /**
     * Reads the previous control word, leaving the file pointer at the position of the control word.
     * <p>
     * @return control word
     * <p>
     * @throws IOException if we cannot read from the file
     * @throws OutOfDataException if we are apparently at the end of the file and cannot read a control word
     */
    public int readPreceedingControlWord(
    ) throws IOException,
             OutOfDataException {
        _file.seek(_file.getFilePointer() - 4);
        int result = readControlWord();
        _file.seek(_file.getFilePointer() - 4);
        return result;
    }

    /**
     * Overrides the call to set the ready flag, preventing setting true if we're not mounted.
     * <p>
     * @param readyFlag
     * <p>
     * @return
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
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
     * @param source the Node which is signalling us
     */
    @Override
    public void signal(
        final Node source
    ) {
        //  nothing to do
    }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}

    /**
     * Unmounts the currently-mounted media
     * <p>
     * @return true if successful, else false
     */
    public boolean unmount(
    ) {
        if (!isMounted()) {
            LOGGER.error(String.format("Device %s Cannot unmount pack - no pack mounted", getName()));
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
     * <p>
     * @param fileName (including full path name)
     * @param maxFileSizeBytes (0 to default to device limit)
     * <p>
     * @throws FileNotFoundException
     * @throws InternalError
     * @throws InvalidBlockSizeException
     * @throws InvalidTrackCountException
     * @throws IOException
     */
    public static void createVolume(
        final String fileName,
        final int maxFileSizeBytes
    ) throws FileNotFoundException,
             InternalError,
             InvalidBlockSizeException,
             InvalidTrackCountException,
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
