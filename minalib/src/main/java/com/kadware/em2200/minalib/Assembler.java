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

    //  Keep track of the amount of code generated per location counter index
    private final Map<Integer, Integer> _codeCount = new HashMap<>();

    //  The various TextLine object which comprise the source and assembled code...
    //  These objects are where most of the work is done throughout the assembly process
    private final TextLine[] _sourceCode;

    //  Name of the module to be created
    private String _moduleName;

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
        LabelFieldComponents lfc = interpretLabelField(labelField, textLine._diagnostics);
        if (lfc._lcIndex != null) {
            _context._currentGenerationLCIndex = lfc._lcIndex;
        }

        //  Does this line of code represent an instruction mnemonic?  (or a label on an otherwise empty line)...
        if (processMnemonic(textLine, lfc, operationField, operandField, textLine._diagnostics)) {
            if (textLine._fields.size() > 3) {
                textLine._diagnostics.append(new ErrorDiagnostic(textLine.getField(3)._locale,
                                                                 "Extraneous fields ignored"));
            }
            return;
        }

        //  Not a mnemonic - is it a directive?  (check the operation field subfield 0 against the dictionary)
        if (processDirective(textLine, lfc, operationField, operandField, textLine._diagnostics)) {
            return;
        }

        //  Hmm.  Is it an expression (or a list of expressions)?
        //  In this case, the operation field actually contains the operand, while the operand field should be empty.
        if (processDataGeneration(textLine, lfc, operationField, textLine._diagnostics)) {
            if (textLine._fields.size() > 2) {
                textLine._diagnostics.append(new ErrorDiagnostic(textLine.getField(2)._locale,
                                                                 "Extraneous fields ignored"));
            }
            return;
        }

        textLine._diagnostics.append(new ErrorDiagnostic(new Locale(textLine._lineNumber, 1),
                                                         "Unrecognizable source code"));
    }

    /**
     * Aggregates the diagnostics from the individual lines of text into the master _diagnostics container
     */
    private void collectDiagnostics(
    ) {
        for (TextLine line : _sourceCode) {
            _diagnostics.append(line._diagnostics);
        }
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
    }

    /**
     * Displays output upon the console
     */
    private void displayResults(
    ) {
        for (TextLine line : _sourceCode) {
            System.out.println(String.format("%04d:%s", line._lineNumber, line._text));

            for (Diagnostic d : line._diagnostics.getDiagnostics()) {
                System.out.println( d.getMessage() );
            }

            for (TextLine.GeneratedWord gw : line._generatedWords) {
                RelocatableWord36 rw36 = gw.produceRelocatableWord36( line );
                String gwBase = String.format("  $(%2d) %06o:  %012o", gw._lcIndex, gw._lcOffset, rw36.getW());
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

        displayDictionary(_context._dictionary);
    }

    /**
     * Generates the given word as a set of subfields for a given location counter index and offset,
     * and places it into the given TextLine objects's set of generated words.
     * @param textLine line of text which is driving this
     * @param form indicates the bit fields - there should be one value per bit-field
     * @param values the values to be used
     * @param lcIndex index of the location counter pool
     */
    private void generate(
        final TextLine textLine,
        final Form form,
        final IntegerValue[] values,
        final int lcIndex
    ) {
        if (values.length != form._fieldSizes.length) {
            throw new RuntimeException("Number of bit-fields in the form differ from number of values");
        }

        int startingBit = form._leftSlop;
        int[] fieldSizes = form._fieldSizes;
        Map<FieldDescriptor, IntegerValue> fields = new HashMap<>();
        for (int fx = 0; fx < values.length; ++fx) {
            FieldDescriptor fd = new FieldDescriptor( startingBit, fieldSizes[fx] );
            fields.put(fd, values[fx]);
            startingBit += fieldSizes[fx];
        }

        if (!_codeCount.containsKey( lcIndex )) {
            _codeCount.put( lcIndex, 0 );
        }
        int lcOffset = _codeCount.get( lcIndex );
        textLine.appendWord( lcIndex, lcOffset, fields );
        _codeCount.put( lcIndex, lcOffset + 1);
    }

    /**
     * Generates the RelocatableModule based on the various internal structures we've built up
     * @param moduleName name of the module
     * @return RelocatableModule object
     */
    private RelocatableModule generateRelocatableModule(
        final String moduleName
    ) {
        Map<Integer, RelocatableWord36[]> temp = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : _codeCount.entrySet()) {
            int lcIndex = entry.getKey();
            int size = entry.getValue();
            temp.put( lcIndex, new RelocatableWord36[size] );
        }

        for (TextLine line : _sourceCode) {
            for (TextLine.GeneratedWord gw : line._generatedWords) {
                temp.get( gw._lcIndex )[gw._lcOffset] = gw.produceRelocatableWord36( line );
            }
        }

        Map<Integer, LocationCounterPool> pools = new HashMap<>();
        for (Map.Entry<Integer, RelocatableWord36[]> entry : temp.entrySet()) {
            int lcIndex = entry.getKey();
            RelocatableWord36[] storage = entry.getValue();
            pools.put( lcIndex, new LocationCounterPool( storage ) );
        }

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

        return new RelocatableModule( moduleName, pools, externalLabels );
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
                           getCurrentLocation(),
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

            generate(textLine, new Form(fieldSizes), values, _context._currentGenerationLCIndex);
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
                               getCurrentLocation(),
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
                           getCurrentLocation(),
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
        IntegerValue aValue = _zeroValue;   //  register value
        IntegerValue bValue = _zeroValue;   //  base register subfield
        IntegerValue uValue = _zeroValue;   //  displacement/address/value subfield
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
            } else {
                if (_context._defaultBaseRegister != null) {
                    bValue = new IntegerValue(false, _context._defaultBaseRegister, null);
                }
            }
        }

        //  Create the instruction word
        if (!iinfo._jFlag && (jField >= 016)) {
            IntegerValue[] values = new IntegerValue[5];
            values[0] = new IntegerValue( false, iinfo._fField, null );
            values[1] = new IntegerValue( false, jField, null );
            values[2] = aValue;
            values[3] = xValue;
            values[4] = uValue;
            generate(textLine, _fjaxuForm, values, _context._currentGenerationLCIndex);
        } else if ((_context._codeMode == CodeMode.Basic) || iinfo._useBMSemantics) {
            IntegerValue[] values = new IntegerValue[7];
            values[0] = new IntegerValue(false, iinfo._fField, null);
            values[1] = new IntegerValue(false, jField, null);
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue(false, (xValue._flagged ? 1 : 0), null);
            values[5] = new IntegerValue(false, (uValue._flagged ? 1 : 0), null);
            values[6] = uValue;
            generate(textLine, _fjaxhiuForm, values, _context._currentGenerationLCIndex);
        } else {
            IntegerValue[] values = new IntegerValue[8];
            values[0] = new IntegerValue(false, iinfo._fField, null);
            values[1] = new IntegerValue(false, jField, null);
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue(false, (xValue._flagged ? 1 : 0), null);
            values[5] = new IntegerValue(false, (uValue._flagged ? 1 : 0), null);
            values[6] = bValue;
            values[7] = uValue;
            generate(textLine, _fjaxhibdForm, values, _context._currentGenerationLCIndex);
        }

        return true;
    }

    /**
     * Resolves any lingering undefined references once initial assembly is complete...
     * These will be the forward-references we picked up along the way.
     * @param dictionary our source for looking up the references
     */
    private void resolveReferences(
        final Dictionary dictionary
    ) {
        for (TextLine line : _sourceCode) {
            for (int wx = 0; wx < line._generatedWords.size(); ++wx) {
                TextLine.GeneratedWord gw = line._generatedWords.get(wx);
                for ( Map.Entry<FieldDescriptor, IntegerValue> entry : gw._fields.entrySet() ) {
                    FieldDescriptor fd = entry.getKey();
                    IntegerValue originalIV = entry.getValue();
                    if (originalIV._undefinedReferences.length > 0) {
                        long newDiscreteValue = originalIV._value;
                        List<IntegerValue.UndefinedReference> newURefs = new LinkedList<>();
                        for (IntegerValue.UndefinedReference intURef : originalIV._undefinedReferences) {
                            try {
                                Value lookupValue = dictionary.getValue(intURef._reference);
                                if ( lookupValue.getType() != ValueType.Integer ) {
                                    Locale loc = new Locale(line._lineNumber, 0);
                                    line._diagnostics.append(
                                        new ValueDiagnostic(
                                            loc,
                                            "Forward reference does not resolve to an integer"));
                                } else {
                                    IntegerValue lookupIntegerValue = (IntegerValue) lookupValue;
                                    newDiscreteValue += (intURef._isNegative ? -1 : 1) * lookupIntegerValue._value;
                                    newURefs.addAll(Arrays.asList(lookupIntegerValue._undefinedReferences));
                                }
                            } catch ( NotFoundException ex ) {
                                //  reference is still not found - propagate it
                                newURefs.add(intURef);
                            }
                        }

                        IntegerValue newIV = new IntegerValue( originalIV._flagged,
                                                               newDiscreteValue,
                                                               newURefs.toArray(new IntegerValue.UndefinedReference[0]) );
                        gw._fields.put( fd, newIV );
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
     */
    public Assembler(
        final String[] source
    ) {
        _systemDictionary = new SystemDictionary();
        _globalDictionary = new Dictionary( _systemDictionary );
        _context = new Context( _globalDictionary );
        _sourceCode = new TextLine[source.length];
        for (int sx = 0; sx < source.length; ++sx) {
            int lineNumber = sx + 1;
            _sourceCode[sx] = new TextLine(lineNumber, source[sx]);
        }
    }

    /**
     * Advances the offset of a particular location counter
     * @param lcIndex index of the location counter
     * @param count amount by which the lc is to be offset - expected to be positive, but it works regardless
     */
    public void advanceLocation(
        final int lcIndex,
        final int count
    ) {
        if (!_codeCount.containsKey(lcIndex)) {
            _codeCount.put(lcIndex, count);
        } else {
            _codeCount.put(lcIndex, _codeCount.get(lcIndex) + count);
        }
    }

    /**
     * Assemble the source code in the object.
     * We do not do things quite in the same way as MASM.
     * @param moduleName name to be given to the module
     * @param display true to display source, code generation, etc on console
     * @return RelocatableModule we create if successful, else null
     */
    public RelocatableModule assemble(
        final String moduleName,
        final boolean display
    ) {
        if (display) {
            System.out.println(String.format("Assembling module %s -----------------------------------", moduleName));
        }

        //  setup
        _moduleName = moduleName;
        _context._characterMode = CharacterMode.ASCII;
        _context._codeMode = CodeMode.Basic;
        _diagnostics.clear();

        //  First step - parse all the source code into fields/subfields.
        for (TextLine line : _sourceCode) {
            line.parseFields();
            if (line._diagnostics.hasFatal()) {
                collectDiagnostics();
                return null;
            }
        }

        //  Next step - assemble all the things
        for (TextLine line : _sourceCode) {
            assembleTextLine(line);
            if (_diagnostics.hasFatal()) {
                collectDiagnostics();
                return null;
            }
        }

        resolveReferences(_context._dictionary);
        RelocatableModule module = generateRelocatableModule( moduleName );
        collectDiagnostics();

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
     * Creates an IntegerValue object with an appropriate undefined reference to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    public IntegerValue getCurrentLocation(
    ) {
        int lcIndex = _context._currentGenerationLCIndex;
        if (!_codeCount.containsKey( lcIndex )) {
            _codeCount.put( lcIndex, 0 );
        }

        int lcOffset = _codeCount.get( lcIndex );
        String ref = String.format( "%s_LC$BASE_%d", _moduleName, lcIndex );
        IntegerValue.UndefinedReference[] refs = { new IntegerValue.UndefinedReference( ref, false ) };
        return new IntegerValue(false, lcOffset, refs);
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
