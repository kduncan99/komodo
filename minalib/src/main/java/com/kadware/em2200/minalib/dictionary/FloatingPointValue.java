/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * A Value which represents a floating point thing.
 */
public class FloatingPointValue extends Value {

    public static class Builder {
        private boolean _flagged = false;
        private Precision _precision = Precision.None;
        private Signed _signed = Signed.None;
        private double _value;

        public Builder setFlagged(
            final boolean flagged
        ) {
            _flagged = flagged;
            return this;
        }

        public Builder setPrecision(
            final Precision precision
        ) {
            _precision = precision;
            return this;
        }

        public Builder setSigned(
            final Signed signed
        ) {
            _signed = signed;
            return this;
        }

        public Builder setValue(
            final double value
        ) {
            _value = value;
            return this;
        }

        public FloatingPointValue build(
        ) {
            return new FloatingPointValue(_flagged, _signed, _precision, _value);
        }
    }

    //???? TODO  - how do we really store these?
    private final double _value;

    /**
     * constructor
     * <p>
     * @param flagged - leading asterisk
     * @param signed - is this signed? pos or neg?
     * @param precision - only affects things at value generation time
     * @param value - native floating point value
     */
    private FloatingPointValue(
        final boolean flagged,
        final Signed signed,
        final Precision precision,
        final double value
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
        return new FloatingPointValue(newFlagged, getSigned(), getPrecision(), _value);
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
        return new FloatingPointValue(getFlagged(), newSigned, getPrecision(), _value);
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
        return new FloatingPointValue(getFlagged(), getSigned(), newPrecision, _value);
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
        return new IntegerValue.Builder().build();
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

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_value);
        super.appendAttributes(sb);
        return sb.toString();
    }
}
