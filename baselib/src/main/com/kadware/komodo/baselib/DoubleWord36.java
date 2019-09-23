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
    public static final DoubleWord36 DW36_NEGATIVE_ZERO             = new DoubleWord36(NEGATIVE_ZERO);
    public static final DoubleWord36 DW36_POSITIVE_ONE              = new DoubleWord36(POSITIVE_ONE);
    public static final DoubleWord36 DW36_POSITIVE_ZERO             = new DoubleWord36(POSITIVE_ZERO);


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

    public BigInteger get()     { return _value; }
    public Word36[] getWords()  { return getWords(_value); }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    public boolean isNegative()                 { return isNegative(_value); }
    public boolean isNegativeZero()             { return isNegativeZero(_value); }
    public boolean isPositive()                 { return isPositive(_value); }
    public boolean isPositiveZero()             { return isPositiveZero(_value); }
    public boolean isZero()                     { return isZero(_value); }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    public AdditionResult add(DoubleWord36 addend)              { return new AdditionResult(add(_value, addend._value)); }
    public int compareTo(DoubleWord36 operand)                  { return compare(_value, operand._value); }
    public DivisionResult divide(DoubleWord36 divisor)          { return new DivisionResult(divide(_value, divisor._value)); }
    public DoubleWord36 extendSign(int fieldSize)               { return new DoubleWord36(extendSign(_value, fieldSize)); }
    public MultiplicationResult multiply(DoubleWord36 factor)   { return new MultiplicationResult(multiply(_value, factor._value)); }
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
     * Divide two 72-bit integer values
     */
    public static StaticDivisionResult divide(
        final BigInteger dividend,
        final BigInteger divisor
    ) {
        BigInteger[] tempResults = getTwosComplement(dividend).divideAndRemainder(getTwosComplement(divisor));
        return new StaticDivisionResult(getOnesComplement(tempResults[0]), getOnesComplement(tempResults[1]));
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
}
