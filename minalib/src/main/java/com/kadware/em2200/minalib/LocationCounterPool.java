/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */
package com.kadware.em2200.minalib;

/**
 * Represents the storage controlled by a particular LocationCounterPool, within a particular Module
 */
public class LocationCounterPool {

    public final RelocatableWord36[] _storage;

    public LocationCounterPool(
        final RelocatableWord36[] storage
    ) {
        _storage = storage;
    }
}
