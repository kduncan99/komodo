/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;

import java.io.IOException;
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
     * Wrapper function for create(UTSByteBuffer)
     * @param buffer wrapper for buffer to be re-wrapped into a Message object
     * @return Message object created from the buffer
     */
    static Message create(final UTSByteBuffer buffer) {
        return create(buffer.setPointer(0).getBuffer());
    }

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
            message = StatusPollMessage.create(data);
        }
        if (message == null) {
            message = StatusMessage.create(data);
        }
        if (message == null) {
            message = DisconnectMessage.create(data);
        }
        if (message == null) {
            message = TextMessage.create(data);
        }
        return message;
    }
}
