/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.textParser;

import com.kadware.em2200.minalib.Locale;

/**
 * Represents a (possibly empty) subfield of text, parsed from a line of assembly code.
 */
public class TextSubfield {

    private final boolean _flagged;     //  true if the subfield is prefixed with an asterisk
    private final Locale _locale;       //  linenumber/column of this subfield within the source code set
    private final String _text;       //  text of the field, not including the prefixing asterisk (if it exists)

    /**
     * Constructor
     * <p>
     * @param locale location of this subfield of text within the source code set
     * @param flagged true if this subfield is prefixed with an asterisk, else false
     * @param text text of this subfield, not including the prefix asterisk (if it exists)
     */
    public TextSubfield(
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
     * <p>
     * @return
     */
    public Locale getLocale(
    ) {
        return _locale;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public String getText(
    ) {
        return _text;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean isFlagged(
    ) {
        return _flagged;
    }

    /**
     * Retrieves a locale representing the lineNumber/column of the position
     * immediately following this subfield in the source code.
     * @return
     */
    public Locale getLocaleLimit(
    ) {
        return new Locale(_locale.getLineNumber(), _locale.getColumn() + _text.length());
    }
}
