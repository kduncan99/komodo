/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * A StatusPollMssage is sent periodically by both ends of a UTS session to check if the connection is still alive.
 * The receiving end should respond with a valid StatusMessage.
 */
public class StatusMessage implements UTSMessage {

    private static final byte[] PATTERN = {ASCII_SOH, ASCII_DLE, 0x36, ASCII_ETX};
    private static final ByteBuffer BUFFER = ByteBuffer.wrap(PATTERN);

    static StatusMessage create(final byte[] data) {
        if (Arrays.equals(data, PATTERN)) {
            return new StatusMessage();
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
        return "StatusMessage";
    }
}
