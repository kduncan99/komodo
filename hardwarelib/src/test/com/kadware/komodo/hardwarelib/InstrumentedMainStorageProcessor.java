/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.RegionTracker;

/**
 * Extended subclass of StaticMainStorageProcessor class, suitably instrumented for special testing
 */
public class InstrumentedMainStorageProcessor extends MainStorageProcessor {

    //  Keep track of the areas of storage which we've allocated
    public final RegionTracker _regions;

    public InstrumentedMainStorageProcessor(
        final String name,
        final short upi,
        final int sizeInWords
    ) {
        super(name, upi, sizeInWords);
        _regions = new RegionTracker(sizeInWords);
    }
}
