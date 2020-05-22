/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.RelocatableModule;

/**
 * Contains useful info regarding the address at which the program starts.
 */
class ProgramStartInfo {

    final int _address;                             //  Address (for PAR) at which execution begins
    final int _bankDescriptorIndex;                 //  BDI of the bank containing the program start address
    final int _bankOffset;                          //  offset from the start of the containing bank, of the address
    final int _lcpOffset;                           //  offset from the start of the LC pool, of the address
    final LCPoolSpecification _lcpSpecification;    //  describes the module and LC pool containing the start address

    ProgramStartInfo(
        final int address,
        final int bankDescriptorIndex,
        final int bankOffset,
        final LCPoolSpecification lcpSpecification,
        final int lcpOffset
    ) {
        _address = address;
        _bankDescriptorIndex = bankDescriptorIndex;
        _bankOffset = bankOffset;
        _lcpOffset = lcpOffset;
        _lcpSpecification = lcpSpecification;
    }
}
