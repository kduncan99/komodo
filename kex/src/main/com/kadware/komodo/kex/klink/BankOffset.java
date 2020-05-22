/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

/**
 * Describes the starting location of a mapped location counter pool...
 * Used primarily as the key for the corresponding map table.
 */
class BankOffset {

    final int _bankDescriptorIndex;
    final int _offset;

    BankOffset(
        final int bankDescriptorIndex,
        final int offset
    ) {
        _bankDescriptorIndex = bankDescriptorIndex;
        _offset = offset;
    }
}
