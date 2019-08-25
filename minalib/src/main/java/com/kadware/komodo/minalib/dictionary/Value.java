/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.minalib.CharacterMode;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.exceptions.*;

/**
 * Base class for a Value
 */
public abstract class Value {

    public final boolean _flagged;
    public final ValuePrecision _precision;
    public final ValueJustification _justification;

    /**
     * Normal constructor
     * @param flagged (i.e., has leading asterisk)
     * @param precision single or double (or default)
     * @param justification left or right (or default)
     */
    protected Value(
        final boolean flagged,
        final ValuePrecision precision,
        final ValueJustification justification
    ) {
        _flagged = flagged;
        _precision = precision;
        _justification = justification;
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
        _justification = ValueJustification.Default;
    }

    /**
     * Simplest constructor
     */
    protected Value() {
        _flagged = false;
        _precision = ValuePrecision.Default;
        _justification = ValueJustification.Default;
    }

    //TODO obsolete?
//    /**
//     * Compares an object to this object
//     * @param obj comparison object
//     * @return -1 if this object sorts before (is less than) the given object
//     *         +1 if this object sorts after (is greater than) the given object,
//     *          0 if both objects sort to the same position (are equal)
//     * @throws FormException if the form attached to the comparison object is not equal to the one attached to this object
//     * @throws RelocationException if the relocation info for this object and the comparison object are incompatible
//     * @throws TypeException if there is no reasonable way to compare the objects
//     */
//    public abstract int compareTo(
//        final Object obj
//    ) throws FormException,
//             RelocationException,
//             TypeException;

//    /**
//     * Create a new copy of this object, with the given flagged value
//     * @param newFlagged new value for Flagged attribute
//     * @return new Value
//     * @throws TypeException if object cannot be copied
//     */
//    public abstract Value copy(
//        final boolean newFlagged
//    ) throws TypeException;

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

    //TODO obsolete?
//    /**
//     * Transform the value to an FloatPointValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new Value
//     * @throws TypeException if object cannot be converted
//     */
//    public abstract FloatingPointValue toFloatingPointValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException;
//
//    /**
//     * Transform the value to an IntegerValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new Value
//     * @throws TypeException if object cannot be converted
//     */
//    public abstract IntegerValue toIntegerValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException;
//
//    /**
//     * Transform the value to a StringValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param characterMode desired character mode
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new Value
//     * @throws TypeException if object cannot be converted
//     */
//    public abstract StringValue toStringValue(
//        final Locale locale,
//        CharacterMode characterMode,
//        Diagnostics diagnostics
//    ) throws TypeException;

    /**
     * Create user-readable string to describe this value.
     * Should be over-ridden by the various subclasses, and they must call back to appendAttributes()
     * @return user-readable string
     */
    public abstract String toString();
}
