/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;

import java.nio.ByteBuffer;

public class DiskIoPacket extends IoPacket {

    private long _blockId;
    private long _blockCount;
    private ByteBuffer _buffer;

    public DiskIoPacket() {}

    public long getBlockCount() { return _blockCount; }
    public long getBlockId() { return _blockId; }
    public ByteBuffer getBuffer() { return _buffer; }
    public DiskIoPacket setBlockCount(final long blockCount) { _blockCount = blockCount; return this; }
    public DiskIoPacket setBlockId(final long blockId) { _blockId = blockId; return this; }
    public DiskIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }

    @Override
    public String toString() {
        return "[" + super.toString() + " blkId:" + _blockId + " blks:" + _blockCount + "]";
    }
}
