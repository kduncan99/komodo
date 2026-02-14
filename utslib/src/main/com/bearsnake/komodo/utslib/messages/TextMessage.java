/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.messages;

import com.bearsnake.komodo.utslib.UTSByteBuffer;import java.nio.ByteBuffer;import java.util.Arrays;import static com.bearsnake.komodo.baselib.Constants.*;

public class TextMessage implements UTSMessage {

    private final byte[] _text;

    public TextMessage(final byte[] text) {
        _text = text.clone();
    }

    public TextMessage(final byte[] text,
                       final int offset,
                       final int count) {
        _text = Arrays.copyOfRange(text, offset, offset + count);
    }

    public byte[] getStream() {
        return _text;
    }

    public static TextMessage create(final byte[] data) {
        if ((data.length >= 3) && (data[0] == ASCII_SOH) && (data[1] == ASCII_STX) && (data[data.length - 1] == ASCII_ETX)) {
            return new TextMessage(data);
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(_text);
    }

    /**
     * Excises the message from between the SOH-STX and ETX (exclusive), wraps it into a UTSByteBuffer, and returns it.
     * @return UTSByteBuffer containing the message
     */
    public UTSByteBuffer unwrap() {
        return new UTSByteBuffer(Arrays.copyOfRange(_text, 2, _text.length - 1));
    }

    @Override
    public String toString() {
        return String.format("TextMessage length:%d", _text.length);
    }
}
