/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.textParser;

import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import java.util.ArrayList;

/**
 * TextParser for jkasm library.
 * Splits a line of assembler code into fields and subfields.
 */
public class TextParser {

    //  Source code, as lines of text in order of processing
    private final ArrayList<TextLine> _sourceCodeSet = new ArrayList<>();

    //  Diagnostics, in order of generation (not necessarily in lineNumber/column order)
    private final Diagnostics _diagnostics = new Diagnostics();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param sourceCode
     */
    public TextParser(
        final String[] sourceCode
    ) {
        _sourceCodeSet.clear();
        int lineNumber = 1;
        for (String sourceLine : sourceCode) {
            _sourceCodeSet.add(new TextLine(lineNumber++, sourceLine));
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public Diagnostics getDiagnostics(
    ) {
        return _diagnostics;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public ArrayList<TextLine> getSourceCodeSet(
    ) {
        return _sourceCodeSet;
    }

    /**
     * Invokes the parse function, which parses the entire source code set into a collection of TextField objects.
     * The results of this process can be obtained via getDiagnostics() and getSourceFields().
     */
    public void parse(
    ) {
        int lineNumber = 1;
        for (TextLine textLine : _sourceCodeSet) {
            textLine.parseFields();
            if (textLine.getDiagnostics().hasFatal()) {
                break;
            }
        }
    }
}
