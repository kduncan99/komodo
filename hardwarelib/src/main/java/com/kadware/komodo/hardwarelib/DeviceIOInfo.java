/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BlockId;
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
     * Reference to a byte buffer for byte data transfers
     */
    final byte[] _byteBuffer;

    /**
     * Reference to a word buffer for word data transfers
     */
    final ArraySlice _wordBuffer;

    /**
     * Number of bytes or words to be transferred
     */
    final int _transferCount;

    /**
     * Identifies a particular hardware block address or offset for device-addressed functions
     */
    final BlockId _blockId;

    /**
     * Sense bytes returned from a physical IO (if we ever implement such)
     */
    byte[] _senseBytes = null;

    /**
     * Result of the operation
     */
    DeviceStatus _status = DeviceStatus.InvalidStatus;

    /**
     * Number of bytes or words successfully transferred
     */
    int _transferredCount = 0;

    private DeviceIOInfo(
        ChannelModule source,
        IOFunction function,
        byte[] byteBuffer,
        ArraySlice wordBuffer,
        int transferCount,
        BlockId blockId
    ) {
        _source = source;
        _ioFunction = function;
        _byteBuffer = byteBuffer;
        _wordBuffer = wordBuffer;
        _transferCount = transferCount;
        _blockId = blockId;
    }

    public static class NonTransferBuilder {

        private ChannelModule _source = null;
        private IOFunction _ioFunction = null;

        private NonTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
        private NonTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }

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
        BlockId _blockId = null;

        private ByteTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
        private ByteTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }
        private ByteTransferBuilder setBuffer(byte[] value) { _buffer = value; return this; }
        private ByteTransferBuilder setTransferCount(int value) { _transferCount = value; return this; }
        private ByteTransferBuilder setBlockId(long value) { _blockId = new BlockId(value); return this; }

        public DeviceIOInfo build() {
            if (_source == null) {
                throw new InvalidArgumentRuntimeException("No source parameter");
            } else if (_ioFunction == null) {
                throw new InvalidArgumentRuntimeException("No ioFunction parameter");
            } else if (_buffer == null) {
                throw new InvalidArgumentRuntimeException("No buffer parameter");
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
        BlockId _blockId = null;

        private WordTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
        private WordTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }
        private WordTransferBuilder setBuffer(ArraySlice value) { _buffer = value; return this; }
        private WordTransferBuilder setTransferCount(int value) { _transferCount = value; return this; }
        private WordTransferBuilder setBlockId(long value) { _blockId = new BlockId(value); return this; }

        public DeviceIOInfo build() {
            if (_source == null) {
                throw new InvalidArgumentRuntimeException("No source parameter");
            } else if (_ioFunction == null) {
                throw new InvalidArgumentRuntimeException("No ioFunction parameter");
            } else if (_buffer == null) {
                throw new InvalidArgumentRuntimeException("No buffer parameter");
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
