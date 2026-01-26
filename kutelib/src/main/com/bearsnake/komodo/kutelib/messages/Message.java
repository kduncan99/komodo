/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.SocketChannelHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Message {

    /**
     * Writes the message to the provided SocketChannel.
     * @param channel the SocketChannel to write the message to
     * @throws IOException if an I/O error occurs
     */
    void write(final SocketChannel channel) throws IOException;

    @Override
    String toString();

    /**
     * Creates a Message object of the appropriate type based on the provided data.
     * @param data must begin with SOH and end with ETX
     * @return Message object, or null if the data is not recognized as a valid message
     */
    static Message create(final byte[] data) {
        Message message = MessageWaitMessage.create(data);
        if (message == null) {
            message = FunctionKeyMessage.create(data);
        }
        if (message == null) {
            message = TextMessage.create(data);
        }
        return message;
    }
}
