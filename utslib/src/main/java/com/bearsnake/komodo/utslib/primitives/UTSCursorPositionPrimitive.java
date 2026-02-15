/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.primitives;

import com.bearsnake.komodo.utslib.UTSByteBuffer;
import com.bearsnake.komodo.utslib.exceptions.UTSBufferOverflowException;
import com.bearsnake.komodo.utslib.exceptions.UTSCoordinateException;
import com.bearsnake.komodo.utslib.exceptions.UTSIncompleteEscapeSequenceException;
import com.bearsnake.komodo.utslib.exceptions.UTSInvalidEscapeSequenceException;

import static com.bearsnake.komodo.baselib.Constants.*;

public class UTSCursorPositionPrimitive extends UTSPrimitive {

    private final int _row;
    private final int _column;

    public UTSCursorPositionPrimitive(final int row,
                                      final int column) {
        super(UTSPrimitiveType.CURSOR_POSITION);
        _row = row;
        _column = column;
    }

    public int getRow() { return _row; }
    public int getColumn() { return _column; }

    public static UTSCursorPositionPrimitive deserializePrimitive(final UTSByteBuffer source)
        throws UTSCoordinateException,
               UTSIncompleteEscapeSequenceException,
               UTSInvalidEscapeSequenceException {
        try {
            if (source.getRemaining() < 2) {
                return null;
            }

            var pointer = source.getPointer();
            if ((source.getNext() != ASCII_ESC) || (source.getNext() != ASCII_VT)) {
                source.setPointer(pointer);
                return null;
            }

            var row = UTSPrimitive.deserializeCoordinate(source);
            var column = UTSPrimitive.deserializeCoordinate(source);
            if (source.getNext() != ASCII_SI) {
                throw new UTSInvalidEscapeSequenceException();
            }

            return new UTSCursorPositionPrimitive(row, column);
        } catch (UTSBufferOverflowException ex) {
            throw new UTSIncompleteEscapeSequenceException();
        }
    }

    @Override
    public void serialize(final UTSByteBuffer destination) throws UTSCoordinateException {
        destination.put(ASCII_ESC)
                   .put(ASCII_VT)
                   .putCoordinate(_row)
                   .putCoordinate(_column)
                   .put(ASCII_SI);
    }

    @Override
    public String toString() {
        return getType().getToken() + "{" + _row + "," + _column + "}";
    }
}
