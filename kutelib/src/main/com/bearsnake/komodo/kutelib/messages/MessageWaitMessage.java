/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.SocketChannelHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class MessageWaitMessage implements Message {

    private static final byte[] PATTERN = {ASCII_SOH, ASCII_BEL, ASCII_ETX};

    static MessageWaitMessage create(final byte[] data) {
        if (Arrays.equals(data, PATTERN)) {
            return new MessageWaitMessage();
        } else {
            return null;
        }
    }

    @Override
    public void write(final SocketChannel channel)
        throws IOException {
        SocketChannelHandler.dumpBuffer("Sending: ", PATTERN);//TODO remove
        channel.write(ByteBuffer.wrap(PATTERN));
    }

    @Override
    public String toString() {
        return "MessageWaitMessage";
    }
}
