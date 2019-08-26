/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for Word36 class
 */
public class Test_DoubleWord36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance method tests
    //  Where possible, the instance methods leverage the logic in the static methods.
    //  This simplifies unit testing as well as reducing the opportunity for bugs.
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Conversions ----------------------------------------------------------------------------------------------------------------

    @Test
    public void toOnes_1() {
        assertEquals(BigInteger.ZERO, DoubleWord36.getOnesComplement(BigInteger.ZERO));
    }

    @Test
    public void toOnes_2() {
        assertEquals(DoubleWord36.SHORT_BIT_MASK, DoubleWord36.getOnesComplement(DoubleWord36.SHORT_BIT_MASK));
    }

    @Test
    public void toOnes_3() {
        BigInteger op = BigInteger.valueOf(0_377777_777777L).shiftLeft(36).or(BigInteger.valueOf(0_777777_777777L));
        assertEquals(op, DoubleWord36.getOnesComplement(op));
    }

    @Test
    public void toOnes_4() {
        BigInteger op = BigInteger.valueOf(-293884);
        BigInteger expected = BigInteger.valueOf(0_777777_777777L).shiftLeft(36).or(BigInteger.valueOf(0_777776_702003L));
        assertEquals(expected, DoubleWord36.getOnesComplement(op));
    }

    @Test
    public void toTwos_1() {
        assertEquals(BigInteger.ZERO, DoubleWord36.getTwosComplement(BigInteger.ZERO));
    }

    @Test
    public void toTwos_2() {
        assertEquals(BigInteger.ZERO, DoubleWord36.getTwosComplement(DoubleWord36.BIT_MASK));
    }

//    @Test
//    public void toTwos_3() {
//        assertEquals(0_377777_777777L, DoubleWord36.getTwosComplement(0_377777_777777L));
//    }
//
//    @Test
//    public void toTwos_4() {
//        assertEquals(0L - 0_314003_300624L, DoubleWord36.getTwosComplement(0463774_477153L));
//    }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

//    @Test
//    public void isNegative_PositiveZero() {
//        Word36 word36 = new Word36(Word36.POSITIVE_ZERO);
//        assertFalse(word36.isNegative());
//    }
//
//    @Test
//    public void isNegative_PositiveInteger() {
//        Word36 word36 = new Word36(0_377777_777777L);
//        assertFalse(word36.isNegative());
//    }
//
//    @Test
//    public void isNegative_NegativeZero() {
//        Word36 word36 = new Word36(Word36.NEGATIVE_ZERO);
//        assertTrue(word36.isNegative());
//    }
//
//    @Test
//    public void isNegative_NegativeInteger() {
//        Word36 word36 = new Word36(0_400000_000000L);
//        assertTrue(word36.isNegative());
//    }
//
//    @Test
//    public void isPositive_PositiveZero() {
//        Word36 word36 = new Word36(Word36.POSITIVE_ZERO);
//        assertTrue(word36.isPositive());
//    }
//
//    @Test
//    public void isPositive_PositiveInteger() {
//        Word36 word36 = new Word36(0_377777_777777L);
//        assertTrue(word36.isPositive());
//    }
//
//    @Test
//    public void isPositive_NegativeZero() {
//        Word36 word36 = new Word36(Word36.NEGATIVE_ZERO);
//        assertFalse(word36.isPositive());
//    }
//
//    @Test
//    public void isPositive_NegativeInteger() {
//        Word36 word36 = new Word36(0_400000_000000L);
//        assertFalse(word36.isPositive());
//    }


    //  Arithmetic -----------------------------------------------------------------------------------------------------------------

//    @Test
//    public void addPosPos() {
//        Word36 w1 = new Word36(25);
//        Word36 w2 = new Word36(1027);
//        Word36.Flags f = w1.add(w2);
//        assertEquals(1052, w1.getTwosComplement());
//        assertFalse(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addPosPosOverflow() {
//        Word36 w1 = new Word36(0_377777_777777L);
//        Word36 w2 = new Word36(1);
//        Word36.Flags f = w1.add(w2);
//        assertFalse(f._carry);
//        assertTrue(f._overflow);
//    }
//
//    @Test
//    public void addPosNegResultPos() {
//        Word36 w1 = new Word36(Word36.getOnesComplement(1234));
//        Word36 w2 = new Word36(Word36.getOnesComplement(-234));
//        Word36.Flags f = w1.add(w2);
//        assertEquals(1000, w1.getTwosComplement());
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addPosNegResultNeg() {
//        Word36 w1 = new Word36(Word36.getOnesComplement(234));
//        Word36 w2 = new Word36(Word36.getOnesComplement(-1234));
//        Word36.Flags f = w1.add(w2);
//        assertEquals(-1000, w1.getTwosComplement());
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addNegNeg() {
//        Word36 w1 = new Word36(Word36.getOnesComplement(-1992));
//        Word36 w2 = new Word36(Word36.getOnesComplement(-2933));
//        Word36.Flags f = w1.add(w2);
//        assertEquals(-1992-2933, w1.getTwosComplement());
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addNegNegOverflow() {
//        Word36 w1 = new Word36(0_400000_000000L);
//        Word36 w2 = new Word36(0_777777_777776L);
//        Word36.Flags f = w1.add(w2);
//        assertTrue(f._carry);
//        assertTrue(f._overflow);
//    }
//
//    @Test
//    public void addPosZPosZ() {
//        Word36 w1 = new Word36(Word36.POSITIVE_ZERO._value);
//        Word36 w2 = new Word36(Word36.POSITIVE_ZERO._value);
//        Word36.Flags f = w1.add(w2);
//        assertEquals(Word36.POSITIVE_ZERO, w1);
//        assertFalse(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addPosZNegZ() {
//        Word36 w1 = new Word36(Word36.POSITIVE_ZERO._value);
//        Word36 w2 = new Word36(Word36.NEGATIVE_ZERO._value);
//        Word36.Flags f = w1.add(w2);
//        assertEquals(Word36.POSITIVE_ZERO, w1);
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addNegZPosZ() {
//        Word36 w1 = new Word36(Word36.NEGATIVE_ZERO._value);
//        Word36 w2 = new Word36(Word36.POSITIVE_ZERO._value);
//        Word36.Flags f = w1.add(w2);
//        assertEquals(Word36.POSITIVE_ZERO, w1);
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addNegZNegZ() {
//        Word36 w1 = new Word36(Word36.NEGATIVE_ZERO._value);
//        Word36 w2 = new Word36(Word36.NEGATIVE_ZERO._value);
//        Word36.Flags f = w1.add(w2);
//        assertEquals(Word36.NEGATIVE_ZERO, w1);
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void addInverses() {
//        Word36 w1 = new Word36(Word36.getOnesComplement(19883));
//        Word36 w2 = new Word36(Word36.getOnesComplement(-19883));
//        Word36.Flags f = w1.add(w2);
//        assertEquals(Word36.POSITIVE_ZERO, w1);
//        assertTrue(f._carry);
//        assertFalse(f._overflow);
//    }
//
//    @Test
//    public void multiply_1() {
//        long factor1 = 0_003234_715364L;
//        long factor2 = 0_073654_717623L;
//        BigInteger product = BigInteger.valueOf(factor1).multiply(BigInteger.valueOf(factor2));
//        BigInteger bi = Word36.multiply(factor1, factor2);
//        assertEquals(product, bi);
//    }
//
//    @Test
//    public void multiply_2() {
//        long factor1 = -29937;
//        long factor2 = 0_073654_717623L;
//        long factor1oc = Word36.getOnesComplement(factor1);
//        long factor2oc = Word36.getOnesComplement(factor2);
//        BigInteger product = BigInteger.valueOf(factor1).multiply(BigInteger.valueOf(factor2));
//        BigInteger bi = DoubleWord36.getTwosComplement(Word36.multiply(factor1oc, factor2oc));
//        assertEquals(product, bi);
//    }
//
//    @Test
//    public void negate36_PositiveOne() {
//        Word36 word36 = new Word36(Word36.POSITIVE_ONE);
//        word36.negate();
//        assertEquals(Word36.NEGATIVE_ONE._value, word36.getW());
//    }
//
//    @Test
//    public void negate36_PositiveZero() {
//        Word36 word36 = new Word36(Word36.POSITIVE_ZERO);
//        word36.negate();
//        assertEquals(Word36.NEGATIVE_ZERO._value, word36.getW());
//    }
//
//    @Test
//    public void negate36_NegativeOne() {
//        Word36 word36 = new Word36(Word36.NEGATIVE_ONE);
//        word36.negate();
//        assertEquals(Word36.POSITIVE_ONE._value, word36.getW());
//    }
//
//    @Test
//    public void negate36_NegativeZero() {
//        Word36 word36 = new Word36(Word36.NEGATIVE_ZERO);
//        word36.negate();
//        assertEquals(Word36.POSITIVE_ZERO._value, word36.getW());
//    }


    //  Shifts ---------------------------------------------------------------------------------------------------------------------

//    @Test
//    public void leftShiftCircular36_by0() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_111222_333444L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftCircular(0);
//        assertEquals(expected, word36.getW());
//    }
//
//    @Test
//    public void leftShiftCircular36_by3() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_112223_334441L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftCircular(3);
//        assertEquals(expected, word36.getW());
//    }
//
//    @Test
//    public void leftShiftCircular36_by36() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_111222_333444L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftCircular(36);
//        assertEquals(expected, word36.getW());
//    }
//
//    @Test
//    public void leftShiftCircular36_byNeg() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_441112_223334L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftCircular(-6);
//        assertEquals(expected, word36.getW());
//    }

//    @Test
//    public void leftShiftLogical36_by3() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_112223_334440L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftLogical(3);
//        assertEquals(expected, word36.getW());
//    }
//
//    @Test
//    public void leftShiftLogical36_by36() {
//        long parameter = 0_111222_333444L;
//        long expected = 0;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftLogical(36);
//        assertEquals(expected, word36.getW());
//    }
//
//    @Test
//    public void leftShiftLogical36_negCount() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_001112_223334L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftLogical(-6);
//        assertEquals(expected, word36.getW());
//    }
//
//    @Test
//    public void leftShiftLogical36_zeroCount() {
//        long parameter = 0_111222_333444L;
//        long expected = 0_111222_333444L;
//        Word36 word36 = new Word36(parameter);
//        word36.leftShiftLogical(0);
//        assertEquals(expected, word36.getW());
//    }

//    @Test
//    public void rightShiftAlgebraic36_negCount() {
//        long parameter = 033225L;
//        long expResult = 0332250L;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(-3);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_neg_3Count() {
//        long parameter = 0400000_112233L;
//        long expResult = 0740000_011223L;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(3);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_neg_34Count() {
//        long parameter = 0_421456_321456L;
//        long expResult = 0_777777_777776L;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(34);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_neg_35Count() {
//        long parameter = 0_421456_321456L;
//        long expResult = 0_777777_777777L;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(35);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_neg_36Count() {
//        long parameter = 0_421456_321456L;
//        long expResult = 0_777777_777777L;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(36);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_pos_3Count() {
//        long parameter = 033225L;
//        long expResult = parameter >> 3;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(3);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_pos_34Count() {
//        long parameter = 0_321456_321456L;
//        long expResult = parameter >> 34;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(34);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_pos_35Count() {
//        long parameter = 0_321456_321456L;
//        long expResult = parameter >> 35;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(35);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_pos_36Count() {
//        long parameter = 0_321456_321456L;
//        long expResult = 0;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(36);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftAlgebraic36_zeroCount() {
//        long parameter = 033225L;
//        long expResult = 033225L;
//        Word36 word36 = new Word36(parameter);
//        word36.rightShiftAlgebraic(0);
//        assertEquals(expResult, word36.getW());
//    }
//
//    @Test
//    public void rightShiftCircular_1() {
//        Word36 word36 = new Word36(0_112233_445566L);
//        word36.rightShiftCircular(6);
//        assertEquals(0_661122_334455L, word36.getW());
//    }
//
//    @Test
//    public void rightShiftCircular_2() {
//        Word36 word36 = new Word36(0_112200_334400L);
//        word36.rightShiftCircular(3);
//        assertEquals(0_011220_033440L, word36.getW());
//    }

//    @Test
//    public void rightShiftLogical() {
//        Word36 word36 = new Word36(0_112233_445566L);
//        word36.rightShiftLogical(9);
//        assertEquals(0_000112_233445L, word36.getW());
//    }


    //  Logic tests ----------------------------------------------------------------------------------------------------------------

//    @Test
//    public void and() {
//        Word36 op1 = new Word36(0_776655_221100L);
//        Word36 op2 = new Word36(0_765432_543210L);
//        Word36 exp = new Word36(0_764410_001000L);
//        op1.logicalAnd(op2);
//        assertEquals(exp, op1);
//    }
//
//    @Test
//    public void not() {
//        Word36 op1 = new Word36(0_776655_221100L);
//        Word36 exp = new Word36(0_001122_556677L);
//        op1.logicalNot();
//        assertEquals(exp, op1);
//    }
//
//    @Test
//    public void or() {
//        Word36 op1 = new Word36(0_776655_221100L);
//        Word36 op2 = new Word36(0_765432_543210L);
//        Word36 exp = new Word36(0_777677_763310L);
//        op1.logicalOr(op2);
//        assertEquals(exp, op1);
//    }
//
//    @Test
//    public void xor() {
//        Word36 op1 = new Word36(0_776655_221100L);
//        Word36 op2 = new Word36(0_765432_543210L);
//        Word36 exp = new Word36(0_013267_762310L);
//        op1.logicalXor(op2);
//        assertEquals(exp, op1);
//    }


    //  Display --------------------------------------------------------------------------------------------------------------------

//    @Test
//    public void toStringFromASCII() {
//        long word = 0_101_102_103_104L;
//        assertEquals("ABCD", Word36.toStringFromASCII(word));
//    }
//
//    @Test
//    public void toStringFromFieldata() {
//        long word = 0_05_06_07_10_11_12L;
//        assertEquals(" ABCDE", Word36.toStringFromFieldata(word));
//    }
//
//    @Test
//    public void toOctal() {
//        long word = 0_05_06_07_10_11_12L;
//        assertEquals("050607101112", Word36.toOctal(word));
//    }


    //  Misc -----------------------------------------------------------------------------------------------------------------------

//    @Test
//    public void stringToWord36ASCII() {
//        Word36 w = Word36.stringToWordASCII("Help");
//        assertEquals(0_110_145_154_160L, w.getW());
//    }
//
//    @Test
//    public void stringToWord36ASCII_over() {
//        Word36 w = Word36.stringToWordASCII("HelpSlop");
//        assertEquals(0_110_145_154_160L, w.getW());
//    }
//
//    @Test
//    public void stringToWord36ASCII_partial() {
//        Word36 w = Word36.stringToWordASCII("01");
//        assertEquals(0_060_061_040_040L, w.getW());
//    }
//
//    @Test
//    public void stringToWord36Fieldata() {
//        Word36 w = Word36.stringToWordFieldata("Abc@23");
//        assertEquals(0_060710_006263L, w.getW());
//    }
//
//    @Test
//    public void stringToWord36Fieldata_over() {
//        Word36 w = Word36.stringToWordFieldata("A B C@D E F");
//        assertEquals(0_060507_051000L, w.getW());
//    }
//
//    @Test
//    public void stringToWord36Fieldata_partial() {
//        Word36 w = Word36.stringToWordFieldata("1234");
//        assertEquals(0_616263_640505L, w.getW());
//    }
}
