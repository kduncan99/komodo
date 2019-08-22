/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.math.BigInteger;
import com.kadware.komodo.baselib.exceptions.DivideByZeroException;

import java.util.Arrays;

/**
 * Static methods for doing ones-complement arithmetic according to the architecture documents
 * All math done here involves ones-complement values wrapped in native discrete integers of equal or larger size,
 * and native (presumably twos-complement) values stored in appropriate containers (discrete or otherwise).
 * <p>
 * 36-bit signed integers are stored in 64-bit (or greater) integers, with the significant value right-justified,
 * and the upper (most-significant) bits of the containing entity set to zero.
 * <p>
 * 72-bit signed integers are stored in two-element arrays of longs, where ar[0] contains the most-significant
 * 36 bits, and ar[1] contains the least-significant 36 bits.
 */
@SuppressWarnings("Duplicates")
public class OnesComplement {

//    public static class Add36Result {
//        public long _sum = 0;
//        public boolean _carry = false;
//        public boolean _overflow = false;
//    }
//
//    public static class Add72Result {
//        public long[] _sum = { 0, 0 };
//        public boolean _carry = false;
//        public boolean _overflow = false;
//    }
//
//    public static class DivideResult {
//        public Word36[] _quotient = { 0, 0 };
//        public Word36[] _remainder = { 0, 0 };
//        public boolean _overflow = false;
//    }
//
//    public static class OnesComplement36Result {
//        public Word36 _result = null;
//        public boolean _overflow = false;
//    }
//
//    public static class OnesComplement72Result {
//        public Word36[] _result = null;
//        public boolean _overflow;
//    }
//
//    public static final long BIT_MASK_12 = 0_7777L;
//    public static final long BIT_MASK_18 = 0_777777L;
//    public static final long BIT_MASK_36 = 0_777777_777777L;
//    public static final long CARRY_BIT_12 = 0_1_0000L;
//    public static final long CARRY_BIT_18 = 0_1_000000L;
//    public static final long CARRY_BIT_36 = 0_1_000000_000000L;
//    public static final long LARGEST_POSITIVE_INTEGER_36 = 0_377777_777777L;
//    public static final long NEGATIVE_BIT_36 = 0_400000_000000L;
//    public static final long NEGATIVE_ZERO_12 = 0_7777L;
//    public static final long NEGATIVE_ZERO_18 = 0_777777L;
//    public static final long NEGATIVE_ZERO_36 = 0_777777_777777L;
//    public static final long POSITIVE_ZERO_12 = 0L;
//    public static final long POSITIVE_ZERO_18 = 0L;
//    public static final long POSITIVE_ZERO_36 = 0L;
//    public static final long SMALLEST_NEGATIVE_INTEGER_36 = 0_400000_000000L;
//
//    public static final long[] BIT_MASK_72 = { BIT_MASK_36, BIT_MASK_36 };
//    public static final long[] LARGEST_POSITIVE_INTEGER_72 = { 0_377777_777777L, 0_777777_777777L };
//    public static final long[] NEGATIVE_BIT_72 = { 0_400000_000000L, 0L };
//    public static final long[] NEGATIVE_ZERO_72 = { BIT_MASK_36, BIT_MASK_36 };
//    public static final long[] POSITIVE_ZERO_72 = { 0L, 0L };
//    public static final long[] SMALLEST_NEGATIVE_INTEGER_72 = { 0_400000_000000L, 0L };
//
//    public static final BigInteger BI_BIT_MASK_36 = BigInteger.valueOf(BIT_MASK_36);
//    public static final BigInteger BI_BIT_MASK_72 = new BigInteger("0777777777777777777777777", 8);
//    public static final BigInteger BI_LARGEST_POSITIVE_INTEGER_72 = new BigInteger("0377777777777777777777777", 8);
//    public static final BigInteger BI_SMALLEST_NEGATIVE_INTEGER_72 = BI_LARGEST_POSITIVE_INTEGER_72.negate();
//
//
//    /**
//     * Retrieves the absolute value of the 36-bit signed integer presented as the operand
//     * @param operand ones-complement signed integer
//     * @return 36-bit ones-complement integer, the absolute value of the operand parameter
//     */
//    public static long absoluteValue36(
//        final Word36 operand
//    ) {
//        return isNegative36(operand) ? negate36(operand) : operand;
//    }
//
//    /**
//     * Retrieves the absolute value of the 72-bit signed integer presented as the operand
//     * @param operand ones-complement signed integer
//     * @param result where we store the result
//     */
//    public static void absoluteValue72(
//        final long[] operand,
//        final long[] result
//    ) {
//        copy72(operand, result);
//        if (isNegative72(result)) {
//            negate72(result, result);
//        }
//    }
//
//    /**
//     * Performs ones-complement addition on two 12-bit signed integers, with no regard to carry or overflow.
//     * @param addend1 addend
//     * @param addend2 another addend
//     * @return result of the operation
//     */
//    public static long add12Simple(
//        final long addend1,
//        final long addend2
//    ) {
//        long result = addend1 + addend2;
//        if ((result & CARRY_BIT_12) != 0) {
//            result &= BIT_MASK_12;
//            ++result;
//        }
//
//        if ((result == NEGATIVE_ZERO_12) && (addend1 != addend2)) {
//            result = POSITIVE_ZERO_12;
//        }
//
//        return result;
//    }
//
//    /**
//     * Performs ones-complement addition on two 18-bit signed integers, with no regard to carry or overflow.
//     * @param addend1 addend
//     * @param addend2 another addend
//     * @return result of the operation
//     */
//    public static long add18Simple(
//        final long addend1,
//        final long addend2
//    ) {
//        long result = addend1 + addend2;
//        if ((result & CARRY_BIT_18) != 0) {
//            result &= BIT_MASK_18;
//            ++result;
//        }
//
//        if ((result == NEGATIVE_ZERO_18) && (addend1 != addend2)) {
//            result = POSITIVE_ZERO_18;
//        }
//
//        return result;
//    }
//
//    /**
//     * Performs ones-complement addition on two 36-bit signed integers.
//     * See the hardware docs sections on general ones-complement math and the binary arithmetic instructions.
//     * @param addend1 addend
//     * @param addend2 another addend
//     * @param result where we store the result of the operation along with carry and overflow flags
//     */
//    public static void add36(
//        final long addend1,
//        final long addend2,
//        Add36Result result
//    ) {
//        result._sum = add36Simple(addend1, addend2);
//
//        boolean neg1 = isNegative36(addend1);
//        boolean neg2 = isNegative36(addend2);
//        boolean negRes = isNegative36(result._sum);
//        result._carry = negRes ? (neg1 && neg2) : (neg1 || neg2);
//        result._overflow = (neg1 == neg2) && (neg1 != negRes);
//    }
//
//    /**
//     * Performs ones-complement addition on two 36-bit signed integers, with no regard to carry or overflow.
//     * @param addend1 addend
//     * @param addend2 another addend
//     * @return result of the operation
//     */
//    public static long add36Simple(
//        final long addend1,
//        final long addend2
//    ) {
//        long result = addend1 + addend2;
//        if ((result & CARRY_BIT_36) != 0) {
//            result &= BIT_MASK_36;
//            ++result;
//        }
//
//        if (isNegativeZero36(result) && (addend1 != addend2)) {
//            result = POSITIVE_ZERO_36;
//        }
//
//        return result;
//    }
//
//    /**
//     * Performs ones-complement addition on two 72-bit signed integers.
//     * See the hardware docs sections on general ones-complement math and the binary arithmetic instructions.
//     * @param addend1 addend
//     * @param addend2 another addend
//     * @param result where we store the result and the carry and overflow flags
//     */
//    public static void add72(
//        final long[] addend1,
//        final long[] addend2,
//        Add72Result result
//    ) {
//        add72Simple(addend1, addend2, result._sum);
//
//        boolean neg1 = isNegative72(addend1);
//        boolean neg2 = isNegative72(addend2);
//        boolean negRes = isNegative72(result._sum);
//        result._carry = negRes ? (neg1 && neg2) : (neg1 || neg2);
//        result._overflow = (neg1 == neg2) && (neg1 != negRes);
//    }
//
//    /**
//     * Performs ones-complement addition on two 72-bit signed integers, with no regard to carry or overflow.
//     * @param addend1 addend
//     * @param addend2 another addend
//     * @param result where we store the result
//     */
//    public static void add72Simple(
//        final long[] addend1,
//        final long[] addend2,
//        long[] result
//    ) {
//        result[1] = addend1[1] + addend2[1];
//        boolean midCarry = (result[1] & CARRY_BIT_36) != 0;
//        if (midCarry) {
//            result[1] &= BIT_MASK_36;
//        }
//
//        result[0] = addend1[0] + addend2[0];
//        if (midCarry) {
//            ++result[0];
//        }
//
//        boolean endCarry = (result[0] & CARRY_BIT_36) != 0;
//        if (endCarry) {
//            result[0] &= BIT_MASK_36;
//        }
//
//        if (endCarry) {
//            ++result[1];
//        }
//
//        if (isNegativeZero72(result) && !isEqual72(addend1, addend2)) {
//            copy72(POSITIVE_ZERO_72, result);
//        }
//    }
//
//    /**
//     * Compares two 36-bit signed ones-complement operands
//     * @param operand1 first operand
//     * @param operand2 second operand
//     * @return -1 if operand1 < operand2, 0 if operand1 == operand2, 1 if operand1 > operand2
//     */
//    public static int compare36(
//        final long operand1,
//        final long operand2
//    ) {
//        return Long.compare( getNative36( operand1 ), getNative36( operand2 ) );
//    }
//
//    /**
//     * Compares two 72-bit signed ones-complement operands
//     * @param operand1 first operand
//     * @param operand2 second operand
//     * @return -1 if operand1 < operand2, 0 if operand1 == operand2, 1 if operand1 > operand2
//     */
//    public static int compare72(
//        final long[] operand1,
//        final long[] operand2
//    ) {
//        BigInteger native1 = getNative72(operand1);
//        BigInteger native2 = getNative72(operand2);
//        return native1.compareTo(native2);
//    }
//
//    /**
//     * Copies the 72-bit source to the 72-bit destination
//     * @param destination destination
//     * @param source source
//     */
//    public static void copy72(
//        final long[] source,
//        final long[] destination
//    ) {
//        destination[0] = source[0] & BIT_MASK_36;
//        destination[1] = source[1] & BIT_MASK_36;
//    }
//
//    /**
//     * Divides the 72-bit dividend by the 72-bit divisor.
//     * @param dividend 72-bit dividend
//     * @param divisor 72-bit divisor
//     * @param result where we store the result and the overflow flag
//     * @throws DivideByZeroException if divisor is zero
//     */
//    public static void divide72(
//        final long[] dividend,
//        final long[] divisor,
//        DivideResult result
//    ) throws DivideByZeroException {
//        if (isZero72(divisor)) {
//            throw new DivideByZeroException("Divisor is zero");
//        }
//
//        BigInteger dividendBig = getNative72(dividend);
//        BigInteger divisorBig = getNative72(divisor);
//        BigInteger quotient = dividendBig.divide(divisorBig);
//        BigInteger remainder = dividendBig.remainder(divisorBig);
//
//        OnesComplement72Result ocresult = new OnesComplement72Result();
//        getOnesComplement72(quotient, ocresult);
//        copy72(ocresult._result, result._quotient);
//        result._overflow = ocresult._overflow;
//
//        getOnesComplement72(remainder, ocresult);
//        copy72(ocresult._result, result._remainder);
//        if (ocresult._overflow) {
//            result._overflow = true;
//        }
//    }
//
//    /**
//     * Converts ones-complement 36-bit signed integer to native format (likely twos-complement)
//     * @param operand 36-bit signed integer to be converted
//     * @return twos-complement integer
//     */
//    public static long getNative36(
//        final long operand
//    ) {
//        return (isNegative36(operand)) ? -((~operand) & BIT_MASK_36) : operand;
//    }
//
//    /**
//     * Converts a ones-complement number, presented as the most-significant and least-significant
//     * 36-bit values wrapped in two longs, to a BigInteger containing the corresponding value.
//     * It may or may not be internally represented as twos-complement, but we treat it that way.
//     * @param operand 72-bit ones-complement signed integer
//     * @return BigInteger containing the 71-bit signed integer value
//     */
//    public static BigInteger getNative72(
//        final long[] operand
//    ) {
//        if (isNegative72(operand)) {
//            long msNot = negate36(operand[0]);
//            long lsNot = negate36(operand[1]);
//            return BigInteger.valueOf(msNot).shiftLeft(36).add(BigInteger.valueOf(lsNot)).negate();
//        } else {
//            long msWord = operand[0] & BIT_MASK_36;
//            long lsWord = operand[1] & BIT_MASK_36;
//            return BigInteger.valueOf(msWord).shiftLeft(36).add(BigInteger.valueOf(lsWord));
//        }
//    }
//
//    /**
//     * Converts native signed integer to ones-complement 36-bit signed integer
//     * @param operand twos-complement (native) signed integer
//     * @param result where we store the result and the overflow flag
//     */
//    public static void getOnesComplement36(
//        final long operand,
//        OnesComplement36Result result
//    ) {
//        if (operand < 0) {
//            result._result = (~(-operand)) & BIT_MASK_36;
//            result._overflow = operand < -( 0_377777_777777L );
//        } else {
//            result._result = operand & BIT_MASK_36;
//            result._overflow = operand > LARGEST_POSITIVE_INTEGER_36;
//        }
//    }
//
//    /**
//     * Converts native big integer to ones-coplement 72-bit signed integer
//     * @param operand native signed integer to be converted
//     * @param result where we store the result and the overflow flag
//     */
//    public static void getOnesComplement72(
//        final BigInteger operand,
//        OnesComplement72Result result
//    ) {
//        if (operand.compareTo(BigInteger.ZERO) < 0) {
//            //  operand is negative
//            BigInteger compBig = operand.negate();
//            result._overflow = operand.compareTo(BI_SMALLEST_NEGATIVE_INTEGER_72) < 0;
//            result._result[0] = negate36(compBig.shiftRight(36).longValue());
//            result._result[1] = negate36(compBig.longValue());
//        } else {
//            result._overflow = operand.compareTo(BI_LARGEST_POSITIVE_INTEGER_72) > 0;
//            result._result[0] = operand.shiftRight(36).longValue() & BIT_MASK_36;
//            result._result[1] = operand.longValue() & BIT_MASK_36;
//        }
//    }
//
//    /**
//     * Indicates whether the two 72-bit operands are equal
//     * @param operand1 first operand
//     * @param operand2 second operand
//     * @return true if the operands are equal, else false
//     */
//    public static boolean isEqual72(
//        final long[] operand1,
//        final long[] operand2
//    ) {
//        return Arrays.equals(operand1, operand2);
//    }
//
//    /**
//     * Determines whether the given 36-bit signed ones-complement integer is negative
//     * @param operand operand to be tested
//     * @return true if the operand is negative, else false
//     */
//    public static boolean isNegative36(
//        final long operand
//    ) {
//        return (operand & NEGATIVE_BIT_36) != 0;
//    }
//
//    /**
//     * Determines whether the given 72-bit signed ones-complement value is negative
//     * @param operand operand to be tested
//     * @return true if the operand is negative, else false
//     */
//    public static boolean isNegative72(
//        final long[] operand
//    ) {
//        return isNegative36(operand[0]);
//    }
//
//    /**
//     * Determines whether the given 36-bit signed ones-complement integer is negative zero
//     * @param operand operand to be tested
//     * @return true if the operand is negative zero, else false
//     */
//    public static boolean isNegativeZero36(
//        final long operand
//    ) {
//        return operand == NEGATIVE_ZERO_36;
//    }
//
//    /**
//     * Determines whether the given 72-bit signed ones-complement integer is negative zero
//     * @param operand operand to be tested
//     * @return true if the operand is negative zero, else false
//     */
//    public static boolean isNegativeZero72(
//        final long[] operand
//    ) {
//        return Arrays.equals(operand, NEGATIVE_ZERO_72);
//    }
//
//    /**
//     * Determines whether the given 36-bit signed ones-complement integer is positive
//     * @param operand operand to be tested
//     * @return true if the operand is positive, else false
//     */
//    public static boolean isPositive36(
//        final long operand
//    ) {
//        return (operand & NEGATIVE_BIT_36) == 0;
//    }
//
//    /**
//     * Determines whether the given 72-bit signed ones-complement integer is positive
//     * @param operand operand to be tested
//     * @return true if the operand is positive, else false
//     */
//    public static boolean isPositive72(
//        final long[] operand
//    ) {
//        return (operand[0] & NEGATIVE_BIT_36) == 0;
//    }
//
//    /**
//     * Determines whether the given 36-bit signed ones-complement integer is positive zero
//     * @param operand operand to be tested
//     * @return true if the operand is positive zero, else false
//     */
//    public static boolean isPositiveZero36(
//        final long operand
//    ) {
//        return operand == 0;
//    }
//
//    /**
//     * Determines whether the given 72-bit signed ones-complement integer is positive zero
//     * @param operand operand to be tested
//     * @return true if the operand is positive zero, else false
//     */
//    public static boolean isPositiveZero72(
//        final long[] operand
//    ) {
//        return (operand[0] == 0) && (operand[1] == 0);
//    }
//
//    /**
//     * Determines whether the given 36-bit signed ones-complement integer is zero (negative or positive)
//     * @param operand operand to be tested
//     * @return true if the operand is zero, else false
//     */
//    public static boolean isZero36(
//        final long operand
//    ) {
//        return isPositiveZero36(operand) || isNegativeZero36(operand);
//    }
//
//    /**
//     * Determines whether the given 72-bit signed ones-complement integer is zero (negative or positive)
//     * @param operand operand to be tested
//     * @return true if the operand is zero, else false
//     */
//    public static boolean isZero72(
//        final long[] operand
//    ) {
//        return isPositiveZero72(operand) || isNegativeZero72(operand);
//    }
//
//    /**
//     * Shifts the operand left by {count} bits, with the msb shifting to the lsb
//     * @param operand value to be shifted
//     * @param count shift count
//     * @return result
//     */
//    public static long leftShiftCircular36(
//        final long operand,
//        final int count
//    ) {
//        //  Not the most efficient way to do it, but it will do for now
//        long result = operand;
//        for (int cx = 0; cx < count; ++cx) {
//            result <<= 1;
//            if ((result & CARRY_BIT_36) != 0) {
//                result &= BIT_MASK_36;
//                result |= 01;
//            }
//        }
//        return result;
//    }
//
//    /**
//     * Shifts the operand left by {count} bits, with the msb shifting to the lsb
//     * @param operand value to be shifted
//     * @param count shift count
//     * @param result where we store the 2-word result
//     */
//    public static void leftShiftCircular72(
//        final long[] operand,
//        final int count,
//        long[] result
//    ) {
//        //  Again, not very efficient.  But it works.
//        copy72(operand, result);
//        for (int cx = 0; cx < count; ++cx) {
//            result[0] <<= 1;
//            result[1] <<= 1;
//
//            if ((result[1] & CARRY_BIT_36) != 0) {
//                result[1] &= BIT_MASK_36;
//                result[0] |= 01;
//            }
//
//            if ((result[0] & CARRY_BIT_36) != 0) {
//                result[0] &= BIT_MASK_36;
//                result[1] |= 01;
//            }
//        }
//    }
//
//    /**
//     * Shifts the operand left by {count} bits, and returns the result
//     * @param operand value to be shifted
//     * @param count if less than zero, we default to zero
//     * @return resulting value
//     */
//    public static long leftShiftLogical36(
//        final long operand,
//        final int count
//    ) {
//        return (count <= 0) ? operand : (operand << count) & BIT_MASK_36;
//    }
//
//    /**
//     * Shifts the operand left by {count} bits, and returns the result
//     * @param operand value to be shifted
//     * @param count if less than zero, we default to zero
//     * @param result where we store the 2-word result
//     */
//    public static void leftShiftLogical72(
//        final long[] operand,
//        final int count,
//        final long[] result
//    ) {
//        if (count <= 0) {
//            copy72(operand, result);
//        } else {
//            BigInteger bi = new BigInteger(String.format("%o%012o", operand[0], operand[1]), 8).shiftLeft(count);
//            result[0] = bi.shiftRight(36).longValue() & BIT_MASK_36;
//            result[1] = bi.longValue() & BIT_MASK_36;
//        }
//    }
//
//    /**
//     * Multiplies two 36-bit ones-complement numbers, producing a 72-bit ones-complement result.
//     * @param factor1 operand
//     * @param factor2 operand
//     * @param product where we store the 2-word result
//     */
//    public static void multiply36(
//        final long factor1,
//        final long factor2,
//        long[] product
//    ) {
//        long absFactor1 = factor1;
//        boolean negative1 = false;
//        if (isNegative36(absFactor1)) {
//            absFactor1 = negate36(absFactor1);
//            negative1 = true;
//        }
//
//        long absFactor2 = factor2;
//        boolean negative2 = false;
//        if (isNegative36(absFactor2)) {
//            absFactor2 = negate36(absFactor2);
//            negative2 = true;
//        }
//
//        BigInteger biProduct = BigInteger.valueOf(absFactor1).multiply(BigInteger.valueOf(absFactor2));
//        if (negative1 != negative2) {
//            biProduct = biProduct.negate();
//        }
//
//        //  We can ignore the overflow flag here, because the factors are restricted to 36-bits each,
//        //  thus guaranteeing a result less than 72 bits (and therefore, no overflow).
//        OnesComplement72Result ocr = new OnesComplement72Result();
//        getOnesComplement72(biProduct, ocr);
//        copy72(ocr._result, product);
//    }
//
//    /**
//     * Returns the arithmetic inverse (the negative) of the given 12-bit signed integer.
//     * @param operand value to be inverted
//     * @return result
//     */
//    public static long negate12(
//        final long operand
//    ) {
//        return (~operand) & BIT_MASK_12;
//    }
//
//    /**
//     * Returns the arithmetic inverse (the negative) of the given 18-bit signed integer.
//     * @param operand value to be inverted
//     * @return result
//     */
//    public static long negate18(
//        final long operand
//    ) {
//        return (~operand) & BIT_MASK_18;
//    }
//
//    /**
//     * Returns the arithmetic inverse (the negative) of the given 36-bit signed integer.
//     * @param operand value to be inverted
//     * @return result
//     */
//    public static long negate36(
//        final long operand
//    ) {
//        return (~operand) & BIT_MASK_36;
//    }
//
//    /**
//     * Returns the arithmetic inverse (the negative) of the given 72-bit signed integer.
//     * @param operand value to be inverted
//     * @param result 2-word result
//     */
//    public static void negate72(
//        final long[] operand,
//        long[] result
//    ) {
//        result[0] = (~operand[0]) & BIT_MASK_36;
//        result[1] = (~operand[1]) & BIT_MASK_36;
//    }
//
//    /**
//     * Performs a right-shift algebraic on the 36-bit operand
//     * @param operand value to be shifted
//     * @param count number of bits to shift - if less than zero, we default to zero
//     * @return result of the shift
//     */
//    public static long rightShiftAlgebraic36(
//        final long operand,
//        final int count
//    ) {
//        if (count <= 0) {
//            return operand;
//        } else if (count >= 35) {
//            return isNegative36(operand) ? NEGATIVE_ZERO_36 : POSITIVE_ZERO_36;
//        } else {
//            long result = operand >> count;
//            if (isNegative36(operand)) {
//                //  create a mask right-aligned, with {count} bits set, then left-align it within 36 bits.
//                long mask = 1L << count;
//                mask -= 1;
//                mask <<= (36 - count);
//                result |= mask;
//            }
//            return result;
//        }
//    }
//
//    /**
//     * Performs a right-shift algebraic on the 72-bit operand
//     * @param operand value to be shifted
//     * @param count number of bits to shift
//     * @param result where we store the 2-word result
//     */
//    public static void rightShiftAlgebraic72(
//        final long[] operand,
//        final int count,
//        long[] result
//    ) {
//        if (count <= 0) {
//            copy72(operand, result);
//        } else if (count >= 71) {
//            if (isNegative72(operand)) {
//                copy72(NEGATIVE_ZERO_72, result);
//            } else {
//                copy72(POSITIVE_ZERO_72, result);
//            }
//        } else {
//            BigInteger biResult = new BigInteger(String.format("%o%012o", operand[0], operand[1]), 8).shiftRight(count);
//            if (isNegative72(operand)) {
//                //  create a mask right-aligned, with {count} bits set, then left-align it within 36 bits.
//                BigInteger biMask = BigInteger.ONE;
//                biMask = biMask.shiftLeft(count);
//                biMask = biMask.subtract(BigInteger.ONE);
//                biMask = biMask.shiftLeft(72 - count);
//                biResult = biResult.or(biMask);
//            }
//
//            result[0] = biResult.shiftRight(36).longValue() & BIT_MASK_36;
//            result[1] = biResult.longValue() & BIT_MASK_36;
//        }
//    }
//
//    /**
//     * Shifts the operand right by {count} bits, with the lsb shifting to the msb
//     * @param operand value to be shifted
//     * @param count shift count
//     * @return result
//     */
//    public static long rightShiftCircular36(
//        final long operand,
//        final int count
//    ) {
//        //  Not the most efficient way to do it, but it will do for now
//        long result = operand;
//        for (int cx = 0; cx < count; ++cx) {
//            boolean carry = (result & 01) != 0;
//            result >>= 1;
//            if (carry) {
//                result |= 0_400000_000000L;
//            }
//        }
//        return result;
//    }
//
//    /**
//     * Shifts the operand right by {count} bits, with the lsb shifting to the msb
//     * @param operand value to be shifted
//     * @param count shift count
//     * @param result where we store the 2-word result
//     */
//    public static void rightShiftCircular72(
//        final long[] operand,
//        final int count,
//        long[] result
//    ) {
//        //  Again, not very efficient.  But it works.
//        copy72(operand, result);
//        for (int cx = 0; cx < count; ++cx) {
//            boolean carryMid = (result[0] & 01) != 0;
//            boolean carryMsb = (result[1] & 01) != 0;
//            result[0] >>= 1;
//            result[1] >>= 1;
//
//            if (carryMid) {
//                result[1] |= 0_400000_000000L;
//            }
//
//            if (carryMsb) {
//                result[0] |= 0_400000_000000L;
//            }
//        }
//    }
//
//    /**
//     * Shifts the operand left by {count} bits, and returns the result
//     * @param operand value to be shifted
//     * @param count if less than zero, we default to zero
//     * @return result
//     */
//    public static long rightShiftLogical36(
//        final long operand,
//        final int count
//    ) {
//        return (count <= 0) ? operand : (operand >> count);
//    }
//
//    /**
//     * Shifts the operand left by {count} bits, and returns the result
//     * @param operand value to be shifted
//     * @param count if less than zero, we default to zero
//     * @param result where we store the 2-word result
//     */
//    public static void rightShiftLogical72(
//        final long[] operand,
//        final int count,
//        final long[] result
//    ) {
//        if (count <= 0) {
//            copy72(operand, result);
//        } else {
//            BigInteger bi = new BigInteger(String.format("%o%012o", operand[0], operand[1]), 8).shiftRight(count);
//            result[0] = bi.shiftRight(36).longValue() & BIT_MASK_36;
//            result[1] = bi.longValue() & BIT_MASK_36;
//        }
//    }
}
