/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.RelocatableModule;

/**
 * A symbol representing an offset (possibly of 0) from the mapped address of a particular
 * location counter pool. All real entry points (such as functions) are in this category.
 */
public class LocationCounterRelativeSymbolEntry extends SymbolEntry {

    final LCPoolSpecification _lcPoolSpecification;

    LocationCounterRelativeSymbolEntry(
        final String symbol,
        final long baseValue,
        final RelocatableModule containingModule,
        final int locationCounterIndex
    ) {
        super(symbol, baseValue);
        _lcPoolSpecification = new LCPoolSpecification(containingModule,locationCounterIndex);
    }
}
