/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.messages;

import com.bearsnake.komodo.utslib.UTSByteBuffer;
import java.nio.ByteBuffer;

public interface UTSMessage {

    /**
     * Retrieves a ByteBuffer representation of the message.
     * @return ByteBuffer containing the message data
     */
    ByteBuffer getByteBuffer();

    @Override
    String toString();

    /**
     * Wrapper function for create(UTSByteBuffer)
     * @param buffer wrapper for buffer to be re-wrapped into a Message object
     * @return Message object created from the buffer
     */
    static UTSMessage create(final UTSByteBuffer buffer) {
        return create(buffer.setPointer(0).getBuffer());
    }

    /**
     * Creates a Message object of the appropriate type based on the provided data.
     * @param data must begin with SOH and end with ETX
     * @return Message object, or null if the data is not recognized as a valid message
     */
    static UTSMessage create(final byte[] data) {
        UTSMessage message = MessageWaitMessage.create(data);
        if (message == null) {
            message = CursorPositionMessage.create(data);
        }
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
