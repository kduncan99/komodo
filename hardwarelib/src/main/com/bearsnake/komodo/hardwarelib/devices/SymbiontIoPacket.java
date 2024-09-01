/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;

import java.nio.ByteBuffer;

public class SymbiontIoPacket extends IoPacket {

    private ByteBuffer _buffer;
    private String _mediaIdentifier; // generally Run-id for output devices
    private int _spacing; // for image print (pre-print line spacing - <0 means page-feed)

    public SymbiontIoPacket() {}

    public ByteBuffer getBuffer() { return _buffer; }
    public String getMediaIdentifier() { return _mediaIdentifier; }
    public int getSpacing() { return _spacing; }
    public SymbiontIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }
    public SymbiontIoPacket setMediaIdentifier(final String mediaIdentifier) { _mediaIdentifier = mediaIdentifier; return this; }
    public SymbiontIoPacket setSpacing(final int value) { _spacing = value; return this; }

    @Override
    public String toString() {
        return "[" + super.toString() + " mediaId:" + _mediaIdentifier + " spacing:" + _spacing + "]";
    }
}
