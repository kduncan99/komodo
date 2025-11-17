/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

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

    public int getRow() { return _row; }
    public int getColumn() { return _column; }

    public void set(final Coordinates coord) {
        _row = coord._row;
        _column = coord._column;
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
