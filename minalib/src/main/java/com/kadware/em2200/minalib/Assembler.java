/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;

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

    //  A useful IntegerValue containing zero, no flags, and no unidentified references.
    private static final IntegerValue _zeroValue = new IntegerValue( false, 0, null );


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
        if (textLine._fields.isEmpty()) {
            return;
        }

        TextField labelField = textLine.getField(0);
        TextField operationField = textLine.getField(1);
        TextField operandField = textLine.getField(2);

        //  Does this line of code represent an instruction mnemonic?  (or a label on an otherwise empty line)...
        if (processMnemonic(textLine, labelField, operationField, operandField, textLine._diagnostics)) {
            if (textLine._fields.size() > 3) {
                textLine._diagnostics.append(new ErrorDiagnostic(textLine.getField(3)._locale,
                                                                 "Extraneous fields ignored"));
            }
            return;
        }

        //  Not a mnemonic - is it a directive?  (check the operation field subfield 0 against the dictionary)
        //TODO

        //  Hmm.  Is it an expression (or a list of expressions)?
        //TODO

        textLine._diagnostics.append(new ErrorDiagnostic(new Locale(textLine._lineNumber, 1),
                                                         "What the heck is this?"));//????
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
        if (values.length != form.getFieldCount()) {
            throw new RuntimeException("Number of bit-fields in the form differ from number of values");
        }

        int startingBit = 0;
        int[] fieldSizes = form.getFieldSizes();
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
     * Creates an IntegerValue object with an appropriate undefined reference to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    private IntegerValue getCurrentLocation(
    ) {
        //  Find the current generation lc index.
        //  If it doesn't exist, it will be created.
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
            ExpressionParser p = new ExpressionParser( subfield._text, subfield._locale );
            Expression e = p.parse( _context, diagnostics );
            return e.evaluate( _context, diagnostics );
        } catch ( ExpressionException eex ) {
            diagnostics.append( new ErrorDiagnostic( subfield._locale,
                                                     "Cannot evaluate expression:" + eex.getMessage() ) );
            return new IntegerValue(false, 0, null);
        } catch ( NotFoundException nfex ) {
            diagnostics.append( new ErrorDiagnostic( subfield._locale,
                                                     "Expected an expression" ) );
            return new IntegerValue(false, 0, null);
        }
    }

    /**
     * For TextLine objecs which contain a label intended to actually *be* a label - that is, to represent the current
     * address in the current generation location counter...
     * @param labelField label field
     * @param diagnostics where we post any appropriate diagnostics
     */
    private void processLabel(
        final TextField labelField,
        final Diagnostics diagnostics
    ) {
        if ( (labelField != null) && (labelField._subfields.size() > 0) ) {
            TextSubfield labelSubfield = labelField.getSubfield(0);
            String label = labelSubfield._text;
            int labelLevel = 0;
            while ( label.endsWith("*") ) {
                label = label.substring(0, label.length() - 1);
                ++labelLevel;
            }

            if ( !Dictionary.isValidUserLabel(label) ) {
                diagnostics.append(new ErrorDiagnostic(labelSubfield._locale,
                                                       "Invalid label"));
            } else {
                if ( labelField._subfields.size() > 1 ) {
                    diagnostics.append(new ErrorDiagnostic(labelField.getSubfield(1)._locale,
                                                           "Extraneous subfields in label field"));
                }

                if ( _context._dictionary.hasValue(label) ) {
                    diagnostics.append(new DuplicateDiagnostic(labelSubfield._locale,
                                                               "Duplicate label"));
                } else {
                    _context._dictionary.addValue(labelLevel, label, getCurrentLocation());
                }
            }
        }
    }

    /**
     * Handles instruction mnemonic lines of code, and blank lines
     * @param textLine where this came from
     * @param labelField represents the label field, if any
     * @param operationField represents the operation field, if any
     * @param operandField represents the operand field, if any
     * @param diagnostics where we post diagnostics if needed
     * @return true if we determined these inputs represent an instruction mnemonic code generation thing (or a blank line)
     */
    private boolean processMnemonic(
        final TextLine textLine,
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
            diagnostics.append(new ErrorDiagnostic( operationField._locale,
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
            int sfc = operandField._subfields.size();
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
                diagnostics.append( new ErrorDiagnostic( operandField.getSubfield( sfx )._locale,
                                                         "Extreanous subfields in operand field ignored") );
            }

            //  Interpret the subfields
            if (iinfo._aFlag) {
                aValue = new IntegerValue( false, iinfo._aField, null );
            } else {
                if ( (registerSubField == null) || (registerSubField._text.isEmpty()) ) {
                    diagnostics.append( new ErrorDiagnostic( operandField._locale,
                                                             "Missing register specification" ) );
                } else {
                    try {
                        ExpressionParser p = new ExpressionParser( registerSubField._text, registerSubField._locale );
                        Expression e = p.parse( _context, diagnostics );
                        Value v = e.evaluate( _context, diagnostics );
                        if (v.getType() != ValueType.Integer) {
                            diagnostics.append( new ValueDiagnostic( registerSubField._locale, "Wrong value type" ) );
                        } else {
                            aValue = (IntegerValue) v;
                            //  Reduce the value appropriately for the a-field
                            if ( iinfo._aSemantics == InstructionWord.ASemantics.A ) {
                                aValue = new IntegerValue( aValue.getFlagged(),
                                                           aValue.getValue() - 12,
                                                           aValue.getUndefinedReferences() );
                            } else if ( iinfo._aSemantics == InstructionWord.ASemantics.R ) {
                                aValue = new IntegerValue( aValue.getFlagged(),
                                                           aValue.getValue() - 64,
                                                           aValue.getUndefinedReferences() );
                            }
                        }
                    } catch ( ExpressionException | NotFoundException ex ) {
                        diagnostics.append( new ErrorDiagnostic( registerSubField._locale,
                                                                 "Syntax Error:" + ex.getMessage() ) );
                    }
                }
            }

            if ((valueSubField == null) || (valueSubField._text.isEmpty())) {
                diagnostics.append(new ErrorDiagnostic(operandField._locale,
                                                       "Missing operand value (U, u, or d subfield)"));
            } else {
                try {
                    ExpressionParser p = new ExpressionParser( valueSubField._text, valueSubField._locale );
                    Expression e = p.parse( _context, diagnostics );
                    Value v = e.evaluate( _context, diagnostics );
                    if (v.getType() != ValueType.Integer) {
                        diagnostics.append( new ValueDiagnostic( valueSubField._locale, "Wrong value type" ) );
                    } else {
                        uValue = (IntegerValue) v;
                    }
                } catch ( ExpressionException | NotFoundException ex ) {
                    diagnostics.append( new ErrorDiagnostic( valueSubField._locale,
                                                             "Syntax Error:" + ex.getMessage() ) );
                }
            }

            if ((indexSubField != null) && !indexSubField._text.isEmpty()) {
                try {
                    ExpressionParser p = new ExpressionParser( indexSubField._text, indexSubField._locale );
                    Expression e = p.parse( _context, diagnostics );
                    Value v = e.evaluate( _context, diagnostics );
                    if (v.getType() != ValueType.Integer) {
                        diagnostics.append( new ValueDiagnostic( indexSubField._locale, "Wrong value type" ) );
                    } else {
                        xValue = (IntegerValue) v;
                    }
                } catch ( ExpressionException | NotFoundException ex ) {
                    diagnostics.append( new ErrorDiagnostic( indexSubField._locale,
                                                             "Syntax Error:" + ex.getMessage() ) );
                }
            }

            if (baseSubFieldAllowed) {
                if ( (baseSubField != null) && !baseSubField._text.isEmpty() ) {
                    try {
                        ExpressionParser p = new ExpressionParser( baseSubField._text, baseSubField._locale );
                        Expression e = p.parse( _context, diagnostics );
                        Value v = e.evaluate( _context, diagnostics );
                        if (v.getType() != ValueType.Integer) {
                            diagnostics.append( new ValueDiagnostic( baseSubField._locale, "Wrong value type" ) );
                        } else {
                            bValue = (IntegerValue) v;
                        }
                    } catch ( ExpressionException | NotFoundException ex ) {
                        diagnostics.append( new ErrorDiagnostic( baseSubField._locale,
                                                                 "Syntax Error:" + ex.getMessage() ) );
                    }
                } else {
                    //TODO what is the default base register?
                }
            }
        }

        //  Create the instruction word
        if ((_context._codeMode == CodeMode.Basic) || iinfo._useBMSemantics) {
            if (iinfo._jFlag || (jField < 016)) {
                IntegerValue[] values = new IntegerValue[7];
                values[0] = new IntegerValue( false, iinfo._fField, null );
                values[1] = new IntegerValue( false, jField, null );
                values[2] = aValue;
                values[3] = xValue;
                values[4] = new IntegerValue( false, (xValue.getFlagged() ? 1 : 0), null );
                values[5] = new IntegerValue( false, (uValue.getFlagged() ? 1 : 0), null );
                values[6] = uValue;
                generate(textLine, _fjaxhiuForm, values, _context._currentGenerationLCIndex);
            } else {
                IntegerValue[] values = new IntegerValue[5];
                values[0] = new IntegerValue( false, iinfo._fField, null );
                values[1] = new IntegerValue( false, jField, null );
                values[2] = aValue;
                values[3] = xValue;
                values[4] = uValue;
                generate(textLine, _fjaxuForm, values, _context._currentGenerationLCIndex);
            }
        } else {
            IntegerValue[] values = new IntegerValue[8];
            values[0] = new IntegerValue( false, iinfo._fField, null );
            values[1] = new IntegerValue( false, jField, null );
            values[2] = aValue;
            values[3] = xValue;
            values[4] = new IntegerValue( false, (xValue.getFlagged() ? 1 : 0), null );
            values[5] = new IntegerValue( false, (uValue.getFlagged() ? 1 : 0), null );
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
                    if (originalIV.getUndefinedReferences().length > 0) {
                        long newDiscreteValue = originalIV.getValue();
                        List<IntegerValue.UndefinedReference> newURefs = new LinkedList<>();
                        for (IntegerValue.UndefinedReference intURef : originalIV.getUndefinedReferences()) {
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
                                    newDiscreteValue += (intURef._isNegative ? -1 : 1) * lookupIntegerValue.getValue();
                                    for (IntegerValue.UndefinedReference urLookup : lookupIntegerValue.getUndefinedReferences()) {
                                        newURefs.add( urLookup );
                                    }
                                }
                            } catch ( NotFoundException ex ) {
                                //  reference is still not found - propagate it
                                newURefs.add(intURef);
                            }
                        }

                        IntegerValue newIV = new IntegerValue( originalIV.getFlagged(),
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
        System.out.println(String.format("Assembling module %s -----------------------------------", moduleName));

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
            assemble(line);
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
        System.out.println("Assembly Ends -------------------------------------------------------");

        return module;
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
}
