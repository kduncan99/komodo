/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import java.math.BigInteger;

import static com.bearsnake.komodo.baselib.Word36.negate;

/**
 * Library for doing architecturally correct 72-bit operations on integers
 */
@SuppressWarnings("Duplicates")
public class DoubleWord36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class AdditionPacket {
        public long[] _addend1 = new long[2];
        public long[] _addend2 = new long[2];
        public long[] _sum = new long[2];
        public boolean _carry;
        public boolean _overflow;

        public void prepare(final long[] addend1Source,
                            final int addend1Offset,
                            final long[] addend2Source,
                            final int addend2Offset) {
            _addend1[0] = addend1Source[addend1Offset];
            _addend1[1] = addend1Source[addend1Offset + 1];
            _addend2[0] = addend2Source[addend2Offset];
            _addend2[1] = addend2Source[addend2Offset + 1];
        }
    }


//    public static class DivisionResult {
//        public final DoubleWord36 _result;
//        public final DoubleWord36 _remainder;
//
//        public DivisionResult(
//            final StaticDivisionResult sdr
//        ) {
//            _result = new DoubleWord36(sdr._result);
//            _remainder = new DoubleWord36(sdr._remainder);
//        }
//    }
//
//    public static class MultiplicationResult {
//        public final boolean _overflow;
//        public final DoubleWord36 _value;
//
//        public MultiplicationResult(
//            final StaticMultiplicationResult smr
//        ) {
//            _overflow = smr._overflow;
//            _value = new DoubleWord36(smr._value);
//        }
//    }
//
//    public static class StaticAdditionResult {
//        public final boolean _carry;
//        public final boolean _overflow;
//        public final BigInteger _value;
//
//        public StaticAdditionResult(
//            final boolean carry,
//            final boolean overflow,
//            final BigInteger value
//        ) {
//            _carry = carry;
//            _overflow = overflow;
//            _value = value;
//        }
//    }
//
//    public static class StaticDivisionResult {
//        public final BigInteger _result;
//        public final BigInteger _remainder;
//
//        public StaticDivisionResult(
//            final BigInteger result,
//            final BigInteger remainder
//        ) {
//            _result = result;
//            _remainder = remainder;
//        }
//    }
//
//    public static class StaticMultiplicationResult {
//        public final boolean _overflow;
//        public final BigInteger _value;
//
//        public StaticMultiplicationResult(
//            final boolean overflow,
//            final BigInteger value
//        ) {
//            _overflow = overflow;
//            _value = value;
//        }
//    }

//    public static final BigInteger CARRY_BIT        = BigInteger.valueOf(1).shiftLeft(72);
    public static final BigInteger SHORT_BIT_MASK   = BigInteger.valueOf(Word36.BIT_MASK);
    public static final BigInteger BIT_MASK         = SHORT_BIT_MASK.shiftLeft(36).or(SHORT_BIT_MASK);
//    public static final BigInteger NOT_BIT_MASK     = BIT_MASK.not();

//    public static final BigInteger NEGATIVE_BIT     = BigInteger.ONE.shiftLeft(71);
//    public static final BigInteger NEGATIVE_ONE     = BIT_MASK.add(BigInteger.ONE.negate());
//    public static final BigInteger NEGATIVE_ZERO    = BIT_MASK;
//    public static final BigInteger POSITIVE_ONE     = BigInteger.ONE;
//    public static final BigInteger POSITIVE_ZERO    = BigInteger.ZERO;

    private DoubleWord36() {}

    /**
     * Converts a twos-complement BigInteger operand to ones-complement
     */
    public static void getOnesComplement(final BigInteger operand,
                                         final long[] destination,
                                         final int offset) {
        if (operand.compareTo(BigInteger.ZERO) >= 0) {
            destination[offset] = operand.shiftRight(36).and(SHORT_BIT_MASK).longValue();
            destination[offset + 1] = operand.and(SHORT_BIT_MASK).longValue();
        } else {
            var magnitude = operand.negate();
            destination[offset] = negate(magnitude.shiftRight(36).and(SHORT_BIT_MASK).longValue());
            destination[offset + 1] = negate(magnitude.and(SHORT_BIT_MASK).longValue());
        }
    }

    /**
     * Converts a twos-complement BigInteger operand to ones-complement 36-bit value
     */
    public static void getOnesComplementShort(final BigInteger operand,
                                              final long[] destination,
                                              final int offset) {
        if (operand.compareTo(BigInteger.ZERO) >= 0) {
            destination[offset] = operand.and(SHORT_BIT_MASK).longValue();
        } else {
            var magnitude = operand.negate();
            destination[offset] = negate(magnitude.and(SHORT_BIT_MASK).longValue());
        }
    }

    /**
     * Converts a ones-complement operand to twos-complement
     */
    public static BigInteger getTwosComplement(final long[] source,
                                               final int sourceOffset) {
        return getTwosComplement(source[sourceOffset], source[sourceOffset + 1]);
    }

    public static BigInteger getTwosComplement(final long msValue,
                                               final long lsValue) {
        if (isPositive(msValue, lsValue)) {
            return BigInteger.valueOf(msValue)
                             .shiftLeft(36)
                             .or(BigInteger.valueOf(lsValue));
        } else {
            return BigInteger.valueOf(Word36.negate(msValue))
                             .shiftLeft(36)
                             .or(BigInteger.valueOf(Word36.negate(lsValue)))
                             .negate();
        }
    }

    /**
     * Tests 72-bit ones-complement value to see if it is negative
     * @param source array containing the 72-bit value
     * @param offset offset into the array where the value starts
     * @return true if the value is negative, false otherwise
     */
    public static boolean isNegative(final long[] source,
                                     final int offset) {
        return (source[offset] & Word36.NEGATIVE_BIT) != 0;
    }

    /**
     * Tests 72-bit ones-complement value to see if it is negative
     * @param msValue most-significant 36-bit word
     * @param lsValue least-significant 36-bit word
     * @return true if the value is negative, false otherwise
     */
    public static boolean isNegative(final long msValue,
                                     final long lsValue) {
        return (msValue & Word36.NEGATIVE_BIT) != 0;
    }

    /**
     * Tests 72-bit ones-complement value to see if it is negative zero
     * @param source array containing the 72-bit value
     * @param offset offset into the array where the value starts
     * @return true if the value is negative zero, false otherwise
     */
    public static boolean isNegativeZero(final long[] source,
                                         final int offset) {
        return source[offset] == Word36.NEGATIVE_ZERO && source[offset + 1] == Word36.NEGATIVE_ZERO;
    }

    /**
     * Tests 72-bit ones-complement value to see if it is negative zero
     * @param msValue most-significant 36-bit word
     * @param lsValue least-significant 36-bit word
     * @return true if the value is negative zero, false otherwise
     */
    public static boolean isNegativeZero(final long msValue,
                                         final long lsValue) {
        return (msValue == Word36.NEGATIVE_ZERO) && (lsValue == Word36.NEGATIVE_ZERO);
    }

    /**
     * Tests 72-bit ones-complement value to see if it is positive
     * @param msValue most-significant 36-bit word
     * @param lsValue least-significant 36-bit word
     * @return true if the value is positive, false otherwise
     */
    public static boolean isPositive(final long msValue,
                                     final long lsValue) {
        return (msValue & Word36.NEGATIVE_BIT) == 0;
    }

    /**
     * Tests 72-bit ones-complement value to see if it is positive zero
     * @param msValue most-significant 36-bit word
     * @param lsValue least-significant 36-bit word
     * @return true if the value is positive zero, false otherwise
     */
    public static boolean isPositiveZero(final long msValue,
                                         final long lsValue) {
        return (msValue == Word36.POSITIVE_ZERO) && (lsValue == Word36.POSITIVE_ZERO);
    }

    /**
     * Tests 72-bit ones-complement value to see if it is zero
     * @param msValue most-significant 36-bit word
     * @param lsValue least-significant 36-bit word
     * @return true if the value is zero, false otherwise
     */
    public static boolean isZero(final long msValue,
                                 final long lsValue) {
        return isPositiveZero(msValue, lsValue) || isNegativeZero(msValue, lsValue);
    }

    /**
     * Adds two 72-bit ones-complement values
     * @param addend1 array containing the first addend
     * @param addend1Offset offset into the addend1 array where the value starts
     * @param addend2 array containing the second addend
     * @param addend2Offset offset into the addend2 array where the value starts
     * @param destination array where the result will be stored
     * @param destinationOffset offset into the destination array where the result will be stored
     * @return flags indicating the result of the addition
     */
    public static Word36.Flags add(final long[] addend1,
                                   final int addend1Offset,
                                   final long[] addend2,
                                   final int addend2Offset,
                                   final long[] destination,
                                   final int destinationOffset) {
        if (isNegativeZero(addend1, addend1Offset) && isNegativeZero(addend2, addend2Offset)) {
            // Special case - if both operands are negative zero, the result is negative zero
            destination[destinationOffset] = Word36.NEGATIVE_ZERO;
            destination[destinationOffset + 1] = Word36.NEGATIVE_ZERO;
            return new Word36.Flags();
        } else {
            // Convert addends to twos-complement, do the addition, and convert the result to ones-complement
            BigInteger native1 = getTwosComplement(addend1, addend1Offset);
            BigInteger native2 = getTwosComplement(addend2, addend2Offset);
            BigInteger sum = native1.add(native2);
            getOnesComplement(sum, destination, destinationOffset);

            boolean neg1 = isNegative(addend1, addend1Offset);
            boolean neg2 = isNegative(addend2, addend2Offset);
            boolean negRes = isNegative(destination, destinationOffset);

            boolean carry = sum.compareTo(BigInteger.ZERO) < 0 ? (neg1 && neg2) : (neg1 || neg2);
            boolean overflow = (neg1 == neg2) && (neg1 != negRes);
            return new Word36.Flags(carry, overflow);
        }
    }

    /**
     * Divide two 72-bit integer values
     * @param dividend array containing the dividend
     * @param dividendOffset offset into the dividend array
     * @param divisor array containing the divisor
     * @param divisorOffset offset into the divisor array
     * @param quotient array to store the 72-bit quotient
     * @param quotientOffset offset into the quotient array
     * @param remainder array to store the 72-bit remainder
     * @param remainderOffset offset into the remainder array
     */
    public static void divide(final long[] dividend,
                              final int dividendOffset,
                              final long[] divisor,
                              final int divisorOffset,
                              final long[] quotient,
                              final int quotientOffset,
                              final long[] remainder,
                              final int remainderOffset) {
        var nativeDividend = getTwosComplement(dividend, dividendOffset);
        var nativeDivisor = getTwosComplement(divisor, divisorOffset);
        var results = nativeDividend.divideAndRemainder(nativeDivisor);
        getOnesComplement(results[0], quotient, quotientOffset);
        getOnesComplement(results[1], remainder, remainderOffset);
    }

    /**
     * Divide two 72-bit integer values
     * @param dividend array containing the dividend
     * @param dividendOffset offset into the dividend array
     * @param divisor array containing the divisor
     * @param divisorOffset offset into the divisor array
     * @param quotient array to store the 36-bit quotient
     * @param quotientOffset offset into the quotient array
     * @param remainder array to store the 36-bit remainder
     * @param remainderOffset offset into the remainder array
     */
    public static void divideShort(final long[] dividend,
                                   final int dividendOffset,
                                   final long[] divisor,
                                   final int divisorOffset,
                                   final long[] quotient,
                                   final int quotientOffset,
                                   final long[] remainder,
                                   final int remainderOffset) {
        var nativeDividend = getTwosComplement(dividend, dividendOffset);
        var nativeDivisor = getTwosComplement(divisor, divisorOffset);
        var results = nativeDividend.divideAndRemainder(nativeDivisor);
        getOnesComplementShort(results[0], quotient, quotientOffset);
        getOnesComplementShort(results[1], remainder, remainderOffset);
    }

    /**
     * Multiplies two 36-bit ones-complement values, producing a 72-bit ones-complement result
     * @param factor1 factor for multiplication
     * @param factor2 factor for multiplication
     * @param product array to store the 72-bit result
     * @param productOffset offset into the product array where the result will be stored
     */
    public static void multiply(final long factor1,
                                final long factor2,
                                final long[] product,
                                final int productOffset) {
        var native1 = getTwosComplement(Word36.isNegative(factor1) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO, factor1);
        var native2 = getTwosComplement(Word36.isNegative(factor2) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO, factor2);
        getOnesComplement(native1.multiply(native2),
                          product,
                          productOffset);
    }

    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

//    /**
//     * Adds two BigInteger operands presuming they represent a 72-bit signed integer value
//     */
//    public static StaticAdditionResult add(
//        final BigInteger addend1,
//        final BigInteger addend2
//    ) {
//        BigInteger result = addSimple(addend1, addend2);
//        if (!result.and(CARRY_BIT).equals(BigInteger.ZERO)) {
//            result = result.and(BIT_MASK).add(BigInteger.ONE);
//        }
//
//        boolean neg1 = isNegative(addend1);
//        boolean neg2 = isNegative(addend2);
//        boolean negRes = isNegative(result);
//
//        boolean carry = result.compareTo(BigInteger.ZERO) < 0 ? (neg1 && neg2) : (neg1 || neg2);
//        boolean overflow = (neg1 == neg2) && (neg1 != negRes);
//        return new StaticAdditionResult(carry, overflow, result);
//    }
//
//    /**
//     * Simple addition of 72-bit signed integers for the case where we don't care about carry or overflow
//     */
//    private static BigInteger addSimple(
//        final BigInteger addend1,
//        final BigInteger addend2
//    ) {
//        if (addend1.equals(NEGATIVE_ZERO) && addend2.equals(NEGATIVE_ZERO)) {
//            return NEGATIVE_ZERO;
//        }
//
//        BigInteger native1 = getTwosComplement(addend1);
//        BigInteger native2 = getTwosComplement(addend2);
//        return getOnesComplement(native1.add(native2));
//    }
//
    /**
     * Compares two values
     * Returns -1 if operand1 < operand2,
     *          1 if operand1 > operand2,
     *          0 if they are equal
     * Remember that negative zero is less than positive zero.
     */
    public static int compare(
        final long leftMSW,
        final long leftLSW,
        final long rightMSW,
        final long rightLSW
    ) {
        if (isNegativeZero(leftMSW, leftLSW) && isPositiveZero(rightMSW, rightLSW)) {
            return -1;
        } else if (isPositiveZero(leftMSW, leftLSW) && isNegativeZero(rightMSW, rightLSW)) {
            return 1;
        } else {
            var left = getTwosComplement(leftMSW, leftLSW);
            var right = getTwosComplement(rightMSW, rightLSW);
            return left.compareTo(right);
        }
    }

//    /**
//     * Divide two 72-bit integer values
//     */
//    public static StaticDivisionResult divide(
//        final BigInteger dividend,
//        final BigInteger divisor
//    ) {
//        BigInteger[] tempResults = getTwosComplement(dividend).divideAndRemainder(getTwosComplement(divisor));
//        return new StaticDivisionResult(getOnesComplement(tempResults[0]), getOnesComplement(tempResults[1]));
//    }
//
//    /**
//     * Extends the sign of an arbitrarily-sized value, to the full 72 bits
//     */
//    public static BigInteger extendSign(
//        final BigInteger operand,
//        final int fieldSize
//    ) {
//        BigInteger signBitMask = BigInteger.ONE.shiftLeft(fieldSize - 1);
//        if (operand.and(signBitMask).equals(BigInteger.ZERO)) {
//            return operand;
//        } else {
//            BigInteger fieldBitMask = BigInteger.ONE.shiftLeft(fieldSize).subtract(BigInteger.ONE);
//            BigInteger fieldNotMask = BIT_MASK.xor(fieldBitMask);
//            return operand.or(fieldNotMask);
//        }
//    }
//
//    /**
//     * Arithmetic multiplication - operands and result are ones-complement
//     */
//    public static StaticMultiplicationResult multiply(
//        final BigInteger factor1,
//        final BigInteger factor2
//    ) {
//        BigInteger tcResult = getTwosComplement(factor1).multiply(getTwosComplement(factor2));
//        boolean overflow;
//        if (tcResult.compareTo(BigInteger.ZERO) < 0) {
//            //  negative result, should have leading 1's
//            overflow = !tcResult.and(NOT_BIT_MASK).equals(NOT_BIT_MASK);
//        } else {
//            //  positive result, should have leading 0's
//            overflow = !tcResult.and(NOT_BIT_MASK).equals(BigInteger.ZERO);
//        }
//
//        BigInteger result = getOnesComplement(tcResult).and(BIT_MASK);
//        return new StaticMultiplicationResult(overflow, result);
//    }
//
//    /**
//     * Arithmetic inverse operation - operand and result are both ones-complement 72-bit integers
//     */
//    public static BigInteger negate(
//        final BigInteger operand
//    ) {
//        return operand.not().and(BIT_MASK);
//    }
//
//
//    //  Logical Operations ---------------------------------------------------------------------------------------------------------
//
//    /**
//     * Logical AND operation (in this context, logical means bitwise)
//     * @param operand1 left hand operand
//     * @param operand2 right hand operand
//     * @return bitwise AND of the two operands
//     */
//    public static BigInteger logicalAnd(
//        final BigInteger operand1,
//        final BigInteger operand2
//    ) {
//        return operand1.and(operand2);
//    }
//
//    /**
//     * Logical NOT operation (in this context, logical means bitwise)
//     * @param operand value to be affected
//     * @return bitwise NOT of the given value
//     */
//    public static BigInteger logicalNot(
//        final BigInteger operand
//    ) {
//        return operand.not();
//    }
//
//    /**
//     * Logical OR operation (in this context, logical means bitwise)
//     * @param operand1 left hand operand
//     * @param operand2 right hand operand
//     * @return bitwise OR of the two operands
//     */
//    public static BigInteger logicalOr(
//        final BigInteger operand1,
//        final BigInteger operand2
//    ) {
//        return operand1.or(operand2);
//    }
//
//    /**
//     * Logical XOR operation (in this context, logical means bitwise)
//     * @param operand1 left hand operand
//     * @param operand2 right hand operand
//     * @return bitwise XOR of the two operands
//     */
//    public static BigInteger logicalXor(
//        final BigInteger operand1,
//        final BigInteger operand2
//    ) {
//        return operand1.xor(operand2);
//    }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

    /**
     * Does an algebraic shift left - the sign bit is never altered.
     * @param operand1 most significant 36 bits of the value to be shifted
     * @param operand2 least significant 36 bits of the value to be shifted
     * @param count number of bits to be shifted
     * @param result 2-element array containing the result of the shift operation.
     */
    public static void leftShiftAlgebraic(
        final long operand1,
        final long operand2,
        final int count,
        final Long[] result
    ) {
        if (count < 0) {
            rightShiftAlgebraic(operand1, operand2, -count, result);
        } else if (count == 0) {
            result[0] = operand1;
            result[1] = operand2;
        } else if (count >= 71) {
            result[0] = operand1 & 0_400000_000000L;
            result[1] = 0L;
        } else {
            long res;
            if (count < 36) {
                res = (operand1 & 0_400000_000000L) | (((operand1 << count) | (operand2 >>> (36 - count))) & 0_377777_777777L);
                result[0] = res;
                res = (operand2 << count) & 0_777777_777777L;
                result[1] = res;
            } else {
                res = (operand1 & 0_400000_000000L) | ((operand2 << (count - 36)) & 0_377777_777777L);
                result[0] = res;
                result[1] = 0L;
            }
        }
    }

    /**
     * Shifts the given 72-bit value left, with bit[0] rotating to bit[71] at each iteration.
     * @param operand1 most significant 36 bits of the value to be shifted
     * @param operand2 least significant 36 bits of the value to be shifted
     * @param count number of bits to be shifted
     * @param result 2-element array containing the result of the shift operation.
     */
    public static void leftShiftCircular(
        final long operand1,
        final long operand2,
        final int count,
        final Long[] result
    ) {
        if (count < 0) {
            rightShiftCircular(operand1, operand2, -count, result);
        } else {
            result[0] = operand1;
            result[1] = operand2;
            int actualCount = (count % 72);
            while (actualCount > 0) {
                var bit0 = (result[0] & 0_400000_000000L) == 0 ? 0 : 1;
                var bit36 = (result[1] & 0_400000_000000L) == 0 ? 0 : 1;
                result[0] = ((result[0] << 1) | bit36) & 0_777777_777777L;
                result[1] = ((result[1] << 1) | bit0) & 0_777777_777777L;
                actualCount--;
            }
        }
    }

    /**
     * Shifts the given 72-bit value left by a number of bits
     * @param operand1 most significant 36 bits of the value to be shifted
     * @param operand2 least significant 36 bits of the value to be shifted
     * @param count number of bits to be shifted
     * @param result 2-element array containing the result of the shift operation.
     */
    public static void leftShiftLogical(
        final long operand1,
        final long operand2,
        final int count,
        final Long[] result
    ) {
        if (count < 0) {
            rightShiftLogical(operand1, operand2, -count, result);
        } else if (count == 0) {
            result[0] = operand1;
            result[1] = operand2;
        } else if (count >= 72) {
            result[0] = 0L;
            result[1] = 0L;
        } else {
            long res;
            if (count < 36) {
                res = ((operand1 << count) | (operand2 >>> (36 - count))) & 0_777777_777777L;
                result[0] = res;
                res = (operand2 << count) & 0_777777_777777L;
                result[1] = res;
            } else {
                res = (operand2 << (count - 36)) & 0_777777_777777L;
                result[0] = res;
                result[1] = 0L;
            }
        }
    }

    /**
     * Does an algebraic shift right - this means the sign bit is always preserved as well as being shifted to the right.
     * @param operand1 most significant 36 bits of the value to be shifted
     * @param operand2 least significant 36 bits of the value to be shifted
     * @param count number of bits to be shifted
     */
    public static void rightShiftAlgebraic(
        final long operand1,
        final long operand2,
        final int count,
        final Long[] result
    ) {
        if (count < 0) {
            leftShiftAlgebraic(operand1, operand2, -count, result);
        } else if (count == 0) {
            result[0] = operand1;
            result[1] = operand2;
        } else if (count >= 71) {
            long fill = (operand1 & 0_400000_000000L) != 0 ? 0_777777_777777L : 0L;
            result[0] = fill;
            result[1] = fill;
        } else {
            long fill = (operand1 & 0_400000_000000L) != 0 ? 0_777777_777777L : 0L;
            long res;
            if (count < 36) {
                res = ((operand1 >> count) | (fill << (36 - count))) & 0_777777_777777L;
                result[0] = res;
                res = ((operand2 >>> count) | ((operand1 & 0_777777_777777L) << (36 - count))) & 0_777777_777777L;
                result[1] = res;
            } else {
                res = fill;
                result[0] = res;
                res = ((operand1 >> (count - 36)) | (fill << (72 - count))) & 0_777777_777777L;
                result[1] = res;
            }
        }
    }

    /**
     * Shifts the given 72-bit value right, with bit[71] rotating to bit[0] at each iteration.
     * @param operand1 most significant 36 bits of the value to be shifted
     * @param operand2 least significant 36 bits of the value to be shifted
     * @param count number of bits to be shifted
     * @param result 2-element array containing the result of the shift operation.
     */
    public static void rightShiftCircular(
        final long operand1,
        final long operand2,
        final int count,
        final Long[] result
    ) {
        if (count < 0) {
            leftShiftCircular(operand1, operand2, -count, result);
        } else {
            result[0] = operand1;
            result[1] = operand2;
            int actualCount = (count % 72);
            while (actualCount > 0) {
                var bit72 = result[1] & 0x1;
                var bit36 = result[0] & 0x1;
                result[0] = (result[0] >> 1) | (bit72 << 35);
                result[1] = (result[1] >> 1) | (bit36 << 35);
                actualCount--;
            }
        }
    }

    /**
     * Shifts the given 72-bit value right by a number of bits
     * @param operand1 most significant 36 bits of the value to be shifted
     * @param operand2 least significant 36 bits of the value to be shifted
     * @param count number of bits to be shifted
     * @param result 2-element array containing the result of the shift operation.
     */
    public static void rightShiftLogical(
        final long operand1,
        final long operand2,
        final int count,
        final Long[] result
    ) {
        if (count < 0) {
            leftShiftLogical(operand1, operand2, -count, result);
        } else if (count == 0) {
            result[0] = operand1;
            result[1] = operand2;
        } else if (count >= 72) {
            result[0] = 0L;
            result[1] = 0L;
        } else {
            long res;
            if (count < 36) {
                res = (operand1 >>> count) & 0_777777_777777L;
                result[0] = res;
                res = ((operand2 >>> count) | (operand1 << (36 - count))) & 0_777777_777777L;
                result[1] = res;
            } else {
                res = (operand1 >>> (count - 36)) & 0_777777_777777L;
                result[1] = res;
                result[0] = 0L;
            }
        }
    }


//    //  Conversions ----------------------------------------------------------------------------------------------------------------

//    /**
//     * Populates this object with quarter-words derived from the ASCII characters in the source string.
//     * If the string does not contain at least 8 characters, we pad the resulting output with blanks as necessary.
//     * Any characters in the string beyond the eighth are ignored.
//     * @param source string to be converted
//     * @return converted data
//     */
//    public static DoubleWord36 stringToWordASCII(
//        final String source
//    ) {
//        String padded = source + "       ";
//        String s1 = padded.substring(0, 4);
//        String s2 = padded.substring(4, 8);
//        var w1 = Word36.stringToWordASCII(s1);
//        var w2 = Word36.stringToWordASCII(s2);
//        return new DoubleWord36(w1, w2);
//    }
//
//    /**
//     * Populates this object with sixth-words representing the fieldata characters derived from the ASCII characters
//     * in the source string. If the string does not contain at least 12 characters, we pad the resulting output with
//     * blanks as necessary. Any characters in the string beyond the twelfth are ignored.
//     * @param source string to be converted
//     * @return converted data
//     */
//    public static DoubleWord36 stringToWordFieldata(
//        final String source
//    ) {
//        String padded = source + "           ";
//        String s1 = padded.substring(0, 6);
//        String s2 = padded.substring(6, 12);
//        var w1 = Word36.stringToWordFieldata(s1);
//        var w2 = Word36.stringToWordFieldata(s2);
//        return new DoubleWord36(w1, w2);
//    }
//
//
//    //  Formatting for display -----------------------------------------------------------------------------------------------------
//
//    /**
//     * Converts to two single word objects.
//     * result[0] is the high value, result[1] is the low value
//     */
//    public static Word36[] getWords(
//        final BigInteger value
//    ) {
//        Word36[] words = new Word36[2];
//        words[0] = new Word36(value.shiftRight(36).longValue());
//        words[1] = new Word36(value.and(SHORT_BIT_MASK).longValue());
//        return words;
//    }
//
//    /**
//     * Interprets the given 36-bit value as a sequence of 12 Octal digits, and produces those characters as a result
//     */
//    public static String toOctal(
//        final BigInteger value
//    ) {
//        return String.format("%024o", value);
//    }
//
//    /**
//     * Interprets the given 72-bit value as a sequence of 8 ASCII characters, and produces those characters as a result
//     */
//    public static String toStringFromASCII(
//        final BigInteger value
//    ) {
//        Word36[] words = getWords(value);
//        return String.format("%s%s",
//                             Word36.toStringFromASCII(words[0]._value),
//                             Word36.toStringFromASCII(words[1]._value));
//    }
//
//    /**
//     * Interprets the given 72-bit value as a sequence of 12 Fieldata characters, and produces those characters as a result
//     */
//    public static String toStringFromFieldata(
//        final BigInteger value
//    ) {
//        Word36[] words = getWords(value);
//        return String.format("%s%s",
//                             Word36.toStringFromFieldata(words[0]._value),
//                             Word36.toStringFromFieldata(words[1]._value));
//    }
}
