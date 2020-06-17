/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.RelocatableModule;
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
     * Advances the next offset for the generated word map.
     * Used for $RES directive
     */
    public void advance(
        final int count
    ) {
        _nextOffset += count;
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
