/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class MessageWaitMessage implements Message {

    private static final byte[] PATTERN = {ASCII_SOH, ASCII_BEL, ASCII_ETX};
    private static final ByteBuffer BUFFER = ByteBuffer.wrap(PATTERN);

    static MessageWaitMessage create(final byte[] data) {
        if (Arrays.equals(data, PATTERN)) {
            return new MessageWaitMessage();
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer getBuffer() {
        return BUFFER.duplicate();
    }

    @Override
    public String toString() {
        return "MessageWaitMessage";
    }
}
