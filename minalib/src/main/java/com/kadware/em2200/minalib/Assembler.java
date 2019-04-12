/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Map;

/**
 * Assembler for minalib
 */
public class Assembler {

    private final Context _context = new Context();
    private final Diagnostics _diagnostics = new Diagnostics();     //  cumulative of all textLine diagnostics
    private final RelocatableModule _module;
    private final TextLine[] _sourceCode;

    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Assemble a single TextLine object into the Relocatable Module
     * @param textLine entity to be assembled
     */
    private void assemble(
        final TextLine textLine
    ) {
        if (textLine._fields.size() == 0) {
            return;
        }

        //  Examine field 0 which contains at most one subfield which we presume is a label.
        String label = null;
        int labelLevel = 0;
        TextField labelField = textLine.getField(0);
        if ((labelField != null) && (labelField.getSubfieldCount() > 0)) {
            TextSubfield labelSubfield = labelField.getSubfield(0);
            label = labelSubfield.getText();
            while (label.endsWith("*")) {
                label = label.substring(0, label.length() - 1);
                ++labelLevel;
            }

            if (!Dictionary.isValidUserLabel(label)) {
                label = null;
                textLine._diagnostics.append(new ErrorDiagnostic(labelSubfield.getLocale(),
                                                                 "Invalid label"));
            } else {
                if ( labelField.getSubfieldCount() > 1 ) {
                    textLine._diagnostics.append(new ErrorDiagnostic(labelField.getSubfield(1).getLocale(),
                                                                     "Extraneous subfields in label field"));
                }

                //  If there are no further fields, then this is a stand-alone label.
                //  Process it as such...
                if ( textLine._fields.size() == 1 ) {
                    Dictionary d = _context._dictionary;
                    if ( d.hasValue(label) ) {
                        textLine._diagnostics.append(new DuplicateDiagnostic(labelSubfield.getLocale(),
                                                                             "Duplicate label"));
                    } else {
                        d.addValue(labelLevel, label, getCurrentLocation());
                    }
                }

                return;
            }
        }

        //TODO is this a mnemonic?

        //TODO Look up the opcode in the dictionary

        //TODO Is it an expression?
    }

    /**
     * Creates an IntegerValue object with appropriate reloc info, to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    private IntegerValue getCurrentLocation(
    ) {
        //  Find the current generation lc index.
        //  If it doesn't exist, it will be created.
        try {
            LocationCounterPool lcPool = _module.getLocationCounterPool(_context._currentGenerationLCIndex);
            LocationCounterRelocationInfo lcri =
                new LocationCounterRelocationInfo(FieldDescriptor.W, _context._currentGenerationLCIndex);
            return new IntegerValue.Builder().setValue(lcPool.getNextOffset())
                                             .setRelocationInfo(lcri)
                                             .build();
        } catch (InvalidParameterException ex) {
            throw new RuntimeException("Internal Error: Caught " + ex.getMessage());
        }
    }


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Create an Assembler object and load it with text from the given array of source lines
     * @param moduleName name of the module
     * @param source array of strings comprising the source code to be assembled
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
        _context._characterMode = CharacterMode.ASCII;
        _context._codeMode = CodeMode.Basic;
        _diagnostics.clear();

        //  First step - parse all the source code into fields/subfields.
        for (TextLine line : _sourceCode) {
            line.parseFields();
            if (line._diagnostics.hasFatal()) {
                return;
            }
            _diagnostics.append(line._diagnostics);
        }

        //  Next step - assemble all the things
        for (TextLine line : _sourceCode) {
            assemble(line);
            if (_diagnostics.hasFatal()) {
                return;
            }
            _diagnostics.append(line._diagnostics);
        }

        //???? TODO
    }

    /**
     * Displays output upon the console
     */
    public void displayResults(
    ) {
        for (TextLine line : _sourceCode) {
            System.out.println(String.format("%04d:%s", line._lineNumber, line._text));
            for (Diagnostic d : line._diagnostics.getDiagnostics()) {
                System.out.println(d.getMessage());
            }
        }

        System.out.println();
        System.out.println("Dictionary");
        for (String label : _context._dictionary.getLabels()) {
            try {
                Value value = _context._dictionary.getValue(label);
                System.out.println(String.format("%s: %s", label, value.toString()));
            } catch (NotFoundException ex) {
                //  can't happen
            }
        }

        System.out.println();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Summary: Lines=%d", _sourceCode.length));
        for (Map.Entry<Diagnostic.Level, Integer> entry : _diagnostics.getCounters().entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(String.format(" %c=%d", Diagnostic.getLevelIndicator(entry.getKey()), entry.getValue()));
            }
        }
        System.out.println(sb.toString());
    }

    /**
     * Getter
     * @return Diagnostics object produced during assembly
     */
    public Diagnostics getDiagnostics(
    ) {
        return _diagnostics;
    }

    /**
     * Getter
     * @return array of TextLine objects comprising the source code
     */
    public TextLine[] getParsedCode(
    ) {
        return _sourceCode;
    }

    /**
     * Getter
     * @return RelocatableModule object produced as a result of the assemble() method
     */
    public RelocatableModule getRelocatableModule(
    ) {
        return _module;
    }
}
