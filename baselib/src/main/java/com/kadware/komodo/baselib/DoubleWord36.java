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

//    public static class AdditionResult {
//        public final Flags _flags;
//        public final long _value;
//
//        public AdditionResult(
//            final Flags flags,
//            final long value
//        ) {
//            _flags = flags;
//            _value = value;
//        }
//    }
//
//    public static class Flags {
//        public final boolean _carry;
//        public final boolean _overflow;
//
//        public Flags(
//            final boolean carry,
//            final boolean overflow
//        ) {
//            _carry = carry;
//            _overflow = overflow;
//        }
//    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constants
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final BigInteger SHORT_BIT_MASK   = BigInteger.valueOf(Word36.BIT_MASK);
    public static final BigInteger BIT_MASK         = SHORT_BIT_MASK.shiftLeft(36).or(SHORT_BIT_MASK);

    public static final DoubleWord36 NEGATIVE_ONE = new DoubleWord36(BIT_MASK.add(BigInteger.ONE.negate()));
    public static final DoubleWord36 NEGATIVE_ZERO = new DoubleWord36(BIT_MASK);
    public static final DoubleWord36 POSITIVE_ONE = new DoubleWord36(BigInteger.ONE);
    public static final DoubleWord36 POSITIVE_ZERO = new DoubleWord36(BigInteger.ZERO);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data items (not much here)
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected BigInteger _value = BigInteger.ZERO;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public DoubleWord36()                   {}
    public DoubleWord36(long value)         { _value = BigInteger.valueOf(value); }
    public DoubleWord36(BigInteger value)   { _value = value; }
    public DoubleWord36(DoubleWord36 value) { _value = value._value; }


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


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    public void negate() {
        _value = negate(_value);
    }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    public void logicalAnd(DoubleWord36 operand)    { _value = logicalAnd(_value, operand._value); }
    public void logicalNot()                        { _value = logicalNot(_value); }
    public void logicalOr(DoubleWord36 operand)     { _value = logicalOr(_value, operand._value); }
    public void logicalXor(DoubleWord36 operand)    { _value = logicalXor(_value, operand._value); }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

//    public void leftShiftAlgebraic(int count)   { _value = leftShiftAlgebraic(_value, count); }
//    public void leftShiftCircular(int count)    { _value = leftShiftCircular(_value, count); }
//    public void leftShiftLogical(int count)     { _value = leftShiftLogical(_value, count); }
//    public void rightShiftAlgebraic(int count)  { _value = rightShiftAlgebraic(_value, count); }
//    public void rightShiftCircular(int count)   { _value = rightShiftCircular(_value, count); }
//    public void rightShiftLogical(int count)    { _value = rightShiftLogical(_value, count); }


    //  Conversions ----------------------------------------------------------------------------------------------------------------

    public BigInteger getTwosComplement() { return getTwosComplement(_value); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods - these operate on and return long integers representing ones-complement values
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Tests ----------------------------------------------------------------------------------------------------------------------

    /**
     * Tests 72-bit ones-complement value to see if it is negative
     */
    public static boolean isNegative(
        final BigInteger value
    ) {
        return (value.shiftRight(71).intValue() == 1);
    }

    public static boolean isNegativeZero(
        final BigInteger value
    ) {
        return value.equals(NEGATIVE_ZERO);
    }

    public static boolean isPositive(
        final BigInteger value
    ) {
        return !isNegative(value);
    }

    public static boolean isPositiveZero(
        final BigInteger value
    ) {
        return value.equals(POSITIVE_ZERO);
    }

    public static boolean isZero(
        final BigInteger value
    ) {
        return isPositiveZero(value) || isNegativeZero(value);
    }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

//    public static AdditionResult add(
//        final long operand1,
//        final long operand2
//    ) {
//        boolean neg1 = isNegative(operand1);
//        boolean neg2 = isNegative(operand2);
//
//        long result = addSimple(operand1, operand2);
//        if ((result & CARRY_BIT) != 0) {
//            result &= BIT_MASK;
//            ++result;
//        }
//
//        boolean negRes = isNegative(result);
//
//        boolean carry = result < 0 ? (neg1 && neg2) : (neg1 || neg2);
//        boolean overflow = (neg1 == neg2) && (neg1 != negRes);
//        return new AdditionResult(new Flags(carry, overflow), result);
//    }
//
//    public static long addSimple(
//        final long operand1,
//        final long operand2
//    ) {
//        if ((operand1 == NEGATIVE_ZERO._value) && (operand2 == NEGATIVE_ZERO._value)) {
//            return NEGATIVE_ZERO._value;
//        }
//
//        long native1 = getTwosComplement(operand1);
//        long native2 = getTwosComplement(operand2);
//        return getOnesComplement(native1 + native2);
//    }

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
        return isNegative(operand) ? negate(operand) : operand;
    }

    /**
     * Arithmetic inverse operation
     */
    public static BigInteger negate(
        final BigInteger operand
    ) {
        return operand.negate();
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
//     * Shifts the given 36-bit value left, with bit[1] rotating to bit[36] at each iteration.
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
//
//    /**
//     * Shifts the given 36-bit value left by a number of bits
//     * @param value value to be shifted
//     * @param count number of bits to be shifted
//     * @return resulting value
//     */
//    public static long leftShiftLogical(
//        final long value,
//        final int count
//    ) {
//        if (count < 0) {
//            return rightShiftLogical(value, -count);
//        } else if (count == 0) {
//            return value;
//        } else {
//            return (count > 35) ? 0 : (value << count) & BIT_MASK;
//        }
//    }
//
//    /**
//     * Does an algebraic shift right - this means the sign bit is always preserved as well as being shifted to the right.
//     * @param value 36-bit value to be shifted
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
//     * Shifts the given 36-bit value right, with bit[36] rotating to bit[1] at each iteration.
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
//
//    /**
//     * Shifts the given 36-bit value right by a number of bits
//     * @param value value to be shifted
//     * @param count number of bits to be shifted
//     * @return resulting value
//     */
//    public static long rightShiftLogical(
//        final long value,
//        final int count
//    ) {
//        if (count < 0) {
//            return leftShiftLogical(value, -count);
//        } else if (count == 0) {
//            return value;
//        } else {
//            return (count > 35) ? 0 : value >> count;
//        }
//    }
//
//
//    //  Conversion from String to Word36 -------------------------------------------------------------------------------------------
//
//    /**
//     * Populates this object with quarter-words derived from the ASCII characters in the source string.
//     * If the string does not contain at least 4 characters, we pad the resulting output with blanks as necessary.
//     * Any characters in the string beyond the fourth are ignored.
//     * @param source string to be converted
//     * @return converted data
//     */
//    public static DoubleWord36 stringToWord36ASCII(
//        final String source
//    ) {
//        DoubleWord36 w = new DoubleWord36(0_040_040_040_040L);
//        switch (source.length() > 4 ? 4 : source.length()) {
//            case 4:
//                w.setQ4(source.charAt(3) & 0xff);
//                //  fall thru
//            case 3:
//                w.setQ3(source.charAt(2) & 0xff);
//                //  fall thru
//            case 2:
//                w.setQ2(source.charAt(1) & 0xff);
//                //  fall thru
//            case 1:
//                w.setQ1(source.charAt(0) & 0xff);
//        }
//
//        return w;
//    }
//
//    /**
//     * Populates this object with sixth-words representing the fieldata characters derived from the ASCII characters
//     * in the source string. If the string does not contain at least 6 characters, we pad the resulting output with
//     * blanks as necessary. Any characters in the string beyond the sixth are ignored.
//     * @param source string to be converted
//     * @return converted data
//     */
//    public static DoubleWord36 stringToWord36Fieldata(
//        final String source
//    ) {
//        DoubleWord36 w = new DoubleWord36(0_050505_050505L);
//        switch (source.length() > 6 ? 6 : source.length()) {
//            case 6:
//                w.setS6(FIELDATA_FROM_ASCII[source.charAt(5) & 0xff]);
//                //  fall thru
//            case 5:
//                w.setS5(FIELDATA_FROM_ASCII[source.charAt(4) & 0xff]);
//                //  fall thru
//            case 4:
//                w.setS4(FIELDATA_FROM_ASCII[source.charAt(3) & 0xff]);
//                //  fall thru
//            case 3:
//                w.setS3(FIELDATA_FROM_ASCII[source.charAt(2) & 0xff]);
//                //  fall thru
//            case 2:
//                w.setS2(FIELDATA_FROM_ASCII[source.charAt(1) & 0xff]);
//                //  fall thru
//            case 1:
//                w.setS1(FIELDATA_FROM_ASCII[source.charAt(0) & 0xff]);
//        }
//        return w;
//    }
//
//
//    //  Formatting for display -----------------------------------------------------------------------------------------------------
//
//    /**
//     * Given an integer which represents an ASCII character, we return the corresponding char if it is displayable,
//     * or else the alternate character.
//     * @param value value to be converted
//     * @param alternate character to be returned if the value presentes an undisplayable character
//     */
//    private static char getASCIIForDisplay(
//        final int value,
//        final char alternate
//    ) {
//        if ((value < 32) || (value >= 127)) {
//            return alternate;
//        } else {
//            return (char) value;
//        }
//    }
//
//    /**
//     * Interprets the given 36-bit value as a sequence of 4 ASCII characters, and produces those characters as a result
//     * @param value 36-bit value
//     * @return displayable result
//     */
//    public static String toASCII(
//        final long value
//    ) {
//        return String.format("%s%s%s%s",
//                             getASCIIForDisplay((int)getQ1(value), '.'),
//                             getASCIIForDisplay((int)getQ2(value), '.'),
//                             getASCIIForDisplay((int)getQ3(value), '.'),
//                             getASCIIForDisplay((int)getQ4(value), '.'));
//    }
//
//    /**
//     * Interprets the given 36-bit value as a sequence of 6 Fieldata characters, and produces those characters as a result
//     * @param value 36-bit value
//     * @return displayable result
//     */
//    public static String toFieldata(
//        final long value
//    ) {
//        return String.format("%s%s%s%s%s%s",
//                             ASCII_FROM_FIELDATA[(int) getS1(value)],
//                             ASCII_FROM_FIELDATA[(int) getS2(value)],
//                             ASCII_FROM_FIELDATA[(int) getS3(value)],
//                             ASCII_FROM_FIELDATA[(int) getS4(value)],
//                             ASCII_FROM_FIELDATA[(int) getS5(value)],
//                             ASCII_FROM_FIELDATA[(int) getS6(value)]);
//    }
//
//    /**
//     * Interprets the given 36-bit value as a sequence of 12 Octal digits, and produces those characters as a result
//     * @param value 36-bit value
//     * @return displayable result
//     */
//    public static String toOctal(
//        final long value
//    ) {
//        return String.format("%012o", value);
//    }
}
