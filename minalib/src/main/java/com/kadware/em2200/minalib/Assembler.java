/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.textParser.*;

/**
 * Assembler for minalib
 */
public class Assembler {

    private final Context _context = new Context();
    private final Diagnostics _diagnostics = new Diagnostics();
    private final TextLine[] _sourceCode;

    /**
     * Create an Assembler object and load it with text from the given array of source lines
     * <p>
     * @param source
     */
    public Assembler(
        final String[] source
    ) {
        _sourceCode = new TextLine[source.length];
        for (int sx = 0; sx < source.length; ++sx) {
            int lineNumber = sx + 1;
            _sourceCode[sx] = new TextLine(lineNumber, source[sx]);
        }
    }

    /**
     * Assemble the source code in the object.
     * We do not do things quite in the same way as MASM.
     */
    public void assemble(
    ) {
        //  setup
        _context.setCharacterMode(CharacterMode.ASCII);
        _context.setCodeMode(CodeMode.Basic);
        _diagnostics.clear();

        //  First step - parse all the source code into fields/subfields.
        for (TextLine line : _sourceCode) {
            line.parseFields();
            if (line.getDiagnostics().hasFatal()) {
                return;
            }

            _diagnostics.append(line.getDiagnostics());
        }

        //  Next step - assemble all the things
        for (TextLine line : _sourceCode) {
            //????
        }
    }

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
    public TextLine[] getParsedCode(
    ) {
        return _sourceCode;
    }
}
