/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import java.io.BufferedWriter;
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
 *
 * Currently IO is synchronous, which will hold up the channel module.
 * For this reason, it is recommended that tape devices be on a channel module apart
 * from anything else, or at least from all disk devices.
 */
@SuppressWarnings("Duplicates")
public class FileSystemTapeDevice extends TapeDevice {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class OutOfDataException extends Exception {}

    /**
     * This class contains information about the pack version and geometry.
     * It is maintained in the first SCRATCH_PAD_BUFFER_SIZE bytes of the file.
     * The conceptual load point is the next byte thereafter.
     */
    static class ScratchPad {

        static final String EXPECTED_IDENTIFIER = "**FSTD**";
        static final short EXPECTED_MAJOR_VERSION = 1;
        static final short EXPECTED_MINOR_VERSION = 1;

        String _identifier;
        short _majorVersion;
        short _minorVersion;
        int _fileSizeLimit;     //  when the file size reaches or exceeds this value in bytes, IOs fail

        /**
         * Constructor where the caller is establishing values
         */
        ScratchPad(
            final int fileSizeLimit
        ) {
            assert(fileSizeLimit >= MIN_FILE_SIZE);
            assert(fileSizeLimit <= MAX_FILE_SIZE);

            _identifier = EXPECTED_IDENTIFIER;
            _majorVersion = EXPECTED_MAJOR_VERSION;
            _minorVersion = EXPECTED_MINOR_VERSION;
            _fileSizeLimit = fileSizeLimit;
        }

        /**
         * Constructor to be used for subsequent deserializing
         */
        ScratchPad(
        ) {
        }

        /**
         * Deserializes a scratch pad object from the given Serializer
         */
        void deserialize(
            final ByteBuffer buffer
        ) {
            _identifier = deserializeString(buffer);
            _majorVersion = buffer.getShort();
            _minorVersion = buffer.getShort();
            _fileSizeLimit = buffer.getInt();
        }

        /**
         * Serializes this object into the given Serializer
         */
        void serialize(
            final ByteBuffer buffer
        ) {
            serializeString(buffer, _identifier);
            buffer.putShort(_majorVersion);
            buffer.putShort(_minorVersion);
            buffer.putInt(_fileSizeLimit);
        }
    };

    private static final int SCRATCH_PAD_BUFFER_SIZE = 32;  //  arbitrary, and larger than necessary by design


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final int FILE_MARK_CONTROL_WORD = 0xffffffff;
    private static final int MAX_BLOCK_SIZE = 128 * 1024;           //  128KBytes
    private static final int MAX_FILE_SIZE = 175 * 1024 * 1024;     //  175MBytes
    private static final int MIN_FILE_SIZE = 40 * 1024 * 1024;      //  40MBytes
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
     * Indicates that we don't actually know where we are
     */
    private boolean _lostPositionFlag;

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

    /**
     * Size of smallest block we can read or write
     */
    private int _noiseConstant;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     */
    FileSystemTapeDevice(
        final String name
    ) {
        super(DeviceModel.FileSystemTape, name);
        _endOfTapeFlag = false;
        _loadPointFlag = false;
        _lostPositionFlag = false;
        _writeFlag = false;
        _writeMarkFlag = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads a 32-bit signed integer from the mounted file.
     * Does not update any state information.
     * @return control word
     * @throws IOException if we cannot read from the file
     * @throws OutOfDataException if we are apparently at the end of the file and cannot read a control word
     */
    private int readControlWord(
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
    private int readPreceedingControlWord(
    ) throws IOException,
             OutOfDataException {
        _file.seek(_file.getFilePointer() - 4);
        int result = readControlWord();
        _file.seek(_file.getFilePointer() - 4);
        return result;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Non-private methods
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
            writer.write(String.format("  End of Tape:     %s\n", String.valueOf(_endOfTapeFlag)));
            writer.write(String.format("  Load Point:      %s\n", String.valueOf(_loadPointFlag)));
            writer.write(String.format("  Lost Position:   %s\n", String.valueOf(_lostPositionFlag)));
            writer.write(String.format("  Write Flag:      %s\n", String.valueOf(_writeFlag)));
            writer.write(String.format("  Write Mark Flag: %s\n", String.valueOf(_writeMarkFlag)));
            writer.write(String.format("  Noise factor:    %d\n", _noiseConstant));
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
        ++_miscCount;

        ArraySlice as = getInfo();
        ioInfo._byteBuffer = new byte[128];
        as.pack(ioInfo._byteBuffer);
        ioInfo._status = DeviceStatus.Successful;
        _unitAttentionFlag = false;
        ioInfo._source.signal();
    }

    /**
     * Handles the MoveBlock command
     */
    @Override
    protected void ioMoveBlock(
        final DeviceIOInfo ioInfo
    ) {
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

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
                ioInfo._transferredCount = 0;

                //  Are we past the end of tape?
                if (_file.getFilePointer() > MAX_FILE_SIZE) {
                    ioInfo._status = DeviceStatus.EndOfTape;
                } else {
                    ioInfo._status = DeviceStatus.Successful;
                }
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;
        } catch (OutOfDataException ex) {
            _lostPositionFlag = true;
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
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

        if (_loadPointFlag) {
            ioInfo._status = DeviceStatus.EndOfTape;
            return;
        }

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
            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;
        } catch (OutOfDataException ex) {
            _lostPositionFlag = true;
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
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

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
            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;
        } catch (OutOfDataException ex) {
            _lostPositionFlag = true;
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
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

        if (_loadPointFlag) {
            ioInfo._status = DeviceStatus.EndOfTape;
            return;
        }

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
            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;
        } catch (OutOfDataException ex) {
            _lostPositionFlag = true;
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
        ++_readCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

        try {
            _writeFlag = false;
            _writeMarkFlag = false;

            //  Loop until we get a valid block, or lose position, or something
            boolean done = false;
            while (!done) {
                int controlWord = readControlWord();
                if (controlWord == FILE_MARK_CONTROL_WORD) {
                    ioInfo._status = DeviceStatus.FileMark;
                } else if (controlWord < _noiseConstant) {
                    //  Ignore a block which is too small - that is, skip the block and the terminating control word.
                    _file.seek(_file.getFilePointer() + controlWord);
                    readControlWord();
                } else {
                    //  We have a valid block to read.  Read it.
                    ioInfo._byteBuffer = new byte[ioInfo._transferCount];
                    int bytes = _file.read(ioInfo._byteBuffer, 0, controlWord);
                    if (bytes != controlWord) {
                        _lostPositionFlag = true;
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
                    done = true;
                }
            }

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;
        } catch (OutOfDataException ex) {
            _lostPositionFlag = true;
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
        //TODO implement this
        ++_readCount;
        ioInfo._status = DeviceStatus.InvalidFunction;
    }

    /**
     * Resets the device - unmount any volume
     */
    @Override
    protected void ioReset(
        final DeviceIOInfo ioInfo
    ) {
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
        } else {
            unmount();
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
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        try {
            _file.seek(SCRATCH_PAD_BUFFER_SIZE);
            _endOfTapeFlag = false;
            _writeFlag = false;
            _writeMarkFlag = false;
            _loadPointFlag = true;
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
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
        } else {
            unmount();
            ioInfo._status = DeviceStatus.Successful;
        }
    }

    /**
     * Writes a data block to the media
     */
    @Override
    protected void ioWrite(
        final DeviceIOInfo ioInfo
    ) {
        ++_writeCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            return;
        }

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

        if (isWriteProtected()) {
            ioInfo._status = DeviceStatus.WriteProtected;
            return;
        }

        if ((ioInfo._byteBuffer.length < ioInfo._transferCount)
            || (ioInfo._byteBuffer.length < _noiseConstant)) {
            ioInfo._status = DeviceStatus.BufferTooSmall;
            return;
        }

        if (ioInfo._transferCount > getMaxBlockSize()) {
            ioInfo._status = DeviceStatus.InvalidBlockSize;
            return;
        }

        try {
            _file.write(ioInfo._byteBuffer, 0, ioInfo._transferCount);

            _writeFlag = true;
            _writeMarkFlag = false;
            ioInfo._transferredCount = (ioInfo._transferCount);

            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;

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
        ++_writeCount;

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

        if (_lostPositionFlag) {
            ioInfo._status = DeviceStatus.LostPosition;
            return;
        }

        if (isWriteProtected()) {
            ioInfo._status = DeviceStatus.WriteProtected;
            ioInfo._source.signal();
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
            bb.putInt(FILE_MARK_CONTROL_WORD);
            _file.write(bb.array());

            _writeFlag = true;
            _writeMarkFlag = true;
            ioInfo._transferredCount = (0);

            _endOfTapeFlag = _file.getFilePointer() >= MAX_FILE_SIZE;
            _loadPointFlag = _file.getFilePointer() == SCRATCH_PAD_BUFFER_SIZE;

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
            byte[] buffer = new byte[SCRATCH_PAD_BUFFER_SIZE];
            _file.seek(0);
            int bytes = _file.read(buffer);
            if (bytes != SCRATCH_PAD_BUFFER_SIZE) {
                LOGGER.error(String.format("Device %s Cannot mount %s:Failed to read %d bytes of scratch pad",
                                           _name,
                                           mediaName,
                                           SCRATCH_PAD_BUFFER_SIZE));
                _file.close();
                _file = null;
                return false;
            }

            ScratchPad scratchPad = new ScratchPad();
            scratchPad.deserialize(ByteBuffer.wrap(buffer));

            if (!scratchPad._identifier.equals(ScratchPad.EXPECTED_IDENTIFIER)) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad identifier expected:'%s' got:'%s'",
                                           _name,
                                           mediaName,
                                           ScratchPad.EXPECTED_IDENTIFIER,
                                           scratchPad._identifier));
                _file.close();
                _file = null;
                return false;
            }

            if (scratchPad._majorVersion != ScratchPad.EXPECTED_MAJOR_VERSION) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad MajorVersion expected:%d got:%d",
                                           _name,
                                           mediaName,
                                           ScratchPad.EXPECTED_MAJOR_VERSION,
                                           scratchPad._majorVersion));
                _file.close();
                _file = null;
                return false;
            }

            if (scratchPad._minorVersion != ScratchPad.EXPECTED_MINOR_VERSION) {
                LOGGER.info(String.format("Device %s:ScratchPad MinorVersion expected:%d got:%d",
                                          _name,
                                          ScratchPad.EXPECTED_MINOR_VERSION,
                                          scratchPad._minorVersion));
            }

            //  Things are good - set up our local state and we're done.
            _volumeName = mediaName;
            setIsMounted(true);
            _loadPointFlag = true;
            _lostPositionFlag = false;
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
        _lostPositionFlag = false;
        _volumeName = null;
        _writeFlag = false;
        _writeMarkFlag = false;

        setIsMounted(false);
        setIsWriteProtected(true);
        return true;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a host system file which can be mounted on this type of device as a volume
     * @param fileName (including full path name)
     * @param maxFileSize max file size in bytes, for this volume
     */
    public static void createVolume(
        final String fileName,
        final int maxFileSize
    ) throws InternalError,
             IOException {
        //  Create system file to contain the pack image
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        //  Write scratch pad
        ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        ScratchPad sp = new ScratchPad(maxFileSize);
        sp.serialize(bb);

        //  Write two file marks, just because we can.
        bb.putInt(FILE_MARK_CONTROL_WORD);
        bb.putInt(FILE_MARK_CONTROL_WORD);

        //  Write the serialized stream to the underlying file and close it
        file.seek(0);
        file.write(bb.array());
        file.close();
    }

    /**
     * Creates a host system file which can be mounted on this type of device as a volume
     * @param fileName (including full path name)
     */
    public static void createVolume(
        final String fileName
    ) throws InternalError,
             IOException {
        createVolume(fileName, MAX_FILE_SIZE);
    }
}
