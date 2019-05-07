/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;

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
        final FieldDescriptor newFieldDescriptor
    ) {
        return new UndefinedReferenceToLabel(newFieldDescriptor, _isNegative, _label);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UndefinedReferenceToLabel) {
            UndefinedReferenceToLabel refObj = (UndefinedReferenceToLabel) obj;
            return (_fieldDescriptor.equals( refObj._fieldDescriptor ))
                   && (_isNegative == refObj._isNegative)
                   && (_label.equals( refObj._label));
        }
        return false;
    }

    @Override
    public String toString(
    ) {
        return String.format("[%d:%d]%s%s",
                             _fieldDescriptor._startingBit,
                             _fieldDescriptor._startingBit + _fieldDescriptor._fieldSize - 1,
                             _isNegative ? "-" : "+",
                             _label);
    }
}
