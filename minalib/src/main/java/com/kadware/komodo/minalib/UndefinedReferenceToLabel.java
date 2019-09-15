/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.FieldDescriptor;

/**
 * Describes a label for an undefined reference.
 */
public class UndefinedReferenceToLabel extends UndefinedReference {

    public final String _label;

    public UndefinedReferenceToLabel(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative,
        final String label
    ) {
        super(fieldDescriptor, isNegative);
        _label = label;
    }

    @Override
    public UndefinedReference copy(
        final boolean isNegative
    ) {
        return new UndefinedReferenceToLabel(_fieldDescriptor, isNegative, _label);
    }

    @Override
    public UndefinedReference copy(
        final FieldDescriptor fieldDescriptor
    ) {
        return new UndefinedReferenceToLabel(fieldDescriptor, _isNegative, _label);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UndefinedReferenceToLabel) {
            UndefinedReferenceToLabel refObj = (UndefinedReferenceToLabel) obj;
            return (_isNegative == refObj._isNegative) && (_label.equals( refObj._label));
        }
        return false;
    }

    @Override public int hashCode() {
        return _label.hashCode();
    }
    @Override public String toString() { return String.format("%s%s", _isNegative ? "-" : "+", _label); }
}
