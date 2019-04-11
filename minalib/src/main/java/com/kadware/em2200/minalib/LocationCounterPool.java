/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */
package com.kadware.em2200.minalib;

import java.util.ArrayList;

/**
 * Represents the storage controlled by a particular LocationCounterPool, within a particular Module
 */
public class LocationCounterPool {

    private int _nextOffset = 0;
    private final ArrayList<RelocatableWord36> _storage = new ArrayList<>();

    /**
     * Appends a value to this location counter's storage
     * <p>
     * @param word
     */
    public void appendStorage(
        final RelocatableWord36 word
    ) {
        _storage.add(word);
        ++_nextOffset;
    }

    /**
     * Getter
     * @return
     */
    public int getNextOffset(
    ) {
        return _nextOffset;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public ArrayList<RelocatableWord36> getStorage(
    ) {
        return _storage;
    }
}
