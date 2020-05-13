package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.RelocatableModule;
import java.util.Map;
import java.util.TreeMap;

/**
 * During assembly, location counter pools are built up.  This is a map of lc offsets to GeneratedWords,
 * in offset order.  It is not necessarily monotonically increasing - it could have holes in it by virtue
 * of incrementing _nextOffset without adding a value (such as happens with $RES directives)
 */
public class GeneratedPool extends TreeMap<Integer, GeneratedWord> {

    private boolean _extendedModeFlag = false;
    private int _nextOffset = 0;

    /**
     * Advances the next offset value without generating words - mainly for $RES
     * @param count number of words to be advanced
     */
    void advance(
        final int count
    ) {
        _nextOffset += count;
    }

    public boolean getExtendedModeFlag() {
        return _extendedModeFlag;
    }

    /**
     * Get the offset of the current location (i.e., where the next generated word will be)
     */
    public int getNextOffset() {
        return _nextOffset;
    }

    /**
     * How big is the pool?
     * @return current size (does not necessarily indicate number of actual words - there might be a $RES hole)
     */
    public int getSize() {
        return _nextOffset;
    }

    public void setExtendedModeFlag(
        final boolean flag
    ) {
        _extendedModeFlag = flag;
    }

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
     * Produces an array of RelocatableWord objects to represent the content of this pool
     */
    RelocatableModule.RelocatableWord[] produceLocationCounterPool(
    ) {
        RelocatableModule.RelocatableWord[] pool = new RelocatableModule.RelocatableWord[getSize()];
        for (Map.Entry<Integer, GeneratedWord> wordEntry : entrySet()) {
            int lcOffset = wordEntry.getKey();
            GeneratedWord gw = wordEntry.getValue();
            pool[lcOffset] = gw.produceRelocatableWord();
        }

        return pool;
    }
}
