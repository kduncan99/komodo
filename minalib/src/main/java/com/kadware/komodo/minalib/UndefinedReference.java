/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

/**
 * Base class for all undefined references
 */
public abstract class UndefinedReference {

    public final boolean _isNegative;

    UndefinedReference(
        final boolean isNegative
    ) {
        _isNegative = isNegative;
    }

    public abstract UndefinedReference copy(
        final boolean isNegative
    );

    @Override
    public abstract boolean equals(final Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
