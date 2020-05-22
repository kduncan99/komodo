/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

/**
 * Describes a symbol entry in our symbol table used for satisfying unresolved references
 */
abstract class SymbolEntry {

    final String _symbol;
    final long _baseValue;

    SymbolEntry(
        final String symbol,
        final long baseValue
    ) {
        _symbol = symbol;
        _baseValue = baseValue;
    }
}
