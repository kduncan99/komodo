/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.math.BigInteger;

/**
 * Library for doing architecturally-correct 36-bit operations on integers
 * Note that we have designed this to provide static operations against long values, which are presumed to contain
 * ones-complement 36-bit values with the high-order 28 bits set to zero.  This is purposeful, as most of the emulated
 * main storage consists of slices of arrays of long integers, *not* Word36 objects (because the latter would take a
 * stupid amount of storage).  Thus, we do have non-static operations implemented against object instances, but they
 * all invoke the static functions which operate against longs.
 * Do NOT change this.
 */
@SuppressWarnings("Duplicates")
public class Word36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class AdditionResult {
        public final Flags _flags;
        public final long _value;

        public AdditionResult(
            final Flags flags,
            final long value
        ) {
            _flags = flags;
            _value = value;
        }
    }

    public static class Flags {
        public final boolean _carry;
        public final boolean _overflow;

        public Flags(
            final boolean carry,
            final boolean overflow
        ) {
            _carry = carry;
            _overflow = overflow;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constants
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final long BIT_MASK       = 0_777777_777777L;
    public static final long CARRY_BIT      = BIT_MASK + 1L;
    public static final long NEGATIVE_BIT   = 0_400000_000000L;

    public static final Word36 NEGATIVE_ONE = new Word36(0_777777_777776L);
    public static final Word36 NEGATIVE_ZERO = new Word36(0_777777_777777L);
    public static final Word36 POSITIVE_ONE = new Word36(1);
    public static final Word36 POSITIVE_ZERO = new Word36(0);

    public static final long MASK_B0        = 1L << 35;
    public static final long MASK_B1        = 1L << 34;
    public static final long MASK_B2        = 1L << 33;
    public static final long MASK_B3        = 1L << 32;
    public static final long MASK_B4        = 1L << 31;
    public static final long MASK_B5        = 1L << 30;
    public static final long MASK_B6        = 1L << 29;
    public static final long MASK_B7        = 1L << 28;
    public static final long MASK_B8        = 1L << 27;
    public static final long MASK_B9        = 1L << 26;
    public static final long MASK_B10       = 1L << 25;
    public static final long MASK_B11       = 1L << 24;
    public static final long MASK_B12       = 1L << 23;
    public static final long MASK_B13       = 1L << 22;
    public static final long MASK_B14       = 1L << 21;
    public static final long MASK_B15       = 1L << 20;
    public static final long MASK_B16       = 1L << 19;
    public static final long MASK_B17       = 1L << 18;
    public static final long MASK_B18       = 1L << 17;
    public static final long MASK_B19       = 1L << 16;
    public static final long MASK_B20       = 1L << 15;
    public static final long MASK_B21       = 1L << 14;
    public static final long MASK_B22       = 1L << 13;
    public static final long MASK_B23       = 1L << 12;
    public static final long MASK_B24       = 1L << 11;
    public static final long MASK_B25       = 1L << 10;
    public static final long MASK_B26       = 1L << 9;
    public static final long MASK_B27       = 1L << 8;
    public static final long MASK_B28       = 1L << 7;
    public static final long MASK_B29       = 1L << 6;
    public static final long MASK_B30       = 1L << 5;
    public static final long MASK_B31       = 1L << 4;
    public static final long MASK_B32       = 1L << 3;
    public static final long MASK_B33       = 1L << 2;
    public static final long MASK_B34       = 1L << 1;
    public static final long MASK_B35       = 1L;

    public static final long MASK_NOT_B0    = BIT_MASK ^ MASK_B0;
    public static final long MASK_NOT_B1    = BIT_MASK ^ MASK_B1;
    public static final long MASK_NOT_B2    = BIT_MASK ^ MASK_B2;
    public static final long MASK_NOT_B3    = BIT_MASK ^ MASK_B3;
    public static final long MASK_NOT_B4    = BIT_MASK ^ MASK_B4;
    public static final long MASK_NOT_B5    = BIT_MASK ^ MASK_B5;
    public static final long MASK_NOT_B6    = BIT_MASK ^ MASK_B6;
    public static final long MASK_NOT_B7    = BIT_MASK ^ MASK_B7;
    public static final long MASK_NOT_B8    = BIT_MASK ^ MASK_B8;
    public static final long MASK_NOT_B9    = BIT_MASK ^ MASK_B9;
    public static final long MASK_NOT_B10   = BIT_MASK ^ MASK_B10;
    public static final long MASK_NOT_B11   = BIT_MASK ^ MASK_B11;
    public static final long MASK_NOT_B12   = BIT_MASK ^ MASK_B12;
    public static final long MASK_NOT_B13   = BIT_MASK ^ MASK_B13;
    public static final long MASK_NOT_B14   = BIT_MASK ^ MASK_B14;
    public static final long MASK_NOT_B15   = BIT_MASK ^ MASK_B15;
    public static final long MASK_NOT_B16   = BIT_MASK ^ MASK_B16;
    public static final long MASK_NOT_B17   = BIT_MASK ^ MASK_B17;
    public static final long MASK_NOT_B18   = BIT_MASK ^ MASK_B18;
    public static final long MASK_NOT_B19   = BIT_MASK ^ MASK_B19;
    public static final long MASK_NOT_B20   = BIT_MASK ^ MASK_B20;
    public static final long MASK_NOT_B21   = BIT_MASK ^ MASK_B21;
    public static final long MASK_NOT_B22   = BIT_MASK ^ MASK_B22;
    public static final long MASK_NOT_B23   = BIT_MASK ^ MASK_B23;
    public static final long MASK_NOT_B24   = BIT_MASK ^ MASK_B24;
    public static final long MASK_NOT_B25   = BIT_MASK ^ MASK_B25;
    public static final long MASK_NOT_B26   = BIT_MASK ^ MASK_B26;
    public static final long MASK_NOT_B27   = BIT_MASK ^ MASK_B27;
    public static final long MASK_NOT_B28   = BIT_MASK ^ MASK_B28;
    public static final long MASK_NOT_B29   = BIT_MASK ^ MASK_B29;
    public static final long MASK_NOT_B30   = BIT_MASK ^ MASK_B30;
    public static final long MASK_NOT_B31   = BIT_MASK ^ MASK_B31;
    public static final long MASK_NOT_B32   = BIT_MASK ^ MASK_B32;
    public static final long MASK_NOT_B33   = BIT_MASK ^ MASK_B33;
    public static final long MASK_NOT_B34   = BIT_MASK ^ MASK_B34;
    public static final long MASK_NOT_B35   = BIT_MASK ^ MASK_B35;

    // general partial-word masks
    public static final long MASK_H1        = 0_777777_000000L;
    public static final long MASK_H2        = 0_000000_777777L;
    public static final long MASK_Q1        = 0_777_000_000_000L;
    public static final long MASK_Q2        = 0_000_777_000_000L;
    public static final long MASK_Q3        = 0_000_000_777_000L;
    public static final long MASK_Q4        = 0_000_000_000_777L;
    public static final long MASK_S1        = 0_77_00_00_00_00_00L;
    public static final long MASK_S2        = 0_00_77_00_00_00_00L;
    public static final long MASK_S3        = 0_00_00_77_00_00_00L;
    public static final long MASK_S4        = 0_00_00_00_77_00_00L;
    public static final long MASK_S5        = 0_00_00_00_00_77_00L;
    public static final long MASK_S6        = 0_00_00_00_00_00_77L;
    public static final long MASK_T1        = 0_7777_0000_0000L;
    public static final long MASK_T2        = 0_0000_7777_0000L;
    public static final long MASK_T3        = 0_0000_0000_7777L;

    public static final long MASK_NOT_H1    = 0_000000_777777L;
    public static final long MASK_NOT_H2    = 0_777777_000000L;
    public static final long MASK_NOT_Q1    = 0_000_777_777_777L;
    public static final long MASK_NOT_Q2    = 0_777_000_777_777L;
    public static final long MASK_NOT_Q3    = 0_777_777_000_777L;
    public static final long MASK_NOT_Q4    = 0_777_777_777_000L;
    public static final long MASK_NOT_S1    = 0_00_77_77_77_77_77L;
    public static final long MASK_NOT_S2    = 0_77_00_77_77_77_77L;
    public static final long MASK_NOT_S3    = 0_77_77_00_77_77_77L;
    public static final long MASK_NOT_S4    = 0_77_77_77_00_77_77L;
    public static final long MASK_NOT_S5    = 0_77_77_77_77_00_77L;
    public static final long MASK_NOT_S6    = 0_77_77_77_77_77_00L;
    public static final long MASK_NOT_T1    = 0_0000_7777_7777L;
    public static final long MASK_NOT_T2    = 0_7777_0000_7777L;
    public static final long MASK_NOT_T3    = 0_7777_7777_0000L;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Character conversion tables
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Using the Fieldata character as an index into this table, one gets the corresponding ASCII character
     */
    public static final char[] ASCII_FROM_FIELDATA =
    {
        '@',	'[',	']',	'#',	'^',	' ',	'A',	'B',
        'C',	'D',	'E',	'F',	'G',	'H',	'I',	'J',
        'K',	'L',	'M',	'N',	'O',	'P',	'Q',	'R',
        'S',	'T',	'U',	'V',	'W',	'X',	'Y',	'Z',
        ')',	'-',	'+',	'<',	'=',	'>',	'&',	'$',
        '*',	'(',	'%',	':',	'?',	'!',	',',	'\\',
        '0',	'1',	'2',	'3',	'4',	'5',	'6',	'7',
        '8',	'9',	'\'',	';',	'/',	'.',	'"',	'_',
    };

    /**
     * Using the integer value corresponding to an ASCII character, one gets the corresponding Fieldata character
     */
    public static final short[] FIELDATA_FROM_ASCII =
    {
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        005, 055, 076, 003, 047, 052, 046, 072, 051, 040, 050, 042, 056, 041, 075, 074,
        060, 061, 062, 063, 064, 065, 066, 067, 070, 071, 053, 073, 043, 044, 045, 054,
        000, 006, 007, 010, 011, 012, 013, 014, 015, 016, 017, 020, 021, 022, 023, 024,
        025, 026, 027, 030, 031, 032, 033, 034, 035, 036, 037, 001, 057, 002, 004, 077,
        077, 006, 007, 010, 011, 012, 013, 014, 015, 016, 017, 020, 021, 022, 023, 024,
        025, 026, 027, 030, 031, 032, 033, 034, 035, 036, 037, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
        077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077, 077,
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data items (not much here)
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected long _value = 0;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     */
    public Word36()             { _value = 0; }
    public Word36(long value)   { _value = value & BIT_MASK; }  //  operand is expected to be ones-complement
    public Word36(Word36 value) { _value = value._value; }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Overrides
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj instanceof Word36) && (_value == ((Word36)obj)._value);
    }

    @Override
    public int hashCode() { return (int) _value; }

    @Override
    public String toString() { return String.format("%012o%", _value); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Non-Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    public long getH1() { return getH1(_value); }
    public long getH2() { return getH2(_value); }
    public long getQ1() { return getQ1(_value); }
    public long getQ2() { return getQ2(_value); }
    public long getQ3() { return getQ3(_value); }
    public long getQ4() { return getQ4(_value); }
    public long getS1() { return getS1(_value); }
    public long getS2() { return getS2(_value); }
    public long getS3() { return getS3(_value); }
    public long getS4() { return getS4(_value); }
    public long getS5() { return getS5(_value); }
    public long getS6() { return getS6(_value); }
    public long getT1() { return getT1(_value); }
    public long getT2() { return getT2(_value); }
    public long getT3() { return getT3(_value); }
    public long getXH1() { return getXH1(_value); }
    public long getXH2() { return getXH2(_value); }
    public long getXT1() { return getXT1(_value); }
    public long getXT2() { return getXT2(_value); }
    public long getXT3() { return getXT3(_value); }
    public long getW() { return _value; }

    public void setH1(long partialValue) { _value = setH1(_value, partialValue); }
    public void setH2(long partialValue) { _value = setH2(_value, partialValue); }
    public void setQ1(long partialValue) { _value = setQ1(_value, partialValue); }
    public void setQ2(long partialValue) { _value = setQ2(_value, partialValue); }
    public void setQ3(long partialValue) { _value = setQ3(_value, partialValue); }
    public void setQ4(long partialValue) { _value = setQ4(_value, partialValue); }
    public void setS1(long partialValue) { _value = setS1(_value, partialValue); }
    public void setS2(long partialValue) { _value = setS2(_value, partialValue); }
    public void setS3(long partialValue) { _value = setS3(_value, partialValue); }
    public void setS4(long partialValue) { _value = setS4(_value, partialValue); }
    public void setS5(long partialValue) { _value = setS5(_value, partialValue); }
    public void setS6(long partialValue) { _value = setS6(_value, partialValue); }
    public void setT1(long partialValue) { _value = setT1(_value, partialValue); }
    public void setT2(long partialValue) { _value = setT2(_value, partialValue); }
    public void setT3(long partialValue) { _value = setT3(_value, partialValue); }
    public void setW(long value) { _value = value & BIT_MASK; }


    //  Negative, Positive, and Zero testing ---------------------------------------------------------------------------------------

    /**
     * Determines if the value of this object is negative (sign bit is set)
     */
    public boolean isNegative() { return (_value & NEGATIVE_BIT) != 0; }

    /**
     * Determines if the value of this object is positive (sign bit is clear)
     */
    public boolean isPositive() { return (_value & NEGATIVE_BIT) == 0; }

    /**
     * Determines if the value of this object is zero (positive or negative)
     */
    public boolean isZero() { return (_value == POSITIVE_ZERO._value) || (_value == NEGATIVE_ZERO._value); }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    /**
     * Arithmetically adds another Word36 object to this object.
     * @return carry/overflow flags object
     */
    public Flags add(
        final Word36 addend
    ) {
        AdditionResult ar = add(_value, addend._value);
        _value = ar._value;
        return ar._flags;
    }

    public void negate() { _value = negate(_value); }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    public void logicalAnd(Word36 operand)  { _value = logicalAnd(_value, operand._value); }
    public void logicalNot()                { _value = logicalNot(_value); }
    public void logicalOr(Word36 operand)   { _value = logicalOr(_value, operand._value); }
    public void logicalXor(Word36 operand)  { _value = logicalXor(_value, operand._value); }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

    public void leftShiftAlgebraic(int count)   { _value = leftShiftAlgebraic(_value, count); }
    public void leftShiftCircular(int count)    { _value = leftShiftCircular(_value, count); }
    public void leftShiftLogical(int count)     { _value = leftShiftLogical(_value, count); }
    public void rightShiftAlgebraic(int count)  { _value = rightShiftAlgebraic(_value, count); }
    public void rightShiftCircular(int count)   { _value = rightShiftCircular(_value, count); }
    public void rightShiftLogical(int count)    { _value = rightShiftLogical(_value, count); }


    //  Conversions ----------------------------------------------------------------------------------------------------------------

    public long getTwosComplement() { return getTwosComplement(_value); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods - these operate on and return long integers representing ones-complement values
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Tests ----------------------------------------------------------------------------------------------------------------------

    public static boolean isNegative(
        final long value
    ) {
        return (value & NEGATIVE_BIT) == NEGATIVE_BIT;
    }

    public static boolean isNegativeZero(
        final long value
    ) {
        return value == NEGATIVE_ZERO._value;
    }

    public static boolean isPositive(
        final long value
    ) {
        return (value & NEGATIVE_BIT) == 0;
    }

    public static boolean isPositiveZero(
        final long value
    ) {
        return value == POSITIVE_ZERO._value;
    }

    public static boolean isZero(
        final long value
    ) {
        return isPositiveZero(value) || isNegativeZero(value);
    }


    //  Partial-word extraction ----------------------------------------------------------------------------------------------------

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getH1(
        final long value
    ) {
        return (value & Word36.MASK_H1) >> 18;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getH2(
        final long value
    ) {
        return value & Word36.MASK_H2;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getQ1(
        final long value
    ) {
        return (value & Word36.MASK_Q1) >> 27;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getQ2(
        final long value
    ) {
        return (value & Word36.MASK_Q2) >> 18;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getQ3(
        final long value
    ) {
        return (value & Word36.MASK_Q3) >> 9;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getQ4(
        final long value
    ) {
        return value & Word36.MASK_Q4;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getS1(
        final long value
    ) {
        return (value & Word36.MASK_S1) >> 30;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getS2(
        final long value
    ) {
        return (value & Word36.MASK_S2) >> 24;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getS3(
        final long value
    ) {
        return (value & Word36.MASK_S3) >> 18;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getS4(
        final long value
    ) {
        return (value & Word36.MASK_S4) >> 12;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getS5(
        final long value
    ) {
        return (value & Word36.MASK_S5) >> 6;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getS6(
        final long value
    ) {
        return value & Word36.MASK_S6;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getT1(
        final long value
    ) {
        return (value & Word36.MASK_T1) >> 24;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getT2(
        final long value
    ) {
        return (value & Word36.MASK_T2) >> 12;
    }

    /**
     * Extracts a partial word (sign-unextended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getT3(
        final long value
    ) {
        return value & Word36.MASK_T3;
    }

    /**
     * Extracts a partial word (sign-extended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getXH1(
        final long value
    ) {
        long result = getH1(value);
        if ((result & 0400000) != 0)
            result |= 0777777_000000L;
        return result;
    }

    /**
     * Extracts a partial word (sign-extended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getXH2(
        final long value
    ) {
        long result = getH2(value);
        if ((result & 0400000) != 0)
            result |= 0777777_000000L;
        return result;
    }

    /**
     * Extracts a partial word (sign-extended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getXT1(
        final long value
    ) {
        long result = getT1(value);
        if ((result & 04000) != 0)
            result |= 07777_7777_0000L;
        return result;
    }

    /**
     * Extracts a partial word (sign-extended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getXT2(
        final long value
    ) {
        long result = getT2(value);
        if ((result & 04000) != 0)
            result |= 07777_7777_0000L;
        return result;
    }

    /**
     * Extracts a partial word (sign-extended) from the given parameter
     * @param value 36-bit architectural value wrapped in a Java 64-bit signed int
     * @return result
     */
    public static long getXT3(
        final long value
    ) {
        long result = getT3(value);
        if ((result & 04000) != 0)
            result |= 07777_7777_0000L;
        return result;
    }


    //  Partial-word injection -----------------------------------------------------------------------------------------------------

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setH1(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777L) | ((partialValue & 0_777777L) << 18);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setH2(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_000000L) | (partialValue & 0_777777L);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setQ1(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_000_777_777_777L) | ((partialValue & 0_777L) << 27);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setQ2(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777_000_777_777L) | ((partialValue & 0_777L) << 18);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setQ3(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777_777_000_777L) | ((partialValue & 0_777L) << 9);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setQ4(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777_777_777_000L) | (partialValue & 0_777L);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setS1(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_007777_777777L) | ((partialValue & 0_77L) << 30);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setS2(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_770077_777777L) | ((partialValue & 0_77L) << 24);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setS3(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777700_777777L) | ((partialValue & 0_77L) << 18);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setS4(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_007777L) | ((partialValue & 0_77L) << 12);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setS5(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_770077L) | ((partialValue & 0_77L) << 6);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setS6(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_777700L) | (partialValue & 0_77L);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setT1(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_0000_7777_7777L) | ((partialValue & 0_7777L) << 24);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setT2(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_7777_0000_7777L) | ((partialValue & 0_7777L) << 12);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a p ortion of the existing value
     * @return resulting value
     */
    public static long setT3(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_7777_7777_0000L) | (partialValue & 0_7777L);
    }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    public static AdditionResult add(
        final long operand1,
        final long operand2
    ) {
        boolean neg1 = isNegative(operand1);
        boolean neg2 = isNegative(operand2);

        long result = addSimple(operand1, operand2);
        if ((result & CARRY_BIT) != 0) {
            result &= BIT_MASK;
            ++result;
        }

        boolean negRes = isNegative(result);

        boolean carry = result < 0 ? (neg1 && neg2) : (neg1 || neg2);
        boolean overflow = (neg1 == neg2) && (neg1 != negRes);
        return new AdditionResult(new Flags(carry, overflow), result);
    }

    public static long addSimple(
        final long operand1,
        final long operand2
    ) {
        if ((operand1 == NEGATIVE_ZERO._value) && (operand2 == NEGATIVE_ZERO._value)) {
            return NEGATIVE_ZERO._value;
        }

        long native1 = getTwosComplement(operand1);
        long native2 = getTwosComplement(operand2);
        return getOnesComplement(native1 + native2);
    }

    public static long getOnesComplement(
        final long operand
    ) {
        return operand < 0 ? negate(-operand) : operand;
    }

    public static long getTwosComplement(
        final long operand
    ) {
        return isNegative(operand) ? -negate(operand) : operand;
    }

    /**
     * Multiplies two ones-complement 36-bit operands, producing a ones-complement 72-bit result.
     * The first word of the result is the most-significant bits of the operation,
     * while the second word is the least-significant.
     */
    public static BigInteger multiply(
        final long operand1,
        final long operand2
    ) {
        long[] result = new long[2];
        BigInteger biOp1 = BigInteger.valueOf(Word36.getTwosComplement(operand1));
        BigInteger biOp2 = BigInteger.valueOf(Word36.getTwosComplement(operand2));
        return DoubleWord36.getOnesComplement(biOp1.multiply(biOp2));
    }

    public static long negate(
        final long operand
    ) {
        return operand ^ 0_777777_777777L;
    }

    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    /**
     * Logical AND operation (in this context, logical means bitwise)
     * @param operand1 left hand operand
     * @param operand2 right hand operand
     * @return bitwise AND of the two operands
     */
    public static long logicalAnd(
        final long operand1,
        final long operand2
    ) {
        return operand1 & operand2;
    }

    /**
     * Logical NOT operation (in this context, logical means bitwise)
     * @param operand value to be affected
     * @return bitwise NOT of the given value
     */
    public static long logicalNot(
        final long operand
    ) {
        return operand ^ BIT_MASK;
    }

    /**
     * Logical OR operation (in this context, logical means bitwise)
     * @param operand1 left hand operand
     * @param operand2 right hand operand
     * @return bitwise OR of the two operands
     */
    public static long logicalOr(
        final long operand1,
        final long operand2
    ) {
        return operand1 | operand2;
    }

    /**
     * Logical XOR operation (in this context, logical means bitwise)
     * @param operand1 left hand operand
     * @param operand2 right hand operand
     * @return bitwise XOR of the two operands
     */
    public static long logicalXor(
        final long operand1,
        final long operand2
    ) {
        return operand1 ^ operand2;
    }


    //  Shift Operations -----------------------------------------------------------------------------------------------------------

    public static long leftShiftAlgebraic(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftAlgebraic(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            return leftShiftLogical(value, count);
        }
    }

    /**
     * Shifts the given 36-bit value left, with bit[0] rotating to bit[35] at each iteration.
     * Actual implementation may not involve iterative shifting.
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static long leftShiftCircular(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftCircular(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            int actualCount = count % 36;
            long residue = value >> (36 - actualCount); // end-around shifted portion
            return ((value << actualCount) & BIT_MASK) | residue;
        }
    }

    /**
     * Shifts the given 36-bit value left by a number of bits
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static long leftShiftLogical(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftLogical(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            return (count > 35) ? 0 : (value << count) & BIT_MASK;
        }
    }

    /**
     * Does an algebraic shift right - this means the sign bit is always preserved as well as being shifted to the right.
     * @param value 36-bit value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static long rightShiftAlgebraic(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return leftShiftAlgebraic(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            boolean wasNegative = isNegative(value);
            if (count > 35) {
                return wasNegative ? NEGATIVE_ZERO._value : 0;
            } else {
                long result = value >> count;
                if (wasNegative)
                    result |= ((~(BIT_MASK >> count)) & BIT_MASK);
                return result;
            }
        }
    }

    /**
     * Shifts the given 36-bit value right, with bit[35] rotating to bit[0] at each iteration.
     * Actual implementation may not involve iterative shifting.
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static long rightShiftCircular(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return leftShiftCircular(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            int actualCount = (count % 36);
            long mask = BIT_MASK >> (36 - actualCount);
            long residue = (value & mask) << (36 - actualCount);
            return ((value >> actualCount) | residue);
        }
    }

    /**
     * Shifts the given 36-bit value right by a number of bits
     * @param value value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static long rightShiftLogical(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return leftShiftLogical(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            return (count > 35) ? 0 : value >> count;
        }
    }


    //  Sign extension of several important partial-words --------------------------------------------------------------------------

    /**
     * Presuming the given value is a signed 12-bit value, we turn it into a 36-bit signed value
     */
    public static long getSignExtended12(
        final long value
    ) {
        if ((value & 04000) == 0) {
            return value;
        } else {
            return value | 0_777777_770000L;
        }
    }

    /**
     * Presuming the given value is a signed 18-bit value, we turn it into a 36-bit signed value
     */
    public static long getSignExtended18(
        final long value
    ) {
        if ((value & 0_400000) == 0) {
            return value;
        } else {
            return value | 0_777777_000000L;
        }
    }

    /**
     * Presuming the given value is a signed 24-bit value, we turn it into a 36-bit signed value
     */
    public static long getSignExtended24(
        final long value
    ) {
        if ((value & 0_000040_000000L) == 0) {
            return value;
        } else {
            return value | 0_777700_000000L;
        }
    }


    //  Conversion from String to Word36 -------------------------------------------------------------------------------------------

    /**
     * Populates this object with quarter-words derived from the ASCII characters in the source string.
     * If the string does not contain at least 4 characters, we pad the resulting output with blanks as necessary.
     * Any characters in the string beyond the fourth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static Word36 stringToWordASCII(
        final String source
    ) {
        Word36 w = new Word36(0_040_040_040_040L);
        switch (source.length() > 4 ? 4 : source.length()) {
            case 4:
                w.setQ4(source.charAt(3) & 0xff);
                //  fall thru
            case 3:
                w.setQ3(source.charAt(2) & 0xff);
                //  fall thru
            case 2:
                w.setQ2(source.charAt(1) & 0xff);
                //  fall thru
            case 1:
                w.setQ1(source.charAt(0) & 0xff);
        }

        return w;
    }

    /**
     * Populates this object with sixth-words representing the fieldata characters derived from the ASCII characters
     * in the source string. If the string does not contain at least 6 characters, we pad the resulting output with
     * blanks as necessary. Any characters in the string beyond the sixth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static Word36 stringToWordFieldata(
        final String source
    ) {
        Word36 w = new Word36(0_050505_050505L);
        switch (source.length() > 6 ? 6 : source.length()) {
            case 6:
                w.setS6(FIELDATA_FROM_ASCII[source.charAt(5) & 0xff]);
                //  fall thru
            case 5:
                w.setS5(FIELDATA_FROM_ASCII[source.charAt(4) & 0xff]);
                //  fall thru
            case 4:
                w.setS4(FIELDATA_FROM_ASCII[source.charAt(3) & 0xff]);
                //  fall thru
            case 3:
                w.setS3(FIELDATA_FROM_ASCII[source.charAt(2) & 0xff]);
                //  fall thru
            case 2:
                w.setS2(FIELDATA_FROM_ASCII[source.charAt(1) & 0xff]);
                //  fall thru
            case 1:
                w.setS1(FIELDATA_FROM_ASCII[source.charAt(0) & 0xff]);
        }
        return w;
    }


    //  Formatting for display -----------------------------------------------------------------------------------------------------

    /**
     * Given an integer which represents an ASCII character, we return the corresponding char if it is displayable,
     * or else the alternate character.
     * @param value value to be converted
     * @param alternate character to be returned if the value presentes an undisplayable character
     */
    private static char getASCIIForDisplay(
        final int value,
        final char alternate
    ) {
        if ((value < 32) || (value >= 127)) {
            return alternate;
        } else {
            return (char) value;
        }
    }

    /**
     * Interprets the given 36-bit value as a sequence of 4 ASCII characters, and produces those characters as a result
     * @param value 36-bit value
     * @return displayable result
     */
    public static String toASCII(
        final long value
    ) {
        return String.format("%s%s%s%s",
                             getASCIIForDisplay((int)getQ1(value), '.'),
                             getASCIIForDisplay((int)getQ2(value), '.'),
                             getASCIIForDisplay((int)getQ3(value), '.'),
                             getASCIIForDisplay((int)getQ4(value), '.'));
    }

    /**
     * Interprets the given 36-bit value as a sequence of 6 Fieldata characters, and produces those characters as a result
     * @param value 36-bit value
     * @return displayable result
     */
    public static String toFieldata(
        final long value
    ) {
        return String.format("%s%s%s%s%s%s",
                             ASCII_FROM_FIELDATA[(int) getS1(value)],
                             ASCII_FROM_FIELDATA[(int) getS2(value)],
                             ASCII_FROM_FIELDATA[(int) getS3(value)],
                             ASCII_FROM_FIELDATA[(int) getS4(value)],
                             ASCII_FROM_FIELDATA[(int) getS5(value)],
                             ASCII_FROM_FIELDATA[(int) getS6(value)]);
    }

    /**
     * Interprets the given 36-bit value as a sequence of 12 Octal digits, and produces those characters as a result
     * @param value 36-bit value
     * @return displayable result
     */
    public static String toOctal(
        final long value
    ) {
        return String.format("%012o", value);
    }
}
