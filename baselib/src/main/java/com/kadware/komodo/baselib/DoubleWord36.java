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

    public static class AdditionResult {
        public final boolean _carry;
        public final boolean _overflow;
        public final BigInteger _value;

        public AdditionResult(
            final boolean carry,
            final boolean overflow,
            final BigInteger value
        ) {
            _carry = carry;
            _overflow = overflow;
            _value = value;
        }
    }

    public static class DivisionResult {
        public final BigInteger _result;
        public final BigInteger _remainder;

        public DivisionResult(
            final BigInteger result,
            final BigInteger remainder
        ) {
            _result = result;
            _remainder = remainder;
        }
    }

    public static class MultiplicationResult {
        public final boolean _overflow;
        public final BigInteger _value;

        public MultiplicationResult(
            final boolean overflow,
            final BigInteger value
        ) {
            _overflow = overflow;
            _value = value;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constants
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final BigInteger CARRY_BIT        = BigInteger.valueOf(1).shiftLeft(72);
    public static final BigInteger SHORT_BIT_MASK   = BigInteger.valueOf(Word36.BIT_MASK);
    public static final BigInteger BIT_MASK         = SHORT_BIT_MASK.shiftLeft(36).or(SHORT_BIT_MASK);

    public static final BigInteger NEGATIVE_ONE     = BIT_MASK.add(BigInteger.ONE.negate());
    public static final BigInteger NEGATIVE_ZERO    = BIT_MASK;
    public static final BigInteger POSITIVE_ONE     = BigInteger.ONE;
    public static final BigInteger POSITIVE_ZERO    = BigInteger.ZERO;

    public static final DoubleWord36 DW36_NEGATIVE_ONE  = new DoubleWord36(NEGATIVE_ONE);
    public static final DoubleWord36 DW36_NEGATIVE_ZERO = new DoubleWord36(NEGATIVE_ZERO);
    public static final DoubleWord36 DW36_POSITIVE_ONE  = new DoubleWord36(POSITIVE_ONE);
    public static final DoubleWord36 DW36_POSITIVE_ZERO = new DoubleWord36(POSITIVE_ZERO);


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


    //  Miscellaneous --------------------------------------------------------------------------------------------------------------

    /**
     * Compares another DoubleWord36 to this one in the context of floating point operations.
     * @return -1 if this object is less than the comparable object,
     *          1 if this object is greater
     *          0 if the objects are equal
     */
    public int floatingPointCompareTo(
        final DoubleWord36 comp
    ) {
        //  If the signs are unequal, the positive value is greater.
        boolean thisNeg = isNegative();
        boolean thatNeg = comp.isNegative();
        if (thisNeg != thatNeg) {
            return thisNeg ? -1 : 1;
        }

        //  If the exponents are unequal the larger value is greater (unless we are negative, then the opposite is true)
        int thisExponent = _value.shiftRight(60).and(BigInteger.valueOf(03777)).intValue();
        int thatExponent = comp._value.shiftRight(60).and(BigInteger.valueOf(03777)).intValue();
        if (thisExponent != thatExponent) {
            if (thisNeg) {
                return thisExponent < thatExponent ? 1 : -1;
            } else {
                return thisExponent < thatExponent ? -1 : 1;
            }
        }

        //  If the mantissas are unequal, the larger is greater (unless we are negative...)
        long thisMantissa = _value.and(BigInteger.valueOf(0_000077_777777_777777_777777L)).longValue();
        long thatMantissa = comp._value.and(BigInteger.valueOf(0_000077_777777_777777_777777L)).longValue();
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

    public AdditionResult add(DoubleWord36 operand)             { return add(_value, operand._value); }
    public DivisionResult divide(DoubleWord36 divisor)          { return divide(_value, divisor._value); }
    public MultiplicationResult multiply(DoubleWord36 operand)  { return multiply(_value, operand._value); }
    public DoubleWord36 negate()                                { return new DoubleWord36(negate(_value)); }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    public DoubleWord36 logicalAnd(DoubleWord36 operand)    { return new DoubleWord36(logicalAnd(_value, operand._value)); }
    public DoubleWord36 logicalNot()                        { return new DoubleWord36(logicalNot(_value)); }
    public DoubleWord36 logicalOr(DoubleWord36 operand)     { return new DoubleWord36(logicalOr(_value, operand._value)); }
    public DoubleWord36 logicalXor(DoubleWord36 operand)    { return new DoubleWord36(logicalXor(_value, operand._value)); }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

//    public void leftShiftAlgebraic(int count)   { _value = leftShiftAlgebraic(_value, count); }
//    public void leftShiftCircular(int count)    { _value = leftShiftCircular(_value, count); }
    public DoubleWord36 leftShiftLogical(int count)     { return new DoubleWord36(leftShiftLogical(_value, count)); }
//    public void rightShiftAlgebraic(int count)  { _value = rightShiftAlgebraic(_value, count); }
//    public void rightShiftCircular(int count)   { _value = rightShiftCircular(_value, count); }
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
    public static boolean isNegative(BigInteger value)      { return (value.shiftRight(71).intValue() == 1); }
    public static boolean isNegativeZero(BigInteger value)  { return value.equals(NEGATIVE_ZERO); }
    public static boolean isPositive(BigInteger value)      { return !isNegative(value); }
    public static boolean isPositiveZero(BigInteger value)  { return value.equals(POSITIVE_ZERO); }
    public static boolean isZero(BigInteger value)          { return isPositiveZero(value) || isNegativeZero(value); }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    public static AdditionResult add(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        boolean neg1 = isNegative(operand1);
        boolean neg2 = isNegative(operand2);

        BigInteger result = addSimple(operand1, operand2);
        if (!result.and(CARRY_BIT).equals(BigInteger.ZERO)) {
            result = result.and(BIT_MASK).add(BigInteger.ONE);
        }

        boolean negRes = isNegative(result);

        boolean carry = result.compareTo(BigInteger.ZERO) < 1 ? (neg1 && neg2) : (neg1 || neg2);
        boolean overflow = (neg1 == neg2) && (neg1 != negRes);
        return new AdditionResult(carry, overflow, result);
    }

    public static BigInteger addSimple(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        if ((operand1 == NEGATIVE_ZERO) && (operand2 == NEGATIVE_ZERO)) {
            return NEGATIVE_ZERO;
        }

        BigInteger native1 = getTwosComplement(operand1);
        BigInteger native2 = getTwosComplement(operand2);
        return getOnesComplement(native1.add(native2));
    }

    public static DivisionResult divide(
        final BigInteger dividend,
        final BigInteger divisor
    ) {
        BigInteger[] tempResults = getTwosComplement(dividend).divideAndRemainder(getTwosComplement(divisor));
        return new DivisionResult(getOnesComplement(tempResults[0]), getOnesComplement(tempResults[1]));
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
     * Converts a ones-complement BigInteger operand to twos-complement
     */
    public static BigInteger getTwosComplement(
        final BigInteger operand
    ) {
        return isNegative(operand) ? negate(operand).negate() : operand;
    }

    /**
     * Arithmetic multiplication - operands and result are ones-complement
     */
    public static MultiplicationResult multiply(
        final BigInteger operand1,
        final BigInteger operand2
    ) {
        BigInteger tempResult = getTwosComplement(operand1).multiply(getTwosComplement(operand2));
        BigInteger result = getOnesComplement(tempResult).and(BIT_MASK);
        return new MultiplicationResult(!tempResult.equals(result), result);
    }

    /**
     * Arithmetic inverse operation - operand and result are both ones-complement
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
//
//    /**
//     * Shifts the given 72-bit value left, with bit[0] rotating to bit[71] at each iteration.
//     * Actual implementation may not involve iterative shifting.
//     * @param value value to be shifted
//     * @param count number of bits to be shifted
//     * @return resulting value
//     */
//    public static long leftShiftCircular(
//        final long value,
//        final int count
//    ) {
//        if (count < 0) {
//            return rightShiftCircular(value, -count);
//        } else if (count == 0) {
//            return value;
//        } else {
//            int actualCount = count % 36;
//            long residue = value >> (36 - actualCount); // end-around shifted portion
//            return ((value << actualCount) & BIT_MASK) | residue;
//        }
//    }

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
            return (count > 35) ? BigInteger.ZERO : value.shiftLeft(count).and(BIT_MASK);
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
//
//    /**
//     * Shifts the given 72-bit value right, with bit[71] rotating to bit[0] at each iteration.
//     * Actual implementation may not involve iterative shifting.
//     * @param value value to be shifted
//     * @param count number of bits to be shifted
//     * @return resulting value
//     */
//    public static long rightShiftCircular(
//        final long value,
//        final int count
//    ) {
//        if (count < 0) {
//            return leftShiftCircular(value, -count);
//        } else if (count == 0) {
//            return value;
//        } else {
//            int actualCount = (count % 36);
//            long mask = BIT_MASK >> (36 - actualCount);
//            long residue = (value & mask) << (36 - actualCount);
//            return ((value >> actualCount) | residue);
//        }
//    }

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
}
