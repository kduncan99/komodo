/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.network.SocketChannelHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class TextMessage implements Message {

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
    public void write(SocketChannel channel)
        throws IOException {
        var bb = ByteBuffer.wrap(_text);
        SocketChannelHandler.dumpBuffer("Sending: ", _text);//TODO remove
        channel.write(bb);
    }

    @Override
    public String toString() {
        return String.format("TextMessage length:%d", _text.length);
    }
}
