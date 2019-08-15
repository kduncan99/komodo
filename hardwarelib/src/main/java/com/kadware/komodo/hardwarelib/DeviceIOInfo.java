/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.exceptions.InvalidArgumentRuntimeException;

/**
 * Represents an IO passed from a ChannelModule object to one of its descendant Device objects
 */
@SuppressWarnings("Duplicates")
public class DeviceIOInfo {

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
    DeviceStatus _status = DeviceStatus.InvalidStatus;

    /**
     * Number of bytes or words successfully transferred - cannot be larger than _transferCount
     */
    int _transferredCount = 0;

    private DeviceIOInfo(
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

        public DeviceIOInfo build() {
            if (_source == null) {
                throw new InvalidArgumentRuntimeException("No source parameter");
            } else if (_ioFunction == null) {
                throw new InvalidArgumentRuntimeException("No ioFunction parameter");
            }

            return new DeviceIOInfo(_source, _ioFunction, null, null, 0, null);
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

        public DeviceIOInfo build() {
            if (_source == null) {
                throw new InvalidArgumentRuntimeException("No source parameter");
            } else if (_ioFunction == null) {
                throw new InvalidArgumentRuntimeException("No ioFunction parameter");
            } else if (_ioFunction.isWriteFunction() && _ioFunction.requiresBuffer() && (_buffer == null)) {
                throw new InvalidArgumentRuntimeException("No buffer parameter");
            } else if ((_ioFunction.isReadFunction() || (!_ioFunction.requiresBuffer()))
                       && (_buffer != null)) {
                throw new InvalidArgumentRuntimeException("Buffer wrongly provided");
            } else if (_transferCount == null) {
                throw new InvalidArgumentRuntimeException("No transferCount parameter");
            }

            return new DeviceIOInfo(_source, _ioFunction, _buffer, null, _transferCount, _blockId);
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

        public DeviceIOInfo build() {
            if (_source == null) {
                throw new InvalidArgumentRuntimeException("No source parameter");
            } else if (_ioFunction == null) {
                throw new InvalidArgumentRuntimeException("No ioFunction parameter");
            } else if (_ioFunction.isWriteFunction() && _ioFunction.requiresBuffer() && (_buffer == null)) {
                throw new InvalidArgumentRuntimeException("No buffer parameter");
            } else if ((_ioFunction.isReadFunction() || (!_ioFunction.requiresBuffer()))
                && (_buffer != null)) {
                throw new InvalidArgumentRuntimeException("Buffer wrongly provided");
            } else if (_transferCount == null) {
                throw new InvalidArgumentRuntimeException("No transferCount parameter");
            }

            return new DeviceIOInfo(_source, _ioFunction, null, _buffer, _transferCount, _blockId);
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
                             String.valueOf(_ioFunction),
                             String.valueOf(_blockId),
                             String.valueOf(_transferCount),
                             String.valueOf(_transferredCount),
                             String.valueOf(_status));
    }
}
