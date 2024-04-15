/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class DiskInfo {

    private final int _blockSize;
    private final long _blockCount; // TODO current - may not be the same as max
    private final long _maxBlockCount;
    private final boolean _isMounted;
    private final boolean _isReady;
    private final boolean _isWriteProtected;

    public DiskInfo(final int blockSize,
                    final long blockCount,
                    final long maxBlockCount,
                    final boolean isMounted,
                    final boolean isReady,
                    final boolean isWriteProtected) {
        _blockSize = blockSize;
        _blockCount = blockCount;
        _maxBlockCount = maxBlockCount;
        _isMounted = isMounted;
        _isReady = isReady;
        _isWriteProtected = isWriteProtected;
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

    public boolean isMounted() {
        return _isMounted;
    }

    public boolean isReady() {
        return _isReady;
    }

    public boolean isWriteProtected() {
        return _isWriteProtected;
    }

    public static DiskInfo deserialize(final ByteBuffer bb) {
        var isMounted = bb.getInt() != 0;
        var isReady = bb.getInt() != 0;
        var isWriteProtected = bb.getInt() != 0;
        var blockSize = bb.getInt();
        var blockCount = bb.getLong();
        var maxBlockCount = bb.getLong();
        return new DiskInfo(blockSize, blockCount, maxBlockCount, isMounted, isReady, isWriteProtected);
    }

    public void serialize(final ByteBuffer bb) {
        bb.putInt(_isMounted ? 1 : 0);
        bb.putInt(_isReady ? 1 : 0);
        bb.putInt(_isWriteProtected ? 1 : 0);
        bb.putInt(_blockSize);
        bb.putLong(_blockCount);
        bb.putLong(_maxBlockCount);
    }

    @Override
    public String toString() {
        return String.format("[mnt:%s rdy:%s wProt:%s blkSize:%d blkCount:%d maxBlocks:%d]",
                             _isMounted,
                             _isReady,
                             _isWriteProtected,
                             _blockSize,
                             _blockCount,
                             _maxBlockCount);
    }
}
