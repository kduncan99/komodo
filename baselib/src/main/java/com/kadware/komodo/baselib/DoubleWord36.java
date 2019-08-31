/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.CharacteristOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristUnderflowException;

import java.math.BigInteger;

/**
 * Library for doing architecturally-correct 72-bit operations on integers
 *
 *  Floating Point notes:
 *      When a floating-point number is represented in a BigInteger, it is made up of the following fields:
 *          sign bit - if set, the number is negative, and the next two fields are logically inverted
 *          Bits1-11:   characteristic - the exponent added to the characteristic bias
 *          Bits12-71:  mantissa - left-justified, zero-filled.
 *                      The most significant bit in the fractional portion is placed in bit 12.
 *      When we talk about the biased exponent, we refer to it as the characteristic, which includes the sign bit.
 *      In all cases where we deal with the component values, they are magnitudes and we preserve a separate flag
 *      to indicate whether the values should be arithmetically inverted (i.e., they are negative).
 *      Component exponents are non-biased, and they are right-justified in an int (usually).
 *      Component mantissas are left-justified in a long, taking up all 64-bits.
 */
@SuppressWarnings("Duplicates")
public class DoubleWord36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class AddFloatingPointResult {
        public boolean _overFlow;
        public boolean _underFlow;
        public DoubleWord36 _value;

        public AddFloatingPointResult(
            final StaticAddFloatingPointResult sfpar
        ) {
            _overFlow = sfpar._overFlow;
            _underFlow = sfpar._underFlow;
            _value = new DoubleWord36(sfpar._value);
        }
    }

    public static class AdditionResult {
        public final boolean _carry;
        public final boolean _overflow;
        public final DoubleWord36 _value;

        public AdditionResult(
            final StaticAdditionResult sar
        ) {
            _carry = sar._carry;
            _overflow = sar._overflow;
            _value = new DoubleWord36(sar._value);
        }
    }

    public static class Components {
        public final boolean _isNegative;
        public final int _exponent;         //  unbiased
        public final long _mantissa;        //  LJSF in 64-bits

        public Components(
            final boolean isNegative,
            final int exponent,
            final long mantissa
        ) {
            _isNegative = isNegative;
            _exponent = exponent;
            _mantissa = mantissa;
        }
    }

    public static class DivisionResult {
        public final DoubleWord36 _result;
        public final DoubleWord36 _remainder;

        public DivisionResult(
            final StaticDivisionResult sdr
        ) {
            _result = new DoubleWord36(sdr._result);
            _remainder = new DoubleWord36(sdr._remainder);
        }
    }

    public static class MultiplicationResult {
        public final boolean _overflow;
        public final DoubleWord36 _value;

        public MultiplicationResult(
            final StaticMultiplicationResult smr
        ) {
            _overflow = smr._overflow;
            _value = new DoubleWord36(smr._value);
        }
    }

    public static class MultiplyFloatingPointResult {
        public final boolean _overflow;
        public final boolean _underflow;
        public final DoubleWord36 _value;

        public MultiplyFloatingPointResult(
            final StaticMultiplyFloatingPointResult smfpr
        ) {
            _overflow = smfpr._overflow;
            _underflow = smfpr._underflow;
            _value = new DoubleWord36(smfpr._value);
        }
    }

    public static class StaticAddFloatingPointResult {
        public boolean _overFlow;
        public boolean _underFlow;
        public final BigInteger _value;

        public StaticAddFloatingPointResult(
            final boolean overFlow,
            final boolean underFlow,
            final BigInteger value
        ) {
            _overFlow = overFlow;
            _underFlow = underFlow;
            _value = value;
        }
    }

    public static class StaticAdditionResult {
        public final boolean _carry;
        public final boolean _overflow;
        public final BigInteger _value;

        public StaticAdditionResult(
            final boolean carry,
            final boolean overflow,
            final BigInteger value
        ) {
            _carry = carry;
            _overflow = overflow;
            _value = value;
        }
    }

    public static class StaticDivisionResult {
        public final BigInteger _result;
        public final BigInteger _remainder;

        public StaticDivisionResult(
            final BigInteger result,
            final BigInteger remainder
        ) {
            _result = result;
            _remainder = remainder;
        }
    }

    public static class StaticMultiplicationResult {
        public final boolean _overflow;
        public final BigInteger _value;

        public StaticMultiplicationResult(
            final boolean overflow,
            final BigInteger value
        ) {
            _overflow = overflow;
            _value = value;
        }
    }

    public static class StaticMultiplyFloatingPointResult {
        public final boolean _overflow;
        public final boolean _underflow;
        public final BigInteger _value;

        public StaticMultiplyFloatingPointResult(
            final boolean overflow,
            final boolean underflow,
            final BigInteger value
        ) {
            _overflow = overflow;
            _underflow = underflow;
            _value = value;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constants
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final BigInteger CARRY_BIT        = BigInteger.valueOf(1).shiftLeft(72);
    public static final BigInteger SHORT_BIT_MASK   = BigInteger.valueOf(Word36.BIT_MASK);
    public static final BigInteger BIT_MASK         = SHORT_BIT_MASK.shiftLeft(36).or(SHORT_BIT_MASK);
    public static final BigInteger NOT_BIT_MASK     = BIT_MASK.not();

    public static final BigInteger NEGATIVE_BIT     = BigInteger.ONE.shiftLeft(71);
    public static final BigInteger NEGATIVE_ONE     = BIT_MASK.add(BigInteger.ONE.negate());
    public static final BigInteger NEGATIVE_ZERO    = BIT_MASK;
    public static final BigInteger POSITIVE_ONE     = BigInteger.ONE;
    public static final BigInteger POSITIVE_ZERO    = BigInteger.ZERO;

    public static final DoubleWord36 DW36_NEGATIVE_ONE              = new DoubleWord36(NEGATIVE_ONE);
    public static final DoubleWord36 DW36_NEGATIVE_ZERO             = new DoubleWord36(NEGATIVE_ZERO);  //  works for floating point as well
    public static final DoubleWord36 DW36_NEGATIVE_ZERO_FLOATING    = DW36_NEGATIVE_ZERO;
    public static final DoubleWord36 DW36_POSITIVE_ONE              = new DoubleWord36(POSITIVE_ONE);
    public static final DoubleWord36 DW36_POSITIVE_ZERO             = new DoubleWord36(POSITIVE_ZERO);  //  works for floating point as well
    public static final DoubleWord36 DW36_POSITIVE_ZERO_FLOATING    = DW36_POSITIVE_ZERO;

    public static final int CHARACTERISTIC_BIAS = 1024;
    private static final int CHARACTERISTIC_BITS = 12;
    private static final int LONG_MANTISSA_BITS = 64;
    private static final long LONG_MANTISSA_MASK = 0xFFFF_FFFF_FFFF_FFFFL;
    private static final long LONG_MANTISSA_LEFTMOST_BIT = 0x8000_0000_0000_0000L;
    private static final int BI_MANTISSA_BITS = 60;
    private static final BigInteger BI_MANTISSA_LEFTMOST_BIT = BigInteger.valueOf(0_4000_0000_0000_0000_0000L);
    private static final BigInteger BI_MANTISSA_MASK = BigInteger.valueOf(0_7777_7777_7777_7777_7777L);
    private static final BigInteger BI_CHARACTERISTIC_MASK = BigInteger.valueOf((1L << CHARACTERISTIC_BITS) - 1).shiftLeft(BI_MANTISSA_BITS);
    public static final int HIGHEST_EXPONENT = 01777;
    public static final int LOWEST_EXPONENT = -02000;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data items (not much here)
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected final BigInteger _value;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public DoubleWord36()                   { _value = BigInteger.ZERO; }
    public DoubleWord36(long value)         { _value = BigInteger.valueOf(value); }
    public DoubleWord36(BigInteger value)   { _value = value; }
    public DoubleWord36(DoubleWord36 value) { _value = value._value; }

    public DoubleWord36(
        final double value
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        _value = fromDouble(value);
    }

    public DoubleWord36(
        final float value
    ) {
        try {
            _value = fromFloat(value);
        } catch (CharacteristOverflowException | CharacteristUnderflowException ex) {
            throw new RuntimeException("Impossible error");
        }
    }


    /**
     * Constructs a 72-bit integer given the component 36-bit integers provided in the high and low parameters
     * @param high Most significant 36 bits wrapped in a long
     * @param low Least significant 36 bits wrapped in a long
     */
    public DoubleWord36(
        final long high,
        final long low
    ) {
        _value = BigInteger.valueOf(high & 0_777777_777777L).shiftLeft(36).or(BigInteger.valueOf(low & 0_777777_777777L));
    }

    /**
     * Constructs a DW36 object representing a normalized floating point number using the component portions thereof
     * @param mantissa The fractional portion, right shifted in 60-bit field (NOT 64-BIT FIELD)
     * @param exponent The unbiased signed exponent
     * @param negative true if the presented value should be arithmetically inverted (i.e., is negative)
     */
    public DoubleWord36(
        final long mantissa,
        final int exponent,
        final boolean negative
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        _value = floatingPointFromComponents(mantissa, exponent, negative);
    }

    /**
     * Constructs a DW36 object representing a normalized floating point number using the component portions thereof
     * @param integral The 64-bit magnitude of the integral portion (right-justified, of course)
     * @param fractional The 64-bit magnitude of the fractional portion, left-justified
     *                   i.e., 10100...00 (for 60 bits) indicates decimal 0.5 + 0.125 == 0.625
     * @param exponent Initial exponent (probably zero, only here to help disambiguate this c'tor from the previous)
     * @param negative true if the presented value should be arithmetically inverted (i.e., is negative)
     */
    public DoubleWord36(
        final long integral,
        final long fractional,
        final int exponent,
        final boolean negative
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        if ((integral == 0) && (fractional == 0)) {
            _value = BigInteger.ZERO;
        } else {
            long[] normalized = normalizeComponents(integral, fractional, exponent);
            long normalizedMantissa = normalized[0] >>> 4;
            long normalizedExponent = normalized[1];
            checkExponent(normalizedExponent);

            int characteristic = (int) normalizedExponent + CHARACTERISTIC_BIAS;
            BigInteger value = BigInteger.valueOf(characteristic).shiftLeft(BI_MANTISSA_BITS);
            value = value.or(BigInteger.valueOf(normalizedMantissa));
            if (negative) {
                value = negate(value);
            }
            _value = value;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Overrides
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof DoubleWord36) {
            DoubleWord36 dw = (DoubleWord36) obj;
            return dw._value.equals(_value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() { return _value.hashCode(); }

    @Override
    public String toString() {
        return String.format("%024o", _value);
    }


    //  Getter(s) ------------------------------------------------------------------------------------------------------------------

    public BigInteger get() { return _value; }

    public Components getComponents() {
        return new Components(isNegative(),
                              getAbsoluteCharacteristic(_value) - CHARACTERISTIC_BIAS,
                              getAbsoluteMantissa(_value));
    }

    public Word36[] getWords() { return getWords(_value); }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    public boolean isNegative()                 { return isNegative(_value); }
    public boolean isNegativeZero()             { return isNegativeZero(_value); }
    public boolean isPositive()                 { return isPositive(_value); }
    public boolean isPositiveZero()             { return isPositiveZero(_value); }
    public boolean isZero()                     { return isZero(_value); }

    //  Floating point +/- is formatted exactly the same as integer +/- zero
    public boolean isNegativeFloatingPoint()     { return isNegative(_value); }
    public boolean isNegativeZeroFloatingPoint() { return isNegativeZero(_value); }
    public boolean isPositiveFloatingPoint()     { return isPositive(_value); }
    public boolean isPositiveZeroFloatingPoint() { return isPositiveZero(_value); }
    public boolean isZeroFloatingPoint()         { return isZero(_value); }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    public AdditionResult add(DoubleWord36 addend)              { return new AdditionResult(add(_value, addend._value)); }
    public AddFloatingPointResult addFloatingPoint(DoubleWord36 addend)
        { return new AddFloatingPointResult(addFloatingPoint(_value, addend._value)); }
    public int compareFloatingPoint(DoubleWord36 operand)       { return compareFloatingPoint(_value, operand._value); }
    public int compareTo(DoubleWord36 operand)                  { return compare(_value, operand._value); }
    public DivisionResult divide(DoubleWord36 divisor)          { return new DivisionResult(divide(_value, divisor._value)); }
    public DoubleWord36 divideFloatingPoint(DoubleWord36 divisor)
        { return new DoubleWord36(divideFloatingPoint(_value, divisor._value)); }
    public DoubleWord36 extendSign(int fieldSize)               { return new DoubleWord36(extendSign(_value, fieldSize)); }
    public MultiplicationResult multiply(DoubleWord36 factor)   { return new MultiplicationResult(multiply(_value, factor._value)); }
    public MultiplyFloatingPointResult multiplyFloatingPoint(DoubleWord36 factor)
        { return new MultiplyFloatingPointResult(multiplyFloatingPoint(_value, factor._value)); }
    public DoubleWord36 negate()                                { return new DoubleWord36(negate(_value)); }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    public DoubleWord36 logicalAnd(DoubleWord36 operand)    { return new DoubleWord36(logicalAnd(_value, operand._value)); }
    public DoubleWord36 logicalNot()                        { return new DoubleWord36(logicalNot(_value)); }
    public DoubleWord36 logicalOr(DoubleWord36 operand)     { return new DoubleWord36(logicalOr(_value, operand._value)); }
    public DoubleWord36 logicalXor(DoubleWord36 operand)    { return new DoubleWord36(logicalXor(_value, operand._value)); }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

    public DoubleWord36 leftShiftAlgebraic(int count)   { return new DoubleWord36(leftShiftAlgebraic(_value, count)); }
    public DoubleWord36 leftShiftCircular(int count)    { return new DoubleWord36(leftShiftCircular(_value, count)); }
    public DoubleWord36 leftShiftLogical(int count)     { return new DoubleWord36(leftShiftLogical(_value, count)); }
    public DoubleWord36 rightShiftAlgebraic(int count)  { return new DoubleWord36(rightShiftAlgebraic(_value, count)); }
    public DoubleWord36 rightShiftCircular(int count)   { return new DoubleWord36(rightShiftCircular(_value, count)); }
    public DoubleWord36 rightShiftLogical(int count)    { return new DoubleWord36(rightShiftLogical(_value, count)); }


    //  Conversions ----------------------------------------------------------------------------------------------------------------

    public BigInteger getTwosComplement()   { return getTwosComplement(_value); }

    public DoubleWord36 normalize(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        return new DoubleWord36(normalize(_value));
    }

    public double toDouble(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        return toDouble(_value);
    }

    public float toFloat(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        return toFloat(_value);
    }

    public String toOctal()                 { return toOctal(_value); }
    public String toStringFromASCII()       { return toStringFromASCII(_value); }
    public String toStringFromFieldata()    { return toStringFromFieldata(_value); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods - these operate on and return long integers representing ones-complement values
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static Components getComponents(
        final BigInteger value
    ) {
        return new Components(isNegative(value),
                              getAbsoluteCharacteristic(value) - CHARACTERISTIC_BIAS,
                              getAbsoluteMantissa(value));
    }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    /**
     * Tests 72-bit ones-complement value to see if it is negative
     */
    public static boolean isNegative(BigInteger value)      { return value.and(NEGATIVE_BIT).equals(NEGATIVE_BIT); }
    public static boolean isNegativeZero(BigInteger value)  { return value.equals(NEGATIVE_ZERO); }
    public static boolean isPositive(BigInteger value)      { return !isNegative(value); }
    public static boolean isPositiveZero(BigInteger value)  { return value.equals(POSITIVE_ZERO); }
    public static boolean isZero(BigInteger value)          { return isPositiveZero(value) || isNegativeZero(value); }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    /**
     * Adds two BigInteger operands presuming they represent a 72-bit signed integer value
     */
    public static StaticAdditionResult add(
        final BigInteger addend1,
        final BigInteger addend2
    ) {
        BigInteger result = addSimple(addend1, addend2);
        if (!result.and(CARRY_BIT).equals(BigInteger.ZERO)) {
            result = result.and(BIT_MASK).add(BigInteger.ONE);
        }

        boolean neg1 = isNegative(addend1);
        boolean neg2 = isNegative(addend2);
        boolean negRes = isNegative(result);

        boolean carry = result.compareTo(BigInteger.ZERO) < 0 ? (neg1 && neg2) : (neg1 || neg2);
        boolean overflow = (neg1 == neg2) && (neg1 != negRes);
        return new StaticAdditionResult(carry, overflow, result);
    }

    /**
     * Adds two BigInteger operands on the presumption they both represent 72-bit floating point values.
     * We do NOT throw over/underflow exceptions, as the caller may wish to ignore them, or to observe them
     * while still utilizing the resulting value.
     * Unfortunately, we cannot use the system double arithmetic, as the ranges for 72-bit exponents and
     * mantissas are larger than for IEE754.
     */
    public static StaticAddFloatingPointResult addFloatingPoint(
        final BigInteger addend1,
        final BigInteger addend2
    ) {
        //  Get the component parts of the addends
        int exponent1 = getAbsoluteCharacteristic(addend1) - CHARACTERISTIC_BIAS;
        long mantissa1 = getAbsoluteMantissa(addend1);

        int exponent2 = getAbsoluteCharacteristic(addend2) - CHARACTERISTIC_BIAS;
        long mantissa2 = getAbsoluteMantissa(addend2);

        //  Shift the value with the smaller exponent to the right until the exponents match
        boolean resultNeg;
        if (exponent1 < exponent2) {
            mantissa1 >>>= (exponent2 - exponent1);
            exponent1 = exponent2;
        } else if (exponent2 < exponent1) {
            mantissa2 >>>= exponent1 - exponent2;
            exponent2 = exponent1;
        }

        //  Convert the 64-bit mantissas to 60-bit so we can add them properly
        long mantissa1_60 = mantissa1 >>> 4;
        long mantissa2_60 = mantissa2 >>> 4;

        //  Now the mantissas are directly comparable... check the signs and decide whehter to add or subtract,
        //  and whether the result is going to be negative or positive.
        boolean isNeg1 = isNegative(addend1);
        boolean isNeg2 = isNegative(addend2);
        boolean resultNegative = (isNeg1 && isNeg2)
                                  || (isNeg1 && (mantissa1_60 > mantissa2_60))
                                  || (isNeg2 && (mantissa1_60 < mantissa2_60));
        boolean addOperation = isNeg1 == isNeg2;

        //  Add or subtract the mantissas depending upon whether the operand signs match.
        //  If the result is zero, we return the positive zero result.
        //  Otherwise, we construct a new floating point value, normalize it, and return it.
        long resultMantissa_60 = Math.abs(addOperation ? mantissa1_60 + mantissa2_60 : mantissa1_60 - mantissa2_60);
        if (resultMantissa_60 == 0) {
            return new StaticAddFloatingPointResult(false, false, POSITIVE_ZERO);
        }

        long[] normalized = normalizeComponents(0, resultMantissa_60 << 4, exponent1);
        long normalMantissa = normalized[0];
        int normalExponent = (int) normalized[1];

        boolean underflow = false;
        boolean overflow = false;
        if (normalExponent < LOWEST_EXPONENT) {
            underflow = true;
            normalExponent = LOWEST_EXPONENT;
        } else if (normalExponent > HIGHEST_EXPONENT) {
            overflow = true;
            normalExponent = HIGHEST_EXPONENT;
        }

        try {
            BigInteger value = floatingPointFromComponents(normalMantissa, normalExponent, resultNegative);
            return new StaticAddFloatingPointResult(overflow, underflow, value);
        } catch (CharacteristOverflowException | CharacteristUnderflowException ex) {
            throw new RuntimeException("Impossible condition:" + ex.getMessage());
        }
    }

    /**
     * Simple addition of 72-bit signed integers for the case where we don't care about carry or overflow
     */
    public static BigInteger addSimple(
        final BigInteger addend1,
        final BigInteger addend2
    ) {
        if (addend1.equals(NEGATIVE_ZERO) && addend2.equals(NEGATIVE_ZERO)) {
            return NEGATIVE_ZERO;
        }

        BigInteger native1 = getTwosComplement(addend1);
        BigInteger native2 = getTwosComplement(addend2);
        return getOnesComplement(native1.add(native2));
    }

    /**
     * Compares two values
     * Returns -1 if operand1 < operand2,
     *          1 if operand1 > operand2,
     *          0 if they are equal
     */
    public static int compare(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        return operand1.compareTo(operand2);
    }

    /**
     * Compares two DoubleWord36 objects in the context of floating point operations.
     * @return -1 if operand1 is less than operand2
     *          1 if operand1 is greater than operand2
     *          0 if the operands are equal
     */
    public static int compareFloatingPoint(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        //  Are they both equal to zero (or negative zero)?
        if ( (operand1.equals(POSITIVE_ZERO) || operand1.equals(NEGATIVE_ZERO))
              && (operand2.equals(POSITIVE_ZERO) || operand2.equals(NEGATIVE_ZERO)) ) {
            return 0;
        }

        //  If the signs are unequal, the positive value is greater.
        boolean neg1 = isNegative(operand1);
        boolean neg2 = isNegative(operand2);
        if (neg1 != neg2) {
            return neg1 ? -1 : 1;
        }

        //  Signs are equal.
        //  Align the values so that the exponents match (make the smaller exponent larger), then compare the mantissas.
        int exp1 = getAbsoluteCharacteristic(operand1) - CHARACTERISTIC_BIAS;
        int exp2 = getAbsoluteCharacteristic(operand2) - CHARACTERISTIC_BIAS;
        long absMantissa1 = getAbsoluteMantissa(operand1);
        long absMantissa2 = getAbsoluteMantissa(operand2);

        if (exp1 < exp2) {
            absMantissa1 >>>= (exp2 - exp1);
            exp1 = exp2;
        } else if (exp2 < exp1) {
            absMantissa2 >>>= (exp1 - exp2);
            exp2 = exp1;
        }

        long effectiveMantissa1 = absMantissa1 * (neg1 ? -1 : 1);
        long effectiveMantissa2 = absMantissa2 * (neg2 ? -1 : 1);

        if (effectiveMantissa1 > effectiveMantissa2) {
            return 1;
        } else if (effectiveMantissa1 < effectiveMantissa2) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Divide two 72-bit integer values
     */
    public static StaticDivisionResult divide(
        final BigInteger dividend,
        final BigInteger divisor
    ) {
        BigInteger[] tempResults = getTwosComplement(dividend).divideAndRemainder(getTwosComplement(divisor));
        return new StaticDivisionResult(getOnesComplement(tempResults[0]), getOnesComplement(tempResults[1]));
    }

    public static BigInteger divideFloatingPoint(
        final BigInteger dividend,
        final BigInteger divisor
    ) {
        return null;//TODO
    }

    /**
     * Extends the sign of an arbitrarily-sized value, to the full 72 bits
     */
    public static BigInteger extendSign(
        final BigInteger operand,
        final int fieldSize
    ) {
        BigInteger signBitMask = BigInteger.ONE.shiftLeft(fieldSize - 1);
        if (operand.and(signBitMask).equals(BigInteger.ZERO)) {
            return operand;
        } else {
            BigInteger fieldBitMask = BigInteger.ONE.shiftLeft(fieldSize).subtract(BigInteger.ONE);
            BigInteger fieldNotMask = BIT_MASK.xor(fieldBitMask);
            return operand.or(fieldNotMask);
        }
    }

    /**
     * Arithmetic multiplication - operands and result are ones-complement
     */
    public static StaticMultiplicationResult multiply(
        final BigInteger factor1,
        final BigInteger factor2
    ) {
        BigInteger tcResult = getTwosComplement(factor1).multiply(getTwosComplement(factor2));
        boolean overflow;
        if (tcResult.compareTo(BigInteger.ZERO) < 0) {
            //  negative result, should have leading 1's
            overflow = !tcResult.and(NOT_BIT_MASK).equals(NOT_BIT_MASK);
        } else {
            //  positive result, should have leading 0's
            overflow = !tcResult.and(NOT_BIT_MASK).equals(BigInteger.ZERO);
        }

        BigInteger result = getOnesComplement(tcResult).and(BIT_MASK);
        return new StaticMultiplicationResult(overflow, result);
    }

    /**
     * Multiply two floating point numbers which are provided in 72-bit fashion inside of BigIntegers
     */
    public static StaticMultiplyFloatingPointResult multiplyFloatingPoint(
        final BigInteger factor1,
        final BigInteger factor2
    ) {
        //  Before we get all extra-curricular, check the operands - if either is zero, the result is positive zero.
        //  Note (once again) that a floating point zero is formatted as a 72-bit integer zero.
        if (factor1.equals(POSITIVE_ZERO) || factor1.equals(NEGATIVE_ZERO)
        || factor2.equals(POSITIVE_ZERO) || factor2.equals(NEGATIVE_ZERO)) {
            return new StaticMultiplyFloatingPointResult(false, false, BigInteger.ZERO);
        }

        //  Make a note of whether the result should be negative, then make sure both operands are positive.
        //  Get unbiased absolute exponents from the biased characteristics so we can manipulate them as signed ints,
        //  and get absolute mantissas (shifted right 1 bit to avoid stupid idiot java's insistence on doing
        //  signed arithmetic when you don't want it to.
        boolean isNeg1 = isNegative(factor1);
        boolean isNeg2 = isNegative(factor2);
        BigInteger op1 = isNeg1 ? getOnesComplement(factor1) : factor1;
        BigInteger op2 = isNeg2 ? getOnesComplement(factor2) : factor2;
        boolean resultNeg = isNeg1 != isNeg2;

        BigInteger biMantissa1 = BigInteger.valueOf(getAbsoluteMantissa(op1) >>> 1);
        BigInteger biMantissa2 = BigInteger.valueOf(getAbsoluteMantissa(op2) >>> 1);
        int exp1 = getAbsoluteCharacteristic(op1) - CHARACTERISTIC_BIAS + 1;
        int exp2 = getAbsoluteCharacteristic(op2) - CHARACTERISTIC_BIAS + 1;

        System.out.println(String.format("abs1:%024o E%d", biMantissa1, exp1));//TODO
        System.out.println(String.format("abs2:%024o E%d", biMantissa2, exp2));//TODO
        //  Do the math and set over/under, then fix the exponents if/as necessary
        //  The product is now in biResult, but it is shifted way to the left - by 64 bits.  Fix it.
        BigInteger biResultMantissa = biMantissa1.multiply(biMantissa2);
        long resultMantissa = biResultMantissa.shiftRight(64).longValue();
        int resultExp = exp1 + exp2;
        System.out.println(String.format("result:%024o E%d", resultMantissa, resultExp));//TODO

        long[] normal = normalizeComponents(0L, resultMantissa, resultExp);
        long normalMantissa = normal[0];
        long normalExponent = normal[1];
        System.out.println(String.format("normal:%024o E%d", normalMantissa, normalExponent));//TODO

        //  Check exponent
        boolean overflow = false;
        boolean underflow = false;
        if (normalExponent < LOWEST_EXPONENT) {
            normalExponent = LOWEST_EXPONENT;
            underflow = true;
        } else if (normalExponent > HIGHEST_EXPONENT) {
            normalExponent = HIGHEST_EXPONENT;
            overflow = true;
        }

        try {
            checkExponent(normalExponent);
            BigInteger biValue = floatingPointFromComponents(normalMantissa, (int) normalExponent, resultNeg);
            return new StaticMultiplyFloatingPointResult(overflow, underflow, biValue);
        } catch (CharacteristOverflowException | CharacteristUnderflowException ex) {
            throw new RuntimeException("Impossible condition");
        }
    }

    /**
     * Arithmetic inverse operation - operand and result are both ones-complement 72-bit integers
     */
    public static BigInteger negate(
        final BigInteger operand
    ) {
        return operand.not().and(BIT_MASK);
    }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    /**
     * Logical AND operation (in this context, logical means bitwise)
     * @param operand1 left hand operand
     * @param operand2 right hand operand
     * @return bitwise AND of the two operands
     */
    public static BigInteger logicalAnd(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        return operand1.and(operand2);
    }

    /**
     * Logical NOT operation (in this context, logical means bitwise)
     * @param operand value to be affected
     * @return bitwise NOT of the given value
     */
    public static BigInteger logicalNot(
        final BigInteger operand
    ) {
        return operand.not();
    }

    /**
     * Logical OR operation (in this context, logical means bitwise)
     * @param operand1 left hand operand
     * @param operand2 right hand operand
     * @return bitwise OR of the two operands
     */
    public static BigInteger logicalOr(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        return operand1.or(operand2);
    }

    /**
     * Logical XOR operation (in this context, logical means bitwise)
     * @param operand1 left hand operand
     * @param operand2 right hand operand
     * @return bitwise XOR of the two operands
     */
    public static BigInteger logicalXor(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        return operand1.xor(operand2);
    }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

    /**
     * Does an algebraic shift left - the sign bit is never altered.
     * @param value 72-bit value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static BigInteger leftShiftAlgebraic(
        final BigInteger value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftAlgebraic(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            BigInteger result = value.shiftLeft(count).and(BIT_MASK.shiftRight(1));
            if (isNegative(value)) {
                result = result.or(NEGATIVE_BIT);
            }
            return result;
        }
    }

    /**
     * Shifts the given 72-bit value left, with bit[0] rotating to bit[71] at each iteration.
     * Actual implementation may not involve iterative shifting.
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static BigInteger leftShiftCircular(
        final BigInteger value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftCircular(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            int actualCount = count % 72;
            BigInteger residue = value.shiftRight(72 - actualCount);  // end-around shifted portion
            return value.shiftLeft(actualCount).and(BIT_MASK).or(residue);
        }
    }

    /**
     * Shifts the given 72-bit value left by a number of bits
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static BigInteger leftShiftLogical(
        final BigInteger value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftLogical(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            return (count > 71) ? BigInteger.ZERO : value.shiftLeft(count).and(BIT_MASK);
        }
    }

    /**
     * Does an algebraic shift right - this means the sign bit is always preserved as well as being shifted to the right.
     * @param value 72-bit value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static BigInteger rightShiftAlgebraic(
        final BigInteger value,
        final int count
    ) {
        if (count < 0) {
            return leftShiftAlgebraic(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            boolean isNegative = isNegative(value);
            if (count > 71) {
                return isNegative ? DoubleWord36.NEGATIVE_ZERO : DoubleWord36.POSITIVE_ZERO;
            } else {
                if (isNegative) {
                    int signBits = count;
                    BigInteger signMask = BigInteger.ONE.shiftLeft(signBits).subtract(BigInteger.ONE).shiftLeft(72 - signBits);
                    return value.shiftRight(count).or(signMask);
                } else {
                    return value.shiftRight(count);
                }
            }
        }
    }

    /**
     * Shifts the given 72-bit value right, with bit[71] rotating to bit[0] at each iteration.
     * Actual implementation may not involve iterative shifting.
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static BigInteger rightShiftCircular(
        final BigInteger value,
        final int count
    ) {
        if (count < 0) {
            return leftShiftCircular(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            int actualCount = (count % 72);
            BigInteger mask = BIT_MASK.shiftRight(72 - actualCount);
            BigInteger residue = (value.and(mask)).shiftLeft(72 - actualCount);
            return value.shiftRight(actualCount).or(residue);
        }
    }

    /**
     * Shifts the given 72-bit value right by a number of bits
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static BigInteger rightShiftLogical(
        final BigInteger value,
        final int count
    ) {
        if (count < 0) {
            return leftShiftLogical(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            return (count > 71) ? BigInteger.ZERO : value.shiftRight(count);
        }
    }


    //  Conversions ----------------------------------------------------------------------------------------------------------------

    /**
     * Constructs a BigInteger object representing a floating point number using the component portions thereof.
     * The result will be normalized if possible, else an exception is thrown.
     * @param mantissa The fractional portion, right shifted in 60-bit field (NOT 64-BIT FIELD)
     * @param exponent The unbiased signed exponent
     * @param negative true if the presented value should be arithmetically inverted (i.e., is negative)
     */
    public static BigInteger floatingPointFromComponents(
        final long mantissa,
        final int exponent,
        final boolean negative
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        if (mantissa == 0) {
            return negative ? NEGATIVE_ZERO : POSITIVE_ZERO;
        } else {
            long[] normalized = normalizeComponents(0L, mantissa, exponent);
            long normalizedMantissa = normalized[0] >>> 4;
            long normalizedExponent = normalized[1];
            checkExponent(normalizedExponent);

            int characteristic = exponent + CHARACTERISTIC_BIAS;
            BigInteger bi = BigInteger.valueOf(characteristic).shiftLeft(BI_MANTISSA_BITS).or(BigInteger.valueOf(normalizedMantissa));
            if (negative) {
                bi = negate(bi);
            }
            return bi;
        }
    }

    /**
     * Converts a 72-bit integer value in a DoubleWord36 object to a
     * 72-bit normalized floating point value in a DoubleWord36 object.
     */
    public static BigInteger floatingPointFromInteger(
        final DoubleWord36 operand
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        if (operand.isPositiveZero()) {
            return DoubleWord36.POSITIVE_ZERO;
        } else if (operand.isNegativeZero()) {
            return DoubleWord36.NEGATIVE_ZERO;
        }

        boolean negative = operand.isNegative();
        BigInteger absolute = negative ? operand.negate()._value : operand._value;

        //  Convert 72-bit integer down to the 64-bit precision required for a long mantissa.
        //  Shift right until the top 8 bits are zero.
        //  If we do shift down, we might lose some of the least significant information.
        //  That is expected going from 72-bit integers to floating point.
        int exponent = 0;
        BigInteger mask8 = BigInteger.valueOf(0xFF00_0000_0000_0000L);
        while (!absolute.and(mask8).equals(BigInteger.ZERO)) {
            absolute = absolute.shiftRight(1);
            ++exponent;
        }

        //  Now normalize
        long[] normalized = normalizeComponents(absolute.longValue(), 0L, exponent);
        long normalizedMantissa = normalized[0] >>> 4;
        int normalizedExponent = (int) normalized[1];
        checkExponent(normalizedExponent);

        int characteristic = normalizedExponent + CHARACTERISTIC_BIAS;
        BigInteger bi = BigInteger.valueOf(characteristic).shiftLeft(BI_MANTISSA_BITS).or(BigInteger.valueOf(normalizedMantissa));
        if (negative) {
            bi = negate(bi);
        }
        return bi;
    }

    /**
     * Creates a 72-bit floating point value from an IEE754 double float
     */
    public static BigInteger fromDouble(
        final double operand
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        long bits = Double.doubleToRawLongBits(operand);
        if (bits == 0) {
            return POSITIVE_ZERO;
        } else if (bits == 0x8000_0000_0000_0000L) {
            return NEGATIVE_ZERO;
        } else {
            int exponentBias = 1023;
            boolean negative = (bits & 0x8000_0000_0000_0000L) != 0;
            int exponent = (int) ((bits & 0x7FF0_0000_0000_0000L) >>> 52) - exponentBias + 1;
            long mantissa = ((bits & 0xF_FFFF_FFFF_FFFFL) | (0x10_0000_0000_0000L)) << (64-53);
            return floatingPointFromComponents(mantissa, exponent, negative);
        }
    }

    /**
     * Creates a 72-bit floating point value from an IEE754 single float
     * Shouldn't ever actually throw, but we're lazy to try/catch here, so we make the callers do it.
     */
    public static BigInteger fromFloat(
        final float operand
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        int bits = Float.floatToRawIntBits(operand);
        if (bits == 0) {
            return POSITIVE_ZERO;
        } else if (bits == 0x8000_0000) {
            return NEGATIVE_ZERO;
        } else {
            int exponentBias = 127;
            boolean negative = (bits & 0x8000_0000) != 0;
            int exponent = ((bits & 0x7F80_0000) >>> 23) - exponentBias + 1;
            long mantissa = ((bits & 0x007F_FFFF) | (0x0080_0000L)) << (64 - 24);
            return floatingPointFromComponents(mantissa, exponent, negative);
        }
    }

    /**
     * Converts a twos-complement BigInteger operand to ones-complement
     */
    public static BigInteger getOnesComplement(
        final BigInteger operand
    ) {
        if (operand.compareTo(BigInteger.ZERO) >= 0) {
            return operand;
        } else {
            return operand.negate().not().and(BIT_MASK);
        }
    }

    /**
     * As above, but conventiently for simple twos-complement integer operands
     */
    public static BigInteger getOnesComplement(
        final long operand
    ) {
        return getOnesComplement(BigInteger.valueOf(operand));
    }

    /**
     * Converts a ones-complement BigInteger operand to twos-complement
     */
    public static BigInteger getTwosComplement(
        final BigInteger operand
    ) {
        return isNegative(operand) ? negate(operand).negate() : operand;
    }

    /**
     * Normalizes a 72-bit floating point value in a DoubleWord36 object
     */
    public static BigInteger normalize(
        final BigInteger operand
    ) throws CharacteristUnderflowException,
             CharacteristOverflowException {
        boolean negative = isNegative(operand);
        long mantissa = getAbsoluteMantissa(operand);

        if (mantissa == 0) {
            return negative ? NEGATIVE_ZERO : POSITIVE_ZERO;
        }

        int exponent = getAbsoluteCharacteristic(operand) - CHARACTERISTIC_BIAS;
        while ((mantissa & LONG_MANTISSA_LEFTMOST_BIT) == 0) {
            mantissa <<= 1;
            --exponent;
        }

        return floatingPointFromComponents(mantissa, exponent, isNegative(operand));
    }

    /**
     * Populates this object with quarter-words derived from the ASCII characters in the source string.
     * If the string does not contain at least 8 characters, we pad the resulting output with blanks as necessary.
     * Any characters in the string beyond the eighth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static DoubleWord36 stringToWordASCII(
        final String source
    ) {
        String padded = source + "       ";
        String s1 = padded.substring(0, 4);
        String s2 = padded.substring(4, 8);
        Word36 w1 = Word36.stringToWordASCII(s1);
        Word36 w2 = Word36.stringToWordASCII(s2);
        return new DoubleWord36(w1._value, w2._value);
    }

    /**
     * Populates this object with sixth-words representing the fieldata characters derived from the ASCII characters
     * in the source string. If the string does not contain at least 12 characters, we pad the resulting output with
     * blanks as necessary. Any characters in the string beyond the twelfth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static DoubleWord36 stringToWordFieldata(
        final String source
    ) {
        String padded = source + "           ";
        String s1 = padded.substring(0, 6);
        String s2 = padded.substring(6, 12);
        Word36 w1 = Word36.stringToWordFieldata(s1);
        Word36 w2 = Word36.stringToWordFieldata(s2);
        return new DoubleWord36(w1._value, w2._value);
    }


    //  Formatting for display -----------------------------------------------------------------------------------------------------

    /**
     * Converts to two single word objects.
     * result[0] is the high value, result[1] is the low value
     */
    public static Word36[] getWords(
        final BigInteger value
    ) {
        Word36[] words = new Word36[2];
        words[0] = new Word36(value.shiftRight(36).longValue());
        words[1] = new Word36(value.and(SHORT_BIT_MASK).longValue());
        return words;
    }

    /**
     * Interprets the given 36-bit value as a sequence of 12 Octal digits, and produces those characters as a result
     */
    public static String toOctal(
        final BigInteger value
    ) {
        return String.format("%024o", value);
    }

    /**
     * Interprets the given 72-bit value as a sequence of 8 ASCII characters, and produces those characters as a result
     */
    public static String toStringFromASCII(
        final BigInteger value
    ) {
        Word36[] words = getWords(value);
        return String.format("%s%s",
                             Word36.toStringFromASCII(words[0]._value),
                             Word36.toStringFromASCII(words[1]._value));
    }

    /**
     * Interprets the given 72-bit value as a sequence of 12 Fieldata characters, and produces those characters as a result
     */
    public static String toStringFromFieldata(
        final BigInteger value
    ) {
        Word36[] words = getWords(value);
        return String.format("%s%s",
                             Word36.toStringFromFieldata(words[0]._value),
                             Word36.toStringFromFieldata(words[1]._value));
    }


    //  Private stuff (protected so unit tests can do their thing) -----------------------------------------------------------------

    /**
     * Checks validity of a given unbiased exponent, and throws if necessary
     */
    protected static void checkExponent(
        final long exponent
    ) throws CharacteristUnderflowException,
             CharacteristOverflowException {
        if (exponent < LOWEST_EXPONENT) {
            throw new CharacteristUnderflowException();
        }

        if (exponent > HIGHEST_EXPONENT) {
            throw new CharacteristOverflowException();
        }
    }

    /**
     * Converts a fractional mantissa to an integral value by conceptually shifting it left until
     * into the integral field until the fractional portion is zero, and then we return the
     * interim result with the accordingly-downward-adjusted exponent.
     * (In practice, we shift right until the right-most bit is non-zero, and present the result as the integral number).
     * @param mantissa initial 64-bit mantissa value (must be positive)
     * @param exponent initial exponent which corresponds to the initial mantissa value
     * @return two-word array: [0] is the resulting 64-bit integral value, [1] is the resulting signed exponent
     */
    protected static long[] convertMantissaToIntegral(
        final long mantissa,
        final long exponent
    ) {
        long[] result = new long[2];
        if (mantissa != 0) {
            long resultMantissa = mantissa;
            int countDown = LONG_MANTISSA_BITS;
            while ((resultMantissa & 01) == 0) {
                resultMantissa >>>= 1;
                --countDown;
            }

            result[0] = resultMantissa;
            result[1] = exponent - countDown;
        }
        return result;
    }

    /**
     * Retrieves the magnitude of the characteristic from the given operand which holds a fully-formed floating point value.
     * The sign bit is retrieved as well, which is okay because we ones-complement negative numbers, and thus in the
     * result the bit will always be zero, so the characteristic can be treated as a 12-bit value.
     * @param operand floating point value
     * @return absolute value of the characteristic
     */
    protected static int getAbsoluteCharacteristic(
        final BigInteger operand
    ) {
        int characteristic = operand.and(BI_CHARACTERISTIC_MASK).shiftRight(BI_MANTISSA_BITS).intValue();
        if (isNegative(operand)) {
            characteristic ^= (1L << CHARACTERISTIC_BITS) - 1;
        }
        return characteristic;
    }

    /**
     * Retrieves the magnitude of the mantissa from the given operand which holds a fully-formed floating point value.
     * Note that the mantissa will be returned as a 64-bit value.
     * @param operand floating point value
     * @return absolute value of the mantissa
     */
    protected static long getAbsoluteMantissa(
        final BigInteger operand
    ) {
        long result = operand.and(BI_MANTISSA_MASK).longValue() << (LONG_MANTISSA_BITS - BI_MANTISSA_BITS);
        if (isNegative(operand)) {
            result = ~(result | (1L << (LONG_MANTISSA_BITS - BI_MANTISSA_BITS)) - 1);
        }

        return result;
    }

    /**
     * Normalizes a positive floating point number expresssed in its component forms.
     * This exists for interim calculations which might result in temporary out-of-range exponents.
     * This method can produce out-of-range exponents.
     * @param integral The integral portion of the number to be created (right-justified, of course)
     *                 range +/- 0x7FFF_FFFF_FFFF_FFFF
     *                 If this value is negative, then it is (and fractional) are in twos-complement,
     *                 and the generated result will be negative twos-complement.
     * @param fractional The magnitude of the fractional portion, left-justified
     *                   i.e., 10100...00 (for 64 bits) indicates decimal 0.5 + 0.125 == 0.625
     *                   range +/- 0x7FFF_FFFF_FFFF_FFFF
     *                   I cannot stress enough that this is a 64-bit number, *NOT* 60-bits.
     *                   This number is twos-complement negative if integral is negative.
     * @param exponent Initial unbiased exponent (zero would be nice, but we take whatever)
     *                 range +/- 0x7FFF_FFFF  (large magnitudes will result in over/underflow)
     * @return normalized twos-complement mantissa in result[0] RJZF,
     *          normalized twos-complement exponent in result[1] LJZF
     */
    protected static long[] normalizeComponents(
        final long integral,
        final long fractional,
        final int exponent
    ) {
        long[] result = new long[2];    //  initialized to positive zero

        if ((integral != 0) || (fractional != 0)) {
            boolean neg = integral < 0;
            long absIntegral = neg ? -integral : integral;
            long absFractional = neg ? -fractional : fractional;

            //  Downshift integral into fractional until integral is zero
            long tempIntegral = integral;
            long tempFractional = fractional;
            int resultExponent = exponent;
            while (tempIntegral > 0) {
                int residue = (int) (tempIntegral & 0x01);
                tempIntegral >>>= 1;
                tempFractional >>>= 1;
                if (residue != 0) {
                    tempFractional |= 0x8000_0000_0000_0000L;
                }
                ++resultExponent;
            }

            //  Upshift fractional until it is normalized TO 64 BITS...
            while ((tempFractional & 0x8000_0000_0000_0000L) == 0) {
                tempFractional <<= 1;
                --resultExponent;
            }

            result[0] = neg ? -tempFractional : tempFractional;
            result[1] = neg ? -resultExponent : resultExponent;
        }

        return result;
    }

    /**
     * Converts a DoubleWord36 floating point value to the system double-precision floating point format
     */
    protected static double toDouble(
        final BigInteger value
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        int exponentBias = 1023;
        int mantissaBits = 52;

        Components components = getComponents(value);
        if (components._exponent < Double.MIN_EXPONENT) {
            throw new CharacteristUnderflowException();
        } else if (components._exponent > Double.MAX_EXPONENT) {
            throw new CharacteristOverflowException();
        }

        long result = ((components._mantissa << 1) >>> (LONG_MANTISSA_BITS - mantissaBits));
        result |= ((long)components._exponent - 1 + exponentBias) << mantissaBits;
        if (components._isNegative) {
            result |= 1L << (Double.SIZE - 1);
        }

        return Double.longBitsToDouble(result);
    }

    /**
     * Converts a DoubleWord36 floating point value to IEEE754 format for 32-bit floats
     */
    protected static float toFloat(
        final BigInteger value
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        int exponentBias = 127;
        int mantissaBits = 23;

        Components components = getComponents(value);
        if (components._exponent < Float.MIN_EXPONENT) {
            throw new CharacteristUnderflowException();
        } else if (components._exponent > Float.MAX_EXPONENT) {
            throw new CharacteristOverflowException();
        }

        int result = (int)((components._mantissa << 1) >>> (LONG_MANTISSA_BITS - mantissaBits));
        result |= (components._exponent - 1 + exponentBias) << mantissaBits;
        if (components._isNegative) {
            result |= 1L << (Float.SIZE - 1);
        }

        return Float.intBitsToFloat(result);
    }
}
