/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.FieldDescriptor;

/**
 * Describes a label for an undefined reference.
 */
public class UnresolvedReferenceToLabel extends UnresolvedReference {

    public final String _label;

    /**
     * constructor
     * @param fieldDescriptor describes the subset of the containing value, to which this reference applies
     * @param isNegative true if this value is to be arithmetically inverted when it is integrated into the containing value
     * @param label label of interest
     */
    public UnresolvedReferenceToLabel(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative,
        final String label
    ) {
        super(fieldDescriptor, isNegative);
        _label = label;
    }

    @Override
    public UnresolvedReference copy(
        final boolean isNegative
    ) {
        return new UnresolvedReferenceToLabel(_fieldDescriptor, isNegative, _label);
    }

    @Override
    public UnresolvedReference copy(
        final FieldDescriptor fieldDescriptor
    ) {
        return new UnresolvedReferenceToLabel(fieldDescriptor, _isNegative, _label);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UnresolvedReferenceToLabel) {
            UnresolvedReferenceToLabel refObj = (UnresolvedReferenceToLabel) obj;
            return (_isNegative == refObj._isNegative)
                   && (_fieldDescriptor.equals(refObj._fieldDescriptor))
                   && (_label.equals( refObj._label));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _label.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s%s", _isNegative ? "-" : "+", _label);
    }
}
