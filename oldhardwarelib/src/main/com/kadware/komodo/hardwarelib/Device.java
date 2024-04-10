/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.logger.LogManager;
import java.io.PrintStream;

/**
 * Abstract base class for a device such as a disk or tape unit
 * Devices are child Nodes of ChannelModules, and are the lowest units in the IO chain.
 * All IO to Devices is done asynchronously (as far as we are concerned at this level).
 * The ChannelModule sends a IOInfo object to the Device.
 * If the IO cannot be scheduled, the IOStatus field of the IOInfo is updated immediately, and the IO is rejected.
 * For IOs which are asynchronous at the system level, the IO is performed, IOStatus is updated,
 *   a signal is sent to the sender, and the IO is considered complete.
 * For IOs which are synchronous at the system level, the IO is scheduled, IOStatus is updated, and the IO is accepted.
 * When the IO completes at the system level, a signal is sent to the sender.
 * Senders must preserve IOInfo objects in memory until the Device marks the IOStatus field with some
 *   value indicating that the IO is complete, rejected, or has failed.
 * Actual system IO may involve invoking the host system's asynchronous IO facility.
 */
@SuppressWarnings("Duplicates")
public abstract class Device extends Node {

    private static final String LOG_SOURCE = "DEVICE";

    /**
     * Represents an IO passed from a ChannelModule object to one of its descendant Device objects
     */
    public static class IOInfo {

        /**
         * Source of the IO request
         */
        final ChannelModule _source;

        /**
         * Function to be performed
         */
        final IOFunction _ioFunction;

        /**
         * Reference to a byte buffer for byte data transfers for byte devices
         * Required for writes, should be null for reads - the device should create this.
         * Must be at least as large as indicated by _transferCount.
         */
        byte[] _byteBuffer;

        /**
         * Reference to a word buffer for word data transfers for word devices
         * Required for writes, should be null for reads - the device should create this.
         * Must be at least as large as indicated by _transferCount.
         */
        ArraySlice _wordBuffer;

        /**
         * Number of bytes to be transferred for byte devices
         * Number of words to be transferred for word devices
         */
        final int _transferCount;

        /**
         * Identifies a particular hardware block address or offset for device-addressed functions
         */
        final long _blockId;

        /**
         * Result of the operation
         */
        IOStatus _status = IOStatus.InvalidStatus;

        /**
         * Number of bytes or words successfully transferred - cannot be larger than _transferCount
         */
        int _transferredCount = 0;

        private IOInfo(
            ChannelModule source,
            IOFunction function,
            byte[] byteBuffer,
            ArraySlice wordBuffer,
            int transferCount,
            Long blockId
        ) {
            _source = source;
            _ioFunction = function;
            _byteBuffer = byteBuffer;
            _wordBuffer = wordBuffer;
            _transferCount = transferCount;
            _blockId = (blockId == null) ? 0 : blockId;
        }

        public static class NonTransferBuilder {

            private ChannelModule _source = null;
            private IOFunction _ioFunction = null;

            NonTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
            NonTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }

            public IOInfo build() {
                if (_source == null) {
                    throw new RuntimeException("No source parameter");
                } else if (_ioFunction == null) {
                    throw new RuntimeException("No ioFunction parameter");
                }

                return new IOInfo(_source, _ioFunction, null, null, 0, null);
            }
        }

        public static class ByteTransferBuilder {

            private ChannelModule _source = null;
            private IOFunction _ioFunction = null;
            private byte[] _buffer = null;
            Integer _transferCount = null;
            Long _blockId = null;

            ByteTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
            ByteTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }
            ByteTransferBuilder setBuffer(byte[] value) { _buffer = value; return this; }
            ByteTransferBuilder setTransferCount(int value) { _transferCount = value; return this; }
            ByteTransferBuilder setBlockId(long value) { _blockId = value; return this; }

            public IOInfo build() {
                if (_source == null) {
                    throw new RuntimeException("No source parameter");
                } else if (_ioFunction == null) {
                    throw new RuntimeException("No ioFunction parameter");
                } else if (_ioFunction.isWriteFunction() && _ioFunction.requiresBuffer() && (_buffer == null)) {
                    throw new RuntimeException("No buffer parameter");
                } else if ((_ioFunction.isReadFunction() || (!_ioFunction.requiresBuffer()))
                           && (_buffer != null)) {
                    throw new RuntimeException("Buffer wrongly provided");
                } else if (_transferCount == null) {
                    throw new RuntimeException("No transferCount parameter");
                }

                return new IOInfo(_source, _ioFunction, _buffer, null, _transferCount, _blockId);
            }
        }

        public static class WordTransferBuilder {

            private ChannelModule _source = null;
            private IOFunction _ioFunction = null;
            private ArraySlice _buffer = null;
            Integer _transferCount = null;
            Long _blockId = null;

            WordTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
            WordTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }
            WordTransferBuilder setBuffer(ArraySlice value) { _buffer = value; return this; }
            WordTransferBuilder setTransferCount(int value) { _transferCount = value; return this; }
            WordTransferBuilder setBlockId(long value) { _blockId = value; return this; }

            public IOInfo build() {
                if (_source == null) {
                    throw new RuntimeException("No source parameter");
                } else if (_ioFunction == null) {
                    throw new RuntimeException("No ioFunction parameter");
                } else if (_ioFunction.isWriteFunction() && _ioFunction.requiresBuffer() && (_buffer == null)) {
                    throw new RuntimeException("No buffer parameter");
                } else if ((_ioFunction.isReadFunction() || (!_ioFunction.requiresBuffer()))
                           && (_buffer != null)) {
                    throw new RuntimeException("Buffer wrongly provided");
                } else if (_transferCount == null) {
                    throw new RuntimeException("No transferCount parameter");
                }

                return new IOInfo(_source, _ioFunction, null, _buffer, _transferCount, _blockId);
            }
        }

        /**
         * Produces a displayable string describing this object
         */
        @Override
        public String toString(
        ) {
            return String.format("Source=%s Fcn=%s BlkId=%s Count=%s Xferd=%s Stat=%s",
                                 _source == null ? "<null>" : _source._name,
                                 _ioFunction,
                                 _blockId,
                                 _transferCount,
                                 _transferredCount,
                                 _status);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    final NodeModel _deviceNodeModel;
    final NodeType _deviceNodeType;

    /**
     * Indicates the device can do reads and writes.
     * If not ready, the device *may* respond to non-read/write IOs.
     */
    boolean _readyFlag;

    /**
     * Indicates that something regarding the physical characteristics of this device has changed.
     * Setting a device ready should ALWAYS set this flag.
     * All reads and writes will be rejected with IOStatus.UnitAttention until an IOFunction.GetInfo is issued.
     */
    boolean _unitAttentionFlag;

    /**
     * IO counters for this device - they are to be kept updated by the various device terminal subclasses.
     * TODO:Should a RESET function clear these?
     */
    long _miscCount = 0;        //  anything non-read/write
    long _readBytes = 0;        //  total bytes transferred from storage
    long _readCount = 0;
    long _writeBytes = 0;       //  total bytes transferred to storage
    long _writeCount = 0;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected Device(
        final NodeType deviceNodeType,
        final NodeModel deviceNodeModel,
        final String name
    ) {
        super(NodeCategory.Device, name);
        _deviceNodeModel = deviceNodeModel;
        _deviceNodeType = deviceNodeType;
        _readyFlag = false;
        _unitAttentionFlag = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates whether this Node can connect as a descendant to the candidate ancestor Node.
     * This is an override of Node.canConnect(), and only appears here as an abstract for documentary purposes.
     */
    @Override
    public abstract boolean canConnect(final Node candidate);

    /**
     * Invoked when a new session is started by the system processor.
     * Any classes which have work to do to enter the cleared state should override this.
     */
    @Override
    public void clear() {}

    /**
     * IO Router - what happens here is depending upon the subclass
     * If we return false, it means we've completed the IO - there will be no scheduling or notification of completion
     */
    public abstract boolean handleIo(IOInfo ioInfo);

    /**
     * Does this device have a byte interface?
     */
    public abstract boolean hasByteInterface();

    /**
     * Does this device have a word interface?
     */
    public abstract boolean hasWordInterface();

    /**
     * Initializes the subclass if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();

    /**
     * Writes IO buffers to the log.
     * What actually happens is dependant upon the particulars of the various subclasses, so this is abstract
     * @param ioInfo describes the IO buffer(s)
     */
    protected abstract void writeBuffersToLog(IOInfo ioInfo);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final PrintStream out,
        final String indent
    ) {
        super.dump(out, indent);
        out.printf("%s  ProcessorType:   %s\n", indent, _deviceNodeType.toString());
        out.printf("%s  Model:           %s\n", indent, _deviceNodeModel.toString());
        out.printf("%s  Ready:           %s\n", indent, _readyFlag);
        out.printf("%s  Unit Attention:  %s\n", indent, _unitAttentionFlag);
        out.printf("%s  Misc IO Count:   %d\n", indent, _miscCount);
        out.printf("%s  Read IO Count:   %d\n", indent, _readCount);
        out.printf("%s  Read Bytes:      %d\n", indent, _readBytes);
        out.printf("%s  Write IO Count:  %d\n", indent, _writeCount);
        out.printf("%s  Write Bytes:     %d\n", indent, _writeBytes);
    }

    /**
     * Produces an array slice which represents, in 36-bit mode, the device info data which is presented to the requestor.
     * Subclasses should call here first to get an ArraySlice with the basic info, add more info, and return the whole thing.
     * The format of the basic info block is:
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
     *  +6  |                                                                 |
     *  ..  |                      device-specific data                       |
     * +27  |                                                                 |
     *      +----------+----------+----------+----------+----------+----------+
     *
     * FLAGS:
     *      Bit 0:  device_ready
     * MODEL:       integer code for the Model
     * TYPE:        integer code for the ProcessorType
     */
    protected ArraySlice getInfo() {
        long[] buffer = new long[28];
        buffer[0] = Word36.setS1(buffer[0], _readyFlag ? 040 : 0);
        buffer[0] = Word36.setS2(buffer[0], _deviceNodeModel.getCode());
        buffer[0] = Word36.setS3(buffer[0], _deviceNodeType.getCode());
        buffer[1] = _miscCount;
        buffer[2] = _readCount;
        buffer[3] = _writeCount;
        buffer[4] = _readBytes;
        buffer[5] = _writeBytes;
        return new ArraySlice(buffer);
    }

    /**
     * Subclasses must call here at the end of handling an IO
     */
    void ioEnd(
        final IOInfo ioInfo
    ) {
        if (LOG_IO_ERRORS) {
            if ((ioInfo._status != IOStatus.Successful) && (ioInfo._status != IOStatus.NoInput)) {
                LogManager.logError(_name, "IoError:%s", ioInfo.toString());
            }
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo._ioFunction.isReadFunction() && (ioInfo._status == IOStatus.Successful)) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Subclasses must call this at the beginning of handling an IO.
     */
    void ioStart(
        final IOInfo ioInfo
    ) {
        if (LOG_DEVICE_IOS) {
            LogManager.logInfo(_name, "ioStart(%s)", ioInfo.toString());
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo._ioFunction.isWriteFunction()) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Sets the device ready or not-ready, depending upon the given state parameter.
     * Subclasses may override this to do more specific stuff (such as verifying that the state can be set as request),
     * but at some point they must call back here to actually effect the state change.
     * @param state true to set ready, false to set not-ready
     * @return true if successful
     */
    boolean setReady(
        final boolean state
    ) {
        LogManager.logTrace(_name, "setReady(%s)", state);
        _readyFlag = state;
        setUnitAttention(state);
        return true;
    }

    /**
     * Sets the unit attention flag (or clears it)
     */
    void setUnitAttention(
        final boolean state
    ) {
        LogManager.logTrace(LOG_SOURCE, "setUnitAttention(%s)", state);
        _unitAttentionFlag = state;
    }
}
