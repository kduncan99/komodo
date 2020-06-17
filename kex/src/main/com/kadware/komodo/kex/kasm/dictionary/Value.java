/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.exceptions.*;

/**
 * Base class for a Value
 */
public abstract class Value {

    public final Locale _locale;            //  Locale where value was created, if appropriate
    public final boolean _flagged;
    public final ValuePrecision _precision;

    //  An astonishing variety of constructors...

    /**
     * Simplest constructor
     * neither locale, nor flagged, nor precision apply to this Value object
     */
    protected Value() {
        _locale = null;
        _flagged = false;
        _precision = ValuePrecision.Default;
    }

    /**
     * @param flagged (i.e., has leading asterisk)
     */
    protected Value(
        final boolean flagged
    ) {
        _locale = null;
        _flagged = flagged;
        _precision = ValuePrecision.Default;
    }

    /**
     * @param precision single or double (or default)
     */
    protected Value(
        final ValuePrecision precision
    ) {
        _locale = null;
        _flagged = false;
        _precision = precision;
    }

    /**
     * @param flagged (i.e., has leading asterisk)
     * @param precision single or double (or default)
     */
    protected Value(
        final boolean flagged,
        final ValuePrecision precision
    ) {
        _locale = null;
        _flagged = flagged;
        _precision = precision;
    }

    /**
     * @param locale locale at which this value was defined
     */
    protected Value(
        final Locale locale
    ) {
        _locale = locale;
        _flagged = false;
        _precision = ValuePrecision.Default;
    }

    /**
     * @param locale locale at which this value was defined
     * @param flagged (i.e., has leading asterisk)
     */
    protected Value(
        final Locale locale,
        final boolean flagged
    ) {
        _locale = locale;
        _flagged = flagged;
        _precision = ValuePrecision.Default;
    }

    /**
     * @param locale locale at which this value was defined
     * @param precision single or double (or default)
     */
    protected Value(
        final Locale locale,
        final ValuePrecision precision
    ) {
        _locale = locale;
        _flagged = false;
        _precision = precision;
    }

    /**
     * @param locale locale at which this value was defined
     * @param flagged (i.e., has leading asterisk)
     * @param precision single or double (or default)
     */
    protected Value(
        final Locale locale,
        final boolean flagged,
        final ValuePrecision precision
    ) {
        _locale = locale;
        _flagged = flagged;
        _precision = precision;
    }


    //  Interesting things which can be invoked

    /**
     * Compares an object to this object.
     * Locales are not considered for equality testing.
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
     * Convenience method for changing the flagged parameter when the locale is not known
     * or it is desired to use the Locale from the source object (this object).
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public Value copy(
        final boolean newFlagged
    ) throws TypeException {
        return copy(_locale, newFlagged);
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param locale new value for Locale
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final Locale locale,
        final boolean newFlagged
    ) throws TypeException;

    /**
     * Convenience method for changing the precision parameter when the local is not known
     * or it is desired to use the Locale from the source object (this object).
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public Value copy(
        final ValuePrecision newPrecision
    ) throws TypeException {
        return copy(_locale, newPrecision);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final Locale locale,
        final ValuePrecision newPrecision
    ) throws TypeException;

    /**
     * Check for equality
     * Must be overridden by subclass
     * Locale is not to be used for determining equality
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
