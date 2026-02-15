/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.bearsnake.komodo.baselib.Constants.*;

/**
 * A DisconnectMessage is sent from either end of a UTS session to indicate that the session should be terminated.
 */
public class DisconnectMessage implements UTSMessage {

    private static final byte[] PATTERN = {ASCII_SOH, ASCII_DLE, ASCII_EOT, ASCII_STX, ASCII_ETX};
    private static final ByteBuffer BUFFER = ByteBuffer.wrap(PATTERN);

    static DisconnectMessage create(final byte[] data) {
        if (Arrays.equals(data, PATTERN)) {
            return new DisconnectMessage();
        } else {
            return null;
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return BUFFER.duplicate();
    }

    @Override
    public String toString() {
        return "DisconnectMessage";
    }
}
