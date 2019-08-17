/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

/**
 * Describes a LC offset for an undefined reference,
 * and will be replaced by the virtual address of the start of the indicated LC pool.
 */
public class UndefinedReferenceToLocationCounter extends UndefinedReference {

    public final int _locationCounterIndex;

    public UndefinedReferenceToLocationCounter(
        final boolean isNegative,
        final int locationCounterIndex
    ) {
        super(isNegative);
        _locationCounterIndex = locationCounterIndex;
    }

    @Override
    public UndefinedReference copy(
        final boolean isNegative
    ) {
        return new UndefinedReferenceToLocationCounter(isNegative, _locationCounterIndex);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UndefinedReferenceToLocationCounter) {
            UndefinedReferenceToLocationCounter refObj = (UndefinedReferenceToLocationCounter) obj;
            return (_isNegative == refObj._isNegative)
                   && (_locationCounterIndex == refObj._locationCounterIndex);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _locationCounterIndex;
    }

    @Override
    public String toString(
    ) {
        return String.format("%s$LC(%d)",
                             _isNegative ? "-" : "+",
                             _locationCounterIndex);
    }
}
