/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.exceptions.InvalidParameterException;
import java.math.BigInteger;

/**
 * A Value which represents a floating point thing.
 * All floating point values are stored in mina as doubles, and will be reduced if necessary to generate single-word floats.
 */
@SuppressWarnings("Duplicates")
public class FloatingPointValue extends Value {

    public final DoubleWord36 _value;

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - 72-bit double word containing floating point value (ones-complement)
     * @param precision - indicates single or double precision (or default)
     */
    public FloatingPointValue(
        final boolean flagged,
        final DoubleWord36 value,
        final ValuePrecision precision
    ) {
        super(flagged, precision, ValueJustification.Default);
        _value = value;
    }

    /**
     * constructor given component scientific notation parts
     * @param flagged - if there is a leading asterisk (probably meaningless)
     * @param isNegative - true for a negative number, else false
     * @param integralPortion - unsigned magnitude of the integral portion of the number.
     *                        as is the case with any integer, this is right-shifted with leading zeroes.
     * @param fractionalPortion - unsigned magnitude of the fractional portion (i.e., .5 -> 0x8000...00L).
     *                          this value is shifted left-most, with trailing zeroes.
     * @param precision - indicates single or double precision (or default)
     */
    public FloatingPointValue(
        final boolean flagged,
        final boolean isNegative,
        final long integralPortion,
        final long fractionalPortion,
        final ValuePrecision precision
    ) throws InvalidParameterException {
        super(flagged, precision, ValueJustification.Default);

        //  Special case for zero (pos or neg)
        if ((integralPortion == 0) && (fractionalPortion == 0)) {
            if (!isNegative) {
                _value = DoubleWord36.DW36_POSITIVE_ZERO;
            } else {
                _value = DoubleWord36.DW36_NEGATIVE_ZERO;
            }

            return;
        }

        //  normalize - only one of the following loops will do.
        int exponent = 0;
        long tempIntegral = integralPortion;
        long mantissa = fractionalPortion;
        while (tempIntegral > 1) {
            //  value shifts right, decimal shifts left, exponent increases
            mantissa = (mantissa >> 1) | (tempIntegral % 2 > 0 ? 0x80000000_00000000L : 0);
            tempIntegral >>= 1;
            exponent++;
        }
        while (tempIntegral == 0) {
            //  value shifts left, decimal shifts right, exponent decreases
            tempIntegral = (mantissa & 0x80000000_00000000L) != 0 ? 1 : 0;
            mantissa <<= 1;
            exponent--;
        }

        if ((exponent < -1023) || (exponent > 1024)) {
            throw new InvalidParameterException("Exponent out of range");
        }

        //  Compose double-word into 11-bit biased exponent and 60-bit mantissa
        long highWord = (exponent + 1023) << 24;
        highWord |= (mantissa & 0xFFFFFF00_00000000L) >> 12;
        long lowWord = (mantissa & 0x000000FF_FFFFFFFFL) >> 4;

        //  If negative, apply NOT to both words
        if (isNegative) {
            highWord = Word36.negate(highWord);
            lowWord = Word36.negate(lowWord);
        }

        //  done
        _value = new DoubleWord36(highWord, lowWord);
    }

//    /**
//     * Compares an object to this object
//     * @param obj comparison object
//     * @return -1 if this object sorts before (is less than) the given object
//     *         +1 if this object sorts after (is greater than) the given object,
//     *          0 if both objects sort to the same position (are equal)
//     * @throws TypeException if there is no reasonable way to compare the objects
//     */
//    @Override
//    public int compareTo(
//        final Object obj
//    ) throws TypeException {
//        if (obj instanceof FloatingPointValue) {
//            FloatingPointValue fpObj = (FloatingPointValue)obj;
//            return _value.floatingPointCompareTo(fpObj._value);
//        } else {
//            throw new TypeException();
//        }
//    }

//    /**
//     * Create a new copy of this object, with the given flagged value
//     * @param newFlagged new value
//     * @return new object
//     */
//    @Override
//    public Value copy(
//        final boolean newFlagged
//    ) {
//        return new FloatingPointValue(newFlagged, _value);
//    }

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

    @Override
    public int hashCode() { return _value.hashCode(); }

//    /**
//     * Simply return this object
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post diagnostics if necessary
//     * @return new object
//     */
//    @Override
//    public FloatingPointValue toFloatingPointValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) {
//        return this;
//    }
//
//    /**
//     * Transform the value to an IntegerValue, if possible.
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post diagnostics if necessary
//     * @throws TypeException always - we cannot translate to a string
//     */
//    @Override
//    public IntegerValue toIntegerValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        diagnostics.append(new ValueDiagnostic(locale, "Cannot convert floating point to integer"));
//        throw new TypeException();
//    }
//
//    /**
//     * Transform the value to a StringValue, if possible.  Actually, it's not possible.
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param characterMode desired character mode
//     * @param diagnostics where we post any necessary diagnostics
//     * @throws TypeException always - we cannot translate to a string
//     */
//    @Override
//    public StringValue toStringValue(
//        final Locale locale,
//        final CharacterMode characterMode,
//        final Diagnostics diagnostics
//    ) throws TypeException {
//        diagnostics.append(new ValueDiagnostic(locale, "Cannot convert floating point to a string"));
//        throw new TypeException();
//    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        if (_value == DoubleWord36.DW36_POSITIVE_ZERO) {
            return "0.0";
        } else if (_value == DoubleWord36.DW36_NEGATIVE_ZERO) {
            return "-0.0";
        }

        Word36[] words = _value.getWords();
        boolean neg = words[0].isNegative();
        if (neg) {
            words[0] = words[0].logicalNot();
            words[1] = words[1].logicalNot();
        }

        //  unnormalize - shift the binary values left or right according to the unbiased exponent.
        int exponent = (int) (words[0].getT1() & 03777) - 1024;
        long fractional = (words[0].getW() & 0_7777_7777L) << 36 | words[1].getW();
        long integral = 1;
        while (exponent > 0) {
            integral <<= 1;
            if ((fractional & 0x80000000_00000000L) != 0) {
                integral |= 01;
            }
            fractional <<= 1;
            --exponent;
        }
        while (exponent < 0) {
            fractional >>= 1;
            if (integral == 1) {
                fractional |= 0x80000000_00000000L;
                integral = 0;
            }
            ++exponent;
        }

        //  Convert the fractional part so that it can be easily displayed
        long conversion = 0;
        long multiplier = 5;
        while (fractional != 0) {
            if ((fractional & 0x80000000_00000000L) != 0) {
                conversion += multiplier;
            }
            multiplier *= 5;
            fractional <<= 1;
        }

        return String.format("%s%d.%d", neg ? "-" : "", integral, conversion);
    }

    /*
        32+4+2 = 38:      0010 0110 . 0000 0000
                               0010 . 0110       e04   2.375 x 2^4        .25
                                                       2.375 x 16 = 38
                                                                          .375
     */



    /**
     * Create a FloatingPointValue from an IntegerValue.
     * There is some trouble, as the integer value is 72bit signed, and the integral portion of a float is only 60 bits.
     * So... we set up the integer value is the fractional portion with the exponent set appropriately, then
     * shift left until we shift a 1 out of the fractional portion, adjusting the exponent appropriately.
     * Then we take the top 60 bits from the 72-bit fractional portion.
     */
    public static FloatingPointValue convertFromInteger(
        final IntegerValue integerValue
    ) {
        try {
            if (integerValue._value.isPositiveZero()) {
                return new FloatingPointValue.Builder().setValue(0)
                                                       .build();
            } else if (integerValue._value.isNegativeZero()) {
                return new FloatingPointValue.Builder().setValue(DoubleWord36.DW36_NEGATIVE_ZERO)
                                                       .build();
            } else {
                long signBit = 0;
                BigInteger fractional;

                if (integerValue._value.isPositive()) {
                    fractional = integerValue._value.get();
                } else {
                    signBit = 0_700000_000000L;
                    fractional = integerValue._value.negate().get();
                }

                int exponent = 72;
                while (!fractional.and(DoubleWord36.CARRY_BIT).equals(BigInteger.ZERO)) {
                    fractional = fractional.shiftLeft(1);
                    --exponent;
                }

                long loWord = fractional.shiftRight(12).longValue();
                long highWord = fractional.shiftRight(48).and(DoubleWord36.SHORT_BIT_MASK).longValue();
                highWord |= signBit;
                highWord |= ((exponent + 127) & 03777) << 24;
                return new FloatingPointValue(false, false, highWord, loWord, ValuePrecision.Default);
            }
        } catch(InvalidParameterException ex) {
            throw new RuntimeException("Caught " + ex);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        boolean _flagged = false;
        ValuePrecision _precision = ValuePrecision.Default;
        DoubleWord36 _value = null;

        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setPrecision(ValuePrecision value)           { _precision = value; return this; }
        public Builder setValue(DoubleWord36 value)                 { _value = value; return this; }
        public Builder setValue(Word36 value)                       { _value = new DoubleWord36(0, value.getW()); return this; }
        public Builder setValue(long value)                         { _value = new DoubleWord36(0, value); return this; }

        public FloatingPointValue build(
        ) throws InvalidParameterException {
            if (_value == null) {
                throw new InvalidParameterException("Value not specified for FloatingPointValue builder");
            }

            return new FloatingPointValue(_flagged, _value, _precision);
        }
    }
}
