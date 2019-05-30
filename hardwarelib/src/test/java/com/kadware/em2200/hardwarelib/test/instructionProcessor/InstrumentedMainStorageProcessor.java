/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.baselib.Region;
import com.kadware.em2200.baselib.RegionTracker;
import com.kadware.em2200.hardwarelib.MainStorageProcessor;

/**
 * Extended subclass of MainStorageProcessor class, suitably instrumented for special testing
 */
public class InstrumentedMainStorageProcessor extends MainStorageProcessor {

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
