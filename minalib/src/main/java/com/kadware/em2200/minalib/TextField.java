/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
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
    private final Locale _locale;

    //  text of this field, with leading/trailing blanks removed
    private final String _text;

    //  Subfield objects which comprise this field
    private final ArrayList<TextSubfield> _subfields = new ArrayList<>();

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods (may be protected for unit testing)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Advances a text index from its current position to a further position to effectively skip any whitespace.
     * <p>
     * @param text
     * @param index
     * <p>
     * @return
     */
    protected static int skipWhiteSpace(
        final String text,
        final int index
    ) {
        int tx = index;
        while ((tx < text.length()) && (text.charAt(tx) == ' ')) {
            ++tx;
        }
        return tx;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param locale location of this subfield of text within the source code set
     * @param text text of this field, including all the textual tokens of all the contained subfields
     *              as well as (potentially) embedded blanks following commas
     */
    public TextField(
        final Locale locale,
        final String text
    ) {
        _locale = locale;
        _text = text;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

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
     * Retrieves a locale representing the lineNumber/column of the position
     * immediately following this field in the source code.
     * @return
     */
    public Locale getLocaleLimit(
    ) {
        return new Locale(_locale.getLineNumber(), _locale.getColumn() + _text.length());
    }

    /**
     * Retrieves a particular TextSubfield object by index
     * <p>
     * @param index
     * <p>
     * @return TextSubfield object if it exists, null if there is no subfield at the given index
     */
    public TextSubfield getSubfield(
        final int index
    ) {
        if (index < _subfields.size()) {
            return _subfields.get(index);
        }
        return null;
    }

    /**
     * Retrieves the number of TextSubfield object that exist, plus any void spaces within the array.
     * That is, if subfields exist at indices 0, 1, and 3, we return 4.
     * <p>
     * @return
     */
    public int getSubfieldCount(
    ) {
        return _subfields.size();
    }

    /**
     * Parses the text comprising this TextField into TextSubfield objects
     * <p>
     * @return Diagnostics object containing any diagnostics generated during the parse procedure
     */
    public Diagnostics parseSubfields(
    ) {
        Diagnostics diagnostics = new Diagnostics();
        _subfields.clear();

        Locale locale = new Locale(_locale.getLineNumber(), _locale.getColumn());
        int parenLevel = 0;
        boolean quoted = false;
        int tx = 0;
        StringBuilder sb = new StringBuilder();
        while (tx < _text.length()) {
            //  We presume here, that the index is not sitting on whitespace
            char ch = _text.charAt(tx++);
            if (quoted) {
                sb.append(ch);
            } else {
                if ((parenLevel == 0) && (ch == ',')) {
                    String sfText = sb.toString();
                    if (sfText.isEmpty()) {
                        //  empty subfield - create a null entry in the array
                        _subfields.add(null);
                    } else {
                        //  non-empty subfield - create a new TextSubfield object and append it to the array
                        boolean flagged = sfText.startsWith("*");
                        if (flagged) {
                            sfText = sfText.substring(1);
                        }

                        TextSubfield subfield = new TextSubfield(locale, flagged, sfText);
                        _subfields.add(subfield);
                    }

                    //  Comma delimiter may be followed by whitespace for clarity in coding.  Skip such whitespace.
                    tx = skipWhiteSpace(_text, tx);
                    sb = new StringBuilder();
                    locale = new Locale(_locale.getLineNumber(), _locale.getColumn() + tx);
                } else {
                    sb.append(ch);

                    //  Handle opening-closing parentheses
                    if (ch == '(') {
                        ++parenLevel;
                    } else if (ch == ')') {
                        if (parenLevel == 0) {
                            Locale diagLoc = new Locale(locale.getLineNumber(), tx);
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

        return diagnostics;
    }
}
