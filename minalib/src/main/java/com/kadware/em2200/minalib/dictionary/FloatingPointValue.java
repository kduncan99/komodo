/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * A Value which represents a floating point thing.
 */
public class FloatingPointValue extends Value {

    public final double _value;

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - native floating point value
     */
    public FloatingPointValue(
        final boolean flagged,
        final double value
    ) {
        super(flagged);
        _value = value;
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
        if (obj instanceof FloatingPointValue) {
            FloatingPointValue fpObj = (FloatingPointValue)obj;
            return Double.compare( _value, fpObj._value );
        } else {
            throw new TypeException();
        }
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new value
     * @return new object
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) {
        return new FloatingPointValue(newFlagged, _value);
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
        return (obj instanceof FloatingPointValue) && (_value == ((FloatingPointValue)obj)._value);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.FloatingPoint;
    }

    /**
     * Simply return this object
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post diagnostics if necessary
     * @return new object
     */
    @Override
    public FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) {
        return this;
    }

    /**
     * Transform the value to an IntegerValue, if possible.
     * This is NOT an arithmetic conversion - we are merely creating an IntegerValue which contains
     * the same two 36-bit words as would represent the floating point number we represent.
     * The result is generally... not useful
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post diagnostics if necessary
     * @return new object
     */
    @Override
    public IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) {
        return new IntegerValue(_flagged, (long)_value, null);
    }

    /**
     * Transform the value to a StringValue, if possible.  Actually, it's not possible.
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * @return new object
     * @throws TypeException always - we cannot translate to a string
     */
    @Override
    public StringValue toStringValue(
        final Locale locale,
        final CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException {
        diagnostics.append(new ValueDiagnostic(locale, "Cannot convert floating point to a string"));
        throw new TypeException();
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return String.format("%s%f", _flagged, _value);
    }
}
