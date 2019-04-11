/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.Dictionary;

/**
 * Assembler for minalib
 */
public class Assembler {

    private final Context _context = new Context();
    private final Diagnostics _diagnostics = new Diagnostics();
    private final RelocatableModule _module;
    private final TextLine[] _sourceCode;

    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    private void assemble(
        final TextLine textLine
    ) {
        if (textLine.getFieldCount() > 0) {
            if (textLine.getFieldCount() == 1) {
                //  Line has only a label
                establishLabel(textLine.getField(0).getSubfield(0));
            } else {
                //???? okay, what kind of thing are we doing here?
            }
        }
    }

    /**
     * Establishes the given subfield text as a label at the current location in the current location counter pool.
     * Does not allow an existing label to be overwritten.
     * @param subfield
     */
    private void establishLabel(
        final TextSubfield subfield
    ) {
        String rawText = subfield.getText();
        int level = 0;
        while (rawText.startsWith("*")) {
            ++level;
            rawText = rawText.substring(1);
        }

        Dictionary d = _context.getDictionary();
        if (d.hasValue(rawText)) {
            _diagnostics.append(new ErrorDiagnostic(subfield.getLocale(),
                                                    String.format("Label '%s' is already defined", rawText)));
        } else {
            //????
        }
    }

    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Create an Assembler object and load it with text from the given array of source lines
     * <p>
     * @param moduleName
     * @param source
     */
    public Assembler(
        final String moduleName,
        final String[] source
    ) {
        _module = new RelocatableModule(moduleName);
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
            assemble(line);
            if (_diagnostics.hasFatal()) {
                break;
            }
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
