/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * A Value which represents a floating point thing.
 */
public class FloatingPointValue extends Value {

    //???? TODO  - how do we really store these?
    private final double _value;

    /**
     * constructor
     * <p>
     * @param value - native floating point value
     * @param flagged - leading asterisk
     * @param signed - is this signed? pos or neg?
     * @param precision - only affects things at value generation time
     */
    public FloatingPointValue(
        final double value,
        final boolean flagged,
        final Signed signed,
        final Precision precision
    ) {
        super(flagged, signed, precision, null, null);
        _value = value;
    }

    /**
     * Compares an object to this object
     * <p>
     * @param obj
     * <p>
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * <p>
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws TypeException {
        //????TODO : Account for signed attribute
        if (obj instanceof IntegerValue) {
            FloatingPointValue fpObj = (FloatingPointValue)obj;
            if (_value < fpObj._value) {
                return -1;
            } else if (_value > fpObj._value) {
                return 1;
            } else {
                return 0;
            }
        } else {
            throw new TypeException();
        }
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * <p>
     * @param newFlagged
     * <p>
     * @return
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) {
        return new FloatingPointValue(_value, newFlagged, getSigned(), getPrecision());
    }

    /**
     * Create a new copy of this object, with the given signed value
     * <p>
     * @param newSigned
     * <p>
     * @return
     */
    @Override
    public Value copy(
        final Signed newSigned
    ) {
        return new FloatingPointValue(_value, getFlagged(), newSigned, getPrecision());
    }

    /**
     * Create a new copy of this object, with the given precision value
     * <p>
     * @param newPrecision
     * <p>
     * @return
     */
    @Override
    public Value copy(
        final Precision newPrecision
    ) {
        return new FloatingPointValue(_value, getFlagged(), getSigned(), newPrecision);
    }

    /**
     * Check for equality
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj instanceof FloatingPointValue) && (_value == ((FloatingPointValue)obj)._value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.FloatingPoint;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public double getValue(
    ) {
        return _value;
    }

    /**
     * Simply return this object
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics
     * <p>
     * @return
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics
     * <p>
     * @return
     */
    @Override
    public IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) {
        //????TODO Fix this later
        return new IntegerValue(0, false, Signed.None, Precision.None, null, null);
    }

    /**
     * Transform the value to a StringValue, if possible.  Actually, it's not possible.
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
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
}
