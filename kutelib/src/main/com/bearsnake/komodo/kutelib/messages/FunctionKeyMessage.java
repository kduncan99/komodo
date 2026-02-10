/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.exceptions.FunctionKeyException;
import com.bearsnake.komodo.kutelib.exceptions.InternalException;
import com.bearsnake.komodo.kutelib.uts.UTSByteBuffer;

import java.nio.ByteBuffer;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class FunctionKeyMessage implements UTSMessage {

    private final int _key;

    public FunctionKeyMessage(final int key) {
        if ((key < 1) || (key > 22)) {
            throw new InternalException("key must be between 1 and 22");
        }
        _key = key;
    }

    public int getKey() {
        return _key;
    }

    static FunctionKeyMessage create(final byte[] data) {
        if ((data.length == 3) && (data[0] == ASCII_SOH) && (data[2] == ASCII_ETX)) {
            switch (data[1]) {
                case 0x37 -> { return new FunctionKeyMessage(1); }
                case 0x47 -> { return new FunctionKeyMessage(2); }
                case 0x57 -> { return new FunctionKeyMessage(3); }
                case 0x67 -> { return new FunctionKeyMessage(4); }
                default -> {
                    if (data[1] >= 0x20 && data[1] <= 0x31) {
                        return new FunctionKeyMessage(data[1] - 0x20 + 5);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ByteBuffer getBuffer() {
        try {
            var bb = new UTSByteBuffer(16);
            bb.put(ASCII_SOH)
              .putFunctionKeyCode(_key)
              .put(ASCII_ETX);
            bb.setPointer(0);
            return ByteBuffer.wrap(bb.getBuffer());
        } catch (FunctionKeyException ex) {
            // program logic prevents this
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("FunctionKeyMessage F%d", _key);
    }
}
