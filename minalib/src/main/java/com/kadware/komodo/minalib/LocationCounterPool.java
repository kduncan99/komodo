/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */
package com.kadware.komodo.minalib;

/**
 * Represents the storage controlled by a particular LocationCounterPool, within a particular Module
 */
public class LocationCounterPool {

    public final boolean _needsExtendedMode;
    public final RelocatableWord[] _storage;

    LocationCounterPool(
        final RelocatableWord[] storage,
        final boolean needsExtendedMode
    ) {
        _storage = storage;
        _needsExtendedMode = needsExtendedMode;
    }
}
