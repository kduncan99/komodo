/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.dictionary.Dictionary;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents the current context under which an assembly is being performed
 */
@SuppressWarnings("Duplicates")
public class Context {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * During assembly, location counter pools are built up.  This is a map of lc offsets to GeneratedWords,
     * in offset order.  It is not necessarily monotonically increasing - it could have holes in it by virtue
     * of incrementing _nextOffset without adding a value (such as happens with $RES directives)
     */
    public static class GeneratedPool extends TreeMap<Integer, GeneratedWord> {

        public boolean _extendedModeFlag = false;
        private int _nextOffset = 0;

        /**
         * Advances the next offset value without generating words - mainly for $RES
         * @param count number of words to be advanced
         */
        void advance( final int count ) { _nextOffset += count; }

        /**
         * How bit is the pool?
         * @return current size (does not necessarily indicate number of actual words - there might be a $RES hole)
         */
        public int getSize() { return _nextOffset; }

        /**
         * Store a new GeneratedWord at the next location in this pool, and advance the offset
         * @param value value to be stored
         */
        public void store(
            final GeneratedWord value
        ) {
            put(_nextOffset++, value);
        }

        /**
         * Produces a LocationCounterPool object to represent the content of this pool
         */
        LocationCounterPool produceLocationCounterPool(
        ) {
            RelocatableWord[] pool = new RelocatableWord[_nextOffset];
            for (Map.Entry<Integer, GeneratedWord> wordEntry : entrySet()) {
                int lcOffset = wordEntry.getKey();
                GeneratedWord gw = wordEntry.getValue();
                pool[lcOffset] = gw.produceRelocatableWord();
            }

            return new LocationCounterPool(pool, _extendedModeFlag);
        }
    }

    //  Class data which is global - that is, there is one copy regardless of the level of
    //  context nesting
    private class Global {

        //  relocatable module flags
        private boolean _arithmeticFaultCompatibilityMode = false;
        private boolean _arithmeticFaultNonInterruptMode = false;
        private boolean _quarterWordMode = false;
        private boolean _thirdWordMode = false;
        private final Diagnostics _diagnostics;

        //  Map of LC indices to the various GeneratedPool objects...
        private final Map<Integer, GeneratedPool> _generatedPools = new TreeMap<>();

        private Global( Diagnostics diagnostics ) { _diagnostics = diagnostics; }
    }

    //  Class data which is local to a particular context object
    private class Local {
        //  What mode are string literals generated in
        private CharacterMode _characterMode = CharacterMode.ASCII;

        //  basic or extended mode generation?
        private CodeMode _codeMode = CodeMode.Extended;

        //  What is the default LC index for code generation?
        private int _currentGenerationLCIndex = 1;

        //  What is the default LC index for literal pool generation?
        private int _currentLitLCIndex = 0;

        //  What dictionary should be used for lookups and label establishment?
        private final Dictionary _dictionary;

        //  Source code objects
        private final TextLine[] _sourceObjects;
        private int _nextSourceIndex = 0;

        private Local(
            Dictionary dictionary,
            String[] sourceText
        ) {
            _dictionary = dictionary;
            _sourceObjects = new TextLine[sourceText.length];
            for (int sx = 0; sx < sourceText.length; ++sx) {
                _sourceObjects[sx] = new TextLine(new LineSpecifier(getNestingLevel(), sx + 1),
                                                  sourceText[sx]);
            }
        }

        private Local(
            Dictionary dictionary,
            TextLine[] parsedCode
        ) {
            _dictionary = dictionary;
            _sourceObjects = parsedCode;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data / Attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private final Global _globalData;
    private final Local _localData;
    private final Context _parent;
    private final Set<Assembler.Option> _optionSet;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * General constructor for the top-level context
     */
    public Context(
        final Dictionary upperLevelDictionary,
        final String[] sourceText,
        final Set<Assembler.Option> optionSet
    ) {
        _globalData = new Global(new Diagnostics());
        _localData = new Local(new Dictionary(upperLevelDictionary), sourceText);
        _optionSet = optionSet;
        _parent = null;
    }

    /**
     * General constructor for a nested context
     * @param parent parent context
     * @param nestedParsedCode code segment for which we are responsible
     */
    public Context(
        final Context parent,
        final TextLine[] nestedParsedCode
    ) {
        _globalData = parent._globalData;
        _localData = new Local(new Dictionary(parent._localData._dictionary), nestedParsedCode);
        _parent = parent;
        _optionSet = parent._optionSet;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Advances the offset of a particular location counter by the given count value
     * @param lcIndex index of the location counter
     * @param count amount by which the lc is to be offset - expected to be positive, but it works regardless
     */
    public void advanceLocation(
        final int lcIndex,
        final int count
    ) {
        GeneratedPool gp = obtainPool(lcIndex);
        gp.advance(count);
    }

    /**
     * Convenience method
     * @param diag diagnostic to be added
     */
    public void appendDiagnostic(
        final Diagnostic diag
    ) {
        _globalData._diagnostics.append(diag);
        getTopLevelTextLine()._diagnostics.add(diag);
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
        if (_localData._dictionary.hasValue(label)) {
            _globalData._diagnostics.append(new DuplicateDiagnostic(locale, "Label " + label + " duplicated"));
        } else {
            _localData._dictionary.addValue(labelLevel, label, value);
        }
    }

    /**
     * Generates a word (with possibly a form attached) for a given location counter index and offset,
     * and places it into the appropriate location counter pool within the given context.
     * Also associates it with the current top-level text line.
     * @param lineSpecifier location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param value the integer/intrinsic value to be used
     */
    void generate(
        final LineSpecifier lineSpecifier,
        final int lcIndex,
        final IntegerValue value
    ) {
        GeneratedPool gp = obtainPool(lcIndex);
        int lcOffset = gp._nextOffset;
        GeneratedWord gw = new GeneratedWord(getTopLevelTextLine(), lineSpecifier, lcIndex, lcOffset, value);
        gp.store(gw);
        gw._topLevelTextLine._generatedWords.add(gw);
    }

    /**
     * Generates a word with a form attached for a given location counter index and offset,
     * and places it into the appropriate location counter pool within the given context.
     * Also associates it with the current top-level text line.
     * The form describes 1 or more fields, the totality of which are expected to describe 36 bits.
     * The values parameter is an array with as many entities as there are fields in the form.
     * Each value must fit within the size of that value's respective field.
     * The overall integer portion of the generated value is the respective component integer values
     * shifted into their field positions, and or'd together.
     * The individual values should not have forms attached, but they may have undefined references.
     * All such undefined references are adjusted to match the field description of the particular
     * field to which the reference applies.
     * @param lineSpecifier location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param form form describing the fields for which values are specified in the values parameter
     * @param values array of component values, each with potential undefined refereces but no attached forms
     * @return value indicating the location which applies to the word which was just generated
     */
    public IntegerValue generate(
        final LineSpecifier lineSpecifier,
        final int lcIndex,
        final Form form,
        final IntegerValue[] values,
        final Locale locale
    ) {
        GeneratedPool gp = obtainPool(lcIndex);
        int lcOffset = gp._nextOffset;
        if (form._fieldSizes.length != values.length) {
            appendDiagnostic(new FormDiagnostic(locale,
                                                "Contradiction between number of values and number of fields in form"));
            return new IntegerValue.Builder().setValue(lcOffset).build();
        }

        BigInteger genInt = BigInteger.ZERO;
        int bit = form._leftSlop;
        List<UndefinedReference> newRefs = new LinkedList<>();
        for (int fx = 0; fx < form._fieldSizes.length; ++fx) {
            genInt = genInt.shiftLeft(form._fieldSizes[fx]);
            BigInteger mask = BigInteger.valueOf((1L << form._fieldSizes[fx]) - 1);

            BigInteger fieldValue = values[fx]._value.get();
            boolean trunc;
            if (values[fx]._value.isPositive()) {
                trunc = !fieldValue.and(mask).equals(fieldValue);
            } else {
                trunc = !fieldValue.and(mask).equals(DoubleWord36.SHORT_BIT_MASK);
            }

            if (trunc) {
                String msg = String.format("Value %012o exceeds size of field in form %s", fieldValue, form.toString());
                appendDiagnostic(new TruncationDiagnostic(locale, msg));
            }

            genInt = genInt.or(values[fx]._value.get().and(mask));
            for (UndefinedReference ur : values[fx]._references) {
                newRefs.add(ur.copy(new FieldDescriptor(bit, form._fieldSizes[fx])));
            }

            bit += form._fieldSizes[fx];
        }

        IntegerValue iv = new IntegerValue.Builder().setValue(new DoubleWord36(genInt))
                                                    .setForm(form)
                                                    .setReferences(newRefs.toArray(new UndefinedReference[0]))
                                                    .build();
        GeneratedWord gw = new GeneratedWord(getTopLevelTextLine(), lineSpecifier, lcIndex, lcOffset, iv);
        gp.store(gw);
        gw._topLevelTextLine._generatedWords.add(gw);

        UndefinedReference[] lcRefs = { new UndefinedReferenceToLocationCounter(FieldDescriptor.W, false, lcIndex) };
        return new IntegerValue.Builder().setValue(lcOffset)
                                         .setReferences(lcRefs)
                                         .build();
    }

    /**
     * Generates the multiple words for a given location counter index and offset,
     * and places them into the appropriate location counter pool within the given context.
     * Also associates it with the current top-level text line.
     * @param lineSpecifier location of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param values the values to be used
     */
    void generate(
        final LineSpecifier lineSpecifier,
        final int lcIndex,
        final long[] values
    ) {
        GeneratedPool gp = obtainPool(lcIndex);
        int lcOffset = gp._nextOffset;

        for (int vx = 0; vx < values.length; ++vx) {
            IntegerValue iv = new IntegerValue.Builder().setValue(values[vx]).build();
            GeneratedWord gw = new GeneratedWord(getTopLevelTextLine(),
                                                 lineSpecifier,
                                                 lcIndex,
                                                 lcOffset + vx,
                                                 iv);
            gp.store(gw);
            gw._topLevelTextLine._generatedWords.add(gw);
        }
    }

    boolean getArithmeticFaultCompatibilityMode() { return _globalData._arithmeticFaultCompatibilityMode; }
    boolean getArithmeticFaultNonInterruptMode() { return _globalData._arithmeticFaultNonInterruptMode; }
    CharacterMode getCharacterMode() { return _localData._characterMode; }
    public CodeMode getCodeMode() { return _localData._codeMode; }
    public int getCurrentGenerationLCIndex() { return _localData._currentGenerationLCIndex; }
    public int getCurrentLitLCIndex() { return _localData._currentLitLCIndex; }
    public Diagnostics getDiagnostics() { return _globalData._diagnostics; }
    public Dictionary getDictionary() { return _localData._dictionary; }
    Set<Map.Entry<Integer, GeneratedPool>> getGeneratedPools() { return _globalData._generatedPools.entrySet(); }
    private int getNestingLevel() { return (_parent == null) ? 0 : _parent.getNestingLevel() + 1; }
    TextLine[] getParsedCode() { return _localData._sourceObjects; }
    boolean getQuarterWordMode() { return _globalData._quarterWordMode; }
    boolean getThirdWordMode() { return _globalData._thirdWordMode; }

    /**
     * Creates an IntegerValue object with an appropriate undefined reference to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    public IntegerValue getCurrentLocation(
    ) {
        GeneratedPool gp = obtainPool(_localData._currentGenerationLCIndex);
        int lcOffset = gp._nextOffset;
        UndefinedReference[] refs = { new UndefinedReferenceToLocationCounter(new FieldDescriptor(0, 36),
                                                                              false,
                                                                              _localData._currentGenerationLCIndex) };
        return new IntegerValue.Builder().setValue(lcOffset)
                                         .setReferences(refs)
                                         .build();
    }

    /**
     * Retrieves the next not-yet-assembled line of source code from this object,
     * after updating its top-level TextLine object to indicate the text from which this derives.
     * For top-level source, this will be the same as the returned object.
     * For proc/func source, this will be the zero-level TextLine at the top of the invoke nesting...
     * @return TextLine object
     */
    public TextLine getNextSourceLine() {
        TextLine result = null;
        if (_localData._nextSourceIndex < _localData._sourceObjects.length) {
            result = _localData._sourceObjects[_localData._nextSourceIndex++];
            result.parseFields(_globalData._diagnostics);
        }
        return result;
    }

    /**
     * Determines the zero-level text line involved in this particular retrieval
     */
    private TextLine getTopLevelTextLine() {
        if (_parent != null) {
            return _parent.getTopLevelTextLine();
        } else {
            return _localData._sourceObjects[_localData._nextSourceIndex - 1];
        }
    }

    /**
     * Indicates whether there is another line of source avaialble
     * @return true if we can return at least one more line of source
     */
    public boolean hasNextSourceLine() {
        return _localData._nextSourceIndex < _localData._sourceObjects.length;
    }

    /**
     * Obtains a reference to the GeneratedPool corresponding to the given location counter index.
     * If such a pool does not exist, it is created.
     * @param lcIndex index of the desired pool
     * @return reference to the pool
     */
    public GeneratedPool obtainPool(
        final int lcIndex
    ) {
        GeneratedPool gp = _globalData._generatedPools.get(lcIndex);
        if (gp == null) {
            gp = new GeneratedPool();
            _globalData._generatedPools.put(lcIndex, gp);
        }
        return gp;
    }

    /**
     * Generates a map of location counter indices to LocationCounterPool object
     * @return the map
     */
    Map<Integer, LocationCounterPool> produceLocationCounterPools(
    ) {
        Map<Integer, LocationCounterPool> result = new TreeMap<>();
        for (Map.Entry<Integer, GeneratedPool> entry : _globalData._generatedPools.entrySet()) {
            result.put(entry.getKey(), entry.getValue().produceLocationCounterPool());
        }
        return result;
    }

    public void setArithmeticFaultCompatibilityMode() { _globalData._arithmeticFaultCompatibilityMode = true; }
    public void setArithmeticFaultNonInterruptMode() { _globalData._arithmeticFaultNonInterruptMode = true; }
    public void setCharacterMode( CharacterMode mode ) { _localData._characterMode = mode; }
    public void setCodeMode( CodeMode mode ) { _localData._codeMode = mode; }
    public void setCurrentGenerationLCIndex( int index ) { _localData._currentGenerationLCIndex = index; }
    public void setCurrentLitLCIndex( int index ) { _localData._currentLitLCIndex = index; }
    public void setQuarterWordMode() { _globalData._quarterWordMode = true; }
    public void setThirdWordMode() { _globalData._thirdWordMode = true; }

    /**
     * Resets our source textline pointer for a new iteration over the source code
     */
    void resetSource() {
        _localData._nextSourceIndex = 0;
    }

    /**
     * Returns the number of lines of source code
     * @return requested value
     */
    public int sourceLineCount() {
        return _localData._sourceObjects.length;
    }
}
