/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class SymbiontIoPacket extends IoPacket {

    private int _wordCount;
    private ByteBuffer _buffer;

    public SymbiontIoPacket() {}

    public ByteBuffer getBuffer() { return _buffer; }
    public int getWordCount() { return _wordCount; }
    public SymbiontIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }
    public SymbiontIoPacket setWordCount(final int value) { _wordCount = value; return this; }

    @Override
    public String toString() {
        return String.format("%s words:%d", getFunction(), _wordCount);
    }
}
