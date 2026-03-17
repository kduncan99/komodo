/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for com.bearsnake.komodo.baselib.Word36 class
 */
public class TestWord36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance method tests
    //  Where possible, the instance methods leverage the logic in the static methods.
    //  This simplifies unit testing as well as reducing the opportunity for bugs.
    //  ----------------------------------------------------------------------------------------------------------------------------

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
    public void toTwos_2() { assertEquals(0L, Word36.getTwosComplement(Word36.BIT_MASK)); }

    @Test
    public void toTwos_3() { assertEquals(0_377777_777777L, Word36.getTwosComplement(0_377777_777777L)); }

    @Test
    public void toTwos_4() { assertEquals(-0_314003_300624L, Word36.getTwosComplement(0463774_477153L)); }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    @Test
    public void isNegative_PositiveZero() {
        assertFalse(Word36.isNegative(Word36.POSITIVE_ZERO));
    }

    @Test
    public void isNegative_PositiveInteger() {
        assertFalse(Word36.isNegative(0_377777_777777L));
    }

    @Test
    public void isNegative_NegativeZero() {
        assertTrue(Word36.isNegative(Word36.NEGATIVE_ZERO));
    }

    @Test
    public void isNegative_NegativeInteger() {
        assertTrue(Word36.isNegative(0_400000_000000L));
    }

    @Test
    public void isPositive_PositiveZero() {
        assertTrue(Word36.isPositive(Word36.POSITIVE_ZERO));
    }

    @Test
    public void isPositive_PositiveInteger() {
        assertTrue(Word36.isPositive(0_377777_777777L));
    }

    @Test
    public void isPositive_NegativeZero() {
        assertFalse(Word36.isPositive(Word36.NEGATIVE_ZERO));
    }

    @Test
    public void isPositive_NegativeInteger() {
        assertFalse(Word36.isPositive(0_400000_000000L));
    }


    //  Partial-word getters -------------------------------------------------------------------------------------------------------

    @Test
    public void getH1() {
        assertEquals(0_123456L, Word36.getH1(0_123456_654321L));
    }

    @Test
    public void getH2() {
        assertEquals(0_654321L, Word36.getH2(0_123456_654321L));
    }

    @Test
    public void getQ1_0() {
        assertEquals(0, Word36.getQ1(07_000_777_777_777L));
    }

    @Test
    public void getQ1_0765() {
        assertEquals(0765, Word36.getQ1(07_765_777_777_777L));
    }

    @Test
    public void getQ2_0() {
        assertEquals(0, Word36.getQ2(07_777_000_777_777L));
    }

    @Test
    public void getQ2_0765() {
        assertEquals(0765, Word36.getQ2(07_777_765_777_777L));
    }

    @Test
    public void getQ3_0() {
        assertEquals(0, Word36.getQ3(07_777_777_000_777L));
    }

    @Test
    public void getQ3_0765() {
        assertEquals(0765, Word36.getQ3(07_777_777_765_777L));
    }

    @Test
    public void getQ4_0() {
        assertEquals(0, Word36.getQ4(07_777_777_777_000L));
    }

    @Test
    public void getQ4_0765() {
        assertEquals(0765, Word36.getQ4(07_777_777_777_765L));
    }

    @Test
    public void getS1() { assertEquals(0_12L, Word36.getS1(0_123456_654321L)); }

    @Test
    public void getS2() { assertEquals(0_34L, Word36.getS2(0_123456_654321L)); }

    @Test
    public void getS3() { assertEquals(0_56L, Word36.getS3(0_123456_654321L)); }

    @Test
    public void getS4() { assertEquals(0_65L, Word36.getS4(0_123456_654321L)); }

    @Test
    public void getS5() { assertEquals(0_43L, Word36.getS5(0_123456_654321L)); }

    @Test
    public void getS6() { assertEquals(0_21L, Word36.getS6(0_123456_654321L)); }

    @Test
    public void getT1() { assertEquals(0_1234L, Word36.getT1(0_1234_5665_4321L)); }

    @Test
    public void getT2() { assertEquals(0_5665L, Word36.getT2(0_1234_5665_4321L)); }

    @Test
    public void getT3() { assertEquals(0_4321L, Word36.getT3(0_1234_5665_4321L)); }

    @Test
    public void getXH1_Pos() { assertEquals(0_012345L, Word36.getXH1(0_012345_234567L)); }

    @Test
    public void getXH1_Neg() { assertEquals(0_777777_403020L, Word36.getXH1(0_403020_407060L)); }

    @Test
    public void getXH2_Pos() { assertEquals(0_234567L, Word36.getXH2(0_012345_234567L)); }

    @Test
    public void getXH2_Neg() { assertEquals(0_777777_407060L, Word36.getXH2(0_403020_407060L)); }

    @Test
    public void getXT1_Pos() { assertEquals(0_1234L, Word36.getXT1(0_1234_2345_3456L)); }

    @Test
    public void getXT1_Neg() { assertEquals(0_7777_7777_4321L, Word36.getXT1(0_4321_5432_6543L)); }

    @Test
    public void getXT2_Pos() { assertEquals(0_2345L, Word36.getXT2(0_1234_2345_3456L)); }

    @Test
    public void getXT2_Neg() { assertEquals(0_7777_7777_5432L, Word36.getXT2(0_4321_5432_6543L)); }

    @Test
    public void getXT3_Pos() { assertEquals(0_3456L, Word36.getXT3(0_1234_2345_3456L)); }

    @Test
    public void getXT3_Neg() { assertEquals(0_7777_7777_6543L, Word36.getXT3(0_4321_5432_6543L)); }

    //  Partial-word setters -------------------------------------------------------------------------------------------------------

    @Test
    public void setH1() {
        long result = Word36.setH1(0_012345, 0_775533L);
        assertEquals(0_775533_012345L, result);
    }

    @Test
    public void setH2() {
        long result = Word36.setH2(0_012345_543210L, 0_775533L);
        assertEquals(0_012345_775533L, result);
    }

    @Test
    public void setQ1() {
        long result = Word36.setQ1(0_525252_252525L, 0252);
        assertEquals(0_252252_252525L, result);
    }

    @Test
    public void setQ2() {
        long result = Word36.setQ2(0_525252_252525L, 0525);
        assertEquals(0_525525_252525L, result);
    }

    @Test
    public void setQ3() {
        long result = Word36.setQ3(0_525252_252525L, 0525);
        assertEquals(0_525252_525525L, result);
    }

    @Test
    public void setQ4() {
        long result = Word36.setQ4(0_525252_252525L, 0252);
        assertEquals(0_525252_252252L, result);
    }

    @Test
    public void setS1() {
        long result = Word36.setS1(0_777777_777777L, 0);
        assertEquals(0_007777_777777L, result);
    }

    @Test
    public void setS2() {
        long result = Word36.setS2(0_777777_777777L, 0);
        assertEquals(0_770077_777777L, result);
    }

    @Test
    public void setS3() {
        long result = Word36.setS3(0_777777_777777L, 0);
        assertEquals(0_777700_777777L, result);
    }

    @Test
    public void setS4() {
        long result = Word36.setS4(0_777777_777777L, 0);
        assertEquals(0_777777_007777L, result);
    }

    @Test
    public void setS5() {
        long result = Word36.setS5(0_777777_777777L, 0);
        assertEquals(0_777777_770077L, result);
    }

    @Test
    public void setS6() {
        long result = Word36.setS6(0_777777_777777L, 0);
        assertEquals(0_777777_777700L, result);
    }

    @Test
    public void setT1() {
        long result = Word36.setT1(0_4444_4444_4444L, 03333);
        assertEquals(0_3333_4444_4444L, result);
    }

    @Test
    public void setT2() {
        long result = Word36.setT2(0_4444_4444_4444L, 03333);
        assertEquals(0_4444_3333_4444L, result);
    }

    @Test
    public void setT3() {
        long result = Word36.setT3(0_4444_4444_4444L, 03333);
        assertEquals(0_4444_4444_3333L, result);
    }

    @Test
    public void setW() {
        long result = 0;
        assertEquals(0, result);
    }


    //  Arithmetic -----------------------------------------------------------------------------------------------------------------

    @Test
    public void addPosPos() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, 25, 1027);
        assertEquals(1052, saResult._value);
        assertFalse(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addPosPosOverflow() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, 0_377777_777777L, 1);
        assertFalse(saResult._flags._carry);
        assertTrue(saResult._flags._overflow);
    }

    @Test
    public void addPosNegResultPos() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, 1234, Word36.getOnesComplement(-234));
        assertEquals(1000, saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addPosNegResultNeg() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, 234, Word36.getOnesComplement(-1234));
        assertEquals(Word36.getOnesComplement(-1000), saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addNegNeg() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, Word36.getOnesComplement(-1992), Word36.getOnesComplement(-2933));
        assertEquals(Word36.getOnesComplement(-1992-2933), saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addNegNegOverflow() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, 0_400000_000000L, 0_777777_777776L);
        assertTrue(saResult._flags._carry);
        assertTrue(saResult._flags._overflow);
    }

    @Test
    public void addPosZPosZ() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, Word36.POSITIVE_ZERO, Word36.POSITIVE_ZERO);
        assertEquals(Word36.POSITIVE_ZERO, saResult._value);
        assertFalse(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addPosZNegZ() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, Word36.POSITIVE_ZERO, Word36.NEGATIVE_ZERO);
        assertEquals(Word36.POSITIVE_ZERO, saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addNegZPosZ() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, Word36.NEGATIVE_ZERO, Word36.POSITIVE_ZERO);
        assertEquals(Word36.POSITIVE_ZERO, saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addNegZNegZ() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, Word36.NEGATIVE_ZERO, Word36.NEGATIVE_ZERO);
        assertEquals(Word36.NEGATIVE_ZERO, saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void addInverses() {
        Word36.AdditionResult saResult = new Word36.AdditionResult();
        saResult._flags = new Word36.Flags();
        Word36.add(saResult, Word36.getOnesComplement(19883), Word36.getOnesComplement(-19883));
        assertEquals(Word36.POSITIVE_ZERO, saResult._value);
        assertTrue(saResult._flags._carry);
        assertFalse(saResult._flags._overflow);
    }

    @Test
    public void negate_PositiveOne() {
        long result = Word36.negate(Word36.POSITIVE_ONE);
        assertEquals(Word36.NEGATIVE_ONE, result);
    }

    @Test
    public void negate_PositiveZero() {
        long result = Word36.negate(Word36.POSITIVE_ZERO);
        assertEquals(Word36.NEGATIVE_ZERO, result);
    }

    @Test
    public void negate_NegativeOne() {
        long result = Word36.negate(Word36.NEGATIVE_ONE);
        assertEquals(Word36.POSITIVE_ONE, result);
    }

    @Test
    public void negate_NegativeZero() {
        long result = Word36.negate(Word36.NEGATIVE_ZERO);
        assertEquals(Word36.POSITIVE_ZERO, result);
    }

    @Test
    public void decrement_Regular() {
        assertEquals(4L, Word36.decrement(5L));
    }

    @Test
    public void decrement_PositiveOne() {
        assertEquals(Word36.POSITIVE_ZERO, Word36.decrement(Word36.POSITIVE_ONE));
    }

    @Test
    public void decrement_PositiveZero() {
        assertEquals(Word36.NEGATIVE_ZERO, Word36.decrement(Word36.POSITIVE_ZERO));
    }

    @Test
    public void decrement_NegativeZero() {
        assertEquals(Word36.NEGATIVE_ONE, Word36.decrement(Word36.NEGATIVE_ZERO));
    }

    @Test
    public void decrement_Negative() {
        assertEquals(0_777777_777771L, Word36.decrement(0_777777_777772L));
    }

    @Test
    public void decrement_Instance() {
        Word36 word = new Word36(5L);
        word.decrement();
        assertEquals(4L, word.getW());
    }


    //  Shifts ---------------------------------------------------------------------------------------------------------------------

    //TODO a few more leftShiftAlgebraic tests

    @Test
    public void leftShiftAlgebraic() {
        //  sign bit always remains unchanged...
        long parameter = 0_3123_4537_0123L;
        long expected =  0_2247_1276_0246L;
        long result = Word36.leftShiftAlgebraic(parameter, 1);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftCircular_by0() {
        long parameter = 0_111222_333444L;
        long expected = 0_111222_333444L;
        long result = Word36.leftShiftCircular(parameter, 0);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftCircular_by3() {
        long parameter = 0_111222_333444L;
        long expected = 0_112223_334441L;
        long result = Word36.leftShiftCircular(parameter, 3);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftCircular_by36() {
        long parameter = 0_111222_333444L;
        long expected = 0_111222_333444L;
        long result = Word36.leftShiftCircular(parameter, 36);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftCircular_byNeg() {
        long parameter = 0_111222_333444L;
        long expected = 0_441112_223334L;
        long result = Word36.leftShiftCircular(parameter, -6);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftLogical_by3() {
        long parameter = 0_111222_333444L;
        long expected = 0_112223_334440L;
        long result = Word36.leftShiftLogical(parameter, 3);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftLogical_by36() {
        long parameter = 0_111222_333444L;
        long expected = 0;
        long result = Word36.leftShiftLogical(parameter, 36);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftLogical_negCount() {
        long parameter = 0_111222_333444L;
        long expected = 0_001112_223334L;
        long result = Word36.leftShiftLogical(parameter, -6);
        assertEquals(expected, result);
    }

    @Test
    public void leftShiftLogical_zeroCount() {
        long parameter = 0_111222_333444L;
        long expected = 0_111222_333444L;
        long result = Word36.leftShiftLogical(parameter, 0);
        assertEquals(expected, result);
    }

    @Test
    public void rightShiftAlgebraic_negCount() {
        long parameter = 033225L;
        long expResult = 0332250L;
        long result = Word36.rightShiftAlgebraic(parameter, -3);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_neg_3Count() {
        long parameter = 0_400000_112233L;
        long expResult = 0_740000_011223L;
        long result = Word36.rightShiftAlgebraic(parameter, 3);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_neg_34Count() {
        long parameter = 0_421456_321456L;
        long expResult = 0_777777_777742L;
        long result = Word36.rightShiftAlgebraic(parameter, 30);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_neg_minus18Count() {
        long parameter = 0_423232_123123L;
        long expResult = 0_523123_000000L;
        long result = Word36.rightShiftAlgebraic(parameter, -18);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_neg_36Count() {
        long parameter = 0_421456_321456L;
        long expResult = 0_777777_777777L;
        long result = Word36.rightShiftAlgebraic(parameter, 36);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_pos_3Count() {
        long parameter = 033225L;
        long expResult = parameter >> 3;
        long result = Word36.rightShiftAlgebraic(parameter, 3);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_pos_34Count() {
        long parameter = 0_321456_321456L;
        long expResult = parameter >> 34;
        long result = Word36.rightShiftAlgebraic(parameter, 34);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_pos_35Count() {
        long parameter = 0_321456_321456L;
        long expResult = 0;
        long result = Word36.rightShiftAlgebraic(parameter, 35);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_pos_36Count() {
        long parameter = 0_321456_321456L;
        long expResult = 0;
        long result = Word36.rightShiftAlgebraic(parameter, 36);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftAlgebraic_zeroCount() {
        long parameter = 033225L;
        long expResult = 033225L;
        long result = Word36.rightShiftAlgebraic(parameter, 0);
        assertEquals(expResult, result);
    }

    @Test
    public void rightShiftCircular_1() {
        long result = Word36.rightShiftCircular(0_112233_445566L, 6);
        assertEquals(0_661122_334455L, result);
    }

    @Test
    public void rightShiftCircular_2() {
        long result = Word36.rightShiftCircular(0_112200_334400L, 3);
        assertEquals(0_011220_033440L, result);
    }

    @Test
    public void rightShiftLogical() {
        long result = Word36.rightShiftLogical(0_112233_445566L, 9);
        assertEquals(0_000112_233445L, result);
    }


    //  Logic tests ----------------------------------------------------------------------------------------------------------------

    @Test
    public void and() {
        long op1 = 0_776655_221100L;
        long op2 = 0_765432_543210L;
        long exp = 0_764410_001000L;
        long result = Word36.logicalAnd(op1, op2);
        assertEquals(exp, result);
    }

    @Test
    public void not() {
        long op1 = 0_776655_221100L;
        long exp = 0_001122_556677L;
        long result = Word36.logicalNot(op1);
        assertEquals(exp, result);
    }

    @Test
    public void or() {
        long op1 = 0_776655_221100L;
        long op2 = 0_765432_543210L;
        long exp = 0_777677_763310L;
        long result = Word36.logicalOr(op1, op2);
        assertEquals(exp, result);
    }

    @Test
    public void xor() {
        long op1 = 0_776655_221100L;
        long op2 = 0_765432_543210L;
        long exp = 0_013267_762310L;
        long result = Word36.logicalXor(op1, op2);
        assertEquals(exp, result);
    }


    //  Display --------------------------------------------------------------------------------------------------------------------

    @Test
    public void toASCII() {
        long word = 0_101_102_103_104L;
        assertEquals("ABCD", Word36.toStringFromASCII(word));
    }

    @Test
    public void toFieldata() {
        long word = 0_05_06_07_10_11_12L;
        assertEquals(" ABCDE", Word36.toStringFromFieldata(word));
    }

    @Test
    public void toOctal() {
        long word = 0_05_06_07_10_11_12L;
        assertEquals("050607101112", Word36.toOctal(word));
    }


    //  Misc -----------------------------------------------------------------------------------------------------------------------

    @Test
    public void stringToWordASCII() {
        var w = Word36.stringToWordASCII("Help");
        assertEquals(0_110_145_154_160L, w);
    }

    @Test
    public void stringToWordASCII_over() {
        var w = Word36.stringToWordASCII("HelpSlop");
        assertEquals(0_110_145_154_160L, w);
    }

    @Test
    public void stringToWordASCII_partial() {
        var w = Word36.stringToWordASCII("01");
        assertEquals(0_060_061_040_040L, w);
    }

    @Test
    public void stringToWordFieldata() {
        var w = Word36.stringToWordFieldata("Abc@23");
        assertEquals(0_060710_006263L, w);
    }

    @Test
    public void stringToWordFieldata_over() {
        var w = Word36.stringToWordFieldata("A B C@D E F");
        assertEquals(0_060507_051000L, w);
    }

    @Test
    public void stringToWordFieldata_partial() {
        var w = Word36.stringToWordFieldata("1234");
        assertEquals(0_616263_640505L, w);
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
