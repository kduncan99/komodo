/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import java.util.ArrayList;

/**
 * TextParser for minalib library.
 * Splits a line of assembler code into fields and subfields.
 */
class TextParser {

    //  Source code, as lines of text in order of processing
    final ArrayList<TextLine> _sourceCodeSet = new ArrayList<>();

    //  Diagnostics, in order of generation (not necessarily in lineNumber/column order)
    final Diagnostics _diagnostics = new Diagnostics();

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param sourceCode lines of text to be parsed
     */
    TextParser(
        final String[] sourceCode
    ) {
        int lineNumber = 1;
        for (String sourceLine : sourceCode) {
            _sourceCodeSet.add(new TextLine(lineNumber++, sourceLine));
        }
    }

    /**
     * Invokes the parse function, which parses the entire source code set into a collection of TextField objects.
     * The results of this process can be obtained via getDiagnostics() and getSourceFields().
     */
    void parse(
    ) {
        int lineNumber = 1;
        for (TextLine textLine : _sourceCodeSet) {
            textLine.parseFields();
            if (textLine._diagnostics.hasFatal()) {
                break;
            }
        }
    }
}
