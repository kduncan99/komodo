/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
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
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

//    public static class TapeDeviceInfo extends DeviceInfo {
//
//        private boolean _endOfTape;
//        private boolean _isMounted;
//        private boolean _isWriteProtected;
//        private boolean _loadPoint;
//
//        /**
//         * Standard constructor - to be used prior to deserialization
//         */
//        public TapeDeviceInfo(
//        ) {
//            _endOfTape = false;
//            _isMounted = false;
//            _isWriteProtected = false;
//            _loadPoint = false;
//        }
//
//        /**
//         * Initial value constructor
//         * <p>
//         * @param model
//         * @param type
//         * @param isReady
//         * @param subsystemIdentifier
//         * @param unitAttention
//         * @param isMounted
//         * @param isWriteProtected
//         * @param endOfTape
//         * @param loadPoint
//         */
//        public TapeDeviceInfo(
//            final DeviceType type,
//            final DeviceModel model,
//            final boolean isReady,
//            final short subsystemIdentifier,
//            final boolean unitAttention,
//            final boolean isMounted,
//            final boolean isWriteProtected,
//            final boolean endOfTape,
//            final boolean loadPoint
//        ) {
//            super(type, model, isReady, subsystemIdentifier, unitAttention);
//            _isMounted = isMounted;
//            _isWriteProtected = isWriteProtected;
//            _endOfTape = endOfTape;
//            _loadPoint = loadPoint;
//        }
//
//        /**
//         * Getter
//         * <p.
//         * @return
//         */
//        public boolean getEndOfTape(
//        ) {
//            return _endOfTape;
//        }
//
//        /**
//         * Getter
//         * <p>
//         * @return
//         */
//        public boolean isLoadPoint(
//        ) {
//            return _loadPoint;
//        }
//
//        /**
//         * Getter
//         * <p>
//         * @return
//         */
//        public boolean isMounted(
//        ) {
//            return _isMounted;
//        }
//
//        /**
//         * Getter
//         * <p>
//         * @return
//         */
//        public boolean isWriteProtected(
//        ) {
//            return _isWriteProtected;
//        }
//
//        /**
//         * Deserializes the pertinent information for this disk device, to a byte stream.
//         * We might be overridden by a subclass, which calls here first, then deserializes its own information.
//         * <p>
//         * @param buffer source for deserialization
//         */
//        @Override
//        public void deserialize(
//            final ByteBuffer buffer
//        ) {
//            super.deserialize(buffer);
//
//            _endOfTape = deserializeBoolean(buffer);
//            _isMounted = deserializeBoolean(buffer);
//            _isWriteProtected = deserializeBoolean(buffer);
//            _loadPoint = deserializeBoolean(buffer);
//        }
//
//        /**
//         * Serializes information for this disk device to a byte stream.
//         * We might be overridden by a subclass, which calls here first, then serializes its own information.
//         * <p>
//         * @param buffer target of serialization
//         */
//        @Override
//        public void serialize(
//            final ByteBuffer buffer
//        ) {
//            super.serialize(buffer);
//
//            serializeBoolean(buffer, _endOfTape);
//            serializeBoolean(buffer, _isMounted);
//            serializeBoolean(buffer, _isWriteProtected);
//            serializeBoolean(buffer, _loadPoint);
//        }
//    }


    private static final Logger LOGGER = LogManager.getLogger(TapeDevice.class);

    /**
     * indicates whether a pack is mounted on the device
     */
    private boolean _isMounted;

    /**
     * indicates whether the device is write-protected
     */
    private boolean _isWriteProtected;


    TapeDevice(
        final DeviceModel deviceModel,
        final String name
    ) {
        super(DeviceType.Tape, deviceModel, name);
    }


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

    abstract void ioGetInfo(final DeviceIOInfo ioInfo);
    abstract void ioMoveBlock(final DeviceIOInfo ioInfo);
    abstract void ioMoveBlockBackward(final DeviceIOInfo ioInfo);
    abstract void ioMoveFile(final DeviceIOInfo ioInfo);
    abstract void ioMoveFileBackward(final DeviceIOInfo ioInfo);
    abstract void ioRead(final DeviceIOInfo ioInfo);
    abstract void ioReadBackward(final DeviceIOInfo ioInfo);
    abstract void ioReset(final DeviceIOInfo ioInfo);
    abstract void ioRewind(final DeviceIOInfo ioInfo);
    abstract void ioUnload(final DeviceIOInfo ioInfo);
    abstract void ioWrite(final DeviceIOInfo ioInfo);
    abstract void ioWriteEndOfFile(final DeviceIOInfo ioInfo);


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
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Calls the appropriate local or virtual function based on the information in the IOInfo object
     * @return true if we scheduled the IO, false if we completed it immediately
     */
    @Override
    public boolean handleIo(
        final DeviceIOInfo ioInfo
    ) {
        synchronized(this) {
            ioStart(ioInfo);

            switch (ioInfo._ioFunction) {
                case None:
                    ioInfo._status = DeviceStatus.Successful;
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
                    ioInfo._status = DeviceStatus.InvalidFunction;
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
        final DeviceIOInfo ioInfo
    ) {
        if (ioInfo._byteBuffer != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo._byteBuffer);
        } else if (ioInfo._wordBuffer != null) {
            logBuffer(LOGGER, Level.INFO, "IO Buffer", ioInfo._wordBuffer);
        }
    }
}
