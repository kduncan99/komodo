/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Library for doing architecturally-correct 36-bit operations on integers
 * We have designed this for two purposes.
 * These are static versions which operate against long values for use in arrays and ArraySlice things
 * so that we do not have to use stupid amounts of storage for lots of Word36 objects.
 */
public class Word36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class AdditionResult {
        public Flags _flags;
        public long _value;
    }

    public static class Flags {
        public boolean _carry;
        public boolean _overflow;

        public Flags() {
            this(false, false);
        }

        Flags(boolean carry, boolean overflow) {
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

    public static final long NEGATIVE_ONE   = 0_777777_777776L;
    public static final long NEGATIVE_ZERO  = 0_777777_777777L;
    public static final long POSITIVE_ONE   = 01L;
    public static final long POSITIVE_ZERO  = 0L;

    public static final long A_OPTION = 1L << 25;
    public static final long B_OPTION = 1L << 24;
    public static final long C_OPTION = 1L << 23;
    public static final long D_OPTION = 1L << 22;
    public static final long E_OPTION = 1L << 21;
    public static final long F_OPTION = 1L << 20;
    public static final long G_OPTION = 1L << 19;
    public static final long H_OPTION = 1L << 18;
    public static final long I_OPTION = 1L << 17;
    public static final long J_OPTION = 1L << 16;
    public static final long K_OPTION = 1L << 15;
    public static final long L_OPTION = 1L << 14;
    public static final long M_OPTION = 1L << 13;
    public static final long N_OPTION = 1L << 12;
    public static final long O_OPTION = 1L << 11;
    public static final long P_OPTION = 1L << 10;
    public static final long Q_OPTION = 1L << 9;
    public static final long R_OPTION = 1L << 8;
    public static final long S_OPTION = 1L << 7;
    public static final long T_OPTION = 1L << 6;
    public static final long U_OPTION = 1L << 5;
    public static final long V_OPTION = 1L << 4;
    public static final long W_OPTION = 1L << 3;
    public static final long X_OPTION = 1L << 2;
    public static final long Y_OPTION = 1L << 1;
    public static final long Z_OPTION = 1L;

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
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_05, 0_55, 0_76, 0_03, 0_47, 0_52, 0_46, 0_72, 0_51, 0_40, 0_50, 0_42, 0_56, 0_41, 0_75, 0_74,
        0_60, 0_61, 0_62, 0_63, 0_64, 0_65, 0_66, 0_67, 0_70, 0_71, 0_53, 0_73, 0_43, 0_44, 0_45, 0_54,
        0_00, 0_06, 0_07, 0_10, 0_11, 0_12, 0_13, 0_14, 0_15, 0_16, 0_17, 0_20, 0_21, 0_22, 0_23, 0_24,
        0_25, 0_26, 0_27, 0_30, 0_31, 0_32, 0_33, 0_34, 0_35, 0_36, 0_37, 0_01, 0_57, 0_02, 0_04, 0_77,
        0_77, 0_06, 0_07, 0_10, 0_11, 0_12, 0_13, 0_14, 0_15, 0_16, 0_17, 0_20, 0_21, 0_22, 0_23, 0_24,
        0_25, 0_26, 0_27, 0_30, 0_31, 0_32, 0_33, 0_34, 0_35, 0_36, 0_37, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
        0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77, 0_77,
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data items (not much here)
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected long _value;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Word36() {
        _value = 0L;
    }

    public Word36(
        final long value
    ) {
        _value = value;
    }

    public long getW() { return _value; }

    public Word36 setW(
        final long newValue
    ) {
        _value = newValue & 0_777777_777777L;
        return this;
    }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    public boolean isNegative()     { return (_value & NEGATIVE_BIT) == NEGATIVE_BIT; }
    public boolean isNegativeZero() { return _value == NEGATIVE_ZERO; }
    public boolean isPositive()     { return (_value & NEGATIVE_BIT) == 0; }
    public boolean isPositiveZero() { return _value == 0L; }
    public boolean isZero()         { return isPositiveZero() || isNegativeZero(); }

    public static boolean isNegative(long value)        { return (value & NEGATIVE_BIT) == NEGATIVE_BIT; }
    public static boolean isNegativeZero(long value)    { return value == NEGATIVE_ZERO; }
    public static boolean isPositive(long value)        { return (value & NEGATIVE_BIT) == 0; }
    public static boolean isPositiveZero(long value)    { return value == POSITIVE_ZERO; }
    public static boolean isZero(long value)            { return isPositiveZero(value) || isNegativeZero(value); }


    //  Partial-word extraction ----------------------------------------------------------------------------------------------------

    public int getH1()    { return (int)((_value & Word36.MASK_H1) >> 18); }
    public int getH2()    { return (int)(_value & Word36.MASK_H2); }
    public int getQ1()    { return (int)((_value & Word36.MASK_Q1) >> 27); }
    public int getQ2()    { return (int)((_value & Word36.MASK_Q2) >> 18); }
    public int getQ3()    { return (int)((_value & Word36.MASK_Q3) >> 9); }
    public int getQ4()    { return (int)(_value & Word36.MASK_Q4); }
    public int getS1()    { return (int)((_value & Word36.MASK_S1) >> 30); }
    public int getS2()    { return (int)((_value & Word36.MASK_S2) >> 24); }
    public int getS3()    { return (int)((_value & Word36.MASK_S3) >> 18); }
    public int getS4()    { return (int)((_value & Word36.MASK_S4) >> 12); }
    public int getS5()    { return (int)((_value & Word36.MASK_S5) >> 6); }
    public int getS6()    { return (int)(_value & Word36.MASK_S6); }
    public int getT1()    { return (int)((_value & Word36.MASK_T1) >> 24); }
    public int getT2()    { return (int)((_value & Word36.MASK_T2) >> 12); }
    public int getT3()    { return (int)(_value & Word36.MASK_T3); }

    public long getXH1()   { return getXH1(_value); }
    public long getXH2()   { return getXH2(_value); }
    public long getXT1()   { return getXT1(_value); }
    public long getXT2()   { return getXT2(_value); }
    public long getXT3()   { return getXT3(_value); }

    public static int getH1(long value)     { return (int)((value & Word36.MASK_H1) >> 18); }
    public static int getH2(long value)     { return (int)(value & Word36.MASK_H2); }
    public static int getQ1(long value)     { return (int)((value & Word36.MASK_Q1) >> 27); }
    public static int getQ2(long value)     { return (int)((value & Word36.MASK_Q2) >> 18); }
    public static int getQ3(long value)     { return (int)((value & Word36.MASK_Q3) >> 9); }
    public static int getQ4(long value)     { return (int)(value & Word36.MASK_Q4); }
    public static int getS1(long value)     { return (int)((value & Word36.MASK_S1) >> 30); }
    public static int getS2(long value)     { return (int)((value & Word36.MASK_S2) >> 24); }
    public static int getS3(long value)     { return (int)((value & Word36.MASK_S3) >> 18); }
    public static int getS4(long value)     { return (int)((value & Word36.MASK_S4) >> 12); }
    public static int getS5(long value)     { return (int)((value & Word36.MASK_S5) >> 6); }
    public static int getS6(long value)     { return (int)(value & Word36.MASK_S6); }
    public static int getT1(long value)     { return (int)((value & Word36.MASK_T1) >> 24); }
    public static int getT2(long value)     { return (int)((value & Word36.MASK_T2) >> 12); }
    public static int getT3(long value)     { return (int)(value & Word36.MASK_T3); }

    public static long getXH1(long value)
    {
        long result = getH1(value);
        if ((result & 0_400000) != 0)
            result |= 0777777_000000L;
        return result;
    }

    public static long getXH2(
        final long value
    ) {
        long result = getH2(value);
        if ((result & 0_400000) != 0)
            result |= 0_777777_000000L;
        return result;
    }

    public static long getXT1(
        final long value
    ) {
        long result = getT1(value);
        if ((result & 0_4000) != 0)
            result |= 0_7777_7777_0000L;
        return result;
    }

    public static long getXT2(
        final long value
    ) {
        long result = getT2(value);
        if ((result & 0_4000) != 0)
            result |= 0_7777_7777_0000L;
        return result;
    }

    public static long getXT3(
        final long value
    ) {
        long result = getT3(value);
        if ((result & 0_4000) != 0)
            result |= 0_7777_7777_0000L;
        return result;
    }


    //  Partial-word injection -----------------------------------------------------------------------------------------------------

    public Word36 setH1(int newValue) {
        _value = setH1(_value, newValue);
        return this;
    }

    public Word36 setH2(int newValue) {
        _value = setH2(_value, newValue);
        return this;
    }

    public Word36 setQ1(int newValue) {
        _value = setQ1(_value, newValue);
        return this;
    }

    public Word36 setQ2(int newValue) {
        _value = setQ2(_value, newValue);
        return this;
    }

    public Word36 setQ3(int newValue) {
        _value = setQ3(_value, newValue);
        return this;
    }

    public Word36 setQ4(int newValue) {
        _value = setQ4(_value, newValue);
        return this;
    }

    public Word36 setS1(int newValue) {
        _value = setS1(_value, newValue);
        return this;
    }

    public Word36 setS2(int newValue) {
        _value = setS2(_value, newValue);
        return this;
    }

    public Word36 setS3(int newValue) {
        _value = setS3(_value, newValue);
        return this;
    }

    public Word36 setS4(int newValue) {
        _value = setS4(_value, newValue);
        return this;
    }

    public Word36 setS5(int newValue) {
        _value = setS5(_value, newValue);
        return this;
    }

    public Word36 setS6(int newValue) {
        _value = setS6(_value, newValue);
        return this;
    }

    public Word36 setT1(int newValue) {
        _value = setT1(_value, newValue);
        return this;
    }

    public Word36 setT2(int newValue) {
        _value = setT2(_value, newValue);
        return this;
    }

    public Word36 setT3(int newValue) {
        _value = setT3(_value, newValue);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
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
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setT3(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_7777_7777_0000L) | (partialValue & 0_7777L);
    }


    //  Arithmetic Operations ------------------------------------------------------------------------------------------------------

    public Word36 decrement() {
        _value = decrement(_value);
        return this;
    }

    public static void add(
        final AdditionResult result,
        final long operand1,
        final long operand2
    ) {
        //  All values are ones-complement
        boolean neg1 = isNegative(operand1);
        boolean neg2 = isNegative(operand2);
        result._value = addSimple(operand1, operand2);
        if ((result._value & CARRY_BIT) != 0) {
            result._value &= BIT_MASK;
            ++result._value;
        }

        boolean negRes = isNegative(result._value);
        result._flags._carry = result._value < 0 ? (neg1 && neg2) : (neg1 || neg2);
        result._flags._overflow = (neg1 == neg2) && (neg1 != negRes);
    }

    public static long addSimple(
        final long operand1,
        final long operand2
    ) {
        //  certain values are twos-complement (native values) - all others are ones-complement
        if ((operand1 == NEGATIVE_ZERO) && (operand2 == NEGATIVE_ZERO)) {
            return NEGATIVE_ZERO;
        }

        long native1 = getTwosComplement(operand1);
        long native2 = getTwosComplement(operand2);
        return getOnesComplement(native1 + native2);
    }

    /**
     * Compares two values
     * Returns -1 if operand1 < operand2,
     *          1 if operand1 > operand2,
     *          0 if they are equal
     */
    public static int compare(
        final long operand1,
        final long operand2
    ) {
        return Long.compare(operand1, operand2);
    }

    /**
     * Subtracts 1 from the operand. Eliminates negative zero.
     */
    public static long decrement(
        final long operand
    ) {
        if (operand == 0L) {
            return NEGATIVE_ZERO;
        } else if (operand == NEGATIVE_ZERO) {
            return NEGATIVE_ONE;
        } else {
            return (operand - 1) & BIT_MASK;
        }
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
     * Arithmetically Negates a ones-complement 36-bit operand (i.e., flips the sign)
     * @param operand the operand to negate
     * @return the negated operand
     */
    public static long negate(
        final long operand
    ) {
        return operand ^ 0_777777_777777L;
    }


    //  Logical Operations ---------------------------------------------------------------------------------------------------------

    public Word36 logicalAnd(
        final long operand
    ) {
        _value = logicalAnd(_value, operand);
        return this;
    }

    public Word36 logicalNot() {
        _value = logicalNot(_value);
        return this;
    }

    public Word36 logicalOr(
        final long operand
    ) {
        _value = logicalOr(_value, operand);
        return this;
    }

    public Word36 logicalXor(
        final long operand
    ) {
        _value = logicalXor(_value, operand);
        return this;
    }

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

    /**
     * Does an algebraic shift left - the sign bit is never altered.
     * @param value 36-bit value to be shifted
     * @param count number of bits to be shifted
     * @return resulting value
     */
    public static long leftShiftAlgebraic(
        final long value,
        final int count
    ) {
        if (count < 0) {
            return rightShiftAlgebraic(value, -count);
        } else if (count == 0) {
            return value;
        } else {
            return (value & 0_400000_000000L) | ((value << count) & 0_377777_777777L);
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
            boolean isNegative = isNegative(value);
            if (count > 35) {
                return isNegative ? NEGATIVE_ZERO : 0;
            } else {
                long result = value >> count;
                if (isNegative) {
                    long bitMask = ((1L << count) - 1) << (36 - count);
                    result |= bitMask;
                }

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
            long mask = BIT_MASK >>> (36 - actualCount);
            long residue = (value & mask) << (36 - actualCount);
            return ((value >>> actualCount) | residue);
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
            return (count > 35) ? 0 : value >>> count;
        }
    }


    /**
     * Presuming the given value is a signed 12-bit value, we turn it into a 36-bit signed value
     */
    public static long getSignExtended12(
        final long value
    ) {
        if ((value & 0_4000) == 0) {
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


    //  Conversion from String to long -------------------------------------------------------------------------------------------

    /**
     * Creates a long value with quarter-words derived from the ASCII characters in the source string, of up to 4 characters.
     * If the string does not contain at least 4 characters, we pad the resulting output with blanks as necessary.
     * Any characters in the string beyond the fourth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static long stringToWordASCII(
        final String source
    ) {
        long value = 0;
        for (int cx = 0; cx < 4; ++cx) {
            value <<= 9;
            if (cx < source.length()) {
                value |= source.charAt(cx) & 0xFF;
            } else {
                value |= 0_40;
            }
        }

        return value;
    }

    /**
     * Creates an array of long values of sufficient length to contain the converted characters in the source string,
     * according to stringToWordASCII() above
     * @param source string to be converted
     * @return converted data
     */
    public static long[] stringToWordsASCII(
        final String source
    ) {
        int words = source.length() / 4;
        if (source.length() % 4 != 0) {
            words++;
        }

        var result = new long[words];
        for (int wx = 0; wx < result.length; ++wx) {
            int sx = wx * 4;
            result[wx] = stringToWordASCII(source.substring(sx, sx + 4));
        }

        return result;
    }

    /**
     * As above, but the given buffer is used for output, and the string is truncated if the output buffer is too small
     * to contain all the converted words.
     * @param source string to be converted
     * @param buffer where we store converted characters
     * @param offset location in the buffer of the first word to be written
     * @param length maximum number of words to be written
     */
    public static void stringToWordsASCII(
        final String source,
        final ArraySlice buffer,
        final int offset,
        final int length
    ) {
        int wx = offset;
        int cx = 0;
        while (cx < source.length()) {
            buffer.set(wx, stringToWordASCII(source.substring(cx, cx + 4)));
            wx++;
            cx += 4;
        }
        while (wx < length) {
            buffer.set(wx, 040040040040L);
            wx++;
        }
    }

    /**
     * Creates a long value with sixth-words representing the fieldata characters derived from the ASCII characters
     * in the source string, of up to 6 characters. If the string does not contain at least 6 characters, we pad the resulting
     * output with blanks as necessary. Any characters in the string beyond the sixth are ignored.
     * @param source string to be converted
     * @return converted data
     */
    public static long stringToWordFieldata(
        final String source
    ) {
        long value = 0;
        for (int cx = 0; cx < 6; ++cx) {
            value <<= 6;
            if (cx < source.length()) {
                value |= FIELDATA_FROM_ASCII[source.charAt(cx) & 0xff];
            } else {
                value |= 0_5;
            }
        }

        return value;
    }

    /**
     * Creates an array of long values of sufficient length to contain the converted characters in the source string,
     * according to stringToWordFieldata() above
     * @param source string to be converted
     * @return converted data
     */
    public static long[] stringToWordsFieldata(
        final String source
    ) {
        int words = source.length() / 6;
        if (source.length() % 6 != 0) {
            words++;
        }

        var result = new long[words];
        for (int wx = 0; wx < result.length; ++wx) {
            int sx = wx * 6;
            result[wx] = stringToWordFieldata(source.substring(sx, sx + 6));
        }

        return result;
    }

    /**
     * As above, but the given buffer is used for output, and the string is truncated if the output buffer is too small
     * to contain all the converted words.
     * @param source string to be converted
     * @param buffer where we store converted characters
     * @param offset location in the buffer of the first word to be written
     * @param length maximum number of words to be written
     */
    public static void stringToWordsFieldata(
        final String source,
        final ArraySlice buffer,
        final int offset,
        final int length
    ) {
        int wx = offset;
        int cx = 0;
        while (cx < source.length()) {
            buffer.set(wx, stringToWordFieldata(source.substring(cx, cx + 6)));
            wx++;
            cx += 6;
        }
        while (wx < length) {
            buffer.set(wx, 050505050505L);
            wx++;
        }
    }


    //  Formatting for display -----------------------------------------------------------------------------------------------------

    /**
     * Given an integer which represents an ASCII character, we return the corresponding char if it is displayable,
     * or else the alternate character.
     * @param value value to be converted
     * @param alternate character to be returned if the value presents an undisplayable character
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
     * Interprets the given 36-bit value as a sequence of 12 Octal digits, and produces those characters as a result
     * @param value 36-bit value
     * @return displayable result
     */
    public static String toOctal(
        final long value
    ) {
        return String.format("%012o", value);
    }

    /**
     * Interprets the given 36-bit value as a sequence of 4 ASCII characters, and produces those characters as a result
     * @param value 36-bit value
     * @return displayable result
     */
    public static String toStringFromASCII(
        final long value
    ) {
        return String.format("%s%s%s%s",
                             getASCIIForDisplay((int)getQ1(value), '.'),
                             getASCIIForDisplay((int)getQ2(value), '.'),
                             getASCIIForDisplay((int)getQ3(value), '.'),
                             getASCIIForDisplay((int)getQ4(value), '.'));
    }

    /**
     * Interprets the given array slice as an LJSF ASCII string
     */
    public static String toStringFromASCII(
        final ArraySlice slice
    ) {
        return Arrays.stream(slice.getAll())
                     .mapToObj(Word36::toStringFromASCII)
                     .collect(Collectors.joining());
    }

    /**
     * Interprets the given 36-bit value as a sequence of 6 Fieldata characters, and produces those characters as a result
     * @param value 36-bit value
     * @return displayable result
     */
    public static String toStringFromFieldata(
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
     * Interprets the given array slice as an LJSF fieldata string
     */
    public static String toStringFromFieldata(
        final ArraySlice slice
    ) {
        return Arrays.stream(slice.getAll())
                     .mapToObj(Word36::toStringFromFieldata)
                     .collect(Collectors.joining());
    }
}
