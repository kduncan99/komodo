/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.RegionTracker;
import com.kadware.komodo.hardwarelib.StaticMainStorageProcessor;

/**
 * Extended subclass of StaticMainStorageProcessor class, suitably instrumented for special testing
 */
public class InstrumentedMainStorageProcessor extends StaticMainStorageProcessor {

    //  Keep track of the areas of storage which we've allocated
    final RegionTracker _regions;

    public InstrumentedMainStorageProcessor(
        final String name,
        final short upi,
        final int sizeInWords
    ) {
        super(name, upi, sizeInWords);
        _regions = new RegionTracker(sizeInWords);
    }
}
