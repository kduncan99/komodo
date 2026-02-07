/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.messages;

import com.bearsnake.komodo.kutelib.exceptions.BufferOverflowException;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.panes.Coordinates;

import java.nio.ByteBuffer;

import static com.bearsnake.komodo.kutelib.Constants.ASCII_ETX;
import static com.bearsnake.komodo.kutelib.Constants.ASCII_SOH;

/**
 * A CursorPositionMessage is sent by the terminal in response to an ESC 'T' sequence.
 * The convention states that this message does NOT contain SOH or ETX, but we insert those anyway
 * to make life simpler for reading messages.
 */
public class CursorPositionMessage implements UTSMessage {

    private final Coordinates _coordinates;

    public CursorPositionMessage(final Coordinates coordinates) {
        _coordinates = coordinates;
    }

    static CursorPositionMessage create(final byte[] data) {
        try {
            UTSByteBuffer bb = new UTSByteBuffer(data);
            if (bb.getNext() != ASCII_SOH) {
                return null;
            }
            var coordinates = bb.getCursorPosition();
            if (coordinates == null) {
                return null;
            }
            if (bb.getNext() != ASCII_ETX) {
                return null;
            }
            return new CursorPositionMessage(coordinates);
        } catch (BufferOverflowException | CoordinateException ex) {
            return null;
        }
    }

    @Override
    public ByteBuffer getBuffer() {
        try {
            var bb = new UTSByteBuffer(16);
            bb.put(ASCII_SOH).putCursorPositionSequence(_coordinates, true).put(ASCII_ETX);
            bb.setPointer(0);
            return ByteBuffer.wrap(bb.getBuffer());
        } catch (CoordinateException ex) {
            // should not be possible at this point
            return null;
        }
    }

    public Coordinates getCoordinates() {
        return _coordinates;
    }

    @Override
    public String toString() {
        return String.format("CursorPositionMessage %s", _coordinates);
    }
}
