/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.directives.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Assembler for minalib
 */
@SuppressWarnings("Duplicates")
public class Assembler {

    public enum Option {
        EMIT_DICTIONARY,
        EMIT_GENERATED_CODE,
        EMIT_MODULE_SUMMARY,
        EMIT_SOURCE,
    }

    //  Context under which assembly is performed
    private Context _context;

    //  Common forms we use for generating instructions
    private static final int[] _fjaxhiuFields = { 6, 4, 4, 4, 1, 1, 16 };
    private static final Form _fjaxhiuForm = new Form(_fjaxhiuFields);
    private static final int[] _fjaxuFields = { 6, 4, 4, 4, 18 };
    private static final Form _fjaxuForm = new Form(_fjaxuFields);
    private static final int[] _fjaxhibdFields = { 6, 4, 4, 4, 1, 1, 4, 12 };
    private static final Form _fjaxhibdForm = new Form(_fjaxhibdFields);

    //  A useful IntegerValue containing zero, no flags, and no unidentified references.
    private static final IntegerValue _zeroValue = new IntegerValue( false, 0, null );

    //  Singleton instance
    private final Assembler _instance;


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ---------------------------------------------------------------------------------------------------------------------------

    public Assembler() {
        _instance = this;
    }


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ---------------------------------------------------------------------------------------------------------------------------

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
    private static void displayModuleSummary(
        final RelocatableModule module
    ) {
        System.out.println("Rel Module Settings:");
        System.out.println(String.format("  Modes:%s%s%s%s",
                                         module._requiresQuarterWordMode ? "QWORD " : "",
                                         module._requiresThirdWordMode ? "TWORD " : "",
                                         module._requiresArithmeticFaultCompatibilityMode ? "ACOMP " : "",
                                         module._requiresArithmeticFaultNonInterruptMode ? "ANON " : ""));

        for (Map.Entry<Integer, LocationCounterPool> entry : module._storage.entrySet()) {
            int lcIndex = entry.getKey();
            LocationCounterPool lcp = entry.getValue();
            System.out.println(String.format("LCPool %d: %d word(s) generated %s",
                                             lcIndex,
                                             lcp._storage.length,
                                             lcp._needsExtendedMode ? "$INFO 10" : ""));
        }

        Set<UndefinedReference> references = new HashSet<>();
        System.out.println("Undefined References:");
        for (Map.Entry<Integer, LocationCounterPool> poolEntry : module._storage.entrySet()) {
            LocationCounterPool lcPool = poolEntry.getValue();
            for (RelocatableWord36 word36 : lcPool._storage) {
                if (word36 != null) {
                    for (UndefinedReference ur : word36._undefinedReferences) {
                        if (ur instanceof UndefinedReferenceToLabel) {
                            references.add(ur);
                        }
                    }
                }
            }
        }

        for (UndefinedReference ref : references) {
            System.out.println("  " + ref.toString());
        }
    }

    /**
     * Displays output upon the console
     */
    private void displayResults(
        final boolean displayCode
    ) {
        //  This is inefficient, but it only applies when the caller wants to display source output.
        while (_context.hasNextSourceLine()) {
            TextLine line = _context.getNextSourceLine();
            System.out.println(String.format("%04d:%s", line._lineNumber, line._text));

            for (Diagnostic d : _context._diagnostics.getDiagnostics(line._lineNumber)) {
                System.out.println(d.getMessage());
            }

            if (displayCode) {
                for (Map.Entry<Integer, Context.GeneratedPool> poolEntry : _context._generatedPools.entrySet()) {
                    int lcIndex = poolEntry.getKey();
                    Context.GeneratedPool gPool = poolEntry.getValue();
                    for (Map.Entry<Integer, Context.GeneratedWord> wordEntry : gPool.entrySet()) {
                        int lcOffset = wordEntry.getKey();
                        Context.GeneratedWord gWord = wordEntry.getValue();
                        if (gWord._locale.getLineNumber() == line._lineNumber) {
                            RelocatableWord36 rw36 = gWord.produceRelocatableWord36(_context._diagnostics);
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
        }
    }

    /**
     * Generates the RelocatableModule based on the various internal structures we've built up
     * @param moduleName name of the module
     * @param globalDictionary dictionary containing all the externalize (global) labels
     * @return RelocatableModule object unless there's a fatal error (then we return null)
     */
    private RelocatableModule generateRelocatableModule(
        final String moduleName,
        final Dictionary globalDictionary
    ) {
        Map<String, IntegerValue> externalLabels = new TreeMap<>();
        for (String label : globalDictionary.getLabels()) {
            try {
                Dictionary.ValueAndLevel val = globalDictionary.getValueAndLevel(label);
                if (val._level == 0) {
                    if ( val._value.getType() == ValueType.Integer ) {
                        externalLabels.put(label, (IntegerValue) val._value);
                    }
                }
            } catch (NotFoundException ex) {
                //  can't happen
            }
        }

        try {
            return new RelocatableModule.Builder().setName(moduleName)
                                                  .setStorage(_context.produceLocationCounterPools())
                                                  .setExternalLabels(externalLabels)
                                                  .setRequiresQuarterWordMode(_context._quarterWordMode)
                                                  .setRequiresThirdWordMode(_context._thirdWordMode)
                                                  .setArithmeticFaultCompatibilityMode(_context._arithmeticFaultCompatibilityMode)
                                                  .setArithmeticFaultNonInterruptMode(_context._arithmeticFaultNonInterruptMode)
                                                  .build();
        } catch (InvalidParameterException ex) {
            _context._diagnostics.append(new FatalDiagnostic(new Locale(1, 1),
                                                             "Could not generate relocatable module:" + ex.getMessage()));
            return null;
        }
    }

    /**
     * Interprets the label field to the extend possible when the purpose of the label is not known.
     * Calling code will do different things depending upon how the label (if any) is to be established.
     * @param labelField TextField containing the label field (might be null or empty)
     * @return an appropriately populated LabelFieldComponents object
     */
    private LabelFieldComponents interpretLabelField(
        final TextField labelField
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
                    _context._diagnostics.append(new ErrorDiagnostic(sfLocale, "Illegal location counter specification"));
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
                    _context._diagnostics.append(new ErrorDiagnostic(sfLocale, "Invalid label specified"));
                }
            }

            //  Warn on anything extra
            if (sfx < labelField._subfields.size()) {
                TextSubfield lcSubField = labelField._subfields.get(sfx);
                Locale sfLocale = lcSubField._locale;
                _context._diagnostics.append(new ErrorDiagnostic(sfLocale, "Extraneous label subfields ignored"));
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
     * @param labelFieldComponents represents the label field components, if any were specified
     * @param operandField represents the operand field, if any
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processDataGeneration(
        final LabelFieldComponents labelFieldComponents,
        final TextField operandField
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
            Expression e1 = p1.parse(_context);
            if (e1 == null) {
                return false;
            }
            firstValue = e1.evaluate(_context);
        } catch (ExpressionException eex) {
            _context._diagnostics.append(new ErrorDiagnostic(sf0Locale, "Syntax error in expression"));
        }

        if (labelFieldComponents._label != null) {
            _context.establishLabel(labelFieldComponents._labelLocale,
                                    labelFieldComponents._label,
                                    labelFieldComponents._labelLevel,
                                    _context.getCurrentLocation());
        }

        //TODO implement fp and string value handling

        if (firstValue instanceof FloatingPointValue) {
            FloatingPointValue fpValue = (FloatingPointValue) firstValue;
            //TODO here
            if (operandField._subfields.size() > 1) {
                Locale loc = operandField._subfields.get(1)._locale;
                _context._diagnostics.append(new ErrorDiagnostic(loc, "Too many subfields for data generation"));
            }

            return true;
        }

        if (firstValue instanceof StringValue) {
            StringValue sValue = (StringValue) firstValue;
            //TODO and here
            if (operandField._subfields.size() > 1) {
                Locale loc = operandField._subfields.get(1)._locale;
                _context._diagnostics.append(new ErrorDiagnostic(loc, "Too many subfields for data generation"));
            }

            return true;
        }

        if (firstValue instanceof IntegerValue) {
            //  Ensure the number of values divides evenly.
            int valueCount = (operandField._subfields.size());
            if (valueCount > 36) {
                _context._diagnostics.append(new ErrorDiagnostic(operandField._locale, "Improper number of data fields"));
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
                    Expression eNext = pNext.parse(_context);
                    if (eNext == null) {
                        _context._diagnostics.append(new ErrorDiagnostic(sf0Locale, "Expression expected"));
                        continue;
                    }

                    Value vNext = eNext.evaluate(_context);
                    if (vNext instanceof IntegerValue) {
                        values[vx] = (IntegerValue) vNext;
                    } else {
                        _context._diagnostics.append(new ValueDiagnostic(sfNextLocale, "Expected integer value"));
                    }
                } catch (ExpressionException ex) {
                    _context._diagnostics.append(new ErrorDiagnostic(sf0Locale, "Syntax error in expression"));
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

        _context._diagnostics.append(new ErrorDiagnostic(sf0Locale, "Wrong value type for data generation"));
        return true;
    }

    /**
     * Handles directives
     * @param directiveValue the DirectiveValue we are processing
     * @param textLine where this came from
     * @param labelFieldComponents represents the label field components, if any were specified
     * @param operationField represents the operation field, if any
     */
    private void processDirective(
        final DirectiveValue directiveValue,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents,
        final TextField operationField
    ) {
        try {
            Class<?> clazz = directiveValue._clazz;
            Constructor<?> ctor = clazz.getConstructor();
            Directive directive = (Directive) (ctor.newInstance());
            directive.process(_context, textLine, labelFieldComponents);
        } catch (IllegalAccessException
                 | InstantiationException
                 | NoSuchMethodException
                 | InvocationTargetException ex) {
            System.out.println("Caught:%s" + ex.toString() + ":" + ex.getMessage());
            _context._diagnostics.append(new FatalDiagnostic(operationField._locale,
                                                             "Internal Error in Assembler.processDirective()"));
        }
    }

    /**
     * Handles instruction mnemonic lines of code
     * @param labelFieldComponents represents the label field components, if any were specified
     * @param operationField represents the operation field, if any
     * @param operandField represents the operand field, if any
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processMnemonic(
        final LabelFieldComponents labelFieldComponents,
        final TextField operationField,
        final TextField operandField
    ) {
        //  Deal with the operation field
        TextSubfield mnemonicSubfield = operationField.getSubfield(0);
        InstructionWord.InstructionInfo iinfo = null;
        try {
            iinfo = processMnemonicOperationField(mnemonicSubfield);
        } catch (NotFoundException ex) {
            //  mnemonic not found - return false
            return false;
        }

        if (operandField == null) {
            _context._diagnostics.append(new ErrorDiagnostic(operationField._locale,
                                                             "Instruction mnemonic requires an operand field"));
            return true;
        }

        //  Establish the label to refer to the current lc pool's current offset (if there is a label).
        //  Use the label level to establish which dictionary level it should be placed in.
        if (labelFieldComponents._label != null) {
            _context.establishLabel(labelFieldComponents._labelLocale,
                                    labelFieldComponents._label,
                                    labelFieldComponents._labelLevel,
                                    _context.getCurrentLocation());
        }

        int jField = processMnemonicGetJField(iinfo, operationField);

        //  We have to be in extended mode, *not* using basic mode semantics, and
        //  either the j-field is part of the instruction, or else it is no U or XU...
        //  If that is the case, then we allow (maybe even require) a base register specification.
        boolean baseSubfieldAllowed =
                (_context._codeMode == CodeMode.Extended)
                && !iinfo._useBMSemantics
                && (iinfo._jFlag || (jField < 016));

        TextSubfield[] opSubfields = processMnemonicGetOperandSubfields(iinfo, operandField, baseSubfieldAllowed);
        IntegerValue aValue = processMnemonicGetAField(iinfo, operandField, opSubfields[0]);
        IntegerValue uValue = processMnemonicGetUField(operandField, opSubfields[1]);
        IntegerValue xValue = processMnemonicGetXField(opSubfields[2]);
        IntegerValue bValue = baseSubfieldAllowed ? processMnemonicGetBField(opSubfields[3]) : _zeroValue;

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
     * Determine the value for the instruction's a-field
     * @param instructionInfo InstructionInfo for the instruction we're generating
     * @param operandField operand field in case we need that locale
     * @param registerSubfield register (a-field) subfield text
     * @return value representing the A-field (which might have a flagged value)
     */
    private IntegerValue processMnemonicGetAField(
        final InstructionWord.InstructionInfo instructionInfo,
        final TextField operandField,
        final TextSubfield registerSubfield
    ) {
        IntegerValue aValue = _zeroValue;

        if (instructionInfo._aFlag) {
            aValue = new IntegerValue(false, instructionInfo._aField, null);
        } else {
            if ((registerSubfield == null) || (registerSubfield._text.isEmpty())) {
                _context._diagnostics.append(new ErrorDiagnostic(operandField._locale,
                                                                 "Missing register specification"));
            } else {
                try {
                    ExpressionParser p = new ExpressionParser(registerSubfield._text, registerSubfield._locale);
                    Expression e = p.parse(_context);
                    if (e == null) {
                        _context._diagnostics.append(new ErrorDiagnostic(registerSubfield._locale,
                                                                         "Syntax Error"));
                    } else {
                        Value v = e.evaluate(_context);
                        if (!(v instanceof IntegerValue)) {
                            _context._diagnostics.append(new ValueDiagnostic(registerSubfield._locale,
                                                                             "Wrong value type"));
                        } else {
                            //  Reduce the value appropriately for the a-field
                            aValue = (IntegerValue) v;
                            switch (instructionInfo._aSemantics) {
                                case A:
                                    aValue = new IntegerValue(aValue._flagged, aValue._value - 12, aValue._undefinedReferences);
                                    break;

                                case R:
                                    aValue = new IntegerValue(aValue._flagged, aValue._value - 64, aValue._undefinedReferences);
                                    break;
                            }
                        }
                    }
                } catch (ExpressionException ex) {
                    _context._diagnostics.append(new ErrorDiagnostic(registerSubfield._locale,
                                                                     "Syntax Error"));
                }
            }
        }

        return aValue;
    }

    /**
     * Determine the value for the instruction's b-field
     * @param baseSubfield index (b-field) subfield text
     * @return value representing the B-field
     */
    private IntegerValue processMnemonicGetBField(
        final TextSubfield baseSubfield
    ) {
        IntegerValue bValue = _zeroValue;

        if ((baseSubfield != null) && !baseSubfield._text.isEmpty()) {
            try {
                ExpressionParser p = new ExpressionParser(baseSubfield._text, baseSubfield._locale);
                Expression e = p.parse(_context);
                if (e == null) {
                    _context._diagnostics.append(new ErrorDiagnostic(baseSubfield._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(_context);
                    if (!(v instanceof IntegerValue)) {
                        _context._diagnostics.append(new ValueDiagnostic(baseSubfield._locale, "Wrong value type"));
                    } else {
                        bValue = (IntegerValue) v;
                    }
                }
            } catch (ExpressionException ex) {
                _context._diagnostics.append(new ErrorDiagnostic(baseSubfield._locale, "Syntax Error"));
            }
        }

        return bValue;
    }

    /**
     * Processes the mnemonic in order to determine the j-field for the instruction word.
     * If j-flag is set, we pull j-field from the iinfo object.  Otherwise, we interpret the j-field.
     * @param instructionInfo InstructionInfo object describing the instruction we're building
     * @param operationField operation field which might contain a j-field specification
     * @return value for instruction's j-field
     */
    private int processMnemonicGetJField(
        final InstructionWord.InstructionInfo instructionInfo,
        final TextField operationField
    ) {
        int jField = 0;
        if (instructionInfo._jFlag) {
            jField = instructionInfo._jField;
            if (operationField._subfields.size() > 1) {
                _context._diagnostics.append(new ErrorDiagnostic(operationField.getSubfield(1)._locale,
                                                                 "Extraneous subfields in operation field"));
            }
        } else if (operationField._subfields.size() > 1) {
            TextSubfield jSubField = operationField.getSubfield(1);
            try {
                jField = InstructionWord.getJFieldValue(jSubField._text);
            } catch ( NotFoundException e ) {
                _context._diagnostics.append(new ErrorDiagnostic(jSubField._locale,
                                                                 "Invalid text for j-field of instruction"));
            }

            if ( operationField._subfields.size() > 2 ) {
                _context._diagnostics.append(new ErrorDiagnostic(operationField.getSubfield(1)._locale,
                                                                 "Extraneous subfields in operation field"));
            }
        }

        return jField;
    }

    /**
     * Find the subfields... if iinfo's a-flag is set, then the iinfo a-field is used for the instruction
     * a field, and there isn't one in the syntax for the operand field.
     * If the flag is clear, then the first subfield is a register specification... which can get a bit
     * complicated as well, since it might be an a-register, an x-register, or an r-register (or even a b...)
     * @param instructionInfo info regarding the instruction we are generating
     * @param operandField operand field for the line of text
     * @param baseSubfieldAllowed true if we are in extended mode and the instruction allows a base register specificaiton
     * @return array of four TextSubfield references, some of which might be null
     */
    private TextSubfield[] processMnemonicGetOperandSubfields(
        final InstructionWord.InstructionInfo instructionInfo,
        final TextField operandField,
        final boolean baseSubfieldAllowed
    ) {
        TextSubfield sfRegister = null;
        TextSubfield sfValue = null;
        TextSubfield sfIndex = null;
        TextSubfield sfBase = null;

        int sfx = 0;
        int sfc = operandField._subfields.size();

        if (!instructionInfo._aFlag && (sfc > sfx)) {
            sfRegister = operandField.getSubfield(sfx++);
        }

        if (sfc > sfx) {
            sfValue = operandField.getSubfield(sfx++);
        }

        if (sfc > sfx) {
            sfIndex = operandField.getSubfield(sfx++);
        }

        if ((sfc > sfx) && baseSubfieldAllowed) {
            sfBase = operandField.getSubfield(sfx++);
        }

        if (sfc > sfx) {
            _context._diagnostics.append(new ErrorDiagnostic( operandField.getSubfield( sfx )._locale,
                                                              "Extreanous subfields in operand field ignored"));
        }

        TextSubfield[] result = { sfRegister, sfValue, sfIndex, sfBase };
        return result;
    }

    /**
     * Determine the value for the instruction's u-field (or d-field)
     * Whatever we do here is valid for u-field for basic mode, and d-field for extended mode.
     * @param operandField operand field in case we need that locale
     * @param valueSubfield value (u-field) subfield text
     * @return value representing the U-field (which might have a flagged value)
     */
    private IntegerValue processMnemonicGetUField(
        final TextField operandField,
        final TextSubfield valueSubfield
    ) {
        IntegerValue uValue = _zeroValue;

        if ((valueSubfield == null) || (valueSubfield._text.isEmpty())) {
            _context._diagnostics.append(new ErrorDiagnostic(operandField._locale,
                                                             "Missing operand value (U, u, or d subfield)"));
        } else {
            try {
                ExpressionParser p = new ExpressionParser(valueSubfield._text, valueSubfield._locale);
                Expression e = p.parse(_context);
                if (e == null) {
                    _context._diagnostics.append(new ErrorDiagnostic(valueSubfield._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(_context);
                    if (!(v instanceof IntegerValue)) {
                        _context._diagnostics.append(new ValueDiagnostic(valueSubfield._locale, "Wrong value type"));
                    } else {
                        uValue = (IntegerValue) v;
                    }
                }
            } catch (ExpressionException ex) {
                _context._diagnostics.append(new ErrorDiagnostic(valueSubfield._locale, "Syntax Error"));
            }
        }

        return uValue;
    }

    /**
     * Determine the value for the instruction's x-field
     * @param indexSubfield index (x-field) subfield text
     * @return value representing the X-field (which might have a flagged value)
     */
    private IntegerValue processMnemonicGetXField(
        final TextSubfield indexSubfield
    ) {
        IntegerValue xValue = _zeroValue;

        if ((indexSubfield != null) && !indexSubfield._text.isEmpty()) {
            try {
                ExpressionParser p = new ExpressionParser(indexSubfield._text, indexSubfield._locale);
                Expression e = p.parse(_context);
                if (e == null) {
                    _context._diagnostics.append(new ErrorDiagnostic(indexSubfield._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(_context);
                    if (!(v instanceof IntegerValue)) {
                        _context._diagnostics.append(new ValueDiagnostic(indexSubfield._locale, "Wrong value type"));
                    } else {
                        xValue = (IntegerValue) v;
                    }
                }
            } catch (ExpressionException ex) {
                _context._diagnostics.append(new ErrorDiagnostic(indexSubfield._locale, "Syntax Error"));
            }
        }

        return xValue;
    }

    /**
     * Process the subfield presumably containing the mnemonic
     * @param subfield subfield containing the mnemonic
     * @return pointer to InstructionInfo object if found - note that it might not be appropriate for the given context
     * @throws NotFoundException if we don't find a valid mnemonic at all
     */
    private InstructionWord.InstructionInfo processMnemonicOperationField(
        final TextSubfield subfield
    ) throws NotFoundException {
        try {
            InstructionWord.Mode imode =
                _context._codeMode == CodeMode.Extended ? InstructionWord.Mode.EXTENDED : InstructionWord.Mode.BASIC;
            return InstructionWord.getInstructionInfo(subfield._text, imode);
        } catch (NotFoundException ex) {
            //  Mnemonic not found - is it dependent on code mode?
            //  If so, coder is asking for a mnemonic in a mode it doesn't exist in; raise a diagnostic and
            //  return true so the assemble method doesn't go any further with this line.
            //  Otherwise, it's just flat not a mnemonic, so return false and let the assemble method do
            //  something else.
            InstructionWord.Mode imode =
                _context._codeMode == CodeMode.Extended ? InstructionWord.Mode.BASIC : InstructionWord.Mode.EXTENDED;
            InstructionWord.InstructionInfo iinfo = InstructionWord.getInstructionInfo(subfield._text, imode);
            _context._diagnostics.append(new ErrorDiagnostic(subfield._locale,
                                                             "Opcode not valid for the current code mode"));
            return iinfo;
        }
    }

    /**
     * Resolves any lingering undefined references once initial assembly is complete...
     * These will be the forward-references we picked up along the way.
     * No point checking for loc ctr refs, those aren't resolved until link time.
     */
    private void resolveReferences(
    ) {
        for (Map.Entry<Integer, Context.GeneratedPool> poolEntry : _context._generatedPools.entrySet()) {
            Context.GeneratedPool pool = poolEntry.getValue();
            for (Map.Entry<Integer, Context.GeneratedWord> wordEntry : pool.entrySet()) {
                Context.GeneratedWord gWord = wordEntry.getValue();
                for (Map.Entry<FieldDescriptor, IntegerValue> entry : gWord.entrySet()) {
                    FieldDescriptor fd = entry.getKey();
                    IntegerValue originalIV = entry.getValue();
                    if (originalIV._undefinedReferences.length > 0) {
                        long newDiscreteValue = originalIV._value;
                        List<UndefinedReference> newURefs = new LinkedList<>();
                        for (UndefinedReference uRef : originalIV._undefinedReferences) {
                            if (uRef instanceof UndefinedReferenceToLabel) {
                                UndefinedReferenceToLabel lRef = (UndefinedReferenceToLabel) uRef;
                                try {
                                    Value lookupValue = _context._dictionary.getValue(lRef._label);
                                    if (lookupValue.getType() != ValueType.Integer) {
                                        _context._diagnostics.append(
                                            new ValueDiagnostic(gWord._locale,
                                                                "Forward reference does not resolve to an integer"));
                                    } else {
                                        IntegerValue lookupIntegerValue = (IntegerValue) lookupValue;
                                        newDiscreteValue += (lRef._isNegative ? -1 : 1) * lookupIntegerValue._value;
                                        newURefs.addAll(Arrays.asList(lookupIntegerValue._undefinedReferences));
                                    }
                                } catch (NotFoundException ex) {
                                    //  reference is still not found - propagate it
                                    newURefs.add(uRef);
                                }
                            } else {
                                newURefs.add(uRef);
                            }
                        }

                        IntegerValue newIV = new IntegerValue(originalIV._flagged,
                                                              newDiscreteValue,
                                                              newURefs.toArray(new UndefinedReference[0]));
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
     * Assemble the source code in the object.
     * We do not do things quite in the same way as MASM.
     * @param moduleName name to be given to the relocatable module
     * @param source array of strings comprising the source code to be assembled
     * @param optionSet Selection of options which should apply to this assembly
     * @return RelocatableModule we create if successful, else null
     */
    public RelocatableModule assemble(
        final String moduleName,
        final String[] source,
        final Option[] optionSet
    ) {
        System.out.println(String.format("Assembling module %s -----------------------------------", moduleName));

        //  setup
        Dictionary globalDictionary = new Dictionary(new SystemDictionary());
        _context = new Context(globalDictionary, source, moduleName);

        _context._characterMode = CharacterMode.ASCII;
        _context._codeMode = CodeMode.Basic;

        //  Assemble all the things
        while (_context.hasNextSourceLine()) {
            TextLine textLine = _context.getNextSourceLine();
            assembleTextLine(textLine);
            if (_context._diagnostics.hasFatal()) {
                displayResults(false);
                return null;
            }
        }

        resolveReferences();
        RelocatableModule module = generateRelocatableModule(moduleName, globalDictionary);

        boolean displayCode = false;
        boolean displayDictionary = false;
        boolean displaySource = false;
        boolean displayModuleSummary = false;
        for (Option opt : optionSet) {
            switch (opt) {
                case EMIT_DICTIONARY:
                    displayDictionary = true;
                    break;
                case EMIT_GENERATED_CODE:
                    displayCode = true;
                    displaySource = true;   //  implied
                    break;
                case EMIT_SOURCE:
                    displaySource = true;
                    break;
                case EMIT_MODULE_SUMMARY:
                    displayModuleSummary = true;
                    break;
            }
        }

        if (displaySource) {
            displayResults(displayCode);
        }
        if (displayModuleSummary && (module != null)) {
            displayModuleSummary(module);
        }
        if (displayDictionary) {
            displayDictionary(_context._dictionary);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Summary: Lines=%d", _context._sourceObjects.length));
        for (Map.Entry<Diagnostic.Level, Integer> entry : _context._diagnostics.getCounters().entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(String.format(" %c=%d", Diagnostic.getLevelIndicator(entry.getKey()), entry.getValue()));
            }
        }
        System.out.println(sb.toString());

        System.out.println("Assembly Ends -------------------------------------------------------");

        return module;
    }

    /**
     * Assemble a single TextLine object into the Relocatable Module
     * @param textLine entity to be assembled
     */
    //TODO this might go back to being private
    public void assembleTextLine(
        final TextLine textLine
    ) {
        if (textLine._fields.isEmpty()) {
            return;
        }

        TextField labelField = textLine.getField(0);
        TextField operationField = textLine.getField(1);
        TextField operandField = textLine.getField(2);

        //  Interpret label field and update current location counter index if appropriate.
        //  We can't do anything with the label at this point (what we do depends on the operator field),
        //  but if there is a location counter spec, it will always set the current generation lc index.
        //  So do that part of it here.
        LabelFieldComponents lfc = interpretLabelField(labelField);
        if (lfc._lcIndex != null) {
            _context._currentGenerationLCIndex = lfc._lcIndex;
        }

        if ((operationField == null) || (operationField._subfields.isEmpty())) {
            //  This is a no-op line - but it might have a label.  Honor the label, if there is one.
            if (lfc._label != null) {
                _context.establishLabel(lfc._labelLocale, lfc._label, lfc._labelLevel, _context.getCurrentLocation());
            }
            return;
        }

        String operation = operationField._subfields.get(0)._text.toUpperCase();

        //  Check the dictionary...
        try {
            Value v = _context._dictionary.getValue(operation);
            if (v instanceof ProcedureValue) {
                //  TODO
            }
            else if (v instanceof FormValue) {
                //  TODO
            } else if (v instanceof DirectiveValue) {
                processDirective((DirectiveValue) v, textLine, lfc, operationField);
                return;
            }
        } catch (NotFoundException ex) {
            //  ignore it and drop through
        }

        //  Does this line of code represent an instruction mnemonic?  (or a label on an otherwise empty line)...
        if (processMnemonic(lfc, operationField, operandField)) {
            if (textLine._fields.size() > 3) {
                _context._diagnostics.append(new ErrorDiagnostic(textLine.getField(3)._locale,
                                                                 "Extraneous fields ignored"));
            }
            return;
        }

        //  Is it an expression (or a list of expressions)?
        //  In this case, the operation field actually contains the operand, while the operand field should be empty.
        if (processDataGeneration(lfc, operationField)) {
            if (textLine._fields.size() > 2) {
                _context._diagnostics.append(new ErrorDiagnostic(textLine.getField(2)._locale,
                                                                 "Extraneous fields ignored"));
            }
            return;
        }

        _context._diagnostics.append(new ErrorDiagnostic(new Locale(textLine._lineNumber, 1),
                                                         "Unrecognizable source code"));
    }

    /**
     * Singleton instance getter
     * @return
     */
    public Assembler getInstance() {
        return _instance;
    }

    /**
     * Getter
     * @return Diagnostics object produced during assembly
     */
    Diagnostics getLastDiagnostics(
    ) {
        return _context._diagnostics;
    }

    /**
     * Getter
     * @return parsed code
     */
    TextLine[] getParsedCode(
    ) {
        return _context._sourceObjects;
    }
}
