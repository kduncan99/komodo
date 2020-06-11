/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.RelocatableModule;

/**
 * An absolute value
 */
class AbsoluteSymbolEntry extends SymbolEntry {

    final RelocatableModule _definingModule;

    /**
     * Constructor for linker-defined values
     */
    AbsoluteSymbolEntry(
        final String symbol,
        final long baseValue
    ) {
        super(symbol, baseValue);
        _definingModule = null;
    }

    /**
     * Constructor for relocatable module-defined values
     */
    AbsoluteSymbolEntry(
        final String symbol,
        final long baseValue,
        final RelocatableModule definingModule
    ) {
        super(symbol, baseValue);
        _definingModule = definingModule;
    }
}
