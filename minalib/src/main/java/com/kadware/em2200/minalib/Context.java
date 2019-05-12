/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    public static class GeneratedWord extends HashMap<FieldDescriptor, IntegerValue> {

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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data / Attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * And here is the map of LC indices to the various GeneratedPool objects...
     */
    public Map<Integer, GeneratedPool> _generatedPools = new TreeMap<>();

    //  What mode are string literals generated in
    public CharacterMode _characterMode = CharacterMode.ASCII;

    //  basic or extended mode generation?
    public CodeMode _codeMode = CodeMode.Extended;

    //  What is the default LC index for code generation?
    public int _currentGenerationLCIndex = 1;

    //  What is the default LC index for literal pool generation?
    public int _currentLitLCIndex = 0;

    //  What dictionary should be used for lookups and label establishment?
    public final Dictionary _dictionary;

    //  name of the relocatable module
    public final String _moduleName;

    //  Things relating to the relocatable module as a whole
    private boolean _arithmeticFaultCompatibilityMode = false;
    private boolean _arithmeticFaultNonInterruptMode = false;
    private boolean _quarterWordMode = false;
    private boolean _thirdWordMode = false;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * General constructor
     */
    public Context(
        final Dictionary upperLevelDictionary,
        final String moduleName
    ) {
        _dictionary = new Dictionary(upperLevelDictionary);
        _moduleName = moduleName;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Obtains a reference to the GeneratedPool corresponding to the given location counter index.
     * If such a pool does not exist, it is created.
     * @param lcIndex index of the desired pool
     * @return reference to the pool
     */
    private GeneratedPool obtainPool(
        final int lcIndex
    ) {
        GeneratedPool gp = _generatedPools.get(lcIndex);
        if (gp == null) {
            gp = new GeneratedPool();
            _generatedPools.put(lcIndex, gp);
        }
        return gp;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  special accessors - the sub context overrides these to call its parent instead of using its regular values
    //  ----------------------------------------------------------------------------------------------------------------------------

    public boolean getArithmeticFaultCompatibilityMode() { return _arithmeticFaultCompatibilityMode; }
    public boolean getArithmeticFaultNonInterruptMode() { return _arithmeticFaultNonInterruptMode; }
    public boolean getQuarterWordMode() { return _quarterWordMode; }
    public boolean getThirdWordMode() { return _thirdWordMode; }

    public void setArithmeticFaultCompatibilityMode( final boolean value ) { _arithmeticFaultCompatibilityMode = value; }
    public void setArithmeticFaultNonInterruptMode( final boolean value ) { _arithmeticFaultNonInterruptMode = value; }
    public void setQuarterWordMode( final boolean value ) { _quarterWordMode = value; }
    public void setThirdWordMode( final boolean value ) { _thirdWordMode = value; }


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

    /**
     * Creates an IntegerValue object with an appropriate undefined reference to represent the current location of the
     * current generation location counter (e.g., for interpreting '$' or whatever).
     * @return IntegerValue object as described
     */
    public IntegerValue getCurrentLocation(
    ) {
        GeneratedPool gp = obtainPool(_currentGenerationLCIndex);
        int lcOffset = gp._nextOffset;
        UndefinedReference[] refs = { new UndefinedReferenceToLocationCounter(new FieldDescriptor(0, 36),
                                                                              false,
                                                                              _currentGenerationLCIndex) };
        return new IntegerValue(false, lcOffset, refs);
    }

    /**
     * Generates a map of location counter indices to LocationCounterPool object
     * @param diagnostics where we store any necessary diagnostics
     * @return the map
     */
    Map<Integer, LocationCounterPool> produceLocationCounterPools(
        final Diagnostics diagnostics
    ) {
        Map<Integer, LocationCounterPool> result = new TreeMap<>();
        for (Map.Entry<Integer, GeneratedPool> entry : _generatedPools.entrySet()) {
            result.put(entry.getKey(), entry.getValue().produceLocationCounterPool(diagnostics));
        }
        return result;
    }
}
