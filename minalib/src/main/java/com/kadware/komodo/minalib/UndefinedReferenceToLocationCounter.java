/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.FieldDescriptor;

/**
 * Describes a LC offset for an undefined reference,
 * and will be replaced by the virtual address of the start of the indicated LC pool.
 */
public class UndefinedReferenceToLocationCounter extends UndefinedReference {

    public final int _locationCounterIndex;

    public UndefinedReferenceToLocationCounter(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative,
        final int locationCounterIndex
    ) {
        super(fieldDescriptor, isNegative);
        _locationCounterIndex = locationCounterIndex;
    }

    @Override
    public UndefinedReference copy(
        final boolean isNegative
    ) {
        return new UndefinedReferenceToLocationCounter(_fieldDescriptor, isNegative, _locationCounterIndex);
    }

    @Override
    public UndefinedReference copy(
        final FieldDescriptor newFieldDescriptor
    ) {
        return new UndefinedReferenceToLocationCounter(newFieldDescriptor, _isNegative, _locationCounterIndex);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UndefinedReferenceToLocationCounter) {
            UndefinedReferenceToLocationCounter refObj = (UndefinedReferenceToLocationCounter) obj;
            return (_fieldDescriptor.equals( refObj._fieldDescriptor ))
                   && (_isNegative == refObj._isNegative)
                   && (_locationCounterIndex == refObj._locationCounterIndex);
        }
        return false;
    }

    @Override
    public String toString(
    ) {
        return String.format("[%d:%d]%s$LC(%d)",
                             _fieldDescriptor._startingBit,
                             _fieldDescriptor._startingBit + _fieldDescriptor._fieldSize - 1,
                             _isNegative ? "-" : "+",
                             _locationCounterIndex);
    }
}
