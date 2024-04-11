/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class TapeIoPacket extends IoPacket {

    public ByteBuffer _buffer;
    public int _bytesTransferred;
    public MountInfo _mountInfo;

    public ByteBuffer getBuffer() { return _buffer; }
    public int getBytesTransferred() { return _bytesTransferred; }
    public MountInfo getMountInfo() { return _mountInfo; }
    public TapeIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }
    public TapeIoPacket setBytesTransferred(final int value) { _bytesTransferred = value; return this; }
    public TapeIoPacket setMountInfo(final MountInfo mountInfo) { _mountInfo = mountInfo; return this; }
}
