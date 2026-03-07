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

public class CursorPositionPrimitive extends Primitive {

    private final int _row;
    private final int _column;

    public CursorPositionPrimitive(final int row,
                                   final int column) {
        super(PrimitiveType.CURSOR_POSITION);
        _row = row;
        _column = column;
    }

    public int getRow() { return _row; }
    public int getColumn() { return _column; }

    public static CursorPositionPrimitive deserializePrimitive(final UTSByteBuffer source)
        throws UTSCoordinateException,
               UTSIncompleteEscapeSequenceException,
               UTSInvalidEscapeSequenceException {
        try {
            if (source.getRemaining() < 2) {
                return null;
            }

            var pointer = source.getIndex();
            if ((source.getNext() != ASCII_ESC) || (source.getNext() != ASCII_VT)) {
                source.setIndex(pointer);
                return null;
            }

            var row = Primitive.deserializeCoordinate(source);
            var column = Primitive.deserializeCoordinate(source);
            if (source.getNext() != ASCII_SI) {
                throw new UTSInvalidEscapeSequenceException();
            }

            return new CursorPositionPrimitive(row, column);
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
