/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.exceptions.InternalErrorRuntimeException;
import com.kadware.komodo.baselib.types.BlockId;

/**
 * Describes an IO at the device level.  This might be extended for more specificity, by a subclass of Device.
 */
@SuppressWarnings("Duplicates")
public class IOInfo {
    /**
     * Source of the IO request
     */
    public final Node _source;

    /**
     * Function to be performed
     */
    public final IOFunction _function;

    /**
     * Reference to a byte buffer for byte data transfers
     */
    public final byte[] _byteBuffer;

    /**
     * Reference to a Word36Array buffer for word data transfers
     */
    public final ArraySlice _wordBuffer;

    /**
     * Number of bytes or words to be transferred
     */
    public final int _transferCount;

    /**
     * Identifies a particular hardware block address or offset for device-addressed functions
     */
    public final BlockId _blockId;

    /**
     * Sense bytes returned from a physical IO (if we ever implement such)
     */
    public byte[] _senseBytes;

    /**
     * Result of the operation
     */
    public IOStatus _status;

    /**
     * Number of bytes or words successfully transferred
     */
    public int _transferredCount;

    private IOInfo(
        final Node source,
        final IOFunction function,
        final byte[] byteBuffer,
        final ArraySlice wordBuffer,
        final int transferCount,
        final BlockId blockId
    ) {
        _source = source;
        _function = function;
        _byteBuffer = byteBuffer;
        _wordBuffer = wordBuffer;
        _transferCount = transferCount;
        _blockId = blockId;
    }

    public void setTransferredCount(int count) { _transferredCount = count; }
    public void setSenseBytes(final byte[] senseBytes) { _senseBytes = senseBytes; }
    public void setStatus(final IOStatus status) { _status = status; }

    /**
     * Produces a displayable string describing this object
     */
    @Override
    public String toString(
    ) {
        return String.format("Src=%s Fcn=%s BlkId=%s Count=%s Xferd=%s Stat=%s",
                             _source == null ? "<null>" : _source._name,
                             String.valueOf(_function),
                             String.valueOf(_blockId),
                             String.valueOf(_transferCount),
                             String.valueOf(_transferredCount),
                             String.valueOf(_status));
    }

    public static class Builder {

        private Node _source = null;
        private IOFunction _function = null;
        private byte[] _byteBuffer = null;
        private ArraySlice _wordBuffer = null;
        private Integer _transferCount = null;
        private BlockId _blockId = null;

        public Builder setSource(Node source) { _source = source; return this; }
        public Builder setFunction(IOFunction function) { _function = function; return this; }
        public Builder setBuffer(byte[] buffer) { _byteBuffer = buffer; return this; }
        public Builder setBuffer(ArraySlice buffer) { _wordBuffer = buffer; return this; }
        public Builder setTransferCount(int transferCount) { _transferCount = transferCount; return this; }
        public Builder setBlockId(BlockId blockId) { _blockId = blockId; return this; }

        public IOInfo build() {
            //  We always need a source and a function
            if (_source == null) {
                throw new InternalErrorRuntimeException("No source for IOInfo");
            }
            if (_function == null) {
                throw new InternalErrorRuntimeException("No function for IOInfo");
            }

            //  Cannot have both types of buffers
            if ((_byteBuffer != null) && (_wordBuffer != null)) {
                throw new InternalErrorRuntimeException("Cannot use both types of buffers for IOInfo");
            }

            return new IOInfo(_source, _function, _byteBuffer, _wordBuffer, _transferCount, _blockId);
        }
    }
}
