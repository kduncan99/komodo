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
@SuppressWarnings("Duplicates")
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
            return new IntegerValue(false, 0, null);
        } catch ( NotFoundException nfex ) {
            diagnostics.append( new ErrorDiagnostic( subfield.getLocale(),
                                                     "Expected an expression" ) );
            return new IntegerValue(false, 0, null);
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

        //  Deal with the operand field - initialize resulting values here, as we do make every attempt to generate
        //  a word, even in the presence of errors which might short-circuit other stuff inside the conditional
        //  expression(s).
        Value aValue = null;    //  register subfield
        Value bValue = null;    //  base register subfield
        Value uValue = null;    //  displacement/address/value subfield
        Value xValue = null;    //  index register subfield

        Value operandValue = null;
        if (operandField == null) {
            diagnostics.append(new ErrorDiagnostic( operationField.getLocale(),
                                                    "An instruction mnemonic requires an operand field"));
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
            if (iinfo._aFlag) {
                aValue = new IntegerValue( false, iinfo._aField, null );
            } else {
                if ( (registerSubField == null) || (registerSubField.getText().isEmpty()) ) {
                    diagnostics.append( new ErrorDiagnostic( operandField.getLocale(),
                                                             "Missing register specification" ) );
                } else {
                    try {
                        ExpressionParser p = new ExpressionParser( registerSubField.getText(), registerSubField.getLocale() );
                        Expression e = p.parse( _context, diagnostics );
                        aValue = e.evaluate( _context, diagnostics );
                        //  Reduce the value appropriately for the a-field
                        if ( iinfo._aSemantics == InstructionWord.ASemantics.A ) {
                            IntegerValue iv = (IntegerValue) aValue;
                            aValue = new IntegerValue( iv.getFlagged(), iv.getValue() - 12, iv.getUndefinedReferences() );
                        } else if ( iinfo._aSemantics == InstructionWord.ASemantics.R ) {
                            IntegerValue iv = (IntegerValue) aValue;
                            aValue = new IntegerValue( iv.getFlagged(), iv.getValue() - 64, iv.getUndefinedReferences() );
                        }
                        //TODO do we need to do anything for B16-B31?  Those registers have the same a-field values as
                        //  B0-B15, with the i-field set, and they only apply at higher processor privileges...
                    } catch ( ExpressionException | NotFoundException ex ) {
                        diagnostics.append( new ErrorDiagnostic( registerSubField.getLocale(),
                                                                 "Syntax Error:" + ex.getMessage() ) );
                    }
                }
            }

            if ((valueSubField == null) || (valueSubField.getText().isEmpty())) {
                diagnostics.append(new ErrorDiagnostic(operandField.getLocale(),
                                                       "Missing operand value (U, u, or d subfield)"));
            } else {
                try {
                    ExpressionParser p = new ExpressionParser( valueSubField.getText(), valueSubField.getLocale() );
                    Expression e = p.parse( _context, diagnostics );
                    uValue = e.evaluate( _context, diagnostics );
                } catch ( ExpressionException | NotFoundException ex ) {
                    diagnostics.append( new ErrorDiagnostic( valueSubField.getLocale(),
                                                             "Syntax Error:" + ex.getMessage() ) );
                }
            }

            if ((indexSubField != null) && !indexSubField.getText().isEmpty()) {
                try {
                    ExpressionParser p = new ExpressionParser( indexSubField.getText(), indexSubField.getLocale() );
                    Expression e = p.parse( _context, diagnostics );
                    xValue = e.evaluate( _context, diagnostics );
                } catch ( ExpressionException | NotFoundException ex ) {
                    diagnostics.append( new ErrorDiagnostic( indexSubField.getLocale(),
                                                             "Syntax Error:" + ex.getMessage() ) );
                }
            }

            //TODO check code mode to see whether we're supposed to have a base register spec
            if ((baseSubField != null) && !baseSubField.getText().isEmpty()) {
                try {
                    ExpressionParser p = new ExpressionParser( baseSubField.getText(), baseSubField.getLocale() );
                    Expression e = p.parse( _context, diagnostics );
                    bValue = e.evaluate( _context, diagnostics );
                } catch ( ExpressionException | NotFoundException ex ) {
                    diagnostics.append( new ErrorDiagnostic( baseSubField.getLocale(),
                                                             "Syntax Error:" + ex.getMessage() ) );
                }
            }
        }

        //  Create the instruction word
        //TODO
        System.out.println(String.format("f=%02o j=%o a=%s val=%s x=%s b=%s",
                                         iinfo._fField,
                                         jField,
                                         String.valueOf(aValue),
                                         String.valueOf(uValue),
                                         String.valueOf(xValue),
                                         String.valueOf(bValue)));

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
     * Creates an IntegerValue object with an appropriate undefined reference to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    private IntegerValue getCurrentLocation(
    ) {
        //  Find the current generation lc index.
        //  If it doesn't exist, it will be created.
        try {
            LocationCounterPool lcPool = _module.getLocationCounterPool(_context._currentGenerationLCIndex);
            String ref = String.format("LC$BASE_%d", _context._currentGenerationLCIndex);
            IntegerValue.UndefinedReference[] refs = { new IntegerValue.UndefinedReference( ref, false ) };
            return new IntegerValue(false, lcPool.getNextOffset(), refs);
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
