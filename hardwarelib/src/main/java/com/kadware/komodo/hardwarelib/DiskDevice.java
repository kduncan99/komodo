/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.kadware.komodo.baselib.types.*;

/**
 * Abstract class descibing the common attributes of a DiskDevice
 */
public abstract class DiskDevice extends Device {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class DiskDeviceInfo extends DeviceInfo {

        private final BlockCount _blockCount;
        private final BlockSize _blockSize;
        private boolean _isMounted;
        private boolean _isWriteProtected;

        /**
         * Standard constructor - to be used prior to deserialization
         */
        public DiskDeviceInfo(
        ) {
            _blockCount = new BlockCount();
            _blockSize = new BlockSize();
            _isMounted = false;
            _isWriteProtected = false;
        }

        /**
         * Initial value constructor
         * <p>
         * @param model
         * @param type
         * @param isReady
         * @param subsystemIdentifier
         * @param unitAttention
         * @param blockCount
         * @param blockSize
         * @param isMounted
         * @param isWriteProtected
         */
        public DiskDeviceInfo(
            final DeviceType type,
            final DeviceModel model,
            final boolean isReady,
            final short subsystemIdentifier,
            final boolean unitAttention,
            final BlockCount blockCount,
            final BlockSize blockSize,
            final boolean isMounted,
            final boolean isWriteProtected
        ) {
            super(type, model, isReady, subsystemIdentifier, unitAttention);
            _blockCount = blockCount;
            _blockSize = blockSize;
            _isMounted = isMounted;
            _isWriteProtected = isWriteProtected;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public BlockCount getBlockCount(
        ) {
            return _blockCount;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public BlockSize getBlockSize(
        ) {
            return _blockSize;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public boolean isMounted(
        ) {
            return _isMounted;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public boolean isWriteProtected(
        ) {
            return _isWriteProtected;
        }

        /**
         * Deserializes the pertinent information for this disk device, to a byte stream.
         * We might be overridden by a subclass, which calls here first, then deserializes its own information.
         * <p>
         * @param buffer
         */
        @Override
        public void deserialize(
            final ByteBuffer buffer
        ) {
            super.deserialize(buffer);

            _blockCount.deserialize(buffer);
            _blockSize.deserialize(buffer);
            _isMounted = deserializeBoolean(buffer);
            _isWriteProtected = deserializeBoolean(buffer);
        }

        /**
         * Serializes information for this disk device to a byte stream.
         * We might be overridden by a subclass, which calls here first, then serializes its own information.
         * <p>
         * @param buffer
         */
        @Override
        public void serialize(
            final ByteBuffer buffer
        ) {
            super.serialize(buffer);

            _blockCount.serialize(buffer);
            _blockSize.serialize(buffer);
            serializeBoolean(buffer, _isMounted);
            serializeBoolean(buffer, _isWriteProtected);
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(DiskDevice.class);

    /**
     * Number of blocks on the mounted pack
     */
    private BlockCount _blockCount;

    /**
     * block size (in bytes) of the mounted pack
     */
    private BlockSize _blockSize;

    /**
     * indicates whether a pack is mounted on the device
     */
    private boolean _isMounted;

    /**
     * indicates whether the device is write-protected
     */
    private boolean _isWriteProtected;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public DiskDevice(
        final DeviceModel deviceModel,
        final String name,
        final short subsystemIdentifier
    ) {
        super(DeviceType.Disk, deviceModel, name, subsystemIdentifier);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public BlockCount getBlockCount(
    ) {
        return _blockCount;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public BlockSize getBlockSize(
    ) {
        return _blockSize;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean isMounted(
    ) {
        return _isMounted;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean isWriteProtected(
    ) {
        return _isWriteProtected;
    }

    /**
     * Setter
     * <p>
     * @param blockCount
     */
    public void setBlockCount(
        final BlockCount blockCount
    ) {
        _blockCount = blockCount;
    }

    /**
     * Setter
     * <p>
     * @param blockSize
     */
    public void setBlockSize(
        final BlockSize blockSize
    ) {
        _blockSize = blockSize;
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setIsMounted(
        final boolean flag
    ) {
        _isMounted = flag;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Does this device have a byte interface?
     * <p>
     * @return
     */
    @Override
    public abstract boolean hasByteInterface(
    );

    /**
     * Does this device have a word interface?
     * <p>
     * @return
     */
    @Override
    public abstract boolean hasWordInterface(
    );

    /**
     * Initializes the subclass if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Handles a GetInfo function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioGetInfo(
        final IOInfo ioInfo
    );

    /**
     * Handles a Read function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioRead(
        final IOInfo ioInfo
    );

    /**
     * Handles a Reset function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioReset(
        final IOInfo ioInfo
    );

    /**
     * Handles an Unload function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioUnload(
        final IOInfo ioInfo
    );

    /**
     * Handles a Write function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioWrite(
        final IOInfo ioInfo
    );


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

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
            writer.write(String.format("  Block Size:      %s\n", String.valueOf(_blockSize)));
            writer.write(String.format("  Block Count:     %s\n", String.valueOf(_blockCount)));
            writer.write(String.format("  Mounted:         %s\n", String.valueOf(_isMounted)));
            writer.write(String.format("  Write Protected: %s\n", String.valueOf(_isWriteProtected)));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Calls the appropriate local or virtual function based on the information in the IOInfo object
     * <p>
     * @param ioInfo
     */
    @Override
    public void handleIo(
        final IOInfo ioInfo
    ) {
        synchronized(this) {
            ioStart(ioInfo);

            switch (ioInfo.getFunction()) {
                case None:
                    ioInfo.setStatus(IOStatus.Successful);
                    if (ioInfo.getSource() != null) {
                        ioInfo.getSource().signal(this);
                    }
                    break;

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
                    ioInfo.setStatus(IOStatus.InvalidFunction);
            }

            ioEnd(ioInfo);
        }

        if (ioInfo.getSource() != null) {
            ioInfo.getSource().signal(this);
        }
    }

    /**
     * Checks whether the mounted pack is prepped (block size is non-null).  This is hardware-prepped... it does not imply
     * an MFD track or anything like that.  However, if the device actually knows its block size, then it is highly likely
     * that the pack *is* prepped - after all, it cannot have a block size until we prep it with a prep factor.
     * We don't check to see if the pack is mounted - if it isn't, then block size is zero anyway, and we still return false
     * <p>
     * @return
     */
    public boolean isPrepped(
    ) {
        return (_blockSize != null) && (_blockSize.getValue() != 0);
    }

    /**
     * Makes sure the requested state change is allowable, then calls the superclass to effect said change.
     * We do not allow the device to be set ready if a pack is not mounted.
     * <p>
     * @param flag true to set ready, false to set not-ready
     * <p>
     * @return true if state change was successful, else false
     */
    @Override
    public boolean setReady(
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
     * <p>
     * @param flag true to set write protect, false to clear
     * <p>
     * @return true if state change was successful, else false
     */
    public boolean setIsWriteProtected(
        final boolean flag
    ) {
        boolean result = false;
        synchronized(this) {
            if (!flag && !isMounted()) {
                return false;
            }

            _isWriteProtected = flag;
            LOGGER.info(String.format("Device %s WriteProtect %s", getName(), flag ? "Set" : "Cleared"));
        }

        return true;
    }

    /**
     * Writes the buffers associated with an IOInfo object, to the log.
     * This should only be done during development for debug and verification.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void writeBuffersToLog(
        final IOInfo ioInfo
    ) {
        if (ioInfo.getByteBuffer() != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo.getByteBuffer());
        }
        if (ioInfo.getWordBuffer() != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo.getWordBuffer());
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks to see if a given disk pack block size is valid
     * <p>
     * @param blockSize
     * <p>
     * @return
     */
    public static boolean isValidBlockSize(
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
