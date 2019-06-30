/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

/**
 * A specification of a level and line number to indicate where a parsed line was generated from
 */
public class LineSpecifier {
    private final int _level;
    private final int _lineNumber;

    public LineSpecifier(
        final int level,
        final int lineNumber
    ) {
        _level = level;
        _lineNumber = lineNumber;
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        return ((obj instanceof LineSpecifier
                 && ((LineSpecifier) obj)._level == _level)
                && (((LineSpecifier) obj)._lineNumber == _lineNumber));
    }

    @Override
    public String toString() {
        return String.format("%d.%6d", _level, _lineNumber);
    }
}
