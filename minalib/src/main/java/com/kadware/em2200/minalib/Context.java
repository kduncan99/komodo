/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import sun.util.resources.cldr.gl.CurrencyNames_gl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents the current context under which an assembly is being performed
 */
public class Context {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * During assembly, each generated word is placed in this table, keyed by the location counter index and offset.
     * It is comprised of the component values which make up the sub-fields of the word, in no particular order.
     */
    public class GeneratedWord extends HashMap<FieldDescriptor, IntegerValue> {

        public final Locale _locale;
        RelocatableWord36 _relocatableWord = null;

        GeneratedWord(
            final Locale locale
        ) {
            _locale = locale;
        }

        /**
         * Constructs a composite RelocatableWord36 object based upon the various component field definitions.
         * Should be called after we've resolved all references local to the containing module.
         * If the word has already been constructed, don't do it twice...
         * @return composite word
         */
        RelocatableWord36 produceRelocatableWord36(
            final Diagnostics diagnostics
        ) {
            if (_relocatableWord == null) {
                long discreteValue = 0;
                List<UndefinedReference> relRefs = new LinkedList<>();
                for (Entry<FieldDescriptor, IntegerValue> entry : entrySet()) {
                    //  convert value from twos- to ones-complement, check for 36-bit truncation
                    long fieldValue = entry.getValue()._value;
                    OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
                    OnesComplement.getOnesComplement36(fieldValue, ocr);
                    boolean trunc = ocr._overflow;
                    long value36 = ocr._result;

                    FieldDescriptor fd = entry.getKey();
                    long mask = (1L << fd._fieldSize) - 1;
                    long maskedValue = value36 & mask;

                    //  Check for field size truncation
                    if (fieldValue > 0) {
                        trunc = (value36 != maskedValue);
                    } else if (fieldValue < 0) {
                        trunc = ((mask | value36) != 0_777777_777777L);
                    }

                    if (trunc) {
                        diagnostics.append(new TruncationDiagnostic(_locale,
                                                                    fd._startingBit,
                                                                    fd._startingBit + fd._fieldSize - 1));
                    }

                    int shiftCount = 36 - fd._startingBit - fd._fieldSize;
                    discreteValue |= (maskedValue << shiftCount);

                    //  Propagate any remaining external references
                    for (UndefinedReference uRef : entry.getValue()._undefinedReferences) {
                        relRefs.add(uRef.copy(fd));
                    }
                }

                _relocatableWord =
                        new RelocatableWord36(discreteValue, relRefs.toArray(new UndefinedReference[0]));
            }

            return _relocatableWord;
        }
    }

    /**
     * During assembly, location counter pools are built up.  This is a map of lc offsets to GeneratedWords,
     * in offset order.  It is not necessaryl monotonically increasing - it could have holes in it by virtual
     * of incrementing _nextOffset without adding a value.
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
         * @param diagnostics where we store any diagnostics we produce
         * @return array
         */
        LocationCounterPool produceLocationCounterPool(
            final Diagnostics diagnostics
        ) {
            RelocatableWord36[] pool = new RelocatableWord36[_nextOffset];
            for (Map.Entry<Integer, GeneratedWord> wordEntry : entrySet()) {
                int lcOffset = wordEntry.getKey();
                GeneratedWord gw = wordEntry.getValue();
                pool[lcOffset] = gw.produceRelocatableWord36(diagnostics);
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

        //  Where diagnostics are posted
        private final Diagnostics _diagnostics;

        //TODO temporarily needed for BDIFunction
        private final String _moduleName;

        //  Map of LC indices to the various GeneratedPool objects...
        private final Map<Integer, GeneratedPool> _generatedPools = new TreeMap<>();

        private Global(
            Diagnostics diagnostics,
            String moduleName
        ) {
            _diagnostics = diagnostics;
            _moduleName = moduleName;
        }
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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * General constructor for the top-level context
     */
    public Context(
        final Dictionary upperLevelDictionary,
        final String[] sourceText,
        final String moduleName
    ) {
        _globalData = new Global(new Diagnostics(), moduleName);
        _localData = new Local(new Dictionary(upperLevelDictionary), sourceText);
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
     * Generates the given word as a set of subfields for a given location counter index and offset,
     * and places it into the appropriate location counter pool within the given context.
     * @param locale locale of the text entity generating this word
     * @param lcIndex index of the location counter pool where-in the value is to be placed
     * @param form indicates the bit fields - there should be one value per bit-field
     * @param values the values to be used
     * @return value indicating the location which applies to the word which was just generated
     */
    public IntegerValue generate(
        final Locale locale,
        final int lcIndex,
        final Form form,
        final IntegerValue[] values
    ) {
        if (values.length != form._fieldSizes.length) {
            throw new RuntimeException("Number of bit-fields in the form differ from number of values");
        }

        int startingBit = form._leftSlop;
        GeneratedWord gw = new GeneratedWord(locale);
        for (int fx = 0; fx < values.length; ++fx) {
            FieldDescriptor fd = new FieldDescriptor(startingBit, form._fieldSizes[fx]);
            gw.put(fd, values[fx]);
            startingBit += form._fieldSizes[fx];
        }

        GeneratedPool gp = obtainPool(lcIndex);
        int lcOffset = gp._nextOffset;
        gp.store(gw);

        UndefinedReference[] refs = { new UndefinedReferenceToLocationCounter(new FieldDescriptor(0, 36),
                                                                              false,
                                                                              lcIndex) };
        return new IntegerValue(false, lcOffset, refs);
    }

    public boolean getArithmeticFaultCompatibilityMode() { return _globalData._arithmeticFaultCompatibilityMode; }
    public boolean getArithmeticFaultNonInterruptMode() { return _globalData._arithmeticFaultNonInterruptMode; }
    public CharacterMode getCharacterMode() { return _localData._characterMode; }
    public CodeMode getCodeMode() { return _localData._codeMode; }
    public int getCurrentGenerationLCIndex() { return _localData._currentGenerationLCIndex; }
    public int getCurrentLitLCIndex() { return _localData._currentLitLCIndex; }
    public Diagnostics getDiagnostics() { return _globalData._diagnostics; }
    public Dictionary getDictionary() { return _localData._dictionary; }
    public Set<Map.Entry<Integer, GeneratedPool>> getGeneratedPools() { return _globalData._generatedPools.entrySet(); }
    public String getModuleName() { return _globalData._moduleName; }
    public int getNestingLevel() { return (_parent == null) ? 0 : _parent.getNestingLevel() + 1; }
    public TextLine[] getParsedCode() { return _localData._sourceObjects; }
    public boolean getQuarterWordMode() { return _globalData._quarterWordMode; }
    public boolean getThirdWordMode() { return _globalData._thirdWordMode; }

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
        return new IntegerValue(false, lcOffset, refs);
    }

    /**
     * Retrieves the next not-yet-assembled line of source code from this object
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
            result.put(entry.getKey(), entry.getValue().produceLocationCounterPool(_globalData._diagnostics));
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
