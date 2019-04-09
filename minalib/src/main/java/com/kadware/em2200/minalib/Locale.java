/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */
package com.kadware.em2200.minalib;

/**
 * Represents a location of text within an assembler source set of text strings
 */
public class Locale {

    private final int _lineNumber;
    private final int _column;

    /**
     * Constructor
     * <p>
     * @param lineNumber 1-based line number of the containing line, within the source code set, of this subfield's text
     * @param column 1-based column number of the first character of this subfield's text
     */
    public Locale(
        final int lineNumber,
        final int column
    ) {
        _lineNumber = lineNumber;
        _column = column;
    }

    /**
     * check for equality
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Locale) {
            Locale loc = (Locale)obj;
            return ((loc._column == _column) && (loc._lineNumber == _lineNumber));
        }

        return false;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getColumn(
    ) {
        return _column;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getLineNumber(
    ) {
        return _lineNumber;
    }

    /**
     * toString() override
     * <p>
     * @return
     */
    @Override
    public String toString(
    ) {
        return String.format("[%d.%d]", _lineNumber, _column);
    }
}
