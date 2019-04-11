/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

/**
 * Represents a (possibly empty) subfield of text, parsed from a line of assembly code.
 */
class TextSubfield {

    private final boolean _flagged;     //  true if the subfield is prefixed with an asterisk
    private final Locale _locale;       //  linenumber/column of this subfield within the source code set
    private final String _text;         //  text of the field, not including the prefixing asterisk (if it exists)

    /**
     * Constructor
     * @param locale location of this subfield of text within the source code set
     * @param flagged true if this subfield is prefixed with an asterisk, else false
     * @param text text of this subfield, not including the prefix asterisk (if it exists)
     */
    TextSubfield(
        final Locale locale,
        final boolean flagged,
        final String text
    ) {
        _flagged = flagged;
        _locale = locale;
        _text = text;
    }

    /**
     * Getter
     * @return locale of this subfield
     */
    public Locale getLocale(
    ) {
        return _locale;
    }

    /**
     * Getter
     * @return raw text for this subfield
     */
    public String getText(
    ) {
        return _text;
    }

    /**
     * Getter
     * @return true if this subfield is flagged, else false
     */
    boolean isFlagged(
    ) {
        return _flagged;
    }

    /**
     * Retrieves a locale representing the lineNumber/column of the position
     * immediately following this subfield in the source code.
     * @return locale per above description
     */
    Locale getLocaleLimit(
    ) {
        return new Locale(_locale.getLineNumber(), _locale.getColumn() + _text.length());
    }
}
