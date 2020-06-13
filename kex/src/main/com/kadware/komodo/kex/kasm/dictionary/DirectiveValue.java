/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;

/**
 * Class which represents a directive
 */
public class DirectiveValue extends Value {

    public final Class<?> _class;

    /**
     * Normal constructor
     * @param clazz class of the function handler
     */
    public DirectiveValue(
        final Class<?> clazz
    ) {
        _class = clazz;
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param locale new value for Locale if known/appropriate; otherwise the existing value is used
     * @param newFlagged new attribute value
     * @return new value
     * @throws TypeException always - cannot be copied
     */
    @Override
    public Value copy(
        final Locale locale,
        final boolean newFlagged
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale if known/appropriate; otherwise the existing value is used
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    @Override
    public Value copy(
        final Locale locale,
        final ValuePrecision newPrecision
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Check for equality
     * @param obj comparison object
     * @return true if objects are equal
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (!(obj instanceof DirectiveValue)) {
            return false;
        }

        DirectiveValue dir = (DirectiveValue)obj;
        return (_class.equals(dir._class));
    }

    @Override public ValueType getType() { return ValueType.Directive; }
    @Override public int hashCode() { return _class.hashCode(); }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return "<directive>";
    }
}
