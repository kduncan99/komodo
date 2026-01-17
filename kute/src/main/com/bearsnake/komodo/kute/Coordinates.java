/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import com.bearsnake.komodo.kute.exceptions.CoordinateException;

import java.io.ByteArrayOutputStream;

import static com.bearsnake.komodo.kute.Constants.*;
import static com.bearsnake.komodo.kute.Constants.ASCII_SI;

public class Coordinates implements Comparable<Coordinates> {

    public static final Coordinates HOME_POSITION = new Coordinates(1, 1);

    private int _row;
    private int _column;

    public Coordinates(final int row, final int column) {
        _row = row;
        _column = column;
    }

    public Coordinates copy() {
        return new Coordinates(_row, _column);
    }

    /**
     * Indicates whether the coordinates are at the home position
     * @return true if row and column values are both 1
     */
    public boolean atHome() {
        return _row == 1 && _column == 1;
    }

    /**
     * Emits a coordinate (a row or a column, one-biased) as a one- or two- byte sequence
     * according to the following convention:
     * For coordinate {n} 1 to 80, we emit [ n + 0x1F ].
     * For coordinates > 80, we emit [ 0x75 + n1 ] [ 0x70 + n2 ] where
     *  n1 is the most significant 4 bits of (n - 80), and n2 is the least significant 4 bits of (n - 80).
     * Coordinates can vary from 1 to 256.
     */
    private static void emitCoordinateTo(final ByteArrayOutputStream strm, final int coordinate) {
        if (coordinate <= 80) {
            strm.write((byte) (coordinate + 31));
        } else {
            var slop = coordinate - 81;
            strm.write((slop >> 4) + 0x75);
            strm.write((byte) (slop & 0x0F));
        }
    }

    /**
     * Emits a Coordinates object as two-, three-, or four-byte sequence
     * according to convention... row first, then column.
     */
    public void emitTo(final ByteArrayOutputStream strm) {
        strm.write(ASCII_ESC);
        strm.write(ASCII_VT);
        emitCoordinateTo(strm, _row);
        emitCoordinateTo(strm, _column);
        strm.write(ASCII_NUL);
        strm.write(ASCII_SI);
    }

    /**
     * Reads the next one or two bytes from the given StreamBuffer, converting it to a
     * row or column coordinate ranging from 1 to 256 (see emitCoordinateTo()).
     */
    private static int ingestCoordinateFrom(final StreamBuffer strm) throws CoordinateException {
        if (strm.atEnd()) {
            throw new CoordinateException("Incomplete or missing coordinate");
        }
        var ch = strm.get();
        if ((ch >= 0x20) && (ch <= 0x6f)) {
            return (ch - 0x20 + 1);
        } else if (ch >= 0x75) {
            if (strm.atEnd()) {
                throw new CoordinateException("Incomplete or missing coordinate");
            }
            var ch2 = strm.get();
            if (ch2 >= 70) {
                return 81 + ((ch - 0x75) << 4) + (ch2 & 0x0F);
            }
        }
        throw new CoordinateException("Invalid coordinate");
    }

    /**
     * Reads the next two, three, or four bytes from the given StreamBuffer, converting them to
     * a Coordinates object according to convention (see emitTo()).
     */
    public static Coordinates ingestFrom(final StreamBuffer strm) throws CoordinateException {
        return new Coordinates(ingestCoordinateFrom(strm), ingestCoordinateFrom(strm));
    }

    public int getRow() { return _row; }
    public int getColumn() { return _column; }

    public void set(final Coordinates coord) {
        _row = coord._row;
        _column = coord._column;
    }

    public void set(final int row, final int column) {
        _row = row;
        _column = column;
    }

    public void setRow(final int row) { _row = row; }
    public void setColumn(final int column) { _column = column; }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof Coordinates c) && (c._column == _column) && (c._row == _row);
    }

    @Override
    public int hashCode() {
        return (_row << 8) | _column;
    }

    @Override
    public String toString() {
        return String.format("[%d.%d]", _row, _column);
    }

    @Override
    public int compareTo(Coordinates o) {
        if (_row > o._row) {
            return 1;
        } else if (_row < o._row) {
            return -1;
        } else {
            if (_column > o._column) {
                return 1;
            } else if (_column < o._column) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
