/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

public class DisplayGeometry {

    private final int _rows;
    private final int _columns;

    public DisplayGeometry(final int rows,
                           final int columns) {
        _rows = rows;
        _columns = columns;
    }

    public DisplayGeometry(final DisplayGeometry geometry) {
        _rows = geometry.getRows();
        _columns = geometry.getColumns();
    }

    public int getColumns() {
        return _columns;
    }

    public int getRows() {
        return _rows;
    }

    public int getCellCount() {
        return _rows * _columns;
    }
}
