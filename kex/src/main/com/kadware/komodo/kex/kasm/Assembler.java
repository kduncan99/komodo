/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.baselib.exceptions.CharacteristicOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristicUnderflowException;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.directives.Directive;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Assembler for kasm
 */
@SuppressWarnings("Duplicates")
public class Assembler {

    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Inner classes
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Entities which are not local to a particular assembler level
     */
    static class GlobalData {

        final Diagnostics _diagnostics = new Diagnostics();
        final Dictionary _globalDictionary = new Dictionary(new SystemDictionary());
        final Set<AssemblerOption> _options;
        final PrintStream _outputStream;

        boolean _arithmeticFaultCompatibilityMode = false;
        boolean _arithmeticFaultNonInterruptMode = false;
        boolean _quarterWordMode = false;
        boolean _thirdWordMode = false;

        ProgramStart _programStart = null;

        //  Map of LC indices to the various GeneratedPool objects...
        //  Keyed by location counter index.
        final GeneratedPools _generatedPools = new GeneratedPools();

        GlobalData(
            Set<AssemblerOption> options,
            PrintStream outputStream
        ) {
            _options = options;
            _outputStream = outputStream;
        }
    }


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ---------------------------------------------------------------------------------------------------------------------------

    //  Common forms we use for generating instructions
    private static final int[] _fjaxhiuFields = { 6, 4, 4, 4, 1, 1, 16 };
    private static final Form _fjaxhiuForm = new Form(_fjaxhiuFields);
    private static final int[] _fjaxuFields = { 6, 4, 4, 4, 18 };
    private static final Form _fjaxuForm = new Form(_fjaxuFields);
    private static final int[] _fjaxhibdFields = { 6, 4, 4, 4, 1, 1, 4, 12 };
    private static final Form _fjaxhibdForm = new Form(_fjaxhibdFields);

    private final int _level;
    private final String _moduleName;
    private final Assembler _outerLevel;    //  parent to this assembler, if this is not the top level
    private final TextLine[] _sourceLines;

    private final Dictionary _dictionary;

    private CharacterMode _characterMode = CharacterMode.ASCII;
    private CodeMode _codeMode = CodeMode.Basic;
    private int _currentGenerationLCIndex = 0;
    private int _currentLiteralLCIndex = 0;
    private boolean _endFound = false;      //  true if this (sub)assembly has processed an $END directive
    private Value _endValue = null;         //  the value of {e} on the $END directive which ends this (sub)assembly (if any)
    private int _nextSourceIndex = 0;

    private final GlobalData _global;


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor for an outer-level assembler
     * @param moduleName name of the module being created
     * @param source body of source code to be assembled
     * @param options includes options to be observed during the compilation process
     * @param outputStream where all diagnostics are emitted
     */
    Assembler(
        final String moduleName,
        final String[] source,
        final Set<AssemblerOption> options,
        final OutputStream outputStream
    ) {
        _global = new GlobalData(options, new PrintStream(outputStream));
        _level = 0;
        _moduleName = moduleName;
        _outerLevel = null;
        _sourceLines = new TextLine[source.length];
        for (int sx = 0; sx < source.length; ++sx) {
            _sourceLines[sx] = new TextLine(new LineSpecifier(0, sx + 1), source[sx]);
        }

        _dictionary = new Dictionary(_global._globalDictionary);
    }

    /**
     * Constructor for an inner-level assembler - only for SubAssembler to invoke
     * @param outerLevel the assembler directly above this one
     * @param subModuleName name of the submodule being assembled
     * @param sourceLines subset of code to be assembled, presented as TextLine objects
     */
    Assembler(
        final Assembler outerLevel,
        final String subModuleName,
        final TextLine[] sourceLines
    ) {
        _global = outerLevel._global;
        _level = outerLevel._level + 1;
        _moduleName = subModuleName;
        _outerLevel = outerLevel;
        _sourceLines = sourceLines;

        _dictionary = new Dictionary(outerLevel._dictionary);

        _characterMode = outerLevel._characterMode;
        _codeMode = outerLevel._codeMode;
    }

    public static class Builder {

        private String _moduleName = "<unnamed>";
        private HashSet<AssemblerOption> _options = new HashSet<>();
        private OutputStream _outputStream = System.out;
        private String[] _source = new String[0];

        public Builder addOption(AssemblerOption option) { _options.add(option); return this; }
        public Builder setModuleName(String moduleName) { _moduleName = moduleName; return this; }
        public Builder setOptions(AssemblerOption[] options) { _options = new HashSet<>(Arrays.asList(options)); return this; }
        public Builder setOptions(Collection<AssemblerOption> options) { _options = new HashSet<>(options); return this; }
        public Builder setOutputStream(OutputStream stream) { _outputStream = stream; return this; }
        public Builder setSource(String[] source) { _source = Arrays.copyOf(source, source.length); return this; }
        public Builder setSource(Collection<String> source) { _source = source.toArray(new String[0]); return this; }

        public Assembler build() {
            return new Assembler(_moduleName, _source, _options ,_outputStream);
        }
    }


    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Assemble a single TextLine object
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

        if (_endFound) {
            appendDiagnostic(new ErrorDiagnostic(labelField._locale, "Label and/or Code follows $END directive"));
            return;
        }

        //  Interpret label field and update current location counter index if appropriate.
        //  We can't do anything with the label at this point (what we do depends on the operator field),
        //  but if there is a location counter spec, it will always set the current generation lc index.
        //  So do that part of it here.
        LabelFieldComponents lfc = interpretLabelField(labelField);
        if (lfc._lcIndex != null) {
            setCurrentGenerationLCIndex(lfc._lcIndex);
        }

        if ((operationField == null) || (operationField._subfields.isEmpty())) {
            //  This is a no-op line - but it might have a label.  Honor the label, if there is one.
            if (lfc._label != null) {
                establishLabel(lfc._labelLocale, lfc._label, lfc._labelLevel, getCurrentLocation());
            }
            return;
        }

        String operation = operationField._subfields.get(0)._text.toUpperCase();

        //  Check the dictionary...
        try {
            Dictionary.ValueInfo valueInfo = _dictionary.getValueInfo(operation);
            if (valueInfo._value instanceof ProcedureValue) {
                processProcedure(operation, (ProcedureValue) valueInfo._value, textLine);
                return;
            } else if (valueInfo._value instanceof FormValue) {
                processForm((FormValue) valueInfo._value, operandField);
                return;
            } else if (valueInfo._value instanceof DirectiveValue) {
                processDirective((DirectiveValue) valueInfo._value, textLine, lfc, operationField);
                return;
            } else {
                appendDiagnostic(new ErrorDiagnostic(operationField._locale,
                                                     "Dictionary value '" + operation + "' used incorrectly"));
                return;
            }
        } catch (NotFoundException ex) {
            //  ignore it and drop through
        }

        //  Does this line of code represent an instruction mnemonic?  (or a label on an otherwise empty line)...
        if (processMnemonic(lfc, operationField, operandField)) {
            if (textLine._fields.size() > 3) {
                appendDiagnostic(new ErrorDiagnostic(textLine.getField(3)._locale,
                                                     "Extraneous fields ignored"));
            }
            return;
        }

        //  Is it an expression (or a list of expressions)?
        //  In this case, the operation field actually contains the operand, while the operand field should be empty.
        if (processDataGeneration(lfc, operationField)) {
            if (textLine._fields.size() > 2) {
                appendDiagnostic(new ErrorDiagnostic(textLine.getField(2)._locale,
                                                     "Extraneous fields ignored"));
            }
            return;
        }

        appendDiagnostic(new ErrorDiagnostic(new Locale(textLine._lineSpecifier, 1),
                                             "Unparseable source code"));
    }

    /**
     * Generates the RelocatableModule based on the various internal structures we've built up
     * @param moduleName name of the module
     * @return RelocatableModule object unless there's a fatal error (then we return null)
     */
    private RelocatableModule createRelocatableModule(
        final String moduleName
    ) {
        RelocatableModule.RelativeEntryPoint entryPoint = null;
        if (_global._programStart != null) {
            entryPoint = new RelocatableModule.RelativeEntryPoint("START$",
                                                                  _global._programStart._locationCounterOffset,
                                                                  _global._programStart._locationCounterIndex);
        }
        RelocatableModule module = new RelocatableModule(moduleName,
                                                         _global._arithmeticFaultNonInterruptMode,
                                                         _global._arithmeticFaultCompatibilityMode,
                                                         _global._quarterWordMode,
                                                         _global._thirdWordMode,
                                                         entryPoint);

        try {
            //  Create location counter pools and info 10 items
            for (Map.Entry<Integer, GeneratedPool> entry : _global._generatedPools.entrySet()) {
                int lcIndex = entry.getKey();
                GeneratedPool gp = entry.getValue();
                module.establishLocationCounterPool(lcIndex, gp.getExtendedModeFlag(), gp.produceLocationCounterPool());
                if (gp.getExtendedModeFlag()) {
                    module.addControlInformation(new RelocatableModule.Info10ControlInformation(lcIndex));
                }
            }

            //  External symbols
            for (Dictionary.ValueInfo vInfo : _global._globalDictionary.getAllValueInfos()) {
                //  At this point we should expect the value to be an integer with proper relocation
                IntegerValue iv = (IntegerValue) vInfo._value;
                RelocatableModule.EntryPoint ep;
                if ((iv._references == null) || (iv._references.length == 0)) {
                    ep = new RelocatableModule.AbsoluteEntryPoint(vInfo._label, iv._value.get().longValue());
                } else {
                    UnresolvedReferenceToLocationCounter urlc = (UnresolvedReferenceToLocationCounter) iv._references[0];
                    ep = new RelocatableModule.RelativeEntryPoint(vInfo._label,
                                                                  iv._value.get().intValue(),
                                                                  urlc._locationCounterIndex);
                }
                module.addEntryPoint(ep);
            }

            //  Control info objects not already created elsewhere
            //  TODO

            //  Element table flag bits
            int flagBits = (_global._arithmeticFaultNonInterruptMode ? 0100 : 0)
                           | (_global._arithmeticFaultCompatibilityMode ? 0040 : 0)
                           | (_global._thirdWordMode ? 0004 : 0)
                           | (_global._thirdWordMode ? 0002 : 0)
                           | (_global._diagnostics.hasError() ? 0001 : 0);
            module.establishElementTableFlagBits(flagBits);

            //  Is there a start address?
            //  TODO
        } catch (ParameterException ex) {
            appendDiagnostic(new FatalDiagnostic("Caught " + ex.getMessage()));
        }

        return module;
    }

    /**
     * Displays the content of a particular dictionary
     * @param dictionary dictionary to be displayed
     */
    private void displayDictionary(
        final Dictionary dictionary
    ) {
        _global._outputStream.println("Main Assembly Dictionary:");
        for (Dictionary.ValueInfo vInfo : dictionary.getAllValueInfos()) {
            System.out.println("  " +
                               vInfo._label +
                               ": " +
                               vInfo._value.toString());
        }

        _global._outputStream.println("External Labels:");
        for (Dictionary.ValueInfo vInfo : _global._globalDictionary.getAllValueInfos()) {
            System.out.println("  " +
                               vInfo._label +
                               ": " +
                               vInfo._value.toString());
        }
    }

    /**
     * Summary of relocatable module
     * @param module relocatable module
     */
    private void displayRelocatableModuleSummary(
        final RelocatableModule module
    ) {
        _global._outputStream.println("Rel Module Settings:");
        _global._outputStream.println(String.format("  Modes:%s%s%s%s",
                                            module.getQuarterWordSensitivity() ? "QWORD " : "",
                                            module.getThirdWordSensitivity() ? "TWORD " : "",
                                            module.getAFCMSetSensitivity() ? "AFCMSet " : "",
                                            module.getAFCMClearSensitivity() ? "AFCMClear " : ""));

        for (int lcIndex : module.getEstablishedLocationCounterIndices()) {
            try {
                RelocatableModule.RelocatablePool rcPool = module.getLocationCounterPool(lcIndex);
                _global._outputStream.println(String.format("LC$(%d): %d word(s) generated", lcIndex, rcPool._content.length));
            } catch (ParameterException ex) {
                //  can't happen
            }
        }

        Set<String> references = new TreeSet<>();
        for (int lcIndex : module.getEstablishedLocationCounterIndices()) {
            try {
                RelocatableModule.RelocatablePool rcPool = module.getLocationCounterPool(lcIndex);
                for (RelocatableModule.RelocatableWord rw : rcPool._content) {
                    if (rw != null) {
                        for (RelocatableModule.RelocatableItem ri : rw._relocatableItems) {
                            if (ri instanceof RelocatableModule.RelocatableItemSymbol) {
                                RelocatableModule.RelocatableItemSymbol ris = (RelocatableModule.RelocatableItemSymbol) ri;
                                references.add(ris._undefinedSymbol);
                            }
                        }
                    }
                }
            } catch (ParameterException ex) {
                //  can't happen
            }
        }

        _global._outputStream.println("Undefined References:");
        for (String ref : references) {
            _global._outputStream.println("  " + ref);
        }
    }

    /**
     * Displays output upon the console
     * @param displayCode true to display generated code
     * @param module the generated relocatable module
     */
    private void displayResults(
        final boolean displayCode,
        final RelocatableModule module
    ) {
        //  This is inefficient, but it only applies when the caller wants to display source output.
        for (TextLine line : _sourceLines) {
            _global._outputStream.println(String.format("%s:%s", line._lineSpecifier, line._text));

            for (Diagnostic d : line._diagnostics) {
                _global._outputStream.println(d.getMessage());
            }

            if (displayCode && (module != null)) {
                for (GeneratedWord gw : line._generatedWords) {
                    try {
                        RelocatableModule.RelocatablePool pool = module.getLocationCounterPool(gw._locationCounterIndex);
                        RelocatableModule.RelocatableWord rw = pool._content[gw._locationCounterOffset];
                        String gwBase = String.format("  $(%2d) %06o:  %012o",
                                                      gw._locationCounterIndex,
                                                      gw._locationCounterOffset,
                                                      rw.getW());
                        if (rw._relocatableItems.length == 0) {
                            _global._outputStream.println(gwBase);
                        } else {
                            for (int urx = 0; urx < rw._relocatableItems.length; ++urx) {
                                RelocatableModule.RelocatableItem ri = rw._relocatableItems[urx];
                                _global._outputStream.println(gwBase + ri.toString());
                                gwBase = "                             ";
                            }
                        }
                    } catch (ParameterException ex) {
                        //  should never happen
                    }
                }
            }
        }

        for (Diagnostic d : _global._diagnostics.getDiagnostics()) {
            if (d._locale == null) {
                _global._outputStream.println(d.getMessage());
            }
        }
    }

    /**
     * Generates data into the given context representing this value
     * @param fpValue value object
     * @param locale locale of the code which generated this word
     */
    private void generateFloatingPoint(
        final FloatingPointValue fpValue,
        final Locale locale
    ) {
        try {
            if (fpValue._precision == ValuePrecision.Single) {
                Word36 w36 = fpValue._value.toWord36();
                long[] word36 = { w36.getW() };
                _global._generatedPools.generate(getTopLevelTextLine(), locale, _currentGenerationLCIndex, word36);
            } else {
                //  double precision generation is the default...
                DoubleWord36 dw36 = fpValue._value.toDoubleWord36();
                Word36[] word36s = dw36.getWords();
                long[] word36 = { word36s[0].getW(), word36s[1].getW() };
                _global._generatedPools.generate(getTopLevelTextLine(), locale, _currentGenerationLCIndex, word36);
            }
        } catch (CharacteristicOverflowException ex) {
            appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic overflow"));
        } catch (CharacteristicUnderflowException ex) {
            appendDiagnostic(new ErrorDiagnostic(locale, "Characteristic underflow"));
        }
    }

    /**
     * Generates data into the given context representing this value.
     */
    private void generateInteger(
        final TextField operandField,
        final Locale locale,
        final Value firstValue
    ) {
        int valueCount = (operandField._subfields.size());
        if (valueCount > 36) {
            appendDiagnostic(new ErrorDiagnostic(operandField._locale, "Improper number of data fields"));
            return;
        }

        int[] fieldSizes = new int[valueCount];
        int fieldSize = 36 / valueCount;
        for (int fx = 0; fx < valueCount; ++fx) {
            fieldSizes[fx] = fieldSize;
        }

        BigInteger intValue = BigInteger.ZERO;
        List<UnresolvedReference> newRefs = new LinkedList<>();
        int startingBit = 0;
        for (int vx = 0; vx < valueCount; ++vx) {
            if (vx > 0) {
                intValue = intValue.shiftLeft(fieldSizes[vx - 1]);
            }

            TextSubfield sfNext = operandField._subfields.get(vx);
            String sfNextText = sfNext._text;
            Locale sfNextLocale = sfNext._locale;
            try {
                ExpressionParser pNext = new ExpressionParser(sfNextText, sfNextLocale);
                Expression eNext = pNext.parse(this);
                if (eNext == null) {
                    appendDiagnostic(new ErrorDiagnostic(locale, "Expression expected"));
                    continue;
                }

                Value vNext = vx == 0 ? firstValue : eNext.evaluate(this);
                if (vNext instanceof IntegerValue) {
                    IntegerValue ivNext = (IntegerValue) vNext;
                    FieldDescriptor fd = new FieldDescriptor(startingBit,fieldSizes[vx]);
                    for (UnresolvedReference ur : ((IntegerValue) vNext)._references) {
                        newRefs.add(ur.copy(fd));
                    }
                    intValue = intValue.or(ivNext._value.get());
                } else {
                    appendDiagnostic(new ValueDiagnostic(sfNextLocale, "Expected integer value"));
                }
            } catch (ExpressionException ex) {
                appendDiagnostic(new ErrorDiagnostic(locale, "Syntax error in expression"));
            }

            startingBit += fieldSizes[vx];
        }

        IntegerValue iv = new IntegerValue.Builder().setLocale(locale)
                                                    .setValue(new DoubleWord36(intValue))
                                                    .setForm(new Form(fieldSizes))
                                                    .setReferences(newRefs)
                                                    .build();
        _global._generatedPools.generate(getTopLevelTextLine(), operandField._locale, _currentGenerationLCIndex, iv);
    }

    /**
     * Generates data into the given context representing this value.
     * Need to account for character mode, precision, and justification.
     * @param sValue value object
     * @param locale locale of the code which generated this word
     */
    private void generateString(
        final StringValue sValue,
        final Locale locale
    ) {
        CharacterMode generateMode =
            sValue._characterMode == CharacterMode.Default ? _characterMode : sValue._characterMode;
        int charsPerWord = generateMode == CharacterMode.ASCII ? 4 : 6;

        int charsExpected;
        if (sValue._precision == ValuePrecision.Single) {
            charsExpected = charsPerWord;
        } else if (sValue._precision == ValuePrecision.Double){
            charsExpected = 2 * charsPerWord;
        } else {
            charsExpected = sValue._value.length();
            int mod = sValue._value.length() % charsPerWord;
            if (mod != 0) {
                charsExpected += (charsPerWord - mod);
            }
        }

        int padChars;
        String effectiveString = sValue._value;
        if (charsExpected < sValue._value.length()) {
            appendDiagnostic(new TruncationDiagnostic(locale, "String truncated"));
        } else if (charsExpected > sValue._value.length()) {
            padChars = charsExpected - sValue._value.length();
            if (sValue._justification == ValueJustification.Right) {
                String padString = generateMode == CharacterMode.ASCII ? "\0\0\0\0\0\0\0\0" : "@@@@@@@@@@@@";
                effectiveString = padString.substring(0, padChars) + effectiveString;
            } else {
                String padString = "            ";
                effectiveString += padString.substring(0, padChars);
            }
        }

        ArraySlice slice;
        if (generateMode == CharacterMode.ASCII) {
            slice = ArraySlice.stringToWord36ASCII(effectiveString);
        } else {
            slice = ArraySlice.stringToWord36Fieldata(effectiveString);
        }

        _global._generatedPools.generate(getTopLevelTextLine(), locale, _currentGenerationLCIndex, slice._array);
    }

    /**
     * Determines the zero-level text line involved in this particular retrieval
     */
    public TextLine getTopLevelTextLine() {
        if (_outerLevel != null) {
            return _outerLevel.getTopLevelTextLine();
        } else {
            return _sourceLines[_nextSourceIndex - 1];
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
                    appendDiagnostic(new ErrorDiagnostic(sfLocale, "Illegal location counter specification"));
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
                int maxLabelLength = _global._options.contains(AssemblerOption.LONG_IDENTIFIERS) ? 48 : 12;
                if (Dictionary.isValidLabel(sfText, maxLabelLength)) {
                    label = sfText;
                    labelLevel = levelers;
                    labelLocale = sfLocale;
                } else {
                    appendDiagnostic(new ErrorDiagnostic(sfLocale, "Invalid label specified"));
                }
                ++sfx;
            }

            //  Warn on anything extra
            if (sfx < labelField._subfields.size()) {
                TextSubfield lcSubField = labelField._subfields.get(sfx);
                Locale sfLocale = lcSubField._locale;
                appendDiagnostic(new ErrorDiagnostic(sfLocale, "Extraneous label subfields ignored"));
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
            Expression e1 = p1.parse(this);
            if (e1 == null) {
                return false;
            }
            firstValue = e1.evaluate(this);
        } catch (ExpressionException eex) {
            appendDiagnostic(new ErrorDiagnostic(sf0Locale, "Syntax error in expression"));
        }

        if (labelFieldComponents._label != null) {
            establishLabel(labelFieldComponents._labelLocale,
                           labelFieldComponents._label,
                           labelFieldComponents._labelLevel,
                           getCurrentLocation());
        }

        if (firstValue instanceof FloatingPointValue) {
            Locale loc = operandField._subfields.get(0)._locale;
            if (operandField._subfields.size() > 1) {
                appendDiagnostic(new ErrorDiagnostic(loc, "Too many subfields for data generation"));
            }

            generateFloatingPoint((FloatingPointValue) firstValue, loc);
            return true;
        }

        if (firstValue instanceof IntegerValue) {
            generateInteger(operandField, sf0Locale, firstValue);
            return true;
        }

        if (firstValue instanceof StringValue) {
            Locale loc = operandField._subfields.get(0)._locale;
            if (operandField._subfields.size() > 1) {
                appendDiagnostic(new ErrorDiagnostic(loc, "Too many subfields for data generation"));
            }

            generateString((StringValue) firstValue, loc);
            return true;
        }

        appendDiagnostic(new ErrorDiagnostic(sf0Locale, "Wrong value type for data generation"));
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
            Class<?> clazz = directiveValue._class;
            Constructor<?> ctor = clazz.getConstructor();
            Directive directive = (Directive) (ctor.newInstance());
            directive.process(this, textLine, labelFieldComponents);
        } catch (IllegalAccessException
            | InstantiationException
            | NoSuchMethodException
            | InvocationTargetException ex) {
            System.out.println("Caught:%s" + ex.toString() + ":" + ex.getMessage());
            appendDiagnostic(new DirectiveDiagnostic(operationField._locale, "Unrecognized directive"));
        }
    }

    /**
     * Handles form invocations - a special case of data generation
     * @param formValue FormValue object which causes us to be here
     * @param operandField represents the operand field, if any
     */
    private void processForm(
        final FormValue formValue,
        final TextField operandField
    ) {
        IntegerValue[] opValues = new IntegerValue[operandField._subfields.size()];
        boolean err = false;
        for (int opx = 0; opx < opValues.length; ++opx) {
            TextSubfield opsf = operandField._subfields.get(opx);
            try {
                ExpressionParser p = new ExpressionParser(opsf._text, opsf._locale);
                Expression e = p.parse(this);
                if (e == null) {
                    appendDiagnostic(new ErrorDiagnostic(opsf._locale, "Syntax error"));
                    err = true;
                } else {
                    Value v = e.evaluate(this);
                    if (v instanceof IntegerValue) {
                        IntegerValue iv = (IntegerValue) v;
                        if (iv._form != null) {
                            appendDiagnostic(new FormDiagnostic(opsf._locale, "Form not allowed"));
                        }

                        opValues[opx] = iv;
                    } else {
                        appendDiagnostic(new ValueDiagnostic(opsf._locale, "Wrong value type"));
                        err = true;
                    }
                }
            } catch (ExpressionException ex) {
                appendDiagnostic(new ErrorDiagnostic(opsf._locale, "Syntax error"));
                err = true;
            }
        }

        if (formValue._form._fieldSizes.length != operandField._subfields.size()) {
            appendDiagnostic(new FormDiagnostic(operandField._locale, "Wrong number of operands for form"));
            err = true;
        }

        if (!err) {
            IntegerValue[] realValues = new IntegerValue[formValue._form._fieldSizes.length];
            for (int opx = 0; opx < formValue._form._fieldSizes.length; ++opx) {
                if (opx > opValues.length) {
                    realValues[opx] = IntegerValue.POSITIVE_ZERO;
                } else {
                    realValues[opx] = opValues[opx];
                }
            }

            _global._generatedPools.generate(getTopLevelTextLine(),
                                             operandField._locale,
                                             _currentGenerationLCIndex,
                                             formValue._form,
                                             realValues,
                                             this);
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
        InstructionWord.InstructionInfo iinfo;
        try {
            iinfo = processMnemonicOperationField(mnemonicSubfield);
        } catch (NotFoundException ex) {
            //  mnemonic not found - return false
            return false;
        }

        if (operandField == null) {
            appendDiagnostic(new ErrorDiagnostic(operationField._locale,
                                                 "Instruction mnemonic requires an operand field"));
            return true;
        }

        //  Establish the label to refer to the current lc pool's current offset (if there is a label).
        //  Use the label level to establish which dictionary level it should be placed in.
        if (labelFieldComponents._label != null) {
            establishLabel(labelFieldComponents._labelLocale,
                           labelFieldComponents._label,
                           labelFieldComponents._labelLevel,
                           getCurrentLocation());
        }

        int jField = processMnemonicGetJField(iinfo, operationField);

        //  We have to be in extended mode, *not* using basic mode semantics, and
        //  either the j-field is part of the instruction, or else it is not U or XU...
        //  If that is the case, then we allow (maybe even require) a base register specification.
        boolean baseSubfieldAllowed = (_codeMode == CodeMode.Extended)
                                      && !iinfo._useBMSemantics
                                      && (iinfo._jFlag || (jField < 016));

        TextSubfield[] opSubfields = processMnemonicGetOperandSubfields(iinfo, operandField, baseSubfieldAllowed);
        IntegerValue aValue = processMnemonicGetAField(iinfo, operationField, operandField, opSubfields[0]);
        IntegerValue uValue = processMnemonicGetUField(operandField, opSubfields[1]);
        IntegerValue xValue = processMnemonicGetXField(opSubfields[2]);
        IntegerValue bValue = baseSubfieldAllowed ? processMnemonicGetBField(opSubfields[3]) : IntegerValue.POSITIVE_ZERO;

        //  Create the instruction word
        Form form;
        IntegerValue[] values;
        Locale jLocale = operandField._subfields.size() > 1 ? operandField._subfields.get(1)._locale : operandField._locale;
        if (!iinfo._jFlag && (jField >= 016)) {
            form = _fjaxuForm;
            values = new IntegerValue[5];
            values[0] = new IntegerValue.Builder().setLocale(operationField._locale).setValue(iinfo._fField).build();
            values[1] = new IntegerValue.Builder().setLocale(jLocale).setValue(jField).build();
            values[2] = aValue;
            values[3] = xValue;
            values[4] = uValue;
        } else if ((_codeMode == CodeMode.Basic) || iinfo._useBMSemantics) {
            form = _fjaxhiuForm;
            values = new IntegerValue[7];
            values[0] = new IntegerValue.Builder().setLocale(operationField._locale).setValue(iinfo._fField).build();
            values[1] = new IntegerValue.Builder().setLocale(jLocale).setValue(jField).build();
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue.Builder().setLocale(xValue._locale).setValue(xValue._flagged ? 1 : 0).build();
            values[5] = new IntegerValue.Builder().setLocale(uValue._locale).setValue(uValue._flagged ? 1 : 0).build();
            values[6] = uValue;
        } else {
            form = _fjaxhibdForm;
            values = new IntegerValue[8];
            values[0] = new IntegerValue.Builder().setLocale(operationField._locale).setValue(iinfo._fField).build();
            values[1] = new IntegerValue.Builder().setLocale(jLocale).setValue(jField).build();
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue.Builder().setLocale(xValue._locale).setValue(xValue._flagged ? 1 : 0).build();
            int bInt = bValue._value.get().intValue();
            if (bInt < 017) {
                values[5] = new IntegerValue.Builder().setLocale(uValue._locale).setValue(uValue._flagged ? 1 : 0).build();
                values[6] = bValue;
            } else {
                values[5] = new IntegerValue.Builder().setLocale(bValue._locale).setValue(1).build();
                values[6] = new IntegerValue.Builder().setLocale(bValue._locale).setValue(bInt & 017).build();
            }
            values[7] = uValue;
        }

        _global._generatedPools.generate(getTopLevelTextLine(),
                                         operationField._locale,
                                         _currentGenerationLCIndex,
                                         form,
                                         values,
                                         this);
        return true;
    }

    /**
     * Determine the value for the instruction's a-field
     * @param instructionInfo InstructionInfo for the instruction we're generating
     * @param operationField operator field in case we need that locale
     * @param operandField operand field in case we need that locale
     * @param registerSubfield register (a-field) subfield text
     * @return value representing the A-field (which might have a flagged value)
     */
    private IntegerValue processMnemonicGetAField(
        final InstructionWord.InstructionInfo instructionInfo,
        final TextField operationField,
        final TextField operandField,
        final TextSubfield registerSubfield
    ) {
        IntegerValue aValue = IntegerValue.POSITIVE_ZERO;

        if (instructionInfo._aFlag) {
            //  a-field comes from the instruction info object
            aValue = new IntegerValue.Builder().setLocale(operationField._locale).setValue(instructionInfo._aField).build();
        } else {
            //  a-field comes from the first subfield in the operand field
            if ((registerSubfield == null) || (registerSubfield._text.isEmpty())) {
                appendDiagnostic(new ErrorDiagnostic(operandField._locale,
                                                     "Missing register specification"));
            } else {
                try {
                    ExpressionParser p = new ExpressionParser(registerSubfield._text, registerSubfield._locale);
                    Expression e = p.parse(this);
                    if (e == null) {
                        appendDiagnostic(new ErrorDiagnostic(registerSubfield._locale,
                                                             "Syntax Error"));
                    } else {
                        Value v = e.evaluate(this);
                        if (!(v instanceof IntegerValue)) {
                            appendDiagnostic(new ValueDiagnostic(registerSubfield._locale,
                                                                 "Wrong value type"));
                        } else {
                            //  Reduce the value appropriately for the a-field
                            aValue = (IntegerValue) v;
                            int aInteger = aValue._value.get().intValue();
                            aValue = switch (instructionInfo._aSemantics) {
                                case A -> new IntegerValue.Builder().setLocale(registerSubfield._locale)
                                                                    .setFlagged(aValue._flagged)
                                                                    .setValue(aInteger - 12)
                                                                    .setReferences(aValue._references)
                                                                    .build();
                                case B_EXEC -> new IntegerValue.Builder().setLocale(registerSubfield._locale)
                                                                         .setFlagged(aValue._flagged)
                                                                         .setValue(aInteger - 16)
                                                                         .setReferences(aValue._references)
                                                                         .build();
                                case R -> new IntegerValue.Builder().setLocale(registerSubfield._locale)
                                                                    .setFlagged(aValue._flagged)
                                                                    .setValue(aInteger - 64)
                                                                    .setReferences(aValue._references)
                                                                    .build();
                                default -> aValue;
                            };
                        }
                    }
                } catch (ExpressionException ex) {
                    appendDiagnostic(new ErrorDiagnostic(registerSubfield._locale, "Syntax Error"));
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
        IntegerValue bValue = IntegerValue.POSITIVE_ZERO;
        if ((baseSubfield != null) && !baseSubfield._text.isEmpty()) {
            try {
                ExpressionParser p = new ExpressionParser(baseSubfield._text, baseSubfield._locale);
                Expression e = p.parse(this);
                if (e == null) {
                    appendDiagnostic(new ErrorDiagnostic(baseSubfield._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(this);
                    if (!(v instanceof IntegerValue)) {
                        appendDiagnostic(new ValueDiagnostic(baseSubfield._locale, "Wrong value type"));
                    } else {
                        bValue = (IntegerValue) v;
                    }
                }
            } catch (ExpressionException ex) {
                appendDiagnostic(new ErrorDiagnostic(baseSubfield._locale, "Syntax Error"));
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
                appendDiagnostic(new ErrorDiagnostic(operationField.getSubfield(1)._locale,
                                                     "Extraneous subfields in operation field"));
            }
        } else if (operationField._subfields.size() > 1) {
            TextSubfield jSubField = operationField.getSubfield(1);

            try {
                ExpressionParser p = new ExpressionParser(jSubField._text, jSubField._locale);
                Expression e = p.parse(this);
                if (e == null) {
                    appendDiagnostic(new ErrorDiagnostic(jSubField._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(this);
                    if (!(v instanceof IntegerValue)) {
                        appendDiagnostic(new ValueDiagnostic(jSubField._locale, "Wrong value type"));
                    } else {
                        jField = ((IntegerValue) v)._value.get().intValue();
                    }
                }
            } catch (ExpressionException ex) {
                appendDiagnostic(new ErrorDiagnostic(jSubField._locale, "Syntax Error"));
            }

            if (operationField._subfields.size() > 2) {
                appendDiagnostic(new ErrorDiagnostic(operationField.getSubfield(1)._locale,
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
            appendDiagnostic(new ErrorDiagnostic( operandField.getSubfield( sfx )._locale,
                                                  "Extraneous subfields in operand field ignored"));
        }

        return new TextSubfield[]{sfRegister, sfValue, sfIndex, sfBase };
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
        IntegerValue uValue = IntegerValue.POSITIVE_ZERO;
        if ((valueSubfield == null) || (valueSubfield._text.isEmpty())) {
            appendDiagnostic(new ErrorDiagnostic(operandField._locale,
                                                 "Missing operand value (U, u, or d subfield)"));
        } else {
            try {
                ExpressionParser p = new ExpressionParser(valueSubfield._text, valueSubfield._locale);
                Expression e = p.parse(this);
                if (e == null) {
                    appendDiagnostic(new ErrorDiagnostic(valueSubfield._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(this);
                    if (v instanceof IntegerValue) {
                        uValue = (IntegerValue) v;
                    } else {
                        appendDiagnostic(new ValueDiagnostic(valueSubfield._locale, "Wrong value type"));
                    }
                }
            } catch (ExpressionException ex) {
                appendDiagnostic(new ErrorDiagnostic(valueSubfield._locale, "Syntax Error"));
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
        IntegerValue xValue = IntegerValue.POSITIVE_ZERO;
        if ((indexSubfield != null) && !indexSubfield._text.isEmpty()) {
            try {
                ExpressionParser p = new ExpressionParser(indexSubfield._text, indexSubfield._locale);
                Expression e = p.parse(this);
                if (e == null) {
                    appendDiagnostic(new ErrorDiagnostic(indexSubfield._locale, "Syntax Error"));
                } else {
                    Value v = e.evaluate(this);
                    if (!(v instanceof IntegerValue)) {
                        appendDiagnostic(new ValueDiagnostic(indexSubfield._locale, "Wrong value type"));
                    } else {
                        xValue = (IntegerValue) v;
                    }
                }
            } catch (ExpressionException ex) {
                appendDiagnostic(new ErrorDiagnostic(indexSubfield._locale, "Syntax Error"));
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
                _codeMode == CodeMode.Extended ? InstructionWord.Mode.EXTENDED : InstructionWord.Mode.BASIC;
            return InstructionWord.getInstructionInfo(subfield._text, imode);
        } catch (NotFoundException ex) {
            //  Mnemonic not found - is it dependent on code mode?
            //  If so, coder is asking for a mnemonic in a mode it doesn't exist in; raise a diagnostic and
            //  return true so the assemble method doesn't go any further with this line.
            //  Otherwise, it's just flat not a mnemonic, so return false and let the assemble method do
            //  something else.
            InstructionWord.Mode imode
                = _codeMode == CodeMode.Extended ? InstructionWord.Mode.BASIC : InstructionWord.Mode.EXTENDED;
            InstructionWord.InstructionInfo iinfo = InstructionWord.getInstructionInfo(subfield._text, imode);
            appendDiagnostic(new ErrorDiagnostic(subfield._locale,
                                                 "Opcode not valid for the current code mode"));
            return iinfo;
        }
    }

    /**
     * Code is invoking a procedure.  Make it so.
     * Note that we have a simpler concept of $PROCs than per convention.
     * Specifically, we have no $NAME (at least for now).  Thus, in order to provide the ability to invoke
     * a proc names FOO as such:
     *     FOO,x,y  a,b,c
     * we fill in the parameter node at major index 0 as it would have been done for a $NAME directive...
     * that is:  FOO(0,0) = 'FOO'
     *           FOO(0,1) = x
     * etc.
     * Also, we support all the fields in the text line beyond the label field...
     * @param procedureName name of the procedure being invoked
     * @param procedureValue indicates the procedure to be invoked
     * @param textLine the parsed code from which we build the parameter list
     */
    private void processProcedure(
        final String procedureName,
        final ProcedureValue procedureValue,
        final TextLine textLine
    ) {
        Assembler subAssembler = new ProcedureAssembler(this, procedureName, procedureValue._source);

        //  Main node contains the proc name and operation subfields, and operand subfields,
        //  and all subsequent fields thereafter.
        //  Its locale corresponds to the operation field of the invoking text line
        NodeValue mainNode = new NodeValue(textLine._fields.get(1)._locale, false);
        for (int fx = 1; fx < textLine._fields.size(); ++fx) {
            TextField field = textLine._fields.get(fx);
            NodeValue subNode = new NodeValue(field._locale, false);
            mainNode.setValue(new IntegerValue.Builder().setValue(fx - 1).build(), subNode);
            for (int sfx = 0; sfx < field._subfields.size(); ++sfx) {
                TextSubfield subField = field._subfields.get(sfx);
                if ((fx == 1) && (sfx == 0)) {
                    //  This is the proc name - handle it accordingly (no expression evaluation)
                    StringValue sValue = new StringValue.Builder().setLocale(subNode._locale)
                                                                  .setValue(subField._text)
                                                                  .build();
                    subNode.setValue(IntegerValue.POSITIVE_ZERO, sValue);
                } else {
                    IntegerValue nodeIndex = new IntegerValue.Builder().setValue(sfx).build();
                    try {
                        //  Evaluate the given subfield, and place the result in the appropriate position of nv
                        ExpressionParser sfParser = new ExpressionParser(subField._text, subField._locale);
                        Expression sfExpression = sfParser.parse(this);
                        Value sfValue = sfExpression.evaluate(this);
                        subNode.setValue(nodeIndex, sfValue);
                    } catch (ExpressionException ex) {
                        subNode.setValue(nodeIndex, IntegerValue.POSITIVE_ZERO);
                        Diagnostic diag = new ErrorDiagnostic(subField._locale, "Syntax error");
                        appendDiagnostic(diag);
                    }
                }
            }
        }

        subAssembler._dictionary.addValue(0, procedureName, new Locale(textLine._lineSpecifier, 1), mainNode);
        for (TextLine procTextLine : procedureValue._source) {
            subAssembler.assembleTextLine(procTextLine);
        }
    }

    //  ---------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Convenience method - adds the diagnostic to the master Diagnostics object as well as the affected
     * top-level TextLine()
     * @param diag diagnostic to be added
     */
    public void appendDiagnostic(
        final Diagnostic diag
    ) {
        _global._diagnostics.append(diag);
        getTopLevelTextLine()._diagnostics.add(diag);
    }

    /**
     * Main code for the assembly process - a top-level, or main assembly, as it were.
     * @return AssemblerResult object describing the results of the assembly process
     */
    public AssemblerResult assemble() {
        if (!_global._options.contains(AssemblerOption.SILENT)) {
            _global._outputStream.println("KASM ---------------------------------------------------");
            _global._outputStream.println("Assembling module " + _moduleName);
        }

        while (hasNextSourceLine()) {
            TextLine textLine = getNextSourceLine();
            textLine.parseFields(_global._diagnostics);
            assembleTextLine(textLine);
            if (_global._diagnostics.hasFatal()) {
                if (!_global._options.contains(AssemblerOption.SILENT)) {
                    displayResults(false, null);
                    return new AssemblerResult(_global._diagnostics, _sourceLines);
                }
            }
        }

        if (_endValue != null) {
            //  ENDDiagnostic guarantees that the end value is a properly-relocated IntegerValue (for the main assembly)
            IntegerValue iv = (IntegerValue) _endValue;
            UnresolvedReferenceToLocationCounter urlc = (UnresolvedReferenceToLocationCounter) iv._references[0];
            _global._programStart = new ProgramStart(urlc._locationCounterIndex, iv._value.get().intValue());
        }

        _global._generatedPools.resolveReferences(this);
        RelocatableModule module = createRelocatableModule(_moduleName);

        if (!_global._options.contains(AssemblerOption.SILENT)) {
            if (_global._options.contains(AssemblerOption.EMIT_GENERATED_CODE)
                || _global._options.contains(AssemblerOption.EMIT_SOURCE)) {
                displayResults(_global._options.contains(AssemblerOption.EMIT_GENERATED_CODE), module);
            }
            if (_global._options.contains(AssemblerOption.EMIT_MODULE_SUMMARY)) {
                displayRelocatableModuleSummary(module);
            }
            if (_global._options.contains(AssemblerOption.EMIT_DICTIONARY)) {
                displayDictionary(_dictionary);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Summary: Lines=%d", _sourceLines.length));
            for (Map.Entry<Diagnostic.Level, Integer> entry : _global._diagnostics.getCounters().entrySet()) {
                if (entry.getValue() > 0) {
                    sb.append(String.format(" %c=%d", Diagnostic.getLevelIndicator(entry.getKey()), entry.getValue()));
                }
            }
            _global._outputStream.println(sb.toString());

            _global._outputStream.println("Assembly Ends ------------------------------------------");
        }

        return new AssemblerResult(module, _global._diagnostics, _sourceLines);
    }

    /**
     * Establishes a label value in the current (or a super-ordinate) dictionary
     * @param locale locale of label (for posting diagnostics)
     * @param label label
     * @param labelLevel label level - 0 to put it in the dictionary, 1 for the next highest, etc
     * @param value value to be associated with the level
     */
    public void establishLabel(
        final Locale locale,
        final String label,
        final int labelLevel,
        final Value value
    ) {
        if (_dictionary.hasValue(label)) {
            appendDiagnostic(new DuplicateDiagnostic(locale, "Label " + label + " duplicated"));
        } else {
            _dictionary.addValue(labelLevel, label, locale, value);
        }
    }

    public boolean getArithmeticFaultCompatibilityMode() {
        return _global._arithmeticFaultCompatibilityMode;
    }

    public boolean getArithmeticFaultNonInterruptMode() {
        return _global._arithmeticFaultNonInterruptMode;
    }

    /**
     * Retrieves the current character mode for this (sub)assembly
     */
    public CharacterMode getCharacterMode() { return _characterMode; }

    /**
     * Retrieves the current code mode for this (sub)assembly
     */
    public CodeMode getCodeMode() {
        return _codeMode;
    }

    /**
     * Retrieves the current LC index for code generation for this (sub)assembly
     */
    public int getCurrentGenerationLCIndex() {
        return _currentGenerationLCIndex;
    }

    /**
     * Retrieves the current LC index for code generation for this (sub)assembly
     */
    public int getCurrentLiteralLCIndex() {
        return _currentLiteralLCIndex;
    }

    /**
     * Creates an IntegerValue object with an appropriate undefined reference to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    public IntegerValue getCurrentLocation(
    ) {
        GeneratedPool gp = _global._generatedPools.obtainPool(_currentGenerationLCIndex);
        int lcOffset = gp.getNextOffset();
        UnresolvedReference[] refs = {
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(0, 36),
                                                     false,
                                                     _currentGenerationLCIndex)
        };

        return new IntegerValue.Builder().setValue(lcOffset)
                                         .setReferences(refs)
                                         .build();
    }

    /**
     * Retrieves the global Diagnostics object
     */
    public Diagnostics getDiagnostics() { return _global._diagnostics; }

    /**
     * Retrieves the dictionary for this (sub)assembler
     */
    public Dictionary getDictionary() { return _dictionary; }

    /**
     * Retrieves our GeneratedPools object
     */
    public GeneratedPools getGeneratedPools() {
        return _global._generatedPools;
    }

    /**
     * Retrieves the assembler level - 0 is the top-level, or main assembly
     */
    public int getLevel() {
        return _level;
    }

    /**
     * Retrieves the next source line from the source array
     */
    public TextLine getNextSourceLine() {
        return _sourceLines[_nextSourceIndex++];
    }

    /**
     * Retrieves the requires-quarter-word-mode setting
     */
    public boolean getQuarterWordMode() { return _global._quarterWordMode; }

    /**
     * Retrieves the requires-third-word-mode setting
     */
    public boolean getThirdWordMode() { return _global._thirdWordMode; }

    /**
     * Indicates whether there is a next source line
     */
    public boolean hasNextSourceLine() {
        return _nextSourceIndex < _sourceLines.length;
    }

    /*
     * What type of assembly is this?
     */
    public boolean isMainAssembly() {
        return true;
    }

    public boolean isSubAssembly() {
        return false;
    }

    public boolean isFunctionSubAssembly() {
        return false;
    }

    public boolean isProcedureSubAssembly() {
        return false;
    }

    /**
     * Quick way to check for an option
     */
    public boolean isOptionSet(
        final AssemblerOption option
    ) {
        return _global._options.contains(option);
    }

    /**
     * Sets AFCM requirement to on
     */
    public void setArithmeticFaultCompatibilityMode() {
        _global._arithmeticFaultCompatibilityMode = true;
    }

    /**
     * Sets AFCM requirement to off
     */
    public void setArithmeticFaultNonInterruptMode() {
        _global._arithmeticFaultNonInterruptMode = true;
    }

    /**
     * Sets the LC index for generated data for this (sub)assembler
     */
    public void setCurrentGenerationLCIndex(
        final int index
    ) {
        _currentGenerationLCIndex = index;
    }

    /**
     * Sets the character mode for this (sub)assembler
     */
    public void setCharacterMode(
        final CharacterMode mode
    ) {
        _characterMode = mode;
    }

    /**
     * Sets the code mode for this (sub)assembler
     */
    public void setCodeMode(
        final CodeMode mode
    ) {
        _codeMode = mode;
    }

    /**
     * Sets the LC index for literal data for this (sub)assembler.
     * Creaes an empty LC pool (if necessary) to represent the pool.
     */
    public void setCurrentLitLCIndex(
        final int index
    ) {
        _currentLiteralLCIndex = index;
    }

    public void setEndFound() {
        _endFound = true;
    }

    public void setEndValue(
        final Value value
    ) {
        _endValue = value;
    }

    public void setProgramStart(
        final ProgramStart programStart
    ) {
        _global._programStart = programStart;
    }

    public void setQuarterWordMode() {
        _global._quarterWordMode = true;
    }

    public void setThirdWordMode() {
        _global._thirdWordMode = true;
    }

    /**
     * Retrieves the number of lines of source
     */
    public int sourceLineCount() {
        return _sourceLines.length;
    }
}
