/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.directives.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;
import sun.nio.cs.ext.EUC_CN;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Assembler for minalib
 */
@SuppressWarnings("Duplicates")
public class Assembler {

    //  The Context object contains things which specifically apply to a particular sub-assembly...
    //  that is to say, whenever we enter a proc or a proc definition, we get a new one.  I think...
    private final Context _context;

    //  Aggregation of all TextLine diagnostics.
    //  This is rarely (if at all) up-to-date, until we reach the end of the assembly process.
    //  Diagnostics, when generated, should be appended to the TextLine objects' _diagnostic members.
    private final Diagnostics _diagnostics = new Diagnostics();

    //  The various TextLine object which comprise the source and assembled code...
    //  These objects are where most of the work is done throughout the assembly process
    private final TextLine[] _sourceCode;

    //  Name of the module to be created
    private final String _moduleName;

    private final Dictionary _globalDictionary;
    private final SystemDictionary _systemDictionary;

    //  Common forms we use for generating instructions
    private static final int[] _fjaxhiuFields = { 6, 4, 4, 4, 1, 1, 16 };
    private static final Form _fjaxhiuForm = new Form(_fjaxhiuFields);
    private static final int[] _fjaxuFields = { 6, 4, 4, 4, 18 };
    private static final Form _fjaxuForm = new Form(_fjaxuFields);
    private static final int[] _fjaxhibdFields = { 6, 4, 4, 4, 1, 1, 4, 12 };
    private static final Form _fjaxhibdForm = new Form(_fjaxhibdFields);

    //  Directives
    private static final Map<String, IDirective> _directives = new HashMap<>();
    static {
        _directives.put("$BASIC", new BASICDirective());
        _directives.put("$EXTEND", new EXTENDDirective());
        _directives.put("$LIT", new LITDirective());
        _directives.put("$RES", new RESDirective());
    }

    //  A useful IntegerValue containing zero, no flags, and no unidentified references.
    private static final IntegerValue _zeroValue = new IntegerValue( false, 0, null );


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Assemble a single TextLine object into the Relocatable Module
     * @param textLine entity to be assembled
     */
    private void assembleTextLine(
        final TextLine textLine
    ) {
        if (textLine._fields.isEmpty()) {
            return;
        }

        TextField labelField = textLine.getField(0);
        TextField operationField = textLine.getField(1);
        TextField operandField = textLine.getField(2);

        //  Interpret label field and update current location counter index if appropriate
        LabelFieldComponents lfc = interpretLabelField(labelField, _diagnostics);
        if (lfc._lcIndex != null) {
            _context._currentGenerationLCIndex = lfc._lcIndex;
        }

        //  Does this line of code represent an instruction mnemonic?  (or a label on an otherwise empty line)...
        if (processMnemonic(textLine, lfc, operationField, operandField, _diagnostics)) {
            if (textLine._fields.size() > 3) {
                _diagnostics.append(new ErrorDiagnostic(textLine.getField(3)._locale, "Extraneous fields ignored"));
            }
            return;
        }

        //  Not a mnemonic - is it a directive?  (check the operation field subfield 0 against the dictionary)
        if (processDirective(textLine, lfc, operationField, operandField, _diagnostics)) {
            return;
        }

        //  Hmm.  Is it an expression (or a list of expressions)?
        //  In this case, the operation field actually contains the operand, while the operand field should be empty.
        if (processDataGeneration(textLine, lfc, operationField, _diagnostics)) {
            if (textLine._fields.size() > 2) {
                _diagnostics.append(new ErrorDiagnostic(textLine.getField(2)._locale, "Extraneous fields ignored"));
            }
            return;
        }

        _diagnostics.append(new ErrorDiagnostic(new Locale(textLine._lineNumber, 1), "Unrecognizable source code"));
    }

    /**
     * Displays the content of a particular dictionary
     * @param dictionary dictionary to be displayed
     */
    private static void displayDictionary(
        final Dictionary dictionary
    ) {
        System.out.println("Dictionary:");
        for ( String label : dictionary.getLabels() ) {
            try {
                Dictionary.ValueAndLevel val = dictionary.getValueAndLevel( label );
                if (val._level < 2) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("  ");
                    sb.append(label);
                    for ( int x = 0; x < val._level; ++x ) {
                        sb.append("*");
                    }

                    sb.append(": ");
                    sb.append(val._value.toString());
                    System.out.println(sb.toString());
                }
            } catch (NotFoundException ex) {
                //  can't happen
            }
        }
    }

    /**
     * Summary of module
     * @param module module
     */
    private void displayModuleSummary(
        final RelocatableModule module
    ) {
        for (Map.Entry<Integer, LocationCounterPool> entry : module._storage.entrySet()) {
            System.out.println(String.format("LCPool %d: %d word(s) generated",
                                             entry.getKey(),
                                             entry.getValue()._storage.length));
        }

        System.out.println("Undefined References:");
        for (Map.Entry<Integer, LocationCounterPool> poolEntry : module._storage.entrySet()) {
            int lcIndex = poolEntry.getKey();
            LocationCounterPool lcPool = poolEntry.getValue();
            for (RelocatableWord36 word36 : lcPool._storage) {
                for (RelocatableWord36.UndefinedReference ur : word36._undefinedReferences) {
                    System.out.println("  " + ur._reference);
                }
            }
        }
    }

    /**
     * Displays output upon the console
     */
    private void displayResults(
    ) {
        //  This is inefficient, but it only applies when the caller wants to display source output.
        for (TextLine line : _sourceCode) {
            System.out.println(String.format("%04d:%s", line._lineNumber, line._text));

            for (Diagnostic d : _diagnostics.getDiagnostics(line._lineNumber)) {
                System.out.println( d.getMessage() );
            }

            for (Map.Entry<Integer, Context.GeneratedPool> poolEntry : _context._generatedPools.entrySet()) {
                int lcIndex = poolEntry.getKey();
                Context.GeneratedPool gPool = poolEntry.getValue();
                for (Map.Entry<Integer, Context.GeneratedWord> wordEntry : gPool.entrySet()) {
                    int lcOffset = wordEntry.getKey();
                    Context.GeneratedWord gWord = wordEntry.getValue();
                    if (gWord._locale.getLineNumber() == line._lineNumber) {
                        RelocatableWord36 rw36 = gWord.produceRelocatableWord36(_diagnostics);
                        String gwBase = String.format("  $(%2d) %06o:  %012o", lcIndex, lcOffset, rw36.getW());
                        if (rw36._undefinedReferences.length == 0) {
                            System.out.println(gwBase);
                        } else {
                            for (int urx = 0; urx < rw36._undefinedReferences.length; ++urx) {
                                System.out.println(String.format("%s %s",
                                                                 urx == 0 ? gwBase : "                           ",
                                                                 rw36._undefinedReferences[urx].toString()));
                            }
                        }
                    }
                }
            }
        }

        displayDictionary(_context._dictionary);
    }

    /**
     * Generates the RelocatableModule based on the various internal structures we've built up
     * @param moduleName name of the module
     * @return RelocatableModule object
     */
    private RelocatableModule generateRelocatableModule(
        final String moduleName
    ) {
        Map<String, IntegerValue> externalLabels = new TreeMap<>();
        for ( String label : _globalDictionary.getLabels() ) {
            try {
                Value val = _globalDictionary.getValue( label );
                if ( val.getType() == ValueType.Integer ) {
                    externalLabels.put( label, (IntegerValue) val );
                }
            } catch (NotFoundException ex) {
                //  can't happen
            }
        }

        return new RelocatableModule(moduleName, _context.produceLocationCounterPools(_diagnostics), externalLabels);
    }

    /**
     * Getter for unit tests
     * @return array of TextLine objects comprising the source code
     */
    TextLine[] getParsedCode(
    ) {
        return _sourceCode;
    }

    /**
     * Interprets the label field to the extend possible when the purpose of the label is not known.
     * Calling code will do different things depending upon how the label (if any) is to be established.
     * @param labelField TextField containing the label field (might be null or empty)
     * @param diagnostics where we post any appropriate diagnostics
     * @return an appropriately populated LabelFieldComponents object
     */
    private LabelFieldComponents interpretLabelField(
            final TextField labelField,
            final Diagnostics diagnostics
    ) {
        Integer lcIndex = null;
        Locale lcLocale = null;
        String label = null;
        Integer labelLevel = null;
        Locale labelLocale = null;

        if (labelField != null) {
            //  Look for a location counter specification.  If one is given, it will be the first subfield
            int sfx = 0;
            if (labelField._subfields.size() > sfx) {
                TextSubfield lcSubField = labelField._subfields.get(sfx);
                Locale sfLocale = lcSubField._locale;
                String sfText = lcSubField._text;
                if (sfText.matches("\\$\\(\\d{1,3}\\)")) {
                    lcIndex = Integer.parseInt(sfText.substring(2, sfText.length() - 1));
                    lcLocale = sfLocale;
                    ++sfx;
                } else if (sfText.startsWith("$(")) {
                    diagnostics.append(new ErrorDiagnostic(sfLocale, "Illegal location counter specification"));
                }
            }

            //  Look for a label specification.  If one is given, it will follow any lc specification
            if (labelField._subfields.size() > sfx) {
                TextSubfield lcSubField = labelField._subfields.get(sfx);
                Locale sfLocale = lcSubField._locale;
                String sfText = lcSubField._text;

                int levelers = 0;
                while (sfText.endsWith("*")) {
                    ++levelers;
                    sfText = sfText.substring(0, sfText.length() - 1);
                }
                if (Dictionary.isValidLabel(sfText)) {
                    label = sfText;
                    labelLevel = levelers;
                    labelLocale = sfLocale;
                    ++sfx;
                } else {
                    diagnostics.append(new ErrorDiagnostic(sfLocale, "Invalid label specified"));
                }
            }

            //  Warn on anything extra
            if (sfx < labelField._subfields.size()) {
                TextSubfield lcSubField = labelField._subfields.get(sfx);
                Locale sfLocale = lcSubField._locale;
                diagnostics.append(new ErrorDiagnostic(sfLocale, "Extraneous label subfields ignored"));
            }
        }

        return new LabelFieldComponents(lcIndex, lcLocale, label, labelLevel, labelLocale);
    }

    /**
     * Handles source lines which generate data implicitly by virtue of specifying an expression list
     * with no operation field (the operand field takes the place of the operation field).
     * We can generate ASCII or FIELDATA text, or we can generate a single word of data made up of one or more
     * bit fields (defined by dividing 36 bits by the number of expressions in the list).
     * The way this works, is we evaluate the expression in the first subfield.
     * If it is a string, then we allow no other subfields, and we generate as many words as necessary.
     * If it is a float, we generate one word, and allow no other subfields
     * If it is an integer, we then expect any other subfields to also evaluate to integers, and proceed accordingly.
     * @param textLine where this came from
     * @param labelFieldComponents represents the label field components, if any were specified
     * @param operandField represents the operand field, if any
     * @param diagnostics where we post diagnostics if needed
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processDataGeneration(
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents,
        final TextField operandField,
        final Diagnostics diagnostics
    ) {
        if ((operandField == null) || (operandField._subfields.isEmpty())) {
            return false;
        }

        TextSubfield sf0 = operandField._subfields.get(0);
        String sf0Text = sf0._text;
        Locale sf0Locale = sf0._locale;
        Value firstValue = null;
        try {
            ExpressionParser p1 = new ExpressionParser(sf0Text, sf0Locale);
            Expression e1 = p1.parse(_context, diagnostics);
            if (e1 == null) {
                return false;
            }
            firstValue = e1.evaluate(_context, diagnostics);
        } catch (ExpressionException eex) {
            diagnostics.append(new ErrorDiagnostic(sf0Locale, "Syntax error in expression"));
        }

        if (labelFieldComponents._label != null) {
            establishLabel(labelFieldComponents._labelLocale,
                           _context._dictionary,
                           labelFieldComponents._label,
                           labelFieldComponents._labelLevel,
                           _context.getCurrentLocation(),
                           diagnostics);
        }

        //TODO implement fp and string value handling

        if (firstValue instanceof FloatingPointValue) {
            FloatingPointValue fpValue = (FloatingPointValue) firstValue;
            //TODO here
            if (operandField._subfields.size() > 1) {
                Locale loc = operandField._subfields.get(1)._locale;
                diagnostics.append(new ErrorDiagnostic(loc, "Too many subfields for data generation"));
            }

            return true;
        }

        if (firstValue instanceof StringValue) {
            StringValue sValue = (StringValue) firstValue;
            //TODO and here
            if (operandField._subfields.size() > 1) {
                Locale loc = operandField._subfields.get(1)._locale;
                diagnostics.append(new ErrorDiagnostic(loc, "Too many subfields for data generation"));
            }
            
            return true;
        }
        
        if (firstValue instanceof IntegerValue) {
            //  Ensure the number of values divides evenly.
            int valueCount = (operandField._subfields.size());
            if (valueCount > 36) {
                diagnostics.append(new ErrorDiagnostic(operandField._locale, "Improper number of data fields"));
                return true;
            }

            IntegerValue[] values = new IntegerValue[valueCount];
            values[0] = (IntegerValue) firstValue;
            for (int vx = 1; vx < valueCount; ++vx) {
                values[vx] = _zeroValue;
                TextSubfield sfNext = operandField._subfields.get(vx);
                String sfNextText = sfNext._text;
                Locale sfNextLocale = sfNext._locale;
                try {
                    ExpressionParser pNext = new ExpressionParser(sfNextText, sfNextLocale);
                    Expression eNext = pNext.parse(_context, diagnostics);
                    if (eNext == null) {
                        diagnostics.append(new ErrorDiagnostic(sf0Locale, "Expression expected"));
                        continue;
                    }

                    Value vNext = eNext.evaluate(_context, diagnostics);
                    if (vNext instanceof IntegerValue) {
                        values[vx] = (IntegerValue) vNext;
                    } else {
                        diagnostics.append(new ValueDiagnostic(sfNextLocale, "Expected integer value"));
                    }
                } catch (ExpressionException ex) {
                    diagnostics.append(new ErrorDiagnostic(sf0Locale, "Syntax error in expression"));
                }
            }

            int[] fieldSizes = new int[valueCount];
            int fieldSize = 36 / valueCount;
            for (int fx = 0; fx < values.length; ++fx) {
                fieldSizes[fx] = fieldSize;
            }

            _context.generate(operandField._locale,
                              _context._currentGenerationLCIndex,
                              new Form(fieldSizes),
                              values);
            return true;
        }

        diagnostics.append(new ErrorDiagnostic(sf0Locale, "Wrong value type for data generation"));
        return true;
    }

    /**
     * Handles directives
     * @param textLine where this came from
     * @param labelFieldComponents represents the label field components, if any were specified
     * @param operationField represents the operation field, if any
     * @param operandField represents the operand field, if any
     * @param diagnostics where we post diagnostics if needed
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processDirective(
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents,
            final TextField operationField,
            final TextField operandField,
            final Diagnostics diagnostics
    ) {
        if ((operationField == null) || (operationField._subfields.isEmpty())) {
            return false;
        }

        IDirective directive = _directives.get(operationField._subfields.get(0)._text.toUpperCase());
        if (directive != null) {
            directive.process(this, _context, labelFieldComponents, operationField, operandField, diagnostics);
            return true;
        }

        return false;
    }

    /**
     * Handles instruction mnemonic lines of code, and blank lines
     * @param textLine where this came from
     * @param labelFieldComponents represents the label field components, if any were specified
     * @param operationField represents the operation field, if any
     * @param operandField represents the operand field, if any
     * @param diagnostics where we post diagnostics if needed
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processMnemonic(
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents,
        final TextField operationField,
        final TextField operandField,
        final Diagnostics diagnostics
    ) {
        if ((operationField == null) || (operationField._subfields.isEmpty())) {
            //  This is a no-op line - but it might have a label.
            //  Do label stuff, then return true indicating that the line has been processed.
            if (labelFieldComponents._label != null) {
                establishLabel(labelFieldComponents._labelLocale,
                               _context._dictionary,
                               labelFieldComponents._label,
                               labelFieldComponents._labelLevel,
                               _context.getCurrentLocation(),
                               diagnostics);
            }
            return true;
        }

        //  Deal with the operation field
        TextSubfield mnemonicSubfield = operationField.getSubfield(0);
        String mnemonic = mnemonicSubfield._text;
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
                InstructionWord.getInstructionInfo(mnemonic, imode);
                diagnostics.append(new ErrorDiagnostic(mnemonicSubfield._locale,
                                                       "Opcode not valid for the current code mode"));
                return true;
            } catch (NotFoundException ex2) {
                //  The mnemonic truly isn't found - return false, this is not an instruction operation
                return false;
            }
        }

        //  Establish the label to refer to the current lc pool's current offset (if there is a label).
        //  Use the label level to establish which dictionary level it should be placed in.
        if (labelFieldComponents._label != null) {
            establishLabel(labelFieldComponents._labelLocale,
                           _context._dictionary,
                           labelFieldComponents._label,
                           labelFieldComponents._labelLevel,
                           _context.getCurrentLocation(),
                           diagnostics);
        }

        //  Is this a special instruction? (such as JGD, BT, etc)
        //TODO

        //  If j-flag is set, we pull j-field from the iinfo object.  Otherwise, we interpret the j-field.
        int jField = 0;
        if (iinfo._jFlag) {
            jField = iinfo._jField;
            if (operationField._subfields.size() > 1) {
                diagnostics.append(new ErrorDiagnostic(operationField.getSubfield(1)._locale,
                                                      "Extraneous subfields in operation field"));
            }
        } else if (operationField._subfields.size() > 1) {
            TextSubfield jSubField = operationField.getSubfield(1);
            try {
                jField = InstructionWord.getJFieldValue(jSubField._text);
            } catch ( NotFoundException e ) {
                diagnostics.append(new ErrorDiagnostic(jSubField._locale,
                                                       "Invalid text for j-field of instruction"));
            }

            if ( operationField._subfields.size() > 2 ) {
                diagnostics.append(new ErrorDiagnostic(
                    operationField.getSubfield(1)._locale,
                    "Extraneous subfields in operation field"));
            }
        }

        //  Deal with the operand field - initialize resulting values here, as we do make every attempt to generate
        //  a word, even in the presence of errors which might short-circuit other stuff inside the conditional
        //  expression(s).
        IntegerValue aValue;                //  register value
        IntegerValue bValue = _zeroValue;   //  base register subfield
        IntegerValue uValue;                //  displacement/address/value subfield
        IntegerValue xValue = _zeroValue;   //  index register subfield

        Value operandValue = null;
        if (operandField == null) {
            diagnostics.append(new ErrorDiagnostic(operationField._locale,
                                                    "Instruction mnemonic requires an operand field"));
            return true;
        }

        //  We have to be in extended mode, *not* using basic mode semantics, and
        //  either the j-field is part of the instruction, or else it is no U or XU...
        //  If that is the case, then we allow (maybe even require) a base register specification.
        boolean sfBaseAllowed =
                (_context._codeMode == CodeMode.Extended)
                && !iinfo._useBMSemantics
                && (iinfo._jFlag || (jField < 016));

        //  Find the subfields... if iinfo's a-flag is set, then the iinfo a-field is used for the instruction
        //  a field, and there isn't one in the syntax for the operand field.
        //  If the flag is clear, then the first subfield is a register specification... which can get a bit
        //  complicated as well, since it might be an a-register, an x-register, or an r-register (or even a b...)
        TextSubfield sfRegister = null;
        TextSubfield sfValue = null;
        TextSubfield sfIndex = null;
        TextSubfield sfBase = null;

        int sfx = 0;
        int sfc = operandField._subfields.size();
        if (!iinfo._aFlag && (sfc > sfx)) {
            sfRegister = operandField.getSubfield( sfx++ );
        }
        if (sfc > sfx) {
            sfValue = operandField.getSubfield( sfx++ );
        }
        if (sfc > sfx) {
            sfIndex = operandField.getSubfield( sfx++ );
        }
        if ((sfc > sfx) && sfBaseAllowed) {
            sfBase = operandField.getSubfield( sfx++ );
        }
        if (sfc > sfx) {
            diagnostics.append( new ErrorDiagnostic( operandField.getSubfield( sfx )._locale,
                                                     "Extreanous subfields in operand field ignored") );
        }

        //  Interpret the subfields
        if (iinfo._aFlag) {
            aValue = new IntegerValue( false, iinfo._aField, null );
        } else {
            if ((sfRegister == null) || (sfRegister._text.isEmpty())) {
                diagnostics.append(new ErrorDiagnostic(operandField._locale, "Missing register specification"));
                return true;
            }

            try {
                ExpressionParser p = new ExpressionParser(sfRegister._text, sfRegister._locale);
                Expression e = p.parse(_context, diagnostics);
                if (e == null) {
                    diagnostics.append(new ErrorDiagnostic(sfRegister._locale, "Syntax Error"));
                    return true;
                }

                Value v = e.evaluate(_context, diagnostics);
                if (!(v instanceof IntegerValue)) {
                    diagnostics.append(new ValueDiagnostic(sfRegister._locale, "Wrong value type"));
                    return true;
                }

                //  Reduce the value appropriately for the a-field
                aValue = (IntegerValue) v;
                switch (iinfo._aSemantics) {
                    case A:
                        aValue = new IntegerValue(aValue._flagged, aValue._value - 12, aValue._undefinedReferences);
                        break;

                    case R:
                        aValue = new IntegerValue(aValue._flagged, aValue._value - 64, aValue._undefinedReferences);
                        break;
                }
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(sfRegister._locale, "Syntax Error"));
                return true;
            }
        }

        if ((sfValue == null) || (sfValue._text.isEmpty())) {
            diagnostics.append(new ErrorDiagnostic(operationField._locale,
                                                   "Missing operand value (U, u, or d subfield)"));
            return true;
        }

        try {
            ExpressionParser p = new ExpressionParser( sfValue._text, sfValue._locale );
            Expression e = p.parse(_context, diagnostics);
            if (e == null) {
                diagnostics.append(new ErrorDiagnostic(sfValue._locale, "Syntax Error"));
                return true;
            }

            Value v = e.evaluate(_context, diagnostics);
            if (!(v instanceof IntegerValue)) {
                diagnostics.append(new ValueDiagnostic(sfValue._locale, "Wrong value type"));
                return true;
            }

            uValue = (IntegerValue) v;
        } catch ( ExpressionException ex ) {
            diagnostics.append(new ErrorDiagnostic(sfValue._locale, "Syntax Error"));
            return true;
        }

        if ((sfIndex != null) && !sfIndex._text.isEmpty()) {
            try {
                ExpressionParser p = new ExpressionParser( sfIndex._text, sfIndex._locale );
                Expression e = p.parse(_context, diagnostics);
                if (e == null) {
                    diagnostics.append(new ErrorDiagnostic(sfIndex._locale, "Syntax Error"));
                    return true;
                }

                Value v = e.evaluate(_context, diagnostics);
                if (!(v instanceof IntegerValue)) {
                    diagnostics.append(new ValueDiagnostic(sfIndex._locale, "Wrong value type"));
                    return true;
                }

                xValue = (IntegerValue) v;
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(sfIndex._locale, "Syntax Error"));
                return true;
            }
        }

        if (sfBaseAllowed) {
            if ((sfBase != null) && !sfBase._text.isEmpty()) {
                try {
                    ExpressionParser p = new ExpressionParser( sfBase._text, sfBase._locale );
                    Expression e = p.parse( _context, diagnostics );
                    if (e == null) {
                        diagnostics.append(new ErrorDiagnostic(sfBase._locale, "Syntax Error"));
                        return true;
                    }

                    Value v = e.evaluate( _context, diagnostics );
                    if (!(v instanceof IntegerValue)) {
                        diagnostics.append(new ValueDiagnostic(sfBase._locale, "Wrong value type"));
                        return true;
                    }

                    bValue = (IntegerValue) v;
                } catch (ExpressionException ex) {
                    diagnostics.append( new ErrorDiagnostic( sfBase._locale,
                                                             "Syntax Error:" + ex.getMessage() ) );
                    return true;
                }
            }
        }

        //  Create the instruction word
        Form form;
        IntegerValue[] values;
        if (!iinfo._jFlag && (jField >= 016)) {
            form = _fjaxuForm;
            values = new IntegerValue[5];
            values[0] = new IntegerValue( false, iinfo._fField, null );
            values[1] = new IntegerValue( false, jField, null );
            values[2] = aValue;
            values[3] = xValue;
            values[4] = uValue;
        } else if ((_context._codeMode == CodeMode.Basic) || iinfo._useBMSemantics) {
            form = _fjaxhiuForm;
            values = new IntegerValue[7];
            values[0] = new IntegerValue(false, iinfo._fField, null);
            values[1] = new IntegerValue(false, jField, null);
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue(false, (xValue._flagged ? 1 : 0), null);
            values[5] = new IntegerValue(false, (uValue._flagged ? 1 : 0), null);
            values[6] = uValue;
        } else {
            form = _fjaxhibdForm;
            values = new IntegerValue[8];
            values[0] = new IntegerValue(false, iinfo._fField, null);
            values[1] = new IntegerValue(false, jField, null);
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue(false, (xValue._flagged ? 1 : 0), null);
            values[5] = new IntegerValue(false, (uValue._flagged ? 1 : 0), null);
            values[6] = bValue;
            values[7] = uValue;
        }

        _context.generate(operationField._locale, _context._currentGenerationLCIndex, form, values);
        return true;
    }

    /**
     * Resolves any lingering undefined references once initial assembly is complete...
     * These will be the forward-references we picked up along the way.
     */
    private void resolveReferences(
    ) {
        for (Map.Entry<Integer, Context.GeneratedPool> poolEntry : _context._generatedPools.entrySet()) {
            int lcIndex = poolEntry.getKey();
            Context.GeneratedPool pool = poolEntry.getValue();
            for (Map.Entry<Integer, Context.GeneratedWord> wordEntry : pool.entrySet()) {
                int lcOffset = wordEntry.getKey();
                Context.GeneratedWord gWord = wordEntry.getValue();

                for (Map.Entry<FieldDescriptor, IntegerValue> entry : gWord.entrySet()) {
                    FieldDescriptor fd = entry.getKey();
                    IntegerValue originalIV = entry.getValue();
                    if (originalIV._undefinedReferences.length > 0) {
                        long newDiscreteValue = originalIV._value;
                        List<IntegerValue.UndefinedReference> newURefs = new LinkedList<>();
                        for (IntegerValue.UndefinedReference intURef : originalIV._undefinedReferences) {
                            try {
                                Value lookupValue = _context._dictionary.getValue(intURef._reference);
                                if (lookupValue.getType() != ValueType.Integer) {
                                    _diagnostics.append(
                                            new ValueDiagnostic(
                                                    gWord._locale,
                                                    "Forward reference does not resolve to an integer"));
                                } else {
                                    IntegerValue lookupIntegerValue = (IntegerValue) lookupValue;
                                    newDiscreteValue += (intURef._isNegative ? -1 : 1) * lookupIntegerValue._value;
                                    newURefs.addAll(Arrays.asList(lookupIntegerValue._undefinedReferences));
                                }
                            } catch (NotFoundException ex) {
                                //  reference is still not found - propagate it
                                newURefs.add(intURef);
                            }
                        }

                        IntegerValue newIV = new IntegerValue(originalIV._flagged,
                                                              newDiscreteValue,
                                                              newURefs.toArray(new IntegerValue.UndefinedReference[0]));
                        gWord.put(fd, newIV);
                    }
                }
            }
        }
    }


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Create an Assembler object and load it with text from the given array of source lines
     * @param source array of strings comprising the source code to be assembled
     * @param moduleName name to be given to the relocatable module
     */
    public Assembler(
        final String[] source,
        final String moduleName
    ) {
        _systemDictionary = new SystemDictionary();
        _globalDictionary = new Dictionary(_systemDictionary);
        _context = new Context(_globalDictionary, moduleName);
        _sourceCode = new TextLine[source.length];
        for (int sx = 0; sx < source.length; ++sx) {
            int lineNumber = sx + 1;
            _sourceCode[sx] = new TextLine(lineNumber, source[sx]);
        }
        _moduleName = moduleName;
    }

    /**
     * Assemble the source code in the object.
     * We do not do things quite in the same way as MASM.
     * @param display true to display source, code generation, etc on console
     * @return RelocatableModule we create if successful, else null
     */
    public RelocatableModule assemble(
        final boolean display
    ) {
        if (display) {
            System.out.println(String.format("Assembling module %s -----------------------------------", _moduleName));
        }

        //  setup
        _context._characterMode = CharacterMode.ASCII;
        _context._codeMode = CodeMode.Basic;
        _diagnostics.clear();

        //  First step - parse all the source code into fields/subfields.
        for (TextLine line : _sourceCode) {
            line.parseFields(_diagnostics);
            if (_diagnostics.hasFatal()) {
                return null;
            }
        }

        //  Next step - assemble all the things
        for (TextLine line : _sourceCode) {
            assembleTextLine(line);
            if (_diagnostics.hasFatal()) {
                return null;
            }
        }

        resolveReferences();
        RelocatableModule module = generateRelocatableModule(_moduleName);

        if (display) {
            displayResults();
            displayModuleSummary(module);
        }

        if (display) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Summary: Lines=%d", _sourceCode.length));
            for (Map.Entry<Diagnostic.Level, Integer> entry : _diagnostics.getCounters().entrySet()) {
                if (entry.getValue() > 0) {
                    sb.append(String.format(" %c=%d", Diagnostic.getLevelIndicator(entry.getKey()), entry.getValue()));
                }
            }
            System.out.println(sb.toString());
            System.out.println("Assembly Ends -------------------------------------------------------");
        }

        return module;
    }

    /**
     * Establishes a label value in the current (or a super-ordinate) dictionary
     * @param locale locale of label (for posting diagnostics)
     * @param dictionary dictionary in which the label is to be created (the base, if level is > 0)
     * @param label label
     * @param labelLevel label level - 0 to put it in the dictionary, 1 for the next highest, etc
     * @param value value to be associated with the level
     * @param diagnostics where we post diagnostics
     */
    public static void establishLabel(
            final Locale locale,
            final Dictionary dictionary,
            final String label,
            final int labelLevel,
            final Value value,
            final Diagnostics diagnostics
    ) {
        if (dictionary.hasValue(label)) {
            diagnostics.append(new DuplicateDiagnostic(locale, "Label " + label + " duplicated"));
        } else {
            dictionary.addValue(labelLevel, label, value);
        }
    }

    /**
     * Getter
     * @return Diagnostics object produced during assembly
     */
    public Diagnostics getDiagnostics(
    ) {
        return _diagnostics;
    }
}
