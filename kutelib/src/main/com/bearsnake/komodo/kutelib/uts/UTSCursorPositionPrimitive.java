/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.uts;

import com.bearsnake.komodo.kutelib.exceptions.BufferOverflowException;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.exceptions.IncompleteEscapeSequenceException;
import com.bearsnake.komodo.kutelib.exceptions.InvalidEscapeSequenceException;

import static com.bearsnake.komodo.kutelib.Constants.*;

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
        throws CoordinateException, IncompleteEscapeSequenceException, InvalidEscapeSequenceException {
        try {
            if (source.getRemaining() < 2) {
                return null;
            }

            var pointer = source.getPointer();
            if ((source.getNext() != ASCII_ESC) || (source.getNext() != ASCII_VT)) {
                source.setPointer(pointer);
                return null;
            }

            var row = deserializeCoordinate(source);
            var column = deserializeCoordinate(source);
            if (source.getNext() != ASCII_SI) {
                throw new InvalidEscapeSequenceException();
            }

            return new UTSCursorPositionPrimitive(row, column);
        } catch (BufferOverflowException ex) {
            throw new IncompleteEscapeSequenceException();
        }
    }

    @Override
    public void serialize(final UTSByteBuffer destination) throws CoordinateException {
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
