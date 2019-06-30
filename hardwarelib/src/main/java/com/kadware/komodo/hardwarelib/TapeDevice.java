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

/**
 * Abstract class descibing the common attributes of a tape device
 */
public abstract class TapeDevice extends Device {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class TapeDeviceInfo extends DeviceInfo {

        private boolean _endOfTape;
        private boolean _isMounted;
        private boolean _isWriteProtected;
        private boolean _loadPoint;

        /**
         * Standard constructor - to be used prior to deserialization
         */
        public TapeDeviceInfo(
        ) {
            _endOfTape = false;
            _isMounted = false;
            _isWriteProtected = false;
            _loadPoint = false;
        }

        /**
         * Initial value constructor
         * <p>
         * @param model
         * @param type
         * @param isReady
         * @param subsystemIdentifier
         * @param unitAttention
         * @param isMounted
         * @param isWriteProtected
         * @param endOfTape
         * @param loadPoint
         */
        public TapeDeviceInfo(
            final DeviceType type,
            final DeviceModel model,
            final boolean isReady,
            final short subsystemIdentifier,
            final boolean unitAttention,
            final boolean isMounted,
            final boolean isWriteProtected,
            final boolean endOfTape,
            final boolean loadPoint
        ) {
            super(type, model, isReady, subsystemIdentifier, unitAttention);
            _isMounted = isMounted;
            _isWriteProtected = isWriteProtected;
            _endOfTape = endOfTape;
            _loadPoint = loadPoint;
        }

        /**
         * Getter
         * <p.
         * @return
         */
        public boolean getEndOfTape(
        ) {
            return _endOfTape;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public boolean isLoadPoint(
        ) {
            return _loadPoint;
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
         * @param buffer source for deserialization
         */
        @Override
        public void deserialize(
            final ByteBuffer buffer
        ) {
            super.deserialize(buffer);

            _endOfTape = deserializeBoolean(buffer);
            _isMounted = deserializeBoolean(buffer);
            _isWriteProtected = deserializeBoolean(buffer);
            _loadPoint = deserializeBoolean(buffer);
        }

        /**
         * Serializes information for this disk device to a byte stream.
         * We might be overridden by a subclass, which calls here first, then serializes its own information.
         * <p>
         * @param buffer target of serialization
         */
        @Override
        public void serialize(
            final ByteBuffer buffer
        ) {
            super.serialize(buffer);

            serializeBoolean(buffer, _endOfTape);
            serializeBoolean(buffer, _isMounted);
            serializeBoolean(buffer, _isWriteProtected);
            serializeBoolean(buffer, _loadPoint);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(TapeDevice.class);

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

    public TapeDevice(
        final DeviceModel deviceModel,
        final String name,
        final short subsystemIdentifier
    ) {
        super(DeviceType.Tape, deviceModel, name, subsystemIdentifier);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

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
     * Getter
     * Various subclasses may have different ideas of the max block size
     * <p>
     * @return
     */
    public abstract int getMaxBlockSize(
    );

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
     * Handles a MoveBlock function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioMoveBlock(
        final IOInfo ioInfo
    );

    /**
     * Handles a MoveBlockBackward function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioMoveBlockBackward(
        final IOInfo ioInfo
    );

    /**
     * Handles a MoveFile function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioMoveFile(
        final IOInfo ioInfo
    );

    /**
     * Handles a MoveFileBackward function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioMoveFileBackward(
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
     * Handles a ReadBackward function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioReadBackward(
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
     * Handles a Rewind function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioRewind(
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

    /**
     * Handles a WriteEndOfFile function
     * <p>
     * @param ioInfo
     */
    protected abstract void ioWriteEndOfFile(
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
                    ioInfo.setStatus(IOStatus.InvalidFunction);
            }

            ioEnd(ioInfo);
        }

        if (ioInfo.getSource() != null) {
            ioInfo.getSource().signal(this);
        }
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
}
