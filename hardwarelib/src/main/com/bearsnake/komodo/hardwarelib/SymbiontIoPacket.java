/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class SymbiontIoPacket extends IoPacket {

    private ByteBuffer _buffer;
    private int _spacing; // for image print (pre-print line spacing - <0 means page-feed)
    private String _identifier; // for image/print output

    public SymbiontIoPacket() {}

    public ByteBuffer getBuffer() { return _buffer; }
    public String getIdentifier() { return _identifier; }
    public int getSpacing() { return _spacing; }
    public SymbiontIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }
    public SymbiontIoPacket setIdentifier(final String value) { _identifier = value; return this; }
    public SymbiontIoPacket setSpacing(final int value) { _spacing = value; return this; }

    @Override
    public String toString() {
        return String.format("%s id=%s space=%d", getFunction(), getIdentifier(), getSpacing());
    }
}
