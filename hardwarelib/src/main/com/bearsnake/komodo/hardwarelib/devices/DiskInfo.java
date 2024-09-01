/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

public class DiskInfo extends DeviceInfo {

    private final int _blockSize;       // bytes per block
    private final long _blockCount;     // current block count - may be less than maximum
    private final long _maxBlockCount;  // max number of blocks this disk can hold

    public DiskInfo(final int blockSize,
                    final long blockCount,
                    final long maxBlockCount,
                    final boolean isMounted,
                    final boolean isReady,
                    final boolean isWriteProtected) {
        super(isMounted, isReady, isWriteProtected);
        _blockSize = blockSize;
        _blockCount = blockCount;
        _maxBlockCount = maxBlockCount;
    }

    public long getBlockCount() {
        return _blockCount;
    }

    public int getBlockSize() {
        return _blockSize;
    }

    public long getMaxBlockCount() {
        return _maxBlockCount;
    }

    @Override
    public String toString() {
        return String.format("[%s blkSize:%d blkCount:%d maxBlocks:%d]", super.toString(), _blockSize, _blockCount, _maxBlockCount);
    }
}
