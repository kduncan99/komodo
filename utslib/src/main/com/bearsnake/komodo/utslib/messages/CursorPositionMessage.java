/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.messages;

import com.bearsnake.komodo.utslib.Coordinates;
import com.bearsnake.komodo.utslib.UTSByteBuffer;
import com.bearsnake.komodo.utslib.exceptions.UTSBufferOverflowException;
import com.bearsnake.komodo.utslib.exceptions.UTSCoordinateException;
import com.bearsnake.komodo.utslib.exceptions.UTSIncompleteEscapeSequenceException;
import com.bearsnake.komodo.utslib.exceptions.UTSInvalidEscapeSequenceException;
import com.bearsnake.komodo.utslib.primitives.UTSCursorPositionPrimitive;

import java.nio.ByteBuffer;
import static com.bearsnake.komodo.baselib.Constants.*;

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

    /**
     * Attempts to create a CursorPositionMessage from the given data.
     * @param data must begin with SOH STX and end with ETX
     * @return CursorPositionMessage object, or null if the data is not recognized as a complete and valid message
     */
    static CursorPositionMessage create(final byte[] data) {
        try {
            UTSByteBuffer bb = new UTSByteBuffer(data);
            if (bb.getNext() != ASCII_SOH) {
                return null;
            }

            if (bb.getNext() != ASCII_STX) {
                return null;
            }

            var prim = UTSCursorPositionPrimitive.deserializePrimitive(bb);
            if (prim == null) {
                return null;
            }

            if (bb.getNext() != ASCII_ETX) {
                return null;
            }

            return new CursorPositionMessage(new Coordinates(prim.getRow(), prim.getColumn()));
        } catch (UTSBufferOverflowException
                 | UTSCoordinateException
                 | UTSIncompleteEscapeSequenceException
                 | UTSInvalidEscapeSequenceException ex) {
            return null;
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        try {
            var prim = new UTSCursorPositionPrimitive(_coordinates.getRow(), _coordinates.getColumn());
            var bb = new UTSByteBuffer(16);
            bb.put(ASCII_SOH).put(ASCII_STX);
            prim.serialize(bb);
            bb.put(ASCII_ETX);
            bb.setPointer(0);
            return ByteBuffer.wrap(bb.getBuffer());
        } catch (UTSCoordinateException ex) {
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
