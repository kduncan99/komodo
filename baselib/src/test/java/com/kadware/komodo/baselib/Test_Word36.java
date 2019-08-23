/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for Word36 class
 */
public class Test_Word36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance method tests
    //  Where possible, the instance methods leverage the logic in the static methods.
    //  This simplifies unit testing as well as reducing the opportunity for bugs.
    //  ----------------------------------------------------------------------------------------------------------------------------

    //TODO need tests for multiplication

    //  Conversions ----------------------------------------------------------------------------------------------------------------

    @Test
    public void toOnes_1() { assertEquals(0L, Word36.getOnesComplement(0)); }

    @Test
    public void toOnes_2() { assertEquals(0_377777_777777L, Word36.getOnesComplement(0_377777_777777L)); }

    @Test
    public void toOnes_3() { assertEquals(0_777776_702003L, Word36.getOnesComplement(-293884)); }

    @Test
    public void toTwos_1() { assertEquals(0L, Word36.getTwosComplement(0)); }

    @Test
    public void toTwos_2() { assertEquals(0_377777_777777L, Word36.getTwosComplement(0_377777_777777L)); }

    @Test
    public void toTwos_3() { assertEquals(0L - 0_314003_300624L, Word36.getTwosComplement(0463774_477153L)); }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    @Test
    public void isNegative_PositiveZero() {
        Word36 word36 = new Word36(Word36.POSITIVE_ZERO);
        assertFalse(word36.isNegative());
    }

    @Test
    public void isNegative_PositiveInteger() {
        Word36 word36 = new Word36(0_377777_777777L);
        assertFalse(word36.isNegative());
    }

    @Test
    public void isNegative_NegativeZero() {
        Word36 word36 = new Word36(Word36.NEGATIVE_ZERO);
        assertTrue(word36.isNegative());
    }

    @Test
    public void isNegative_NegativeInteger() {
        Word36 word36 = new Word36(0_400000_000000L);
        assertTrue(word36.isNegative());
    }

    @Test
    public void isPositive_PositiveZero() {
        Word36 word36 = new Word36(Word36.POSITIVE_ZERO);
        assertTrue(word36.isPositive());
    }

    @Test
    public void isPositive_PositiveInteger() {
        Word36 word36 = new Word36(0_377777_777777L);
        assertTrue(word36.isPositive());
    }

    @Test
    public void isPositive_NegativeZero() {
        Word36 word36 = new Word36(Word36.NEGATIVE_ZERO);
        assertFalse(word36.isPositive());
    }

    @Test
    public void isPositive_NegativeInteger() {
        Word36 word36 = new Word36(0_400000_000000L);
        assertFalse(word36.isPositive());
    }


    //  Partial-word getters -------------------------------------------------------------------------------------------------------

    @Test
    public void getH1() {
        assertEquals(0_123456L, (new Word36(0_123456_654321L).getH1()));
    }

    @Test
    public void getH2() {
        assertEquals(0_654321L, (new Word36(0_123456_654321L).getH2()));
    }

    @Test
    public void getQ1_0() {
        Word36 w = new Word36(07_000_777_777_777L);
        assertEquals(0, w.getQ1());
    }

    @Test
    public void getQ1_0765() {
        Word36 w = new Word36(07_765_777_777_777L);
        assertEquals(0765, w.getQ1());
    }

    @Test
    public void getQ2_0() {
        Word36 w = new Word36(07_777_000_777_777L);
        assertEquals(0, w.getQ2());
    }

    @Test
    public void getQ2_0765() {
        Word36 w = new Word36(07_777_765_777_777L);
        assertEquals(0765, w.getQ2());
    }

    @Test
    public void getQ3_0() {
        Word36 w = new Word36(07_777_777_000_777L);
        assertEquals(0, w.getQ3());
    }

    @Test
    public void getQ3_0765() {
        Word36 w = new Word36(07_777_777_765_777L);
        assertEquals(0765, w.getQ3());
    }

    @Test
    public void getQ4_0() {
        Word36 w = new Word36(07_777_777_777_000L);
        assertEquals(0, w.getQ4());
    }

    @Test
    public void getQ4_0765() {
        Word36 w = new Word36(07_777_777_777_765L);
        assertEquals(0765, w.getQ4());
    }

    @Test
    public void getS1() { assertEquals(0_12L, (new Word36(0_123456_654321L).getS1())); }

    @Test
    public void getS2() { assertEquals(0_34L, (new Word36(0_123456_654321L).getS2())); }

    @Test
    public void getS3() { assertEquals(0_56L, (new Word36(0_123456_654321L).getS3())); }

    @Test
    public void getS4() { assertEquals(0_65L, (new Word36(0_123456_654321L).getS4())); }

    @Test
    public void getS5() { assertEquals(0_43L, (new Word36(0_123456_654321L).getS5())); }

    @Test
    public void getS6() { assertEquals(0_21L, (new Word36(0_123456_654321L).getS6())); }

    @Test
    public void getT1() { assertEquals(0_1234L, (new Word36(0_1234_5665_4321L).getT1())); }

    @Test
    public void getT2() { assertEquals(0_5665L, (new Word36(0_1234_5665_4321L).getT2())); }

    @Test
    public void getT3() { assertEquals(0_4321L, (new Word36(0_1234_5665_4321L).getT3())); }

    @Test
    public void getXH1_Pos() { assertEquals(0_012345L, (new Word36(0_012345_234567L).getXH1())); }

    @Test
    public void getXH1_Neg() { assertEquals(0_777777_403020L, (new Word36(0_403020_407060L).getXH1())); }

    @Test
    public void getXH2_Pos() { assertEquals(0_234567L, (new Word36(0_012345_234567L).getXH2())); }

    @Test
    public void getXH2_Neg() { assertEquals(0_777777_407060L, (new Word36(0_403020_407060L).getXH2())); }

    @Test
    public void getXT1_Pos() { assertEquals(0_1234L, (new Word36(0_1234_2345_3456L).getXT1())); }

    @Test
    public void getXT1_Neg() { assertEquals(0_7777_7777_4321L, (new Word36(0_4321_5432_6543L).getXT1())); }

    @Test
    public void getXT2_Pos() { assertEquals(0_2345L, (new Word36(0_1234_2345_3456L).getXT2())); }

    @Test
    public void getXT2_Neg() { assertEquals(0_7777_7777_5432L, (new Word36(0_4321_5432_6543L).getXT2())); }

    @Test
    public void getXT3_Pos() { assertEquals(0_3456L, (new Word36(0_1234_2345_3456L).getXT3())); }

    @Test
    public void getXT3_Neg() { assertEquals(0_7777_7777_6543L, (new Word36(0_4321_5432_6543L).getXT3())); }

    @Test
    public void getW() { assertEquals(0_777555_333111L, (new Word36(0_777555_333111L).getW())); }


    //  Partial-word setters -------------------------------------------------------------------------------------------------------

    @Test
    public void setH1() {
        Word36 word36 = new Word36(0_012345);
        word36.setH1(0_775533L);
        assertEquals(0_775533_012345L, word36.getW());
    }

    @Test
    public void setH2() {
        Word36 word36 = new Word36(0_012345_543210L);
        word36.setH2(0_775533L);
        assertEquals(0_012345_775533L, word36.getW());
    }

    @Test
    public void setQ1() {
        Word36 word36 = new Word36(0_525252_252525L);
        word36.setQ1(0252);
        assertEquals(0_252252_252525L, word36.getW());
    }

    @Test
    public void setQ2() {
        Word36 word36 = new Word36(0_525252_252525L);
        word36.setQ2(0525);
        assertEquals(0_525525_252525L, word36.getW());
    }

    @Test
    public void setQ3() {
        Word36 word36 = new Word36(0_525252_252525L);
        word36.setQ3(0525);
        assertEquals(0_525252_525525L, word36.getW());
    }

    @Test
    public void setQ4() {
        Word36 word36 = new Word36(0_525252_252525L);
        word36.setQ4(0252);
        assertEquals(0_525252_252252L, word36.getW());
    }

    @Test
    public void setS1() {
        Word36 word36 = new Word36(0_777777_777777L);
        word36.setS1(0);
        assertEquals(0_007777_777777L, word36.getW());
    }

    @Test
    public void setS2() {
        Word36 word36 = new Word36(0_777777_777777L);
        word36.setS2(0);
        assertEquals(0_770077_777777L, word36.getW());
    }

    @Test
    public void setS3() {
        Word36 word36 = new Word36(0_777777_777777L);
        word36.setS3(0);
        assertEquals(0_777700_777777L, word36.getW());
    }

    @Test
    public void setS4() {
        Word36 word36 = new Word36(0_777777_777777L);
        word36.setS4(0);
        assertEquals(0_777777_007777L, word36.getW());
    }

    @Test
    public void setS5() {
        Word36 word36 = new Word36(0_777777_777777L);
        word36.setS5(0);
        assertEquals(0_777777_770077L, word36.getW());
    }

    @Test
    public void setS6() {
        Word36 word36 = new Word36(0_777777_777777L);
        word36.setS6(0);
        assertEquals(0_777777_777700L, word36.getW());
    }

    @Test
    public void setT1() {
        Word36 word36 = new Word36(0_4444_4444_4444L);
        word36.setT1(03333);
        assertEquals(0_3333_4444_4444L, word36.getW());
    }

    @Test
    public void setT2() {
        Word36 word36 = new Word36(0_4444_4444_4444L);
        word36.setT2(03333);
        assertEquals(0_4444_3333_4444L, word36.getW());
    }

    @Test
    public void setT3() {
        Word36 word36 = new Word36(0_4444_4444_4444L);
        word36.setT3(03333);
        assertEquals(0_4444_4444_3333L, word36.getW());
    }

    @Test
    public void setW() {
        Word36 word36 = new Word36(0_4444_4444_4444L);
        word36.setW(0);
        assertEquals(0, word36.getW());
    }


    //  Arithmetic -----------------------------------------------------------------------------------------------------------------

    @Test
    public void addPosPos() {
        Word36 w1 = new Word36(25);
        Word36 w2 = new Word36(1027);
        Word36.Flags f = w1.add(w2);
        assertEquals(1052, w1.getTwosComplement());
        assertFalse(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addPosPosOverflow() {
        Word36 w1 = new Word36(0_377777_777777L);
        Word36 w2 = new Word36(1);
        Word36.Flags f = w1.add(w2);
        assertFalse(f._carry);
        assertTrue(f._overflow);
    }

    @Test
    public void addPosNegResultPos() {
        Word36 w1 = new Word36(Word36.getOnesComplement(1234));
        Word36 w2 = new Word36(Word36.getOnesComplement(-234));
        Word36.Flags f = w1.add(w2);
        assertEquals(1000, w1.getTwosComplement());
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addPosNegResultNeg() {
        Word36 w1 = new Word36(Word36.getOnesComplement(234));
        Word36 w2 = new Word36(Word36.getOnesComplement(-1234));
        Word36.Flags f = w1.add(w2);
        assertEquals(-1000, w1.getTwosComplement());
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addNegNeg() {
        Word36 w1 = new Word36(Word36.getOnesComplement(-1992));
        Word36 w2 = new Word36(Word36.getOnesComplement(-2933));
        Word36.Flags f = w1.add(w2);
        assertEquals(-1992-2933, w1.getTwosComplement());
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addNegNegOverflow() {
        Word36 w1 = new Word36(0_400000_000000L);
        Word36 w2 = new Word36(0_777777_777776L);
        Word36.Flags f = w1.add(w2);
        assertTrue(f._carry);
        assertTrue(f._overflow);
    }

    @Test
    public void addPosZPosZ() {
        Word36 w1 = new Word36(Word36.POSITIVE_ZERO._value);
        Word36 w2 = new Word36(Word36.POSITIVE_ZERO._value);
        Word36.Flags f = w1.add(w2);
        assertEquals(Word36.POSITIVE_ZERO, w1);
        assertFalse(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addPosZNegZ() {
        Word36 w1 = new Word36(Word36.POSITIVE_ZERO._value);
        Word36 w2 = new Word36(Word36.NEGATIVE_ZERO._value);
        Word36.Flags f = w1.add(w2);
        assertEquals(Word36.POSITIVE_ZERO, w1);
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addNegZPosZ() {
        Word36 w1 = new Word36(Word36.NEGATIVE_ZERO._value);
        Word36 w2 = new Word36(Word36.POSITIVE_ZERO._value);
        Word36.Flags f = w1.add(w2);
        assertEquals(Word36.POSITIVE_ZERO, w1);
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addNegZNegZ() {
        Word36 w1 = new Word36(Word36.NEGATIVE_ZERO._value);
        Word36 w2 = new Word36(Word36.NEGATIVE_ZERO._value);
        Word36.Flags f = w1.add(w2);
        assertEquals(Word36.NEGATIVE_ZERO, w1);
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void addInverses() {
        Word36 w1 = new Word36(Word36.getOnesComplement(19883));
        Word36 w2 = new Word36(Word36.getOnesComplement(-19883));
        Word36.Flags f = w1.add(w2);
        assertEquals(Word36.POSITIVE_ZERO, w1);
        assertTrue(f._carry);
        assertFalse(f._overflow);
    }

    @Test
    public void negate36_PositiveOne() {
        Word36 word36 = new Word36(Word36.POSITIVE_ONE);
        word36.negate();
        assertEquals(Word36.NEGATIVE_ONE._value, word36.getW());
    }

    @Test
    public void negate36_PositiveZero() {
        Word36 word36 = new Word36(Word36.POSITIVE_ZERO);
        word36.negate();
        assertEquals(Word36.NEGATIVE_ZERO._value, word36.getW());
    }

    @Test
    public void negate36_NegativeOne() {
        Word36 word36 = new Word36(Word36.NEGATIVE_ONE);
        word36.negate();
        assertEquals(Word36.POSITIVE_ONE._value, word36.getW());
    }

    @Test
    public void negate36_NegativeZero() {
        Word36 word36 = new Word36(Word36.NEGATIVE_ZERO);
        word36.negate();
        assertEquals(Word36.POSITIVE_ZERO._value, word36.getW());
    }


    //  Shifts ---------------------------------------------------------------------------------------------------------------------

    @Test
    public void leftShiftCircular36_by0() {
        long parameter = 0_111222_333444L;
        long expected = 0_111222_333444L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftCircular(0);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftCircular36_by3() {
        long parameter = 0_111222_333444L;
        long expected = 0_112223_334441L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftCircular(3);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftCircular36_by36() {
        long parameter = 0_111222_333444L;
        long expected = 0_111222_333444L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftCircular(36);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftCircular36_byNeg() {
        long parameter = 0_111222_333444L;
        long expected = 0_441112_223334L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftCircular(-6);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftLogical36_by3() {
        long parameter = 0_111222_333444L;
        long expected = 0_112223_334440L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftLogical(3);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftLogical36_by36() {
        long parameter = 0_111222_333444L;
        long expected = 0;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftLogical(36);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftLogical36_negCount() {
        long parameter = 0_111222_333444L;
        long expected = 0_001112_223334L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftLogical(-6);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void leftShiftLogical36_zeroCount() {
        long parameter = 0_111222_333444L;
        long expected = 0_111222_333444L;
        Word36 word36 = new Word36(parameter);
        word36.leftShiftLogical(0);
        assertEquals(expected, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_negCount() {
        long parameter = 033225L;
        long expResult = 0332250L;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(-3);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_neg_3Count() {
        long parameter = 0400000_112233L;
        long expResult = 0740000_011223L;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(3);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_neg_34Count() {
        long parameter = 0_421456_321456L;
        long expResult = 0_777777_777776L;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(34);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_neg_35Count() {
        long parameter = 0_421456_321456L;
        long expResult = 0_777777_777777L;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(35);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_neg_36Count() {
        long parameter = 0_421456_321456L;
        long expResult = 0_777777_777777L;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(36);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_pos_3Count() {
        long parameter = 033225L;
        long expResult = parameter >> 3;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(3);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_pos_34Count() {
        long parameter = 0_321456_321456L;
        long expResult = parameter >> 34;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(34);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_pos_35Count() {
        long parameter = 0_321456_321456L;
        long expResult = parameter >> 35;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(35);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_pos_36Count() {
        long parameter = 0_321456_321456L;
        long expResult = 0;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(36);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftAlgebraic36_zeroCount() {
        long parameter = 033225L;
        long expResult = 033225L;
        Word36 word36 = new Word36(parameter);
        word36.rightShiftAlgebraic(0);
        assertEquals(expResult, word36.getW());
    }

    @Test
    public void rightShiftCircular_1() {
        Word36 word36 = new Word36(0_112233_445566L);
        word36.rightShiftCircular(6);
        assertEquals(0_661122_334455L, word36.getW());
    }

    @Test
    public void rightShiftCircular_2() {
        Word36 word36 = new Word36(0_112200_334400L);
        word36.rightShiftCircular(3);
        assertEquals(0_011220_033440L, word36.getW());
    }

    @Test
    public void rightShiftLogical() {
        Word36 word36 = new Word36(0_112233_445566L);
        word36.rightShiftLogical(9);
        assertEquals(0_000112_233445L, word36.getW());
    }


    //  Logic tests ----------------------------------------------------------------------------------------------------------------

    @Test
    public void and() {
        Word36 op1 = new Word36(0_776655_221100L);
        Word36 op2 = new Word36(0_765432_543210L);
        Word36 exp = new Word36(0_764410_001000L);
        op1.logicalAnd(op2);
        assertEquals(exp, op1);
    }

    @Test
    public void not() {
        Word36 op1 = new Word36(0_776655_221100L);
        Word36 exp = new Word36(0_001122_556677L);
        op1.logicalNot();
        assertEquals(exp, op1);
    }

    @Test
    public void or() {
        Word36 op1 = new Word36(0_776655_221100L);
        Word36 op2 = new Word36(0_765432_543210L);
        Word36 exp = new Word36(0_777677_763310L);
        op1.logicalOr(op2);
        assertEquals(exp, op1);
    }

    @Test
    public void xor() {
        Word36 op1 = new Word36(0_776655_221100L);
        Word36 op2 = new Word36(0_765432_543210L);
        Word36 exp = new Word36(0_013267_762310L);
        op1.logicalXor(op2);
        assertEquals(exp, op1);
    }


    //  Display --------------------------------------------------------------------------------------------------------------------

    @Test
    public void toASCII() {
        long word = 0_101_102_103_104L;
        assertEquals("ABCD", Word36.toASCII(word));
    }

    @Test
    public void toFieldata() {
        long word = 0_05_06_07_10_11_12L;
        assertEquals(" ABCDE", Word36.toFieldata(word));
    }

    @Test
    public void toOctal() {
        long word = 0_05_06_07_10_11_12L;
        assertEquals("050607101112", Word36.toOctal(word));
    }


    //  Misc -----------------------------------------------------------------------------------------------------------------------

    @Test
    public void stringToWord36ASCII() {
        Word36 w = Word36.stringToWordASCII("Help");
        assertEquals(0_110_145_154_160L, w.getW());
    }

    @Test
    public void stringToWord36ASCII_over() {
        Word36 w = Word36.stringToWordASCII("HelpSlop");
        assertEquals(0_110_145_154_160L, w.getW());
    }

    @Test
    public void stringToWord36ASCII_partial() {
        Word36 w = Word36.stringToWordASCII("01");
        assertEquals(0_060_061_040_040L, w.getW());
    }

    @Test
    public void stringToWord36Fieldata() {
        Word36 w = Word36.stringToWordFieldata("Abc@23");
        assertEquals(0_060710_006263L, w.getW());
    }

    @Test
    public void stringToWord36Fieldata_over() {
        Word36 w = Word36.stringToWordFieldata("A B C@D E F");
        assertEquals(0_060507_051000L, w.getW());
    }

    @Test
    public void stringToWord36Fieldata_partial() {
        Word36 w = Word36.stringToWordFieldata("1234");
        assertEquals(0_616263_640505L, w.getW());
    }


    //  Sign-extension tests -------------------------------------------------------------------------------------------------------

    @Test
    public void getSignExtended12_positive() {
        assertEquals(03765, Word36.getSignExtended12(03765));
    }

    @Test
    public void getSignExtended12_negative() {
        assertEquals(0_777777_774765L, Word36.getSignExtended12(04765));
    }

    @Test
    public void getSignExtended18_positive() {
        assertEquals(0_376500, Word36.getSignExtended18(0_376500));
    }

    @Test
    public void getSignExtended18_negative() {
        assertEquals(0_777777_400001L, Word36.getSignExtended18(0_400001));
    }

    @Test
    public void getSignExtended24_positive() {
        assertEquals(0_000037_776500L, Word36.getSignExtended24(0_000037_776500L));
    }

    @Test
    public void getSignExtended24_negative() {
        assertEquals(0_777767_776500L, Word36.getSignExtended24(0_000067_776500L));
    }
}
