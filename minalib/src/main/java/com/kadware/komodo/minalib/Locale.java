/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

/**
 * Represents a location of text within an assembler source set of text strings
 */
public class Locale {

    private final LineSpecifier _lineSpecifier;
    private final int _column;

    /**
     * Constructor
     * @param lineSpecifier indicates the nesting level and line number of this locale
     * @param column 1-based column number of the first character of this subfield's text
     */
    public Locale(
        final LineSpecifier lineSpecifier,
        final int column
    ) {
        _lineSpecifier = lineSpecifier;
        _column = column;
    }

    /**
     * check for equality
     * @param obj comparison object
     * @return true if objects are equal, else false
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Locale) {
            Locale loc = (Locale)obj;
            return ((loc._column == _column) && (loc._lineSpecifier.equals(_lineSpecifier)));
        }

        return false;
    }

    /**
     * Getter
     * @return column
     */
    public int getColumn(
    ) {
        return _column;
    }

    /**
     * Getter
     * @return level and line number
     */
    public LineSpecifier getLineSpecifier(
    ) {
        return _lineSpecifier;
    }

    /**
     * toString() override
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return String.format("[L%s.C%d]", _lineSpecifier.toString(), _column);
    }
}
