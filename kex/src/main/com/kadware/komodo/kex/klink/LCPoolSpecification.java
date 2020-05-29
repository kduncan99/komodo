/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.RelocatableModule;

/**
 * Specifies a particular lc pool within a relocatable module -
 * one or more of these are encapsulated into the BankDeclaration object
 */
public class LCPoolSpecification {
    final RelocatableModule _module;
    final int _lcIndex;

    public LCPoolSpecification(
        final RelocatableModule module,
        final int lcIndex
    ) {
        _module = module;
        _lcIndex = lcIndex;
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof LCPoolSpecification) {
            LCPoolSpecification lcps = (LCPoolSpecification) obj;
            return ((lcps._module == _module) && (lcps._lcIndex == _lcIndex));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (_module.hashCode() << 9) | _lcIndex;
    }

    @Override
    public String toString() {
        return String.format("{%s(%d).%d}", _module.getModuleName(), _module.hashCode(), _lcIndex);//TODO remove hashcode
    }
}
