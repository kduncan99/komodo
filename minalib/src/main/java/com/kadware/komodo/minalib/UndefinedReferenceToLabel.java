/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

/**
 * Describes a label for an undefined reference.
 */
public class UndefinedReferenceToLabel extends UndefinedReference {

    public final String _label;

    public UndefinedReferenceToLabel(
        final boolean isNegative,
        final String label
    ) {
        super(isNegative);
        _label = label;
    }

    @Override
    public UndefinedReference copy(
        final boolean isNegative
    ) {
        return new UndefinedReferenceToLabel(isNegative, _label);
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

    @Override
    public int hashCode() {
        return _label.hashCode();
    }

    @Override
    public String toString(
    ) {
        return String.format("%s%s",
                             _isNegative ? "-" : "+",
                             _label);
    }
}
