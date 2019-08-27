/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.math.BigInteger;

/**
 * Library for doing architecturally-correct 72-bit operations on integers
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

    public static final DoubleWord36 DW36_NEGATIVE_ONE  = new DoubleWord36(NEGATIVE_ONE);
    public static final DoubleWord36 DW36_NEGATIVE_ZERO = new DoubleWord36(NEGATIVE_ZERO);  //  works for floating point as well
    public static final DoubleWord36 DW36_POSITIVE_ONE  = new DoubleWord36(POSITIVE_ONE);
    public static final DoubleWord36 DW36_POSITIVE_ZERO = new DoubleWord36(POSITIVE_ZERO);  //  works for floating point as well


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
        final long high,
        final long low
    ) {
        _value = BigInteger.valueOf(high).shiftLeft(36).or(BigInteger.valueOf(low));
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
    public Word36[] getWords() { return getWords(_value); }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    public boolean isNegative() { return isNegative(_value); }
    public boolean isNegativeZero() { return isNegativeZero(_value); }
    public boolean isPositive() { return isPositive(_value); }
    public boolean isPositiveZero() { return isPositiveZero(_value); }
    public boolean isZero()     { return isZero(_value); }


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
    public DoubleWord36 negate()                                { return new DoubleWord36(negate(_value)); }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    public DoubleWord36 logicalAnd(DoubleWord36 operand)    { return new DoubleWord36(logicalAnd(_value, operand._value)); }
    public DoubleWord36 logicalNot()                        { return new DoubleWord36(logicalNot(_value)); }
    public DoubleWord36 logicalOr(DoubleWord36 operand)     { return new DoubleWord36(logicalOr(_value, operand._value)); }
    public DoubleWord36 logicalXor(DoubleWord36 operand)    { return new DoubleWord36(logicalXor(_value, operand._value)); }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

//    public void leftShiftAlgebraic(int count)   { _value = leftShiftAlgebraic(_value, count); }
    public DoubleWord36 leftShiftCircular(int count)    { return new DoubleWord36(leftShiftCircular(_value, count)); }
    public DoubleWord36 leftShiftLogical(int count)     { return new DoubleWord36(leftShiftLogical(_value, count)); }
//    public void rightShiftAlgebraic(int count)  { _value = rightShiftAlgebraic(_value, count); }
    public DoubleWord36 rightShiftCircular(int count)   { return new DoubleWord36(rightShiftCircular(_value, count)); }
    public DoubleWord36 rightShiftLogical(int count)    { return new DoubleWord36(rightShiftLogical(_value, count)); }


    //  Conversions ----------------------------------------------------------------------------------------------------------------

    public BigInteger getTwosComplement()   { return getTwosComplement(_value); }
    public String toOctal()                 { return toOctal(_value); }
    public String toStringFromASCII()       { return toStringFromASCII(_value); }
    public String toStringFromFieldata()    { return toStringFromFieldata(_value); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods - these operate on and return long integers representing ones-complement values
    //  ----------------------------------------------------------------------------------------------------------------------------

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
        boolean neg1 = isNegative(addend1);
        boolean neg2 = isNegative(addend2);

        BigInteger result = addSimple(addend1, addend2);
        if (!result.and(CARRY_BIT).equals(BigInteger.ZERO)) {
            result = result.and(BIT_MASK).add(BigInteger.ONE);
        }

        boolean negRes = isNegative(result);

        boolean carry = result.compareTo(BigInteger.ZERO) < 0 ? (neg1 && neg2) : (neg1 || neg2);
        boolean overflow = (neg1 == neg2) && (neg1 != negRes);
        return new StaticAdditionResult(carry, overflow, result);
    }

    /**
     * Adds two BigInteger operands on the presumption they both represent 72-bit floating point values.
     */
    public static StaticAddFloatingPointResult addFloatingPoint(
        final BigInteger addend1,
        final BigInteger addend2
    ) {
        //  Get the magnitude of the caracteristics, then get the magnitude of their difference.
        int characteristic1 = getAbsoluteCharacteristic(addend1);
        int characteristic2 = getAbsoluteCharacteristic(addend2);
        int expDiff = Math.abs(characteristic1 - characteristic2);

        //  Get the mantissas, which are in ones-complement form, and shift the one corresponding to the
        //  smaller exponent to the right by the number of bits indicated by expDiff.
        //  If the mantissa is negative, this is an arithmetic shift (preserving the sign bit).
        //  However, rather than mess with arithmetic shifting, we convert to positive, do the shift,
        //  then convert back to negative.
        //  Also note the sign of the result - it will be the sign of the characteristic with the larger magnitude.
        boolean op1Neg = isNegative(addend1);
        boolean op2Neg = isNegative(addend2);
        long mantissa1 = getMantissa(addend1);
        long mantissa2 = getMantissa(addend2);

        boolean resultNeg = false;
        if (characteristic1 < characteristic2) {
            mantissa1 = shiftMantissaRight(mantissa1, expDiff, op1Neg);
            resultNeg = op2Neg;
        } else if (characteristic2 < characteristic1) {
            mantissa2 = shiftMantissaRight(mantissa2, expDiff, op2Neg);
            resultNeg = op1Neg;
        }

        //  Add the values.
        //  If the result is zero, then we construct the special positive zero floating point value.
        long resultMantissa = addMantissas(mantissa1, mantissa2);
        if (resultMantissa == 0) {
            return new StaticAddFloatingPointResult(false, false, POSITIVE_ZERO);
        }

        //  Normalize the mantissa.  If the biased characteristic goes negative we have an underflow.
        //  If the biased characteristic goes greater than 11 bits, we have an overflow.
        //  Start with the larger absolute characteristic.
        int resultCharacteristic = Math.max(characteristic1, characteristic2);
        long[] normalized = normalizeMantissa(resultMantissa, resultCharacteristic, resultNeg);
        long normalMantissa = normalized[0];
        long normalCharacteristic = normalized[1];

        //  Check for over/under, then construct the floating point value
        boolean underFlow = normalCharacteristic < 0;
        boolean overFlow = normalCharacteristic > 03777;
        BigInteger value = resultNeg ? BigInteger.ONE.shiftLeft(71) : BigInteger.ZERO;
        value = value.or(BigInteger.valueOf(normalCharacteristic & 03777).shiftLeft(60));
        value = value.or(BigInteger.valueOf(normalMantissa));

        return new StaticAddFloatingPointResult(overFlow, underFlow, value);
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
        //  If the signs are unequal, the positive value is greater.
        boolean thisNeg = isNegative(operand1);
        boolean thatNeg = isNegative(operand2);
        if (thisNeg != thatNeg) {
            return thisNeg ? -1 : 1;
        }

        //  If the absolute values of the characteristics are unequal the larger value
        //  belongs to the greater floating point value (unless we are negative, then the opposite is true)
        int thisCharacteristic = getAbsoluteCharacteristic(operand1);
        int thatCharacteristic = getAbsoluteCharacteristic(operand2);
        if (thisCharacteristic != thatCharacteristic) {
            if (thisNeg) {
                return thisCharacteristic < thatCharacteristic ? 1 : -1;
            } else {
                return thisCharacteristic < thatCharacteristic? -1 : 1;
            }
        }

        //  If the mantissas are unequal, the larger is greater (unless we are negative...)
        long thisMantissa = getMantissa(operand1);
        long thatMantissa = getMantissa(operand2);
        if (thisMantissa != thatMantissa) {
            if (thisNeg) {
                return thisMantissa < thatMantissa ? 1 : -1;
            } else {
                return thisMantissa < thatMantissa ? -1 : 1;
            }
        }

        //  Values are equal
        return 0;
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
     * Multiply two floating point numbers
     */
    public static StaticMultiplyFloatingPointResult multiplyFloatingPoint(
        final BigInteger factor1,
        final BigInteger factor2
    ) {
        //  Make a note of whether the result should be negative, then make sure both operands are positive.
        //  Get unbiased exponents from the biased characteristics so we can manipulate them as signed ints.
        boolean isNeg1 = isNegative(factor1);
        boolean isNeg2 = isNegative(factor2);
        boolean resultNeg = isNeg1 != isNeg2;

        BigInteger op1 = isNeg1 ? getOnesComplement(factor1) : factor1;
        BigInteger op2 = isNeg2 ? getOnesComplement(factor2) : factor2;
        int exp1 = getAbsoluteCharacteristic(op1);
        int exp2 = getAbsoluteCharacteristic(op2);

        //  Before we get all extra-curricular, check the operands - if either is zero, the result is positive zero.
        //  Note (once again) that a floating point zero is formatted as a 72-bit integer zero.
        if (op1.equals(BigInteger.ZERO) || op2.equals(BigInteger.ZERO)) {
            return new StaticMultiplyFloatingPointResult(false, false, BigInteger.ZERO);
        }

        //  Now convert the fractional mantissas to integral, with corresponding adjustments to the exponents
        long[] conversion1 = convertMantissaToIntegral(getMantissa(op1), exp1);
        long integral1 = conversion1[0];
        exp1 = (int) conversion1[1];

        long[] conversion2 = convertMantissaToIntegral(getMantissa(op2), exp2);
        long integral2 = conversion2[0];
        exp2 = (int) conversion2[1];

        //  Multiple the integrals and add the exponents.
        //  Do it with a BigInteger since 60bits x 60bits is potentially 119 bits.
        //  Then shift that result back to the right until there are no ones to the left of the 60-bit mantissa field.
        //  This fits the mantissa value back down into 60 bits.
        BigInteger resultIntegral = BigInteger.valueOf(integral1).multiply(BigInteger.valueOf(integral2));
        int resultExponent = exp1 + exp2;
        while (!resultIntegral.and(BIT_MASK).equals(resultIntegral)) {
            resultIntegral.shiftRight(1);
            ++resultExponent;
        }

        //  Now normalize the thing (left-justifying if/as necessary) and note over/underflow
        BigInteger leftBig = BigInteger.valueOf(0x0800_0000_0000_0000L);
        while (resultIntegral.and(leftBig).equals(BigInteger.ZERO)) {
            resultIntegral.shiftLeft(1);
            --resultExponent;
        }

        boolean overflow = resultExponent > 01777;
        boolean underflow = resultExponent < -02000;

        //  Finish rebuilding the overall floating point value
        BigInteger resultValue = resultIntegral.or(BigInteger.valueOf(resultExponent + 1024).shiftLeft(60));
        if (resultNeg) {
            resultValue = logicalNot(resultValue);
        }

        return new StaticMultiplyFloatingPointResult(overflow, underflow, resultValue);
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


//    //  Shift Operations -----------------------------------------------------------------------------------------------------------
//
//    public static long leftShiftAlgebraic(
//        final long value,
//        final int count
//    ) {
//        if (count < 0) {
//            return rightShiftAlgebraic(value, -count);
//        } else if (count == 0) {
//            return value;
//        } else {
//            return leftShiftLogical(value, count);
//        }
//    }

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

//    /**
//     * Does an algebraic shift right - this means the sign bit is always preserved as well as being shifted to the right.
//     * @param value 72-bit value to be shifted
//     * @param count number of bits to be shifted
//     * @return resulting value
//     */
//    public static long rightShiftAlgebraic(
//        final long value,
//        final int count
//    ) {
//        if (count < 0) {
//            return leftShiftAlgebraic(value, -count);
//        } else if (count == 0) {
//            return value;
//        } else {
//            boolean wasNegative = isNegative(value);
//            if (count > 35) {
//                return wasNegative ? NEGATIVE_ZERO._value : 0;
//            } else {
//                long result = value >> count;
//                if (wasNegative)
//                    result |= ((~(BIT_MASK >> count)) & BIT_MASK);
//                return result;
//            }
//        }
//    }

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
     * Converts a 72-bit integer value in a DoubleWord36 object to a 72-bit floating point value
     * in a DoubleWord36 object.
     */
    public static BigInteger floatingPointFromInteger(
        final DoubleWord36 operand
    ) {
        if (operand.isPositiveZero()) {
            return DoubleWord36.POSITIVE_ZERO;
        } else if (operand.isNegativeZero()) {
            return DoubleWord36.NEGATIVE_ZERO;
        }

        boolean isNegative = operand.isNegative();

        //  Create a mantissa and normalize it down to 60 bits
        boolean truncation = false;
        BigInteger mantissa = operand.get();
        int characteristic = 1024;
        BigInteger badMask = BigInteger.valueOf(0xFFF).shiftLeft(60);
        while (!mantissa.and(badMask).equals(BigInteger.ZERO)) {
            ++characteristic;
            if (mantissa.and(BigInteger.ONE).equals(BigInteger.ONE)) {
                truncation = true;
            }
            mantissa = mantissa.shiftRight(1);
        }

        //  Now normalize it the other direction as necessary.
        //  No need to check for over/underflow - can't happen here.
        long[] result = normalizeMantissa(mantissa.longValue(), characteristic + 1024, isNegative);
        long normalizedMantissa = result[0];
        long normalizedCharacteristic = result[1];

        BigInteger value = isNegative ? BigInteger.ONE.shiftLeft(71) : BigInteger.ZERO;
        value = value.or(BigInteger.valueOf(normalizedCharacteristic).shiftLeft(60));
        value = value.or(BigInteger.valueOf(normalizedMantissa));
        return value;
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
     * Populates this object with quarter-words derived from the ASCII characters in the source string.
     * If the string does not contain at least 8 characters, we pad the resulting output with blanks as necessary.
     * Any characters in the string beyond the eighth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static DoubleWord36 stringToWordASCII(
        final String source
    ) {
        String s1 = source.substring(0, 4);
        String s2 = source.length() > 4 ? source.substring(4, 8) : "";
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
        String s1 = source.substring(0, 6);
        String s2 = source.length() > 6 ? source.substring(6, 12) : "";
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


    //  Private stuff --------------------------------------------------------------------------------------------------------------

    /**
     * Ones-complement addition of 60-bit mantissa operands.
     * We do *not* check for over/underflow, but those conditions are deducible by the caller.
     * If the signs of the operands match, but they do not match the result, we had an under/overflow.
     */
    //TODO fold this back into addFloatingPoint if we don't use it anywhere else
    private static long addMantissas(
        final long addend1,
        final long addend2
    ) {
        long sum = addend1 + addend2;
        if (sum > 0x0FFF_FFFF_FFFF_FFFFL) {
            sum &= 0x0FFF_FFFF_FFFF_FFFFL;
            ++sum;
        }

        return sum;
    }

    /**
     * Converts a fractional mantissa to an integral value by conceptually shifting it left until
     * into the integral field until the fractional portion is zero, and then we return the
     * interim result with the accordingly-downward-adjusted exponent.
     * (In practice, we shift right until the right-most bit is non-zero, and present the result as the integral number).
     * @param mantissa initial mantissa value
     * @param exponent initial exponent which corresponds to the initial mantissa value
     * @return two-word array: [0] is the resulting integral value, [1] is the resulting signed exponent
     */
    private static long[] convertMantissaToIntegral(
        final long mantissa,
        final long exponent
    ) {
        long resultMantissa = mantissa;
        int countDown = 60;
        while ((resultMantissa & 01) == 0) {
            resultMantissa >>= 1;
            --countDown;
        }

        long[] result = new long[2];
        result[0] = resultMantissa;
        result[1] = exponent - countDown;
        return result;
    }

    /**
     * Retrieves the magnitude of the characteristic from the given operand which holds a fully-formed floating point value.
     * The sign bit is retrieved as well, which is okay because we ones-complement negative numbers, and thus in the
     * result the bit will always be zero, so the characteristic can be treated as a 12-bit value.
     * @param operand floating point value
     * @return absolute value of the characteristic
     */
    private static int getAbsoluteCharacteristic(
        final BigInteger operand
    ) {
        int characteristic = operand.shiftRight(60).and(BigInteger.valueOf(07777)).intValue();
        if ((characteristic & 04000) > 0) { characteristic ^= 07777; }
        return characteristic;
    }

    private static long getMantissa(
        final BigInteger operand
    ) {
        return operand.and(BigInteger.valueOf(0xFFFF_FFFF_FFFFL)).longValue();
    }

    /**
     * Converts a 60-bit mantissa to it's arithmetic ones-complement inverse
     */
    private static long negateMantissa(
        final long operand
    ) {
        return ~(operand | 0xF000_0000_0000_0000L);
    }

    /**
     * Normalizes the mantissa according to the sign of the characteristic.
     * The characteristic is the 11-bit biased characteristic, ones-complemented if isNegative is true
     * The mantissa is a 60-bit value, ones-complemented if isNegative is true
     * @return two long values: [1] is the new characteristic and [0] is the shifted mantissa
     */
    private static long[] normalizeMantissa(
        final long mantissa,
        final long characteristic,
        final boolean isNegative
    ) {
        long resultCharacteristic = characteristic;
        long resultMantissa = mantissa;
        if (isNegative) {
            resultCharacteristic = (resultCharacteristic ^ 03777) & 03777;
            resultMantissa = (resultMantissa ^ 0x0FFF_FFFF_FFFF_FFFFL) & 0x0FFF_FFFF_FFFF_FFFFL;
        }

        while ((resultCharacteristic & 0x0800_0000_0000_0000L) == 0) {
            resultCharacteristic <<= 1;
            --resultCharacteristic;
        }

        if (isNegative) {
            resultCharacteristic = (resultCharacteristic ^ 03777) & 03777;
            resultMantissa = (resultMantissa ^ 0x0FFF_FFFF_FFFF_FFFFL) & 0x0FFF_FFFF_FFFF_FFFFL;
        }

        long[] result = new long[2];
        result[0] = resultMantissa;
        result[1] = resultCharacteristic;
        return result;
    }

    /**
     * Shifts the bits of a mantissa to the right, in order to increase the corresponding exponent.
     * This version uses a known number of bits.
     * @param operand 60-bit mantissa, which will be ones-complemented if the containing fp value is negative
     * @param count number of bits to be shifted
     * @param isNegative true if the containing fp value is negative
     * @return new mantissa value shifted accordingly (with leading bits preserved for negative numbers)
     */
    private static long shiftMantissaRight(
        final long operand,
        final int count,
        final boolean isNegative
    ) {
        long result = operand;
        if (isNegative) { result = negateMantissa(result); }
        result >>= count;
        if (isNegative) { result = negateMantissa(result); }
        return result;
    }
}
