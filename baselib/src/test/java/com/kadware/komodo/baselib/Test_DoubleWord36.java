/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.math.BigInteger;
import org.junit.Test;
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

    //TODO

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

    @Test
    public void toOnes_Zero() {
        assertEquals(BigInteger.ZERO, DoubleWord36.getOnesComplement(BigInteger.ZERO));
    }

    @Test
    public void toOnes_PositiveValue() {
        assertEquals(DoubleWord36.SHORT_BIT_MASK, DoubleWord36.getOnesComplement(DoubleWord36.SHORT_BIT_MASK));
    }

    @Test
    public void toOnes_NegativeOne() {
        BigInteger op = BigInteger.ONE.negate();
        BigInteger expResult = DoubleWord36.BIT_MASK.subtract(BigInteger.ONE);
        assertEquals(expResult, DoubleWord36.getOnesComplement(op));
    }

    @Test
    public void toOnes_NegativeValue() {
        BigInteger op = BigInteger.valueOf(-293884);
        BigInteger expected = BigInteger.valueOf(0_777777_777777L).shiftLeft(36).or(BigInteger.valueOf(0_777776_702003L));
        assertEquals(expected, DoubleWord36.getOnesComplement(op));
    }

    @Test
    public void toTwos_PositiveZero() {
        assertEquals(BigInteger.ZERO, DoubleWord36.getTwosComplement(BigInteger.ZERO));
    }

    @Test
    public void toTwos_NegativeZero() {
        assertEquals(BigInteger.ZERO, DoubleWord36.getTwosComplement(DoubleWord36.BIT_MASK));
    }

    @Test
    public void toTwos_NegativeOne() {
        assertEquals(BigInteger.ONE.negate(), DoubleWord36.getTwosComplement(DoubleWord36.BIT_MASK.subtract(BigInteger.ONE)));
    }

    @Test
    public void toTwos_NegativeValue() {
        BigInteger negativeOnesOperand = DoubleWord36.BIT_MASK.xor(BigInteger.valueOf(07777)); // negative 07777
        BigInteger expected = BigInteger.valueOf(-07777);
        assertEquals(expected, DoubleWord36.getTwosComplement(negativeOnesOperand));
    }


    //  Tests ----------------------------------------------------------------------------------------------------------------------

    @Test
    public void isNegative_PositiveZero() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        assertFalse(dw.isNegative());
    }

    @Test
    public void isNegative_PositiveInteger() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.BIT_MASK.shiftRight(1));
        assertFalse(dw.isNegative());
    }

    @Test
    public void isNegative_NegativeZero() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        assertTrue(dw.isNegative());
    }

    @Test
    public void isNegative_NegativeInteger() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.NEGATIVE_BIT);
        assertTrue(dw.isNegative());
    }

    @Test
    public void isPositive_PositiveZero() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        assertTrue(dw.isPositive());
    }

    @Test
    public void isPositive_PositiveInteger() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.BIT_MASK.shiftRight(1));
        assertTrue(dw.isPositive());
    }

    @Test
    public void isPositive_NegativeZero() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        assertFalse(dw.isPositive());
    }

    @Test
    public void isPositive_NegativeInteger() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.NEGATIVE_BIT);
        assertFalse(dw.isPositive());
    }


    //  Arithmetic -----------------------------------------------------------------------------------------------------------------

    @Test
    public void addPosPos() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.getOnesComplement(25));
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.getOnesComplement(1027));
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(BigInteger.valueOf(1052), ar._value.getTwosComplement());
        assertFalse(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addPosPosOverflow() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.NEGATIVE_BIT.subtract(BigInteger.ONE));
        DoubleWord36 w2 = new DoubleWord36(1);
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertFalse(ar._carry);
        assertTrue(ar._overflow);
    }

    @Test
    public void addPosNegResultPos() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.getOnesComplement(1234));
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.getOnesComplement(-234));
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(BigInteger.valueOf(1000), ar._value.getTwosComplement());
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addPosNegResultNeg() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.getOnesComplement(234));
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.getOnesComplement(-1234));
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(BigInteger.valueOf(-1000), ar._value.getTwosComplement());
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addNegNeg() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.getOnesComplement(-1992));
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.getOnesComplement(-2933));
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(BigInteger.valueOf(-1992-2933), ar._value.getTwosComplement());
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addNegNegOverflow() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.NEGATIVE_BIT);  //  highest magnitude negative number
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.NEGATIVE_BIT);
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertTrue(ar._carry);
        assertTrue(ar._overflow);
    }

    @Test
    public void addPosZPosZ() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(DoubleWord36.POSITIVE_ZERO, ar._value._value);
        assertFalse(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addPosZNegZ() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        DoubleWord36.AdditionResult ar  = w1.add(w2);
        assertEquals(DoubleWord36.POSITIVE_ZERO, ar._value._value);
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addNegZPosZ() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(DoubleWord36.POSITIVE_ZERO, ar._value._value);
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addNegZNegZ() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(DoubleWord36.NEGATIVE_ZERO, ar._value._value);
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void addInverses() {
        DoubleWord36 w1 = new DoubleWord36(DoubleWord36.getOnesComplement(19883));
        DoubleWord36 w2 = new DoubleWord36(DoubleWord36.getOnesComplement(-19883));
        DoubleWord36.AdditionResult ar = w1.add(w2);
        assertEquals(DoubleWord36.POSITIVE_ZERO, ar._value._value);
        assertTrue(ar._carry);
        assertFalse(ar._overflow);
    }

    @Test
    public void multiply_1() {
        BigInteger factor1 = BigInteger.valueOf(0_003234_715364L);
        BigInteger factor2 = BigInteger.valueOf(0_073654_717623L);
        BigInteger expProduct = factor1.multiply(factor2);

        DoubleWord36 dwFactor1 = new DoubleWord36(DoubleWord36.getOnesComplement(factor1));
        DoubleWord36 dwFactor2 = new DoubleWord36(DoubleWord36.getOnesComplement(factor2));
        DoubleWord36.MultiplicationResult mr = dwFactor1.multiply(dwFactor2);
        BigInteger product = mr._value.getTwosComplement();

        assertEquals(expProduct, product);
        assertFalse(mr._overflow);
    }

    @Test
    public void multiply_2() {
        BigInteger factor1 = BigInteger.valueOf(-29937);
        BigInteger factor2 = BigInteger.valueOf(0_073654_717623L);
        BigInteger expProduct = factor1.multiply(factor2);

        DoubleWord36 dwFactor1 = new DoubleWord36(DoubleWord36.getOnesComplement(factor1));
        DoubleWord36 dwFactor2 = new DoubleWord36(DoubleWord36.getOnesComplement(factor2));
        DoubleWord36.MultiplicationResult mr = dwFactor1.multiply(dwFactor2);
        BigInteger product = mr._value.getTwosComplement();

        assertEquals(expProduct, product);
        assertFalse(mr._overflow);
    }

    @Test
    public void multiply_3() {
        BigInteger factor1 = DoubleWord36.BIT_MASK.shiftRight(1);
        BigInteger factor2 = DoubleWord36.BIT_MASK.shiftRight(1);

        DoubleWord36 dwFactor1 = new DoubleWord36(DoubleWord36.getOnesComplement(factor1));
        DoubleWord36 dwFactor2 = new DoubleWord36(DoubleWord36.getOnesComplement(factor2));
        DoubleWord36.MultiplicationResult mr = dwFactor1.multiply(dwFactor2);

        assertTrue(mr._overflow);
    }

    @Test
    public void multiply_zero() {
        BigInteger factor1 = BigInteger.ZERO;
        BigInteger factor2 = DoubleWord36.SHORT_BIT_MASK;
        BigInteger expProduct = factor1.multiply(factor2);

        DoubleWord36 dwFactor1 = new DoubleWord36(DoubleWord36.getOnesComplement(factor1));
        DoubleWord36 dwFactor2 = new DoubleWord36(DoubleWord36.getOnesComplement(factor2));
        DoubleWord36.MultiplicationResult mr = dwFactor1.multiply(dwFactor2);
        BigInteger product = mr._value.getTwosComplement();

        assertEquals(expProduct, product);
        assertFalse(mr._overflow);
    }

    @Test
    public void negate_PositiveOne() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.POSITIVE_ONE);
        DoubleWord36 result = dw.negate();
        assertEquals(DoubleWord36.NEGATIVE_ONE, result._value);
    }

    @Test
    public void negate_PositiveZero() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.POSITIVE_ZERO);
        DoubleWord36 result = dw.negate();
        assertEquals(DoubleWord36.NEGATIVE_ZERO, result._value);
    }

    @Test
    public void negate_NegativeOne() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.NEGATIVE_ONE);
        DoubleWord36 result = dw.negate();
        assertEquals(DoubleWord36.POSITIVE_ONE, result._value);
    }

    @Test
    public void negate_NegativeZero() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.NEGATIVE_ZERO);
        DoubleWord36 result = dw.negate();
        assertEquals(DoubleWord36.POSITIVE_ZERO, result._value);
    }


    //  Shifts ---------------------------------------------------------------------------------------------------------------------

    //TODO  leftShiftAlgebraic tests

    @Test
    public void leftShiftCircular_by0() {
        long partial1 = 0_111222_333444L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        BigInteger expected = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftCircular(0);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftCircular_by3() {
        long partial1 = 0_111222_333444L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_112223_334444L;
        long expPartial2 = 0_445556_667771L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftCircular(3);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftCircular_by36() {
        long partial1 = 0_111222_333444L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_444555_666777L;
        long expPartial2 = 0_111222_333444L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftCircular(36);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftCircular_by72() {
        long partial1 = 0_111222_333444L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_111222_333444L;
        long expPartial2 = 0_444555_666777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftCircular(72);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftCircular_byNeg() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_770001_112223L;
        long expPartial2 = 0_334445_556667L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftCircular(-6);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftLogical_by3() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_001112_223334L;
        long expPartial2 = 0_445556_667770L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftLogical(3);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftLogical_by36() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_444555_666777L;
        long expPartial2 = 0_000000_000000L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftLogical(36);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftLogical_negCount() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_0000_0001_1122L;
        long expPartial2 = 0_2333_4445_5566L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftLogical(-12);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftLogical_zeroCount() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_000111_222333L;
        long expPartial2 = 0_444555_666777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftLogical(0);
        assertEquals(expected, result._value);
    }

    //TODO
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

    @Test
    public void rightShiftCircular_neg9() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_777000_111222L;
        long expPartial2 = 0_333444_555666L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftCircular(9);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftCircular_pos9() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_777000_111222L;
        long expPartial2 = 0_333444_555666L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftCircular(9);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftCircular_pos36() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_444555_666777L;
        long expPartial2 = 0_000111_222333L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftCircular(36);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftCircular_pos72() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_000111_222333L;
        long expPartial2 = 0_444555_666777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftCircular(72);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftLogical() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_000111_222333L;
        long expPartial2 = 0_444555_666777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftCircular(72);
        assertEquals(expected, result._value);
    }


    //  Logic tests ----------------------------------------------------------------------------------------------------------------

    @Test
    public void and() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        long op2high = 0_776655_443322L;
        long op2low = 0_110077_665544L;

        BigInteger op1 = BigInteger.valueOf(op1high).shiftLeft(36).or(BigInteger.valueOf(op1low));
        BigInteger op2 = BigInteger.valueOf(op2high).shiftLeft(36).or(BigInteger.valueOf(op2low));
        BigInteger result = op1.and(op2);

        DoubleWord36 dw1 = new DoubleWord36(op1);
        DoubleWord36 dw2 = new DoubleWord36(op2);
        DoubleWord36 expResult = new DoubleWord36(result);

        DoubleWord36 dwResult = dw1.logicalAnd(dw2);
        assertEquals(expResult, dwResult);
    }

    @Test
    public void not() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;

        BigInteger op1 = BigInteger.valueOf(op1high).shiftLeft(36).or(BigInteger.valueOf(op1low));
        BigInteger result = op1.not();

        DoubleWord36 dw1 = new DoubleWord36(op1);
        DoubleWord36 expResult = new DoubleWord36(result);

        DoubleWord36 dwResult = dw1.logicalNot();
        assertEquals(expResult, dwResult);
    }

    @Test
    public void or() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        long op2high = 0_776655_443322L;
        long op2low = 0_110077_665544L;

        BigInteger op1 = BigInteger.valueOf(op1high).shiftLeft(36).or(BigInteger.valueOf(op1low));
        BigInteger op2 = BigInteger.valueOf(op2high).shiftLeft(36).or(BigInteger.valueOf(op2low));
        BigInteger result = op1.or(op2);

        DoubleWord36 dw1 = new DoubleWord36(op1);
        DoubleWord36 dw2 = new DoubleWord36(op2);
        DoubleWord36 expResult = new DoubleWord36(result);

        DoubleWord36 dwResult = dw1.logicalOr(dw2);
        assertEquals(expResult, dwResult);
    }

    @Test
    public void xor() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        long op2high = 0_776655_443322L;
        long op2low = 0_110077_665544L;

        BigInteger op1 = BigInteger.valueOf(op1high).shiftLeft(36).or(BigInteger.valueOf(op1low));
        BigInteger op2 = BigInteger.valueOf(op2high).shiftLeft(36).or(BigInteger.valueOf(op2low));
        BigInteger result = op1.xor(op2);

        DoubleWord36 dw1 = new DoubleWord36(op1);
        DoubleWord36 dw2 = new DoubleWord36(op2);
        DoubleWord36 expResult = new DoubleWord36(result);

        DoubleWord36 dwResult = dw1.logicalXor(dw2);
        assertEquals(expResult, dwResult);
    }


    //  Floating Point -------------------------------------------------------------------------------------------------------------

    //TODO


    //  Display --------------------------------------------------------------------------------------------------------------------

    //TODO
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
}
