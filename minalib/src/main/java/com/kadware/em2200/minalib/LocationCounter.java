/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */
package com.kadware.em2200.minalib;

import java.util.ArrayList;

/**
 * Represents the storage controlled by a particular LocationCounter, within a particular Module
 */
public class LocationCounter {

    private int _index = 0;
    private final ArrayList<RelocatableWord36> _storage = new ArrayList<>();
    //???? need to store value modifiers somehow (i.e., LC ctr offset, external ref's, etc)

    /**
     * Appends a value to this location counter's storage
     * <p>
     * @param word
     */
    public void appendStorage(
        final RelocatableWord36 word
    ) {
        _storage.add(word);
        ++_index;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getIndex(
    ) {
        return _index;
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
