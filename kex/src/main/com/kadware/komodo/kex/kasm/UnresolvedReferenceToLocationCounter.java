/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.FieldDescriptor;

/**
 * Describes a LC offset for an undefined reference,
 * and will be replaced by the virtual address of the start of the indicated LC pool.
 */
public class UnresolvedReferenceToLocationCounter extends UnresolvedReference {

    public final int _locationCounterIndex;

    /**
     * constructor
     * @param fieldDescriptor describes the subset of the containing value, to which this reference applies
     * @param isNegative true if this value is to be arithmetically inverted when it is integrated into the containing value
     * @param locationCounterIndex index of the location counter which contains the literal of interest
     */
    public UnresolvedReferenceToLocationCounter(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative,
        final int locationCounterIndex
    ) {
        super(fieldDescriptor, isNegative);
        _locationCounterIndex = locationCounterIndex;
    }

    @Override
    public UnresolvedReference copy(
        final boolean isNegative
    ) {
        return new UnresolvedReferenceToLocationCounter(_fieldDescriptor, isNegative, _locationCounterIndex);
    }

    @Override
    public UnresolvedReference copy(
        final FieldDescriptor fieldDescriptor
    ) {
        return new UnresolvedReferenceToLocationCounter(fieldDescriptor, _isNegative, _locationCounterIndex);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UnresolvedReferenceToLocationCounter) {
            UnresolvedReferenceToLocationCounter refObj = (UnresolvedReferenceToLocationCounter) obj;
            return (_isNegative == refObj._isNegative)
                   && (_fieldDescriptor.equals(refObj._fieldDescriptor))
                   && (_locationCounterIndex == refObj._locationCounterIndex);
        }
        return false;
    }

    @Override public int hashCode() {
        return _locationCounterIndex;
    }

    @Override
    public String toString() {
        return String.format("%s$LC(%d)", _isNegative ? "-" : "+", _locationCounterIndex);
    }
}
