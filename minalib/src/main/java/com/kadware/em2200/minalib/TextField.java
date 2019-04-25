/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import java.util.ArrayList;

/**
 * Represents a (possibly empty) field of text, parsed from a line of assembly code.
 */
public class TextField {

    //  linenumber and column of this field
    public final Locale _locale;

    //  text of this field, with leading/trailing blanks removed
    public final String _text;

    //  Subfield objects which comprise this field
    public final ArrayList<TextSubfield> _subfields = new ArrayList<>();

    /**
     * Constructor
     * @param locale location of this subfield of text within the source code set
     * @param text text of this field, including all the textual tokens of all the contained subfields
     *              as well as (potentially) embedded blanks following commas
     */
    TextField(
        final Locale locale,
        final String text
    ) {
        _locale = locale;
        _text = text;
    }

    /**
     * Adds a subfield to this field, at the given columne
     * @param text text of the subfield
     * @param column column of the subfield
     */
    private void addSubfield(
        final String text,
        final int column
    ) {
        Locale loc = new Locale(_locale.getLineNumber(), column);
        _subfields.add(new TextSubfield(loc, text));
    }

    /**
     * Retrieves a locale representing the lineNumber/column of the position
     * immediately following this field in the source code.
     * @return locale entity reflecting the position following this field
     */
    Locale getLocaleLimit(
    ) {
        return new Locale(_locale.getLineNumber(), _locale.getColumn() + _text.length());
    }

    /**
     * Retrieves a particular TextSubfield object by index
     * @param index indicates which subfield to retrieve
     * @return TextSubfield object if it exists, null if there is no subfield at the given index
     */
    TextSubfield getSubfield(
        final int index
    ) {
        if (index < _subfields.size()) {
            return _subfields.get(index);
        }
        return null;
    }

    /**
     * Parses the text comprising this TextField into TextSubfield objects
     * <p>
     * @return Diagnostics object containing any diagnostics generated during the parse procedure
     */
    Diagnostics parseSubfields(
    ) {
        Diagnostics diagnostics = new Diagnostics();
        _subfields.clear();

        int parenLevel = 0;
        boolean quoted = false;
        int tx = 0;
        int baseColumn = _locale.getColumn();
        StringBuilder sb = new StringBuilder();
        while (tx < _text.length()) {
            //  We presume here, that the index is not sitting on whitespace
            char ch = _text.charAt(tx++);
            if (quoted) {
                sb.append(ch);
            } else {
                if ((parenLevel == 0) && (ch == ',')) {
                    addSubfield(sb.toString(), baseColumn);

                    //  Comma delimiter may be followed by whitespace for clarity in coding.  Skip such whitespace.
                    tx = skipWhiteSpace(_text, tx);
                    baseColumn = _locale.getColumn() + tx;
                    sb = new StringBuilder();
                } else {
                    sb.append(ch);

                    //  Handle opening-closing parentheses
                    if (ch == '(') {
                        ++parenLevel;
                    } else if (ch == ')') {
                        if (parenLevel == 0) {
                            Locale diagLoc = new Locale(_locale.getLineNumber(), baseColumn);
                            diagnostics.append(new ErrorDiagnostic(diagLoc, "Too many closing parentheses"));
                            return diagnostics;
                        }
                        --parenLevel;
                    }
                }
            }

            if (ch == '\'') {
                quoted = !quoted;
            }
        }

        addSubfield(sb.toString(), baseColumn);
        return diagnostics;
    }

    /**
     * Advances a text index from its current position to a further position to effectively skip any whitespace.
     * @param text base text
     * @param index starting index
     * @return ending index
     */
    static int skipWhiteSpace(
        final String text,
        final int index
    ) {
        int tx = index;
        while ((tx < text.length()) && (text.charAt(tx) == ' ')) {
            ++tx;
        }
        return tx;
    }
}
