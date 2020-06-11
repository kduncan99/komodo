/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.exceptions.*;

/**
 * Base class for a Value
 */
public abstract class Value {

    public final boolean _flagged;
    public final ValuePrecision _precision;

    /**
     * Normal constructor
     * @param flagged (i.e., has leading asterisk)
     * @param precision single or double (or default)
     */
    protected Value(
        final boolean flagged,
        final ValuePrecision precision
    ) {
        _flagged = flagged;
        _precision = precision;
    }

    /**
     * Simpler constructor
     * @param flagged (i.e., has leading asterisk)
     */
    protected Value(
        final boolean flagged
    ) {
        _flagged = flagged;
        _precision = ValuePrecision.Default;
    }

    /**
     * Simplest constructor
     */
    protected Value() {
        _flagged = false;
        _precision = ValuePrecision.Default;
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws FormException if the form attached to the comparison object is not equal to the one attached to this object
     * @throws RelocationException if the relocation info for this object and the comparison object are incompatible
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    public abstract int compareTo(
        final Object obj
    ) throws FormException,
             RelocationException,
             TypeException;

    /**
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final boolean newFlagged
    ) throws TypeException;

    /**
     * Create a new copy of this object, with the given precision value
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final ValuePrecision newPrecision
    ) throws TypeException;

    /**
     * Check for equality
     * Must be overridden by subclass
     * @param obj comparison object
     * @return true if objects are equal, else false
     */
    @Override
    public abstract boolean equals(
        final Object obj
    );

    /**
     * Getter
     * @return value type
     */
    public abstract ValueType getType();

    @Override
    public abstract int hashCode();

    /**
     * Create user-readable string to describe this value.
     * Should be over-ridden by the various subclasses, and they must call back to appendAttributes()
     * @return user-readable string
     */
    public abstract String toString();
}
