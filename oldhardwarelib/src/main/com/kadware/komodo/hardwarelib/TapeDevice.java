/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Abstract class descibing the common attributes of a tape device
 */
public abstract class TapeDevice extends Device {

    private static final Logger LOGGER = LogManager.getLogger(TapeDevice.class);

    /**
     * indicates whether a pack is mounted on the device
     */
    protected boolean _isMounted = false;

    /**
     * indicates whether the device is write-protected
     */
    protected boolean _isWriteProtected = true;

    /**
     * Indicates we have reached or exceeded max file size
     */
    protected boolean _endOfTapeFlag = false;

    /**
     * Indicates the volume is positioned at load point
     */
    protected boolean _loadPointFlag = false;

    /**
     * Indicates that we don't actually know where we are
     */
    protected boolean _lostPositionFlag = false;

    /**
     * Last operation was a write
     */
    protected boolean _writeFlag = false;

    /**
     * Last operation was a write mark
     */
    protected boolean _writeMarkFlag = false;

    /**
     * Size of smallest block we can read or write
     */
    protected int _noiseConstant = 63;

    /**
     * Number of blocks / files since last file mark, load, or rewind
     */
    protected int _blocksExtended = 0;
    protected int _filesExtended = 0;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected TapeDevice(
        final NodeModel deviceNodeModel,
        final String name
    ) {
        super(NodeType.Tape, deviceNodeModel, name);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    boolean isMounted() { return _isMounted; }
    boolean isWriteProtected() { return _isWriteProtected; }
    void setIsMounted(boolean flag) { _isMounted = flag; }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    public abstract int getMaxBlockSize();

    @Override
    public abstract boolean hasByteInterface();

    @Override
    public abstract boolean hasWordInterface();

    @Override
    public abstract void initialize();

    abstract void ioGetInfo(final IOInfo ioInfo);
    abstract void ioMoveBlock(final IOInfo ioInfo);
    abstract void ioMoveBlockBackward(final IOInfo ioInfo);
    abstract void ioMoveFile(final IOInfo ioInfo);
    abstract void ioMoveFileBackward(final IOInfo ioInfo);
    abstract void ioRead(final IOInfo ioInfo);
    abstract void ioReadBackward(final IOInfo ioInfo);
    abstract void ioReset(final IOInfo ioInfo);
    abstract void ioRewind(final IOInfo ioInfo);
    abstract void ioSetMode(final IOInfo ioInfo);
    abstract void ioUnload(final IOInfo ioInfo);
    abstract void ioWrite(final IOInfo ioInfo);
    abstract void ioWriteEndOfFile(final IOInfo ioInfo);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Mounted:         %s\n", String.valueOf(_isMounted)));
            writer.write(String.format("  Write Protected: %s\n", String.valueOf(_isWriteProtected)));
            writer.write(String.format("  End of Tape:     %s\n", String.valueOf(_endOfTapeFlag)));
            writer.write(String.format("  Load Point:      %s\n", String.valueOf(_loadPointFlag)));
            writer.write(String.format("  Lost Position:   %s\n", String.valueOf(_lostPositionFlag)));
            writer.write(String.format("  Write Flag:      %s\n", String.valueOf(_writeFlag)));
            writer.write(String.format("  Write Mark Flag: %s\n", String.valueOf(_writeMarkFlag)));
            writer.write(String.format("  Noise factor:    %d\n", _noiseConstant));
            writer.write(String.format("  Blocks Extended: %d\n", _blocksExtended));
            writer.write(String.format("  Files Extended:  %d\n", _filesExtended));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Produces an array slice which represents, in 36-bit mode, the device info data which is presented to the requestor.
     * The format of the info block is:
     *      +----------+----------+----------+----------+----------+----------+
     *  +0  |  FLAGS   |  MODEL   |   TYPE   |          |  NOISE   |  FLAGS2  |
     *      +----------+----------+----------+----------+----------+----------+
     *  +1  |                          MISC_IO_COUNT                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +2  |                          READ_IO_COUNT                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +3  |                          WRITE_IO_COUNT                         |
     *      +----------+----------+----------+----------+----------+----------+
     *  +4  |                          READ_IO_BYTES                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +5  |                          WRITE_IO_BYTES                         |
     *      +----------+----------+----------+----------+----------+----------+
     *  +6  |        BLOCKS_EXTENDED              FILES_EXTENDED              |
     *      +----------+----------+----------+----------+----------+----------+
     *  +7  |                                                                 |
     *  ..  |                             zeroes                              |
     * +27  |                                                                 |
     *      +----------+----------+----------+----------+----------+----------+
     *
     * FLAGS:
     *      Bit 0:  device_ready
     *      Bit 3:  mounted
     *      Bit 4:  write_protected
     * MODEL:       integer code for the Model
     * TYPE:        integer code for the ProcessorType
     * NOISE:       current noise constant
     * FLAGS2:
     *      Bit 30: load point
     *      Bit 31: end of tape
     *      Bit 32: lost position
     *      Bit 33: write flag
     * BLOCKS_EXTENDED: number of blocks read since load, rewind, or most recent file mark
     * FILES_EXTENDED:  number of files read since load or rewind
     */
    protected ArraySlice getInfo() {
        ArraySlice as = super.getInfo();
        long[] buffer = as._array;

        long flags = (long)((_isMounted ? 04 : 0) | (_isWriteProtected ? 02 : 0)) << 30;
        buffer[0] |= flags;
        buffer[0] |= (_noiseConstant << 6);

        int flags2 = (_loadPointFlag ? 040 : 0)
                     | (_endOfTapeFlag ? 020 : 0)
                     | (_lostPositionFlag ? 010 : 0)
                     | (_writeFlag || _writeMarkFlag ? 04 : 0);
        buffer[0] |= flags2;
        buffer[6] = ((long) (_blocksExtended & 0777777)) << 18 | (_filesExtended & 0777777);

        return as;
    }

    /**
     * Calls the appropriate local or virtual function based on the information in the IOInfo object
     * @return true if we scheduled the IO, false if we completed it immediately
     */
    @Override
    public boolean handleIo(
        final IOInfo ioInfo
    ) {
        ioInfo._transferredCount = 0;
        ioInfo._status = IOStatus.InProgress;
        synchronized(this) {
            ioStart(ioInfo);
            switch (ioInfo._ioFunction) {
                case None:
                    ioInfo._status = IOStatus.Successful;
                    ioEnd(ioInfo);
                    return false;

                case GetInfo:
                    ioGetInfo(ioInfo);
                    break;

                case MoveBlock:
                    ioMoveBlock(ioInfo);
                    break;

                case MoveBlockBackward:
                    ioMoveBlockBackward(ioInfo);
                    break;

                case MoveFile:
                    ioMoveFile(ioInfo);
                    break;

                case MoveFileBackward:
                    ioMoveFileBackward(ioInfo);
                    break;

                case Read:
                    ioRead(ioInfo);
                    break;

                case ReadBackward:
                    ioReadBackward(ioInfo);
                    break;

                case Reset:
                    ioReset(ioInfo);
                    break;

                case Rewind:
                    ioRewind(ioInfo);
                    break;

                case SetMode:
                    ioSetMode(ioInfo);
                    break;

                case Unload:
                    ioUnload(ioInfo);
                    break;

                case Write:
                    ioWrite(ioInfo);
                    break;

                case WriteEndOfFile:
                    ioWriteEndOfFile(ioInfo);
                    break;

                default:
                    ioInfo._status = IOStatus.InvalidFunction;
                    return false;
            }

            return true;
        }
    }

    /**
     * Makes sure the requested state change is allowable, then calls the superclass to effect said change.
     * We do not allow the device to be set ready if a pack is not mounted.
     * @param flag true to set ready, false to set not-ready
     * @return true if state change was successful, else false
     */
    @Override
    boolean setReady(
        final boolean flag
    ) {
        synchronized(this) {
            if (flag && !isMounted()) {
                return false;
            }

            return super.setReady(flag);
        }
    }

    /**
     * Sets or clears the write protected flag.  Will not set the flag if a pack is not mounted.
     * @param flag true to set write protect, false to clear
     * @return true if state change was successful, else false
     */
    boolean setIsWriteProtected(
        final boolean flag
    ) {
        boolean result = false;
        synchronized(this) {
            if (!flag && !isMounted()) {
                return false;
            }

            _isWriteProtected = flag;
            LOGGER.info(String.format("Device %s WriteProtect %s", _name, flag ? "Set" : "Cleared"));
        }

        return true;
    }

    /**
     * Writes the buffers associated with an IOInfo object, to the log.
     * This should only be done during development for debug and verification.
     */
    @Override
    protected void writeBuffersToLog(
        final IOInfo ioInfo
    ) {
        if (ioInfo._byteBuffer != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo._byteBuffer);
        } else if (ioInfo._wordBuffer != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo._wordBuffer);
        }
    }
}
