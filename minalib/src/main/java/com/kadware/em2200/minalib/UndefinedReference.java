/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.FieldDescriptor;

/**
 * Base class for all undefined references
 */
public abstract class UndefinedReference {

    final FieldDescriptor _fieldDescriptor;
    public final boolean _isNegative;

    UndefinedReference(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative
    ) {
        _fieldDescriptor = fieldDescriptor;
        _isNegative = isNegative;
    }

    public abstract UndefinedReference copy(
        final boolean isNegative
    );

    public abstract UndefinedReference copy(
        final FieldDescriptor newFieldDescriptor
    );

    @Override
    public abstract boolean equals(final Object obj);

    @Override
    public abstract String toString();
}
