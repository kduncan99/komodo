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
import org.omg.CosNaming.NamingContextPackage.NotFound;

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
     * Interprets a subfield as an expression
     * @param subfield subfield to be interpreted
     * @param diagnostics where we post any appropriate diagnostics
     * @return interpreted value (may be zero with no other information)
     */
    private Value interpretSubfield(
        final TextSubfield subfield,
        final Diagnostics diagnostics
    ) {
        try {
            ExpressionParser p = new ExpressionParser( subfield.getText(), subfield.getLocale() );
            Expression e = p.parse( _context, diagnostics );
            return e.evaluate( _context, diagnostics );
        } catch ( ExpressionException eex ) {
            diagnostics.append( new ErrorDiagnostic( subfield.getLocale(),
                                                     "Cannot evaluate expression:" + eex.getMessage() ) );
            return new IntegerValue.Builder().build();
        } catch ( NotFoundException nfex ) {
            diagnostics.append( new ErrorDiagnostic( subfield.getLocale(),
                                                     "Expected an expression" ) );
            return new IntegerValue.Builder().build();
        }
    }

    /**
     * For textlines which contain a label intended to actually *be* a label - that is, to represent the current
     * address in the current generation location counter...
     * @param labelField label field
     * @param diagnostics where we post any appropriate diagnostics
     */
    private void processLabel(
        final TextField labelField,
        final Diagnostics diagnostics
    ) {
        if ( (labelField != null) && (labelField.getSubfieldCount() > 0) ) {
            TextSubfield labelSubfield = labelField.getSubfield(0);
            String label = labelSubfield.getText();
            int labelLevel = 0;
            while ( label.endsWith("*") ) {
                label = label.substring(0, label.length() - 1);
                ++labelLevel;
            }

            if ( !Dictionary.isValidUserLabel(label) ) {
                label = null;
                diagnostics.append(new ErrorDiagnostic(labelSubfield.getLocale(),
                                                       "Invalid label"));
            } else {
                if ( labelField.getSubfieldCount() > 1 ) {
                    diagnostics.append(new ErrorDiagnostic(labelField.getSubfield(1).getLocale(),
                                                           "Extraneous subfields in label field"));
                }

                if ( _context._dictionary.hasValue(label) ) {
                    diagnostics.append(new DuplicateDiagnostic(labelSubfield.getLocale(),
                                                               "Duplicate label"));
                } else {
                    _context._dictionary.addValue(labelLevel, label, getCurrentLocation());
                }
            }
        }
    }

    /**
     * Handles instruction mnemonic lines of code, and blank lines
     * @param labelField represents the label field, if any
     * @param operationField represents the operation field, if any
     * @param operandField represents the operand field, if any
     * @param diagnostics where we post diagnostics if needed
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processMnemonic(
        final TextField labelField,
        final TextField operationField,
        final TextField operandField,
        final Diagnostics diagnostics
    ) {
        if ( operationField == null ) {
            //  This is a no-op line - but it might have a label.
            //  Do label stuff, then return true indicating that the line has been processed.
            processLabel(labelField, diagnostics);
            return true;
        }

        //  Deal with the operation field
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

        //  We've found an iinfo - from here on, we are sure this is a mnemonic text line.
        //  It might have all kinds of errors in it (or not), but it's a mnemonic line.
        //  So, a) the label field is to be processed as such, and b) we return true from here onward.
        processLabel(labelField, diagnostics);

        //  Is this a special instruction? (such as JGD, BT, etc)
        //TODO

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

        //  Deal with the operand field
        int aField = 0;
        Value operandValue = null;
        if (operandField == null) {
            diagnostics.append(new ErrorDiagnostic( operationField.getLocale(), "Mnemonic requires an operand field"));
        } else {
            //  Find the subfields... if iinfo's a-flag is set, then the iinfo a-field is used for the instruction
            //  a field, and there isn't one in the syntax for the operand field.
            //  If the flag is clear, then the first subfield is a register specification... which can get a bit
            //  complicated as well, since it might be an a-register, an x-register, or an r-register (or even a b...)
            TextSubfield registerSubField = null;
            TextSubfield valueSubField = null;
            TextSubfield indexSubField = null;
            TextSubfield baseSubField = null;

            boolean baseSubFieldAllowed = (_context._codeMode == CodeMode.Extended) && !iinfo._useBMSemantics;
            int sfx = 0;
            int sfc = operandField.getSubfieldCount();
            if (!iinfo._aFlag && (sfc > sfx)) {
                registerSubField = operandField.getSubfield( sfx++ );
            }
            if (sfc > sfx) {
                valueSubField = operandField.getSubfield( sfx++ );
            }
            if (sfc > sfx) {
                indexSubField = operandField.getSubfield( sfx++ );
            }
            if ((sfc > sfx) && baseSubFieldAllowed) {
                baseSubField = operandField.getSubfield( sfx++ );
            }

            if (sfc > sfx) {
                diagnostics.append( new ErrorDiagnostic( operandField.getSubfield( sfx ).getLocale(),
                                                         "Extreanous subfields in operand field ignored") );
            }

            //  Interpret the subfields
            if ((registerSubField == null) || (registerSubField.getText().isEmpty())) {
                diagnostics.append( new ErrorDiagnostic( operandField.getLocale(),
                                                         "Missing register specification" ) );
            } else {
                try {
                    switch (iinfo._aSemantics) {
                        case A:
                            aField = GeneralRegisterSet.getGRSIndex( registerSubField.getText().toUpperCase() ) - 12;
                            break;
                        case R:
                            aField = GeneralRegisterSet.getGRSIndex( registerSubField.getText().toUpperCase() ) - 64;
                            break;
                        case X:
                            aField = GeneralRegisterSet.getGRSIndex( registerSubField.getText().toUpperCase() ) ;
                            break;
                        case B:
                        case B_EXEC:
                            //TODO
                    }
                } catch (NotFoundException nfex) {
                    diagnostics.append(new ErrorDiagnostic( registerSubField.getLocale(),
                                                            "Unrecognized value in register subfield"));
                }

                if ((aField < 0) || (aField > 017)) {
                    diagnostics.append(new TruncationDiagnostic(
                        registerSubField.getLocale(),
                        "Illegal value "));
                }
            }

            if ((valueSubField == null) || (valueSubField.getText().isEmpty())) {
                diagnostics.append(new ErrorDiagnostic(operandField.getLocale(),
                                                       "Missing operand value (U, u, or d subfield)"));
            } else {
                ExpressionParser p = new ExpressionParser(valueSubField.getText(), valueSubField.getLocale());
                try {
                    Expression e = p.parse(_context, diagnostics);
                    Value v = e.evaluate(_context, diagnostics);
                    if (v.getType() != ValueType.Integer) {
                        diagnostics.append(new ValueDiagnostic(valueSubField.getLocale(),
                                                               "Operand value is not an integer type"));
                        operandValue = new IntegerValue.Builder().build();
                    } else {
                        operandValue = v;
                    }
                } catch (ExpressionException | NotFoundException ex) {
                    diagnostics.append(new ErrorDiagnostic(valueSubField.getLocale(),
                                                           "Syntax error in operand value"));
                    operandValue = new IntegerValue.Builder().build();
                }
            }

            //TODO x-field
            //TODO maybe b-field
        }

        //  Create the instruction word...
        //TODO
        System.out.println(String.format("f=%02o j=%02o a=%02o val=%s%s",
                                         iinfo._fField,
                                         jField,
                                         aField,
                                         operandValue.getFlagged() ? "*" : "",
                                         operandValue.toString()));//TODO temporary

        return true;
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

        //  Does this line of code represent an instruction mnemonic?  (or a label on an otherwise empty line)...
        if (processMnemonic(labelField, operationField, operandField, textLine._diagnostics)) {
            if (textLine._fields.size() > 3) {
                _diagnostics.append(new ErrorDiagnostic(textLine.getField(3).getLocale(),
                                                        "Extraneous fields ignored"));
            }
            return;
        }

        //  Not a mnemonic - is it a directive?  (check the operation field subfield 0 against the dictionary)
        //TODO
        Dictionary d = _context._dictionary;

        //  Hmm.  Is it an expression (or a list of expressions)?
        //TODO

        _diagnostics.append(new ErrorDiagnostic(new Locale(textLine._lineNumber, 1), "What the heck is this?"));
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

        //  Resolve all references which can be resolved
        //TODO
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
                Dictionary.ValueAndLevel val = _context._dictionary.getValueAndLevel(label);
                StringBuilder sb = new StringBuilder();
                sb.append(label);
                for (int lx = 0; lx < val._level; ++lx) {
                    sb.append("*");
                }
                sb.append(": ");
                sb.append(val._value.toString());

                System.out.println(sb.toString());
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
