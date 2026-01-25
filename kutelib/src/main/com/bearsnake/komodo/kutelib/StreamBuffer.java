/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class StreamBuffer {

    ByteBuffer _buffer;

    public StreamBuffer(final ByteBuffer buffer) {
        _buffer = buffer;
    }

    public StreamBuffer(final byte[] array, final int offset, final int length) {
        _buffer = ByteBuffer.wrap(array, offset, length);
    }

    public boolean atEnd() {
        return _buffer.position() == _buffer.limit();
    }

    public byte[] getArray() {
        return _buffer.array();
    }

    public byte get() throws BufferUnderflowException {
        byte ch = 0;
        while (ch == 0) {
            ch = _buffer.get();
        }
        return ch;
    }

    public int getPosition() {
        return _buffer.position();
    }
}
