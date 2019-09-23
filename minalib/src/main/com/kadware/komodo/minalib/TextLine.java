/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.minalib.diagnostics.Diagnostic;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.minalib.diagnostics.QuoteDiagnostic;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a line of source code
 */
public class TextLine {

    //  line number of this line of text
    public final LineSpecifier _lineSpecifier;

    //  source code for this line of text
    public final String _text;

    //  parsed TextField objects parsed from the line of text - may be empty
    public final ArrayList<TextField> _fields = new ArrayList<>();

    //  References to all Diagnostic and GeneratedWord objects created for this line of text.
    //  Only applies to top-level TextLine objects (not to temporary func or proc objects)
    public final List<Diagnostic> _diagnostics = new LinkedList<>();
    public final List<GeneratedWord> _generatedWords = new LinkedList<>();

    /**
     * Constructor
     * @param lineSpecifier level/line number of this object
     * @param text original text for this object
     */
    TextLine(
        final LineSpecifier lineSpecifier,
        final String text
    ) {
        _lineSpecifier = lineSpecifier;
        _text = text;
    }

    /**
     * Getter
     * @param index index of field
     * @return text field
     */
    TextField getField(
        final int index
    ) {
        if (index < _fields.size()) {
            return _fields.get(index);
        }
        return null;
    }

    /**
     * Parses the text into TextField objects
     */
    void parseFields(
        final Diagnostics diagnostics
    ) {
        //  We should only ever be called once, but just in case...
        _fields.clear();

        //  Create clean text string by removing all commentary and trailing whitespace.
        String cleanText = removeComments(_text);

        //  Get ready for iterating character-by-character over the line of code
        int tx = 0;

        //  If first character of the clean text is whitespace, skip all the whitespace,
        //  and generate a null entry in the TextField array list.
        if (!cleanText.isEmpty() && (cleanText.charAt(tx) == ' ')) {
            ++tx;
            while ((tx < cleanText.length()) && (cleanText.charAt(tx) == ' ')) {
                ++tx;
            }
            _fields.add(null);
        }

        //  Now we start parsing the clean text into fields, delimited by occurrences of one or more blank characters.
        //  The presumption is, at the top of the while loop, tx does NOT index a field-delimiting blank character.
        //  Note that we do NOT scan for syntax errors here - in particular, we are pretty lax with where parenthesis
        //  and especially quote delimiters are located.  As long as they are balanced properly, we are happy.
        int parenLevel = 0;
        boolean prevComma = false;
        boolean prevSign = false;
        boolean quoted = false;
        StringBuilder sb = new StringBuilder();
        Locale locale = new Locale(_lineSpecifier, tx + 1);
        while (tx < cleanText.length()) {
            char ch = cleanText.charAt(tx++);

            if (quoted) {
                sb.append(ch);
            } else {
                if ((parenLevel == 0) && (ch == ' ')) {
                    //  We have found an unquoted blank.  If the previous character was a comma,
                    //  this is whitespace embedded within a field, and should *not* terminate the field.
                    if (prevComma) {
                        sb.append(ch);
                        while ((tx < cleanText.length()) && (cleanText.charAt(tx) == ' ')) {
                            sb.append(' ');
                            ++tx;
                        }
                    } else if (prevSign) {
                        //  This is incidental whitespace following a unary prefix sign character.
                        //  Skip it, and any subsequent contiguous whitespace.
                        sb.append(ch);
                        while ((tx < cleanText.length()) && (cleanText.charAt(tx) == ' ')) {
                            sb.append(' ');
                            ++tx;
                        }
                    } else {
                        //  We've reached the end of the current field.
                        //  Create a TextField object, then parse the field text into subfields.
                        String fieldText = sb.toString().trim();
                        TextField field = new TextField(locale, fieldText);
                        _fields.add(field);
                        Diagnostics parseDiags = field.parseSubfields();
                        diagnostics.append(parseDiags);

                        //  Skip field-delimiting whitespace
                        while ((tx < cleanText.length()) && (cleanText.charAt(tx) == ' ')) {
                            ++tx;
                        }
                        sb = new StringBuilder();
                        locale = new Locale(_lineSpecifier, tx + 1);
                    }
                } else {
                    sb.append(ch);

                    //  Is this a leading '+' or '-'?  If so, skip whitespace
                    if ((sb.length() ==1) && ((ch == '+') || (ch == '-'))) {
                        while ((tx < cleanText.length()) && (cleanText.charAt(tx) == ' ')) {
                            sb.append(" ");
                            ++tx;
                        }
                    }

                    //  Handle opening-closing parentheses
                    if (ch == '(') {
                        ++parenLevel;
                    } else if (ch == ')') {
                        if (parenLevel == 0) {
                            Locale diagLoc = new Locale(_lineSpecifier, tx);
                            diagnostics.append(new ErrorDiagnostic(diagLoc, "Too many closing parentheses"));
                            return;
                        }
                        --parenLevel;
                    }
                }
            }

            if (ch == '\'') {
                quoted = !quoted;
            }

            prevComma = (ch == ',');
            prevSign = (sb.length() == 1) && ((ch == '+') || (ch == '-'));
        }

        String fieldText = sb.toString();
        fieldText = fieldText.trim();
        if (!fieldText.isEmpty()) {
            TextField field = new TextField(locale, fieldText);
            _fields.add(field);
            Diagnostics parseDiags = field.parseSubfields();
            diagnostics.append(parseDiags);
        }

        Locale endloc = new Locale(_lineSpecifier, tx - 1);
        if (quoted) {
            diagnostics.append(new QuoteDiagnostic(endloc, "Unterminated string"));
        }

        if (parenLevel > 0) {
            diagnostics.append(new ErrorDiagnostic(endloc, "Unterminated parenthesized expression"));
        }
    }

    /**
     * Remove comment from assembler text, if it exists.
     * Comments are signaled by space-period-space, or a period-space in the first two columns,
     * or a space-period in the last two columns, or by a single column containing a space.
     * @param text input text to be scanned
     * @return copy of input text with commentary removed, or the original text if no comment was found.
     */
    static String removeComments(
        final String text
    ) {
        if (text.equals(".") || text.startsWith(". ")) {
            return "";
        }

        boolean quoted = false;
        boolean prevSpace = true;
        int tx = 0;
        while (tx < text.length()) {
            char ch = text.charAt(tx++);
            if (!quoted) {
                if ((ch == '.') && prevSpace) {
                    if ((tx == text.length()) || (text.charAt(tx) == ' ')) {
                        return text.substring(0, tx - 2);
                    }
                }
            }

            prevSpace = (ch == ' ');
            if (ch == '\'') {
                quoted = !quoted;
            }
        }

        //  No comment found
        return text;
    }
}
