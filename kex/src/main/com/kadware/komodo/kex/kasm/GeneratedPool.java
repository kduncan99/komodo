/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostics;
import com.kadware.komodo.kex.kasm.diagnostics.FormDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.TruncationDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * During assembly, location counter pools are built up.  This is a map of lc offsets to GeneratedWords,
 * in offset order.  It is not necessarily monotonically increasing - it could have holes in it by virtue
 * of incrementing _nextOffset without adding a value (such as happens with $RES directives)
 */
@SuppressWarnings("DuplicatedCode")
public class GeneratedPool {

    //  LC Index for this pool
    private final int _locationCounterIndex;

    //  Code which is directly generated into this pool
    private final Map<Integer, GeneratedWord> _generatedWords = new TreeMap<>();

    //  Offset of the next word to be generated in _generatedWords
    private int _nextOffset = 0;

    //  Generated literals in this pool - stored here temporarily until assembly is complete,
    //  and then added to the generated words map as we resolve the temporary lit references.
    private final List<GeneratedWord> _literalPool = new LinkedList<>();

    //  Set by $INFO directives... indicates this location counter pool should be in a bank
    //  which has the extended mode flag set.
    private boolean _extendedModeFlag;

    /**
     * Constructor
     */
    GeneratedPool(
        final int locationCounterIndex
    ) {
        _locationCounterIndex = locationCounterIndex;
    }

    /**
     * Advances the next offset for the generated word map.
     * Used for $RES directive
     */
    public void advance(
        final int count
    ) {
        _nextOffset += count;
    }

    /**
     * Generates a word (with possibly a form attached) at the next generated-word offset.
     * Also associates it with the given text line.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param value the integer/intrinsic value to be used
     */
    void generate(
        final TextLine textLine,
        final Locale locale,
        final IntegerValue value
    ) {
        int lcOffset = _nextOffset++;
        GeneratedWord gw = new GeneratedWord(textLine, locale, _locationCounterIndex, lcOffset, value);
        _generatedWords.put(lcOffset, gw);
        gw._topLevelTextLine._generatedWords.add(gw);
    }

    /**
     * Generates the multiple words for a given location counter index, beginning at the next generated-word offset
     * and places them into the appropriate location counter pool within the given context.
     * Also associates it with the given text line.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param values the values to be used
     */
    void generate(
        final TextLine textLine,
        final Locale locale,
        final long[] values
    ) {
        for (long value : values) {
            int lcOffset = _nextOffset++;
            IntegerValue iv = new IntegerValue.Builder().setLocale(locale).setValue(value).build();
            GeneratedWord gw = new GeneratedWord(textLine, locale, _locationCounterIndex, lcOffset, iv);
            _generatedWords.put(lcOffset, gw);
            gw._topLevelTextLine._generatedWords.add(gw);
        }
    }

    /**
     * Generates a word with a form attached at the next generated-word offset.
     * Also associates it with the given text line.
     * The form describes 1 or more fields, the totality of which are expected to describe 36 bits.
     * The values parameter is an array with as many entities as there are fields in the form.
     * Each value must fit within the size of that value's respective field.
     * The overall integer portion of the generated value is the respective component integer values
     * shifted into their field positions, and or'd together.
     * The individual values should not have forms attached, but they may have undefined references.
     * All such undefined references are adjusted to match the field description of the particular
     * field to which the reference applies.
     * @param textLine the top-level TextLine object which is responsible for generating this code
     * @param locale location of the text entity generating this word
     * @param form form describing the fields for which values are specified in the values parameter
     * @param values array of component values, each with potential undefined refereces but no attached forms
     * @param assembler where we post diagnostics if any need to be generated
     * @return value indicating the location which applies to the word which was just generated...
     *          This value is used in generating literals, and is not used in other situations.
     */
    IntegerValue generate(
        final TextLine textLine,
        final Locale locale,
        final Form form,
        final IntegerValue[] values,
        final Assembler assembler
    ) {
        int lcOffset = _nextOffset++;
        if (form._fieldSizes.length != values.length) {
            String msg = "Contradiction between number of values and number of fields in form";
            assembler.appendDiagnostic(new FormDiagnostic(locale, msg));
            return new IntegerValue.Builder().setLocale(locale).setValue(lcOffset).build();
        }

        BigInteger genInt = BigInteger.ZERO;
        int bit = form._leftSlop;
        List<UnresolvedReference> newRefs = new LinkedList<>();
        for (int fx = 0; fx < form._fieldSizes.length; ++fx) {
            genInt = genInt.shiftLeft(form._fieldSizes[fx]);
            BigInteger mask = BigInteger.valueOf((1L << form._fieldSizes[fx]) - 1);
            BigInteger fieldValue = values[fx]._value.get();
            boolean trunc;
            if (values[fx]._value.isPositive()) {
                trunc = !fieldValue.and(mask).equals(fieldValue);
            } else {
                trunc = !fieldValue.or(mask).equals(DoubleWord36.BIT_MASK);
            }

            if (trunc) {
                String msg = String.format("Value %012o exceeds size of field in form %s", fieldValue, form.toString());
                assembler.appendDiagnostic(new TruncationDiagnostic(locale, msg));
            }

            genInt = genInt.or(values[fx]._value.get().and(mask));
            for (UnresolvedReference ur : values[fx]._references) {
                newRefs.add(ur.copy(new FieldDescriptor(bit, form._fieldSizes[fx])));
            }

            bit += form._fieldSizes[fx];
        }

        IntegerValue iv = new IntegerValue.Builder().setLocale(locale)
                                                    .setValue(genInt)
                                                    .setForm(form)
                                                    .setReferences(newRefs)
                                                    .build();
        GeneratedWord gw = new GeneratedWord(textLine, locale, _locationCounterIndex, lcOffset, iv);
        gw._topLevelTextLine._generatedWords.add(gw);
        _generatedWords.put(lcOffset, gw);

        UnresolvedReference[] lcRefs = {
            new UnresolvedReferenceToLocationCounter(FieldDescriptor.W, false, _locationCounterIndex)
        };

        return new IntegerValue.Builder().setLocale(locale)
                                         .setValue(lcOffset)
                                         .setReferences(lcRefs)
                                         .build();
    }

    /**
     * Retrieves the state of the extended mode flag
     */
    public boolean getExtendedModeFlag() {
        return _extendedModeFlag;
    }

    /**
     * Retrieves an iterator for walking through the generated words container.
     */
    public Iterator<Map.Entry<Integer, GeneratedWord>> getGeneratedWordsIterator() {
        return _generatedWords.entrySet().iterator();
    }

    /**
     * Retrieves the offset which will be assigned to the next generated word
     * (assuming to intervening $RES)
     */
    public int getNextOffset() {
        return _nextOffset;
    }

    /**
     * Sets the state of the extended mode flag
     */
    public void setExtendedModeFlag(
        final boolean flag
    ) {
        _extendedModeFlag = flag;
    }

    /**
     * Store a new GeneratedWord at the next location in this pool, and advance the offset
     * @param value value to be stored
     */
    public void storeGeneratedWord(
        final GeneratedWord value
    ) {
        _generatedWords.put(_nextOffset++, value);
    }

    /**
     * Store a new GeneratedWord at the specified location in this pool.
     * Does not alter _nextOffset.
     * @param offset offset at which this word is to be stored
     * @param value value to be stored
     */
    public void storeGeneratedWord(
        final int offset,
        final GeneratedWord value
    ) {
        _generatedWords.put(offset, value);
    }

    /**
     * Stores a new GeneratedWord in the temporary literal pool for this location counter pool
     * @param value value to be stored
     * @return literal offset where the word is stored
     */
    public int storeLiteral(
        final GeneratedWord value
    ) {
        int offset = _literalPool.size();
        _literalPool.add(value);
        return offset;
    }

    /**
     * Produces an array of RelocatableWord objects to represent the content of this pool
     * Do not invoke this until the literal pool has been collapsed into the generated word map
     * for all generated pools.
     */
    RelocatableModule.RelocatableWord[] produceLocationCounterPool(
    ) {
        int totalWords = _nextOffset + _literalPool.size();
        RelocatableModule.RelocatableWord[] pool = new RelocatableModule.RelocatableWord[totalWords];
        for (Map.Entry<Integer, GeneratedWord> wordEntry : _generatedWords.entrySet()) {
            int lcOffset = wordEntry.getKey();
            GeneratedWord gw = wordEntry.getValue();
            pool[lcOffset] = gw.produceRelocatableWord();
        }

        int lcOffset = _nextOffset;
        for (GeneratedWord gw : _literalPool) {
            pool[lcOffset++] = gw.produceRelocatableWord();
        }

        return pool;
    }
}
