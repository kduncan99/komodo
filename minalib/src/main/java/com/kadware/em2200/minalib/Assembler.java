/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.baselib.exceptions.NotFoundException;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;
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

    private boolean processMnemonic(
            final TextField labelField,
            final TextField operationField,
            final TextField operandField,
            final Diagnostics diagnostics
    ) {
        if ( operationField != null ) {
            TextSubfield mnemonicSubfield = operationField.getSubfield(0);
            String mnemonic = mnemonicSubfield.getText();
            InstructionWord.InstructionInfo iinfo;
            try {
                InstructionWord.Mode imode =
                        _context._codeMode == CodeMode.Extended ? InstructionWord.Mode.EXTENDED : InstructionWord.Mode.BASIC;
                iinfo = InstructionWord.getInstructionInfo(mnemonic, imode);
            } catch (NotFoundException ex) {
                //  Mnemonic not found - is it dependent on code mode?
                //  If so, coder is asking for a mnemonic in a mode it doesn't exist in; raise a diagnostic and
                //  return true so the assemble method doesn't go any further with this line.
                //  Otherwise, it's just flat not a mnemonic, so return false and let the assemble method do
                //  something else.
                try {
                    InstructionWord.Mode imode =
                        _context._codeMode == CodeMode.Extended ? InstructionWord.Mode.BASIC : InstructionWord.Mode.EXTENDED;
                    iinfo = InstructionWord.getInstructionInfo(mnemonic, imode);
                    diagnostics.append(new ErrorDiagnostic(mnemonicSubfield.getLocale(),
                                                           "Opcode not valid for the current code mode"));
                    return true;
                } catch (NotFoundException ex2) {
                    //  The mnemonic truly isn't found - return false, this is not an instruction operation
                    return false;
                }
            }

            //  If j-flag is set, we pull j-field from the iinfo object.  Otherwise, we interpret the j-field.
            int jField = 0;
            if (iinfo._jFlag) {
                jField = iinfo._jField;
                if (operationField.getSubfieldCount() > 1) {
                    diagnostics.append(new ErrorDiagnostic(operationField.getSubfield(1).getLocale(),
                                                          "Extraneous subfields in operation field"));
                }
            } else if (operationField.getSubfieldCount() > 1) {
                TextSubfield jSubField = operationField.getSubfield(1);
                try {
                    jField = InstructionWord.getJFieldValue(jSubField.getText());
                } catch ( NotFoundException e ) {
                    diagnostics.append(new ErrorDiagnostic(jSubField.getLocale(),
                                                           "Invalid text for j-field of instruction"));
                }

                if ( operationField.getSubfieldCount() > 2 ) {
                    diagnostics.append(new ErrorDiagnostic(
                        operationField.getSubfield(1).getLocale(),
                        "Extraneous subfields in operation field"));
                }
            }

            //TODO make sense of the operand field

            System.out.println(String.format("f=%02o j=%02o", iinfo._fField, jField));//TODO temporary
            return true;
        }

        return false;
    }

    /**
     * Assemble a single TextLine object into the Relocatable Module
     * @param textLine entity to be assembled
     */
    private void assemble(
        final TextLine textLine
    ) {
        if ( textLine._fields.size() == 0 ) {
            return;
        }

        TextField labelField = textLine.getField(0);
        TextField operationField = textLine.getField(1);
        TextField operandField = textLine.getField(2);

        if (processMnemonic(labelField, operationField, operandField, textLine._diagnostics)) {
            return;
        }

        Dictionary d = _context._dictionary;

        //  Examine field 0 which contains at most one subfield which we presume is a label.
        String label = null;
        int labelLevel = 0;
        if ( (labelField != null) && (labelField.getSubfieldCount() > 0) ) {
            TextSubfield labelSubfield = labelField.getSubfield(0);
            label = labelSubfield.getText();
            while ( label.endsWith("*") ) {
                label = label.substring(0, label.length() - 1);
                ++labelLevel;
            }

            if ( !Dictionary.isValidUserLabel(label) ) {
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

        //  Look up the opcode in the dictionary

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
