/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Abstract class descibing the common attributes of a DiskDevice
 */
@SuppressWarnings("Duplicates")
public abstract class DiskDevice extends Device {

    private static final Logger LOGGER = LogManager.getLogger(DiskDevice.class);

    /**
     * Number of blocks on the mounted pack
     */
    Long _blockCount = null;

    /**
     * block size (in bytes) for the blocks on the mounted pack
     */
    Integer _blockSize = null;

    /**
     * indicates whether a pack is mounted on the device
     */
    boolean _isMounted = false;

    /**
     * indicates whether the device is write-protected
     */
    boolean _isWriteProtected = false;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    DiskDevice(
        final DeviceModel deviceModel,
        final String name
    ) {
        super(DeviceType.Disk, deviceModel, name);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public abstract boolean hasByteInterface();

    @Override
    public abstract boolean hasWordInterface();

    @Override
    public abstract void initialize();

    abstract void ioGetInfo(DeviceIOInfo ioInfo);
    abstract void ioRead(DeviceIOInfo ioInfo);
    abstract void ioReset(DeviceIOInfo ioInfo);
    abstract void ioUnload(DeviceIOInfo ioInfo);
    abstract void ioWrite(DeviceIOInfo ioInfo);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Block Size:      %s\n", String.valueOf(_blockSize)));
            writer.write(String.format("  Block Count:     %s\n", String.valueOf(_blockCount)));
            writer.write(String.format("  Mounted:         %s\n", String.valueOf(_isMounted)));
            writer.write(String.format("  Write Protected: %s\n", String.valueOf(_isWriteProtected)));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Produces an array slice which represents, in 36-bit mode, the device info data which is presented to the requestor.
     * The format of the info block is:
     *      +----------+----------+----------+----------+----------+----------+
     *  +0  |  FLAGS   |  MODEL   |   TYPE   |             zeroes             |
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
     *  +6  |             zeroes             |          PREP_FACTOR           |
     *      +----------+----------+----------+----------+----------+----------+
     *  +7  |                           BLOCK_COUNT                           |
     *      +----------+----------+----------+----------+----------+----------+
     *  +8  |                                                                 |
     *  ..  |                             zeroes                              |
     * +27  |                                                                 |
     *      +----------+----------+----------+----------+----------+----------+
     *
     * FLAGS:
     *      Bit 0:  device_ready
     *      Bit 3:  mounted
     *      Bit 4:  write_protected
     * MODEL:       integer code for the DeviceModel
     * TYPE:        integer code for the DeviceType
     * PREP_FACTOR: indicates the block size in words, of a disk block
     * BLOCK_COUNT: number of blocks on the media
     */
    protected ArraySlice getInfo() {
        ArraySlice as = super.getInfo();
        long[] buffer = as._array;

        long flags = (long)((_isMounted ? 04 : 0) | (_isWriteProtected ? 02 : 0)) << 30;
        buffer[0] |= flags;

        buffer[6] = PrepFactor.getPrepFactorFromBlockSize(_blockSize);
        buffer[7] = _blockCount;
        return as;
    }

    /**
     * Calls the appropriate local or virtual function based on the information in the IOInfo object.
     * @return true if we scheduled the IO, false if we completed it immediately
     */
    @Override
    public boolean handleIo(
        final DeviceIOInfo ioInfo
    ) {
        ioInfo._transferredCount = 0;
        ioInfo._status = DeviceStatus.InProgress;
        synchronized(this) {
            ioStart(ioInfo);
            switch (ioInfo._ioFunction) {
                case None:
                    ++_miscCount;
                    ioInfo._status = DeviceStatus.Successful;
                    ioEnd(ioInfo);
                    return false;

                case GetInfo:
                    ioGetInfo(ioInfo);
                    break;

                case Read:
                    ioRead(ioInfo);
                    break;

                case Reset:
                    ioReset(ioInfo);
                    break;

                case Unload:
                    ioUnload(ioInfo);
                    break;

                case Write:
                    ioWrite(ioInfo);
                    break;

                default:
                    ioInfo._status = DeviceStatus.InvalidFunction;
                    ioEnd(ioInfo);
                    return false;
            }

            return true;
        }
    }

    /**
     * Checks whether the mounted pack is prepped (block size is non-null).  This is hardware-prepped... it does not imply
     * an MFD track or anything like that.  However, if the device actually knows its block size, then it is highly likely
     * that the pack *is* prepped - after all, it cannot have a block size until we prep it with a prep factor.
     * We don't check to see if the pack is mounted - if it isn't, then block size is zero anyway, and we still return false
     */
    boolean isPrepped() { return (_blockSize != null) && (_blockSize != 0); }

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
            if (flag && !_isMounted) {
                return false;
            }

            return super.setReady(flag);
        }
    }

    /**
     * Sets or clears the write protected flag.  Will not set the flag if a pack is not mounted.
     * <p>
     * @param flag true to set write protect, false to clear
     * <p>
     * @return true if state change was successful, else false
     */
    boolean setIsWriteProtected(
        final boolean flag
    ) {
        synchronized(this) {
            if (!flag && !_isMounted) {
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
        final DeviceIOInfo ioInfo
    ) {
        if (ioInfo._byteBuffer != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo._byteBuffer);
        } else if (ioInfo._wordBuffer != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo._wordBuffer);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks to see if a given disk pack block size is valid
     */
    static boolean isValidBlockSize(
        final int blockSize
    ) {
        switch (blockSize) {
            case 128:
            case 256:
            case 512:
            case 1024:
            case 2048:
            case 4096:
            case 8192:
                return true;
        }

        return false;
    }
}
