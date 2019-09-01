/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.CharacteristOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristUnderflowException;
import java.math.BigInteger;

/**
 * This class captures all floating point manipulations.
 * The plan is to keep everything componetized internally,
 * ingesting various FP formats and emitting them as necessary.
 * It is invariant on purpose, so ... there's a bit of a performance hit.
 */
@SuppressWarnings("Duplicates")
public class FloatingPointComponents {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constants
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Things we use internally
    public static final int CHARACTERISTIC_BIAS = 1024;
    private static final int CHARACTERISTIC_BITS = 12;
    private static final int MANTISSA_BITS = 60;
    private static final long MANTISSA_MASK = (1L << MANTISSA_BITS) - 1;
    private static final long MANTISSA_LEFTMOST_BIT = 1L << (MANTISSA_BITS - 1);
    private static final BigInteger BI_CHARACTERISTIC_MASK = BigInteger.valueOf((1L << CHARACTERISTIC_BITS) - 1).shiftLeft(MANTISSA_BITS);
    private static final BigInteger BI_MANTISSA_MASK = BigInteger.valueOf(MANTISSA_MASK);
    public static final int HIGHEST_EXPONENT = 01777;
    public static final int LOWEST_EXPONENT = -02000;

    public static final FloatingPointComponents COMP_NEGATIVE_ZERO = new FloatingPointComponents(true, 0, 0, 0);
    public static final FloatingPointComponents COMP_POSITIVE_ZERO = new FloatingPointComponents(false, 0, 0, 0);

    //  External floating point format information
    public static final int W36_CHARACTERISTIC_BITS = 8;
    public static final int W36_EXPONENT_BIAS = 128;
    public static final int W36_MANTISSA_BITS = 27;
    public static final long W36_CHARACTERISTIC_MASK = ((1L << W36_CHARACTERISTIC_BITS) - 1) << W36_MANTISSA_BITS;
    public static final long W36_MANTISSA_MASK = (1L << W36_MANTISSA_BITS) - 1;
    public static final long W36_SIGN_BIT = 1L << (W36_CHARACTERISTIC_BITS + W36_MANTISSA_BITS);

    public static final long W36_NEGATIVE_ZERO = 0_777777_777777L;
    public static final long W36_POSITIVE_ZERO = 0L;

    public static final int DW36_CHARACTERISTIC_BITS = 11;
    public static final int DW36_EXPONENT_BIAS = 1024;
    public static final BigInteger DW36BI_EXPONENT_BIAS = BigInteger.valueOf(DW36_EXPONENT_BIAS);
    public static final int DW36_MANTISSA_BITS = 60;
    public static final BigInteger DW36_CHARACTERISTIC_MASK = BigInteger.valueOf((1L << DW36_CHARACTERISTIC_BITS) - 1).shiftLeft(DW36_MANTISSA_BITS);
    public static final BigInteger DW36_MANTISSA_MASK = BigInteger.valueOf((1L << DW36_MANTISSA_BITS) - 1);
    public static final BigInteger DW36_SIGN_BIT = BigInteger.valueOf(1L << (DW36_CHARACTERISTIC_BITS + DW36_MANTISSA_BITS));
    public static final BigInteger DW36_CHARACTERISTIC_MANTISSA_MASK = DW36_CHARACTERISTIC_MASK.or(DW36_MANTISSA_MASK);

    public static final BigInteger DW36_NEGATIVE_ZERO = BigInteger.valueOf(0_777777_777777L).shiftLeft(36).or(BigInteger.valueOf(0_777777_777777L));
    public static final BigInteger DW36_POSITIVE_ZERO = BigInteger.ZERO;

    public static final int IEEE754_SINGLE_CHARACTERISTIC_BITS = 8;
    public static final int IEEE754_SINGLE_EXPONENT_BIAS = 127;
    public static final int IEEE754_SINGLE_MANTISSA_BITS = 23;
    public static final int IEEE754_SINGLE_CHARACTERISTIC_MASK = (1 << (IEEE754_SINGLE_CHARACTERISTIC_BITS - 1)) << IEEE754_SINGLE_MANTISSA_BITS;
    public static final int IEEE754_SINGLE_MANTISSA_MASK = (1 << IEEE754_SINGLE_MANTISSA_BITS) - 1;
    public static final int IEEE754_SINGLE_SIGN_BIT = (1 << (IEEE754_SINGLE_CHARACTERISTIC_BITS + IEEE754_SINGLE_MANTISSA_BITS));

    public static final int IEEE754_SINGLE_NEGATIVE_ZERO = 0x8000_0000;
    public static final int IEEE754_SINGLE_POSITIVE_ZERO = 0;

    public static final int IEEE754_DOUBLE_CHARACTERISTIC_BITS = 11;
    public static final int IEEE754_DOUBLE_EXPONENT_BIAS = 1023;
    public static final int IEEE754_DOUBLE_MANTISSA_BITS = 52;
    public static final long IEEE754_DOUBLE_CHARACTERISTIC_MASK = (1L << IEEE754_DOUBLE_CHARACTERISTIC_BITS - 1) << IEEE754_DOUBLE_MANTISSA_BITS;
    public static final long IEEE754_DOUBLE_MANTISSA_MASK = (1L << IEEE754_DOUBLE_MANTISSA_BITS) - 1;
    public static final long IEEE754_DOUBLE_SIGN_BIT = (1L << (IEEE754_DOUBLE_CHARACTERISTIC_BITS + IEEE754_DOUBLE_MANTISSA_BITS));

    public static final long IEEE754_DOUBLE_NEGATIVE_ZERO = 0x8000_0000_0000_0000L;
    public static final long IEEE754_DOUBLE_POSITIVE_ZERO = 0L;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public final boolean _isNegative;
    public final int _exponent;
    public final long _integral;    //  integral portion (usually zero, esp if normalized) - absolute evalue
    public final long _mantissa;    //  fractional portion if _integral is zero - absolute value


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a not-necessarily-normalized FloatingPointComponents object from the various components.
     */
    public FloatingPointComponents(
        final boolean isNegative,
        final int exponent,
        final long integral,
        final long mantissa
    ) {
        _isNegative = isNegative;
        _exponent = exponent;
        _integral = integral;
        _mantissa = mantissa;
    }

    /**
     * Creates a not-necessarily-normalized FPC from a single signed 64-bit integer value, adjusting as necessary.
     * The value is interpreted as an integral, the fractional portion being assumed to be zero.
     */
    public FloatingPointComponents(
        final long value
    ) {
        _isNegative = value < 0;
        long absValue = _isNegative ? -value : value;

        if (absValue == 0) {
            _integral = 0;
            _mantissa = 0;
            _exponent = 0;
        } else {
            long integral = absValue;
            long fractional = 0;
            int exponent = 0;
            while (integral > 0) {
                fractional >>>= 1;
                if ((integral % 1) == 1) {
                    fractional |= MANTISSA_LEFTMOST_BIT;
                }
                integral >>>= 1;
                ++exponent;
            }

            _integral = integral;
            _mantissa = fractional;
            _exponent = exponent;
        }
    }

    /**
     * Creates a not-necessarily-normalized FPC from an IEEE 754 single-precision floating point value
     */
    public FloatingPointComponents(
        final float value
    ) {
        int bits = Float.floatToRawIntBits(value);
        _isNegative = (bits & IEEE754_SINGLE_SIGN_BIT) != 0;
        _exponent = ((bits & IEEE754_SINGLE_CHARACTERISTIC_MASK) >> IEEE754_SINGLE_MANTISSA_BITS) - IEEE754_SINGLE_EXPONENT_BIAS;
        _mantissa = (IEEE754_SINGLE_MANTISSA_MASK + 1) << (MANTISSA_BITS - IEEE754_SINGLE_MANTISSA_BITS);
        _integral = 0L;
    }

    /**
     * Creates a not-necessarily-normalized from an IEEE 754 double-precision floating point value
     */
    public FloatingPointComponents(
        final double value
    ) {
        long bits = Double.doubleToRawLongBits(value);
        _isNegative = (bits & IEEE754_DOUBLE_SIGN_BIT) != 0;
        _exponent = (int) ((bits & IEEE754_DOUBLE_CHARACTERISTIC_MASK) >> IEEE754_DOUBLE_MANTISSA_BITS) - IEEE754_DOUBLE_EXPONENT_BIAS;
        _mantissa = (IEEE754_DOUBLE_MANTISSA_MASK + 1) << (MANTISSA_BITS - IEEE754_DOUBLE_MANTISSA_BITS);
        _integral = 0L;
    }

    /**
     * Creates a not-necessarily-normalized from a 36-bit floating point value stored in a Word36 object
     */
    public FloatingPointComponents(
        final Word36 value
    ) {
        _isNegative = value.isNegative();
        long absValue = _isNegative ? value.negate().getW() : value.getW();

        if (absValue == 0) {
            _integral = 0;
            _mantissa = 0;
            _exponent = 0;
        } else {
            _integral = 0;
            _exponent = (int) ((absValue & W36_CHARACTERISTIC_MASK) >> W36_MANTISSA_BITS) - W36_EXPONENT_BIAS;
            _mantissa = absValue & W36_MANTISSA_MASK;
        }
    }

    /**
     * Creates a not-necessarily-normalized from a 72-bit floating point value stored in a DoubleWord36 object
     */
    public FloatingPointComponents(
        final DoubleWord36 value
    ) {
        _isNegative = value.isNegative();
        BigInteger absValue = value.get();
        if (_isNegative) {
            absValue = absValue.xor(DoubleWord36.BIT_MASK);
        }

        if (absValue.equals(DW36_POSITIVE_ZERO)) {
            _integral = 0;
            _mantissa = 0;
            _exponent = 0;
        } else {
            _integral = 0;
            _exponent = absValue.and(DW36_CHARACTERISTIC_MASK).shiftRight(DW36_MANTISSA_BITS).subtract(DW36BI_EXPONENT_BIAS).intValue();
            _mantissa = absValue.and(DW36_MANTISSA_MASK).longValue();
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Overrides
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof FloatingPointComponents) {
            FloatingPointComponents fpc = (FloatingPointComponents) obj;
            return (_isNegative == fpc._isNegative)
                   && (_exponent == fpc._exponent)
                   && (_integral == fpc._integral)
                   && (_mantissa == fpc._mantissa);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) (_exponent ^ _integral ^ _mantissa);
    }

    @Override
    public String toString() {
        return String.format("%s%o.%020o E%d",
                             _isNegative ? "-" : "",
                             _integral,
                             _mantissa,
                             _exponent);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Emitters
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Emits a normalized IEEE 754 double-precision floating point value corresponding to the attributes of this object.
     * Format is Sign(1) | Exponent(12) | Mantissa(51)
     *  Sign:       0x0 number is positive, 0x1 number is negative
     *  Exponent:   Absolute value of biased exponent
     *  Mantissa:   Absolute value of the fractional portion of the value, with the significant bit dropped.
     * Positive zero format is 0x0000_0000_0000_0000
     * Negative zero format is 0x8000_0000_0000_0000
     * The most-significant bit is elided, as it can always be deduced from the remaining information.
     * This provides one extra bit of precision.
     */
    public double toDouble(
    ) throws CharacteristOverflowException,
            CharacteristUnderflowException {
        long rawBits;
        FloatingPointComponents normalized = normalize();

        if ((_integral == 0) && (_mantissa == 0)) {
            rawBits = _isNegative ? IEEE754_DOUBLE_NEGATIVE_ZERO : IEEE754_DOUBLE_POSITIVE_ZERO;
        } else {
            if (normalized._exponent < Double.MIN_EXPONENT) {
                throw new CharacteristUnderflowException();
            } else if (normalized._exponent > Double.MAX_EXPONENT) {
                throw new CharacteristOverflowException();
            }

            //  Our internal mantissa is always bigger than the IEEE mantissa.  Down-shift it to fit.
            //  Don't forget to eliminate the MSB, which is always implicit in IEEE.
            long resultMantissa = normalized._mantissa >> (MANTISSA_BITS - IEEE754_DOUBLE_MANTISSA_BITS - 1);
            resultMantissa &= IEEE754_DOUBLE_MANTISSA_MASK;
            long resultCharacteristic = ((long)normalized._exponent + IEEE754_DOUBLE_EXPONENT_BIAS - 1) << IEEE754_DOUBLE_MANTISSA_BITS;
            long resultSign = _isNegative ? IEEE754_DOUBLE_SIGN_BIT : 0;
            rawBits = resultSign | resultCharacteristic | resultMantissa;
        }

        return Double.longBitsToDouble(rawBits);
    }

    /**
     * Emits an IEEE 754 single-precision floating point value corresponding to the attributes of this object.
     * Format is Sign(1) | Exponent(8) | Mantissa(23)
     *  Sign:       0x0 number is positive, 0x1 number is negative
     *  Exponent:   Absolute value of biased exponent
     *  Mantissa:   Absolute value of the fractional portion of the value, with the significant bit dropped.
     * Positive zero format is 0x0000_0000
     * Negative zero format is 0x8000_0000
     * The most-significant bit is elided, as it can always be deduced from the remaining information.
     * This provides one extra bit of precision.
     */
    public float toFloat(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        int rawBits;
        FloatingPointComponents normalized = normalize();

        if ((_integral == 0) && (_mantissa == 0)) {
            rawBits = _isNegative ? IEEE754_SINGLE_NEGATIVE_ZERO : IEEE754_SINGLE_POSITIVE_ZERO;
        } else {
            if (normalized._exponent < Float.MIN_EXPONENT) {
                throw new CharacteristUnderflowException();
            } else if (normalized._exponent > Float.MAX_EXPONENT) {
                throw new CharacteristOverflowException();
            }

            //  Our internal mantissa is always bigger than the IEEE mantissa.  Down-shift it to fit.
            //  Don't forget to eliminate the MSB, which is always implicit in IEEE.
            int resultMantissa = (int)(normalized._mantissa >> (MANTISSA_BITS - IEEE754_SINGLE_MANTISSA_BITS - 1));
            resultMantissa &= IEEE754_SINGLE_MANTISSA_MASK;
            int resultCharacteristic = (normalized._exponent + IEEE754_SINGLE_EXPONENT_BIAS - 1) << IEEE754_SINGLE_MANTISSA_BITS;
            int resultSign = _isNegative ? IEEE754_SINGLE_SIGN_BIT : 0;
            rawBits = resultSign | resultCharacteristic | resultMantissa;
        }

        return Float.intBitsToFloat(rawBits);
    }

    /**
     * Emits a 72-bit DoubleWord36 containing a canonically-formatted representation of this object.
     * Format is Sign(1) | Exponent(11) | Mantissa(60)
     *  Sign:       0x0 number is positive, 0x1 number is negative
     *  Exponent:   Value of biased exponent, logically inverted for negative numbers
     *  Mantissa:   Value of the fractional portion, logically inverted for negative numbers
     * Positive zero format is 0_000000_000000_000000_000000
     * Negative zero format is 0_777777_777777_777777_777777
     */
    public DoubleWord36 toDoubleWord36(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger rawBits;
        FloatingPointComponents normalized = normalize();

        if ((_integral == 0) && (_mantissa == 0)) {
            rawBits = _isNegative ? DW36_NEGATIVE_ZERO : DW36_NEGATIVE_ZERO;
        } else {
            if (normalized._exponent < Float.MIN_EXPONENT) {
                throw new CharacteristUnderflowException();
            } else if (normalized._exponent > Float.MAX_EXPONENT) {
                throw new CharacteristOverflowException();
            }

            rawBits = BigInteger.valueOf(normalized._exponent).add(DW36BI_EXPONENT_BIAS).shiftLeft(DW36_MANTISSA_BITS);
            rawBits = rawBits.or(BigInteger.valueOf(normalized._mantissa).and(DW36_MANTISSA_MASK));
            if (normalized._isNegative) {
                rawBits = rawBits.xor(DW36_CHARACTERISTIC_MANTISSA_MASK);
            }
        }

        return new DoubleWord36(rawBits);
    }

    /**
     * Emits a 36-bit Word36 containing a canonically-formatted representation of this object.
     * Format is Sign(1) | Exponent(8) | Mantissa(27)
     *  Sign:       0x0 number is positive, 0x1 number is negative
     *  Exponent:   Value of biased exponent, logically inverted for negative numbers
     *  Mantissa:   Value of the fractional portion, logically inverted for negative numbers
     * Positive zero format is 0_000000_000000
     * Negative zero format is 0_777777_777777
     */
    public Word36 toWord36(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        long rawBits;
        FloatingPointComponents normalized = normalize();

        if ((_integral == 0) && (_mantissa == 0)) {
            rawBits = _isNegative ? W36_NEGATIVE_ZERO : W36_NEGATIVE_ZERO;
        } else {
            if (normalized._exponent < Float.MIN_EXPONENT) {
                throw new CharacteristUnderflowException();
            } else if (normalized._exponent > Float.MAX_EXPONENT) {
                throw new CharacteristOverflowException();
            }

            rawBits = (normalized._exponent + W36_EXPONENT_BIAS) << W36_MANTISSA_BITS;
            rawBits |= normalized._mantissa & W36_MANTISSA_MASK;
            if (normalized._isNegative) {
                rawBits ^= W36_CHARACTERISTIC_MASK | W36_MANTISSA_MASK;
            }
        }

        return new Word36(rawBits);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Arithmetic
    //  ----------------------------------------------------------------------------------------------------------------------------

    public FloatingPointComponents add(
        final FloatingPointComponents addend
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        //  Normalize the addends, Check for zeros and then shift the addend with the smaller exponent such that the exponents match.
        FloatingPointComponents normal1 = normalize();
        FloatingPointComponents normal2 = addend.normalize();
        if (normal1._mantissa == 0) {
            return normal2;
        } else if (normal2._mantissa == 0) {
            return normal1;
        }

        if (normal1._exponent < normal2._exponent) {
            int diff = normal2._exponent - normal1._exponent;
            normal1 = new FloatingPointComponents(normal1._isNegative,
                                                  normal1._exponent + diff,
                                                  0L,
                                                  normal1._mantissa);
        } else if (normal2._exponent < normal1._exponent) {
            int diff = normal1._exponent - normal2._exponent;
            normal2 = new FloatingPointComponents(normal2._isNegative,
                                                  normal2._exponent + diff,
                                                  0L,
                                                  normal2._mantissa);
        }

        //  Are we going to be adding (addend signs are equal) or subtracting (signs are unequal)?
        //  Is the result going to be negative?
        boolean adding = _isNegative == addend._isNegative;
        boolean resultNegative = (_isNegative && addend._isNegative)
            || (_isNegative && (normal1._mantissa > normal2._mantissa))
            || (addend._isNegative && (normal2._mantissa > normal1._mantissa));

        //  Now we can add (or subtract) the mantissas.  Check whether we've overflowed the mantissa and adjust accordingly.
        long resultMantissa = adding ? normal1._mantissa + normal2._mantissa : Math.abs(normal1._mantissa - normal2._mantissa);
        int resultExponent = normal1._exponent;
        if (adding & resultMantissa > MANTISSA_MASK) {
            resultMantissa >>= 1;
            ++resultExponent;
        }

        //  Done.
        return new FloatingPointComponents(resultNegative, resultExponent, 0L, resultMantissa);
    }

    /**
     * Compares this object to another, taking into account this and that and trying to do the right thing.
     * @param operand value we compare against
     * @param precision fractional precision in bits (anything over 59 is pointless, but accepted)
     * @return 0 if the objects match,
     *          -1 if this object is less than the operand,
     *          1 if this object is greater than the operand
     */
    public int compare(
        final FloatingPointComponents operand,
        final int precision
    ) {
        return 0;//TODO
    }

    /**
     * Divides this value by a divisor
     */
    public FloatingPointComponents divide(
        final FloatingPointComponents divisor
    ) {
        return null;//TODO
    }

    /**
     * Takes the multiplicative inverse
     */
    public FloatingPointComponents invert(
        final FloatingPointComponents divisor
    ) {
        return null;//TODO
    }

    public boolean isNegativeZero() {
        return _isNegative && (_mantissa == 0) && (_integral == 0);
    }

    public boolean isPositiveZero() {
        return !_isNegative && (_mantissa == 0) && (_integral == 0);
    }

    public boolean isZero() {
        return (_mantissa == 0) && (_integral == 0);
    }

    /**
     * Multiplies this value by a factor
     */
    public FloatingPointComponents multiply(
        final FloatingPointComponents factor
    ) {
        return null;//TODO
    }

    /**
     * Takes the additive inverse
     */
    public FloatingPointComponents negate() {
        return new FloatingPointComponents(!_isNegative, _exponent, _integral, _mantissa);
    }

    /**
     * Normalizes the value represented by the component portions by shifting integral and/or mantissa as needed,
     * and incrementing or decrementing the exponent accordingly.
     */
    public FloatingPointComponents normalize(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        if (isZero()) {
            return this;
        }

        long tempIntegral = _integral;
        long tempMantissa = _mantissa;
        int tempExponent = _exponent;

        if (tempIntegral > 0) {
            do {
                tempMantissa >>= 1;
                if ((tempIntegral & 0x01) == 0x01) {
                    tempMantissa |= MANTISSA_LEFTMOST_BIT;
                }
                tempIntegral >>= 1;
                ++tempExponent;
            } while (tempIntegral > 0);
        } else {
            while ((tempMantissa & MANTISSA_LEFTMOST_BIT) == 0) {
                tempMantissa <<= 1;
                --tempExponent;
            }
        }

        checkExponent(tempExponent);
        return new FloatingPointComponents(_isNegative, tempExponent, tempIntegral, tempMantissa);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private Methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected static void checkExponent(
        final int exponent
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        if (exponent < LOWEST_EXPONENT) {
            throw new CharacteristUnderflowException();
        } else if (exponent > HIGHEST_EXPONENT) {
            throw new CharacteristOverflowException();
        }
    }
}
