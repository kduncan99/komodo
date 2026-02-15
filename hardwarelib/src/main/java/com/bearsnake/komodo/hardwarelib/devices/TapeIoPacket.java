/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;

import java.nio.ByteBuffer;

public class TapeIoPacket extends IoPacket {

    private ByteBuffer _buffer;
    private int _bytesTransferred;

    public TapeIoPacket() {}

    public ByteBuffer getBuffer() { return _buffer; }
    public int getBytesTransferred() { return _bytesTransferred; }
    public TapeIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }
    public TapeIoPacket setBytesTransferred(final int value) { _bytesTransferred = value; return this; }

    @Override
    public String toString() {
        return "[" + super.toString() + " bytesXferd:" + _bytesTransferred + "]";
    }
}
