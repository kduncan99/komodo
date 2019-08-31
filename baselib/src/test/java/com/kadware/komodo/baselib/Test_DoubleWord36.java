/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.math.BigInteger;

import com.kadware.komodo.baselib.exceptions.CharacteristOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristUnderflowException;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
    public void stringToWordASCII() {
        DoubleWord36 dw = DoubleWord36.stringToWordASCII("HelpSlop");
        DoubleWord36 exp = new DoubleWord36(0_110_145_154_160L, 0_123_154_157_160L);
        assertEquals(exp, dw);
    }

    @Test
    public void stringToWord36ASCII_over() {
        DoubleWord36 dw = DoubleWord36.stringToWordASCII("0123456789");
        DoubleWord36 exp = new DoubleWord36(0_060_061_062_063L, 0_064_065_066_067L);
        assertEquals(exp, dw);
    }

    @Test
    public void stringToWordASCII_partial() {
        DoubleWord36 dw = DoubleWord36.stringToWordASCII("Help10");
        DoubleWord36 exp = new DoubleWord36(0_110_145_154_160L, 0_061_060_040_040L);
        assertEquals(exp, dw);
    }

    @Test
    public void stringToWordFieldata() {
        DoubleWord36 dw = DoubleWord36.stringToWordFieldata("Abc@23HIJKLM");
        DoubleWord36 exp = new DoubleWord36(0_060710_006263L, 0_151617_202122L);
        assertEquals(exp, dw);
    }

    @Test
    public void stringToWordFieldata_over() {
        DoubleWord36 dw = DoubleWord36.stringToWordFieldata("0123 0123 0123 0123 0123");
        DoubleWord36 exp = new DoubleWord36(0_606162_630560L, 0_616263_056061L);
        assertEquals(exp, dw);
    }

    @Test
    public void stringToWordFieldata_partial1() {
        DoubleWord36 dw = DoubleWord36.stringToWordFieldata("1234");
        DoubleWord36 exp = new DoubleWord36(0_616263_640505L, 0_050505_050505L);
        assertEquals(exp, dw);
    }

    @Test
    public void stringToWordFieldata_partial2() {
        DoubleWord36 dw = DoubleWord36.stringToWordFieldata("12345678");
        DoubleWord36 exp = new DoubleWord36(0_616263_646566L, 0_677005_050505L);
        assertEquals(exp, dw);
    }

    @Test
    public void toOnes_Zero() {
        DoubleWord36 dw = new DoubleWord36(BigInteger.ZERO);
        DoubleWord36 exp = new DoubleWord36(BigInteger.ZERO);
        assertEquals(exp, dw);
    }

    @Test
    public void toOnes_PositiveValue() {
        assertEquals(DoubleWord36.SHORT_BIT_MASK, DoubleWord36.getOnesComplement(DoubleWord36.SHORT_BIT_MASK));
    }

    @Test
    public void toOnes_NegativeOne() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.getOnesComplement(BigInteger.ONE.negate()));
        DoubleWord36 exp = new DoubleWord36(DoubleWord36.BIT_MASK.subtract(BigInteger.ONE));
        assertEquals(exp, dw);
    }

    @Test
    public void toOnes_NegativeValue() {
        DoubleWord36 dw = new DoubleWord36(DoubleWord36.getOnesComplement(BigInteger.valueOf(-293884)));
        DoubleWord36 exp = new DoubleWord36(BigInteger.valueOf(0_777777_777777L).shiftLeft(36).or(BigInteger.valueOf(0_777776_702003L)));
        assertEquals(exp, dw);
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

    @Test
    public void leftShiftAlgebraic_zeroCount() {
        long partial1 = 0_111222_333444L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        BigInteger expected = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftAlgebraic(0);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftAlgebraic_pos() {
        long partial1 = 0_111222_333444L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long exp1 = 0_222333_444444L;
        long exp2 = 0_555666_777000L;
        BigInteger expected = BigInteger.valueOf(exp1).shiftLeft(36).or(BigInteger.valueOf(exp2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftAlgebraic(9);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftAlgebraic_neg() {
        long partial1 = 0_777666_555444L;
        long partial2 = 0_333222_111000L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long exp1 = 0_732221_110000L;
        long exp2 = 0_000000_000000L;
        BigInteger expected = BigInteger.valueOf(exp1).shiftLeft(36).or(BigInteger.valueOf(exp2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftAlgebraic(39);
        assertEquals(expected, result._value);
    }

    @Test
    public void leftShiftAlgebraic_negCount() {
        long partial1 = 0_777666_555444L;
        long partial2 = 0_333222_111000L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long exp1 = 0_7777_7776_6655L;
        long exp2 = 0_5444_3332_2211L;
        BigInteger expected = BigInteger.valueOf(exp1).shiftLeft(36).or(BigInteger.valueOf(exp2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.leftShiftAlgebraic(-12);
        assertEquals(expected, result._value);
    }

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

    @Test
    public void rightShiftAlgebraic_negCount() {
        long partial1 = 0_400000_000000L;
        long partial2 = 0_112233_445566L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_400000_000011L;
        long expPartial2 = 0_223344_556600L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(-6);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftAlgebraic36_neg_24Count() {
        long partial1 = 0_400000_111222L;
        long partial2 = 0_334455_000000L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_777777_774000L;
        long expPartial2 = 0_001112_223344L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(24);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftAlgebraic36_neg_71Count() {
        long partial1 = 0_400000_111222L;
        long partial2 = 0_334455_000000L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_777777_777777L;
        long expPartial2 = 0_777777_777777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(71);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftAlgebraic36_pos_3Count() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_000011_122233L;
        long expPartial2 = 0_344455_566677L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(3);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftAlgebraic36_pos_33Count() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_000000_000000L;
        long expPartial2 = 0_001112_223334L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(33);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftAlgebraic36_pos_72Count() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_000000_000000L;
        long expPartial2 = 0_000000_000000L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(72);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftAlgebraic36_zeroCount() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));

        long expPartial1 = 0_000111_222333L;
        long expPartial2 = 0_444555_666777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));

        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftAlgebraic(0);
        assertEquals(expected, result._value);
    }

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
    public void rightShiftLogical_zeroCount() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_000111_222333L;
        long expPartial2 = 0_444555_666777L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftLogical(0);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftLogical_normal() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_0000_0001_1122L;
        long expPartial2 = 0_2333_4445_5566L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftLogical(12);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftLogical_72Count() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0L;
        long expPartial2 = 0L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftLogical(72);
        assertEquals(expected, result._value);
    }

    @Test
    public void rightShiftLogical_negCount() {
        long partial1 = 0_000111_222333L;
        long partial2 = 0_444555_666777L;
        BigInteger parameter = BigInteger.valueOf(partial1).shiftLeft(36).or(BigInteger.valueOf(partial2));
        long expPartial1 = 0_222333_444555L;
        long expPartial2 = 0_666777_000000L;
        BigInteger expected = BigInteger.valueOf(expPartial1).shiftLeft(36).or(BigInteger.valueOf(expPartial2));
        DoubleWord36 dw = new DoubleWord36(parameter);
        DoubleWord36 result = dw.rightShiftLogical(-18);
        assertEquals(expected, result._value);
    }


    //  Logic tests ----------------------------------------------------------------------------------------------------------------

    @Test
    public void and() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        DoubleWord36 dw1 = new DoubleWord36(op1high, op1low);

        long op2high = 0_776655_443322L;
        long op2low = 0_110077_665544L;
        DoubleWord36 dw2 = new DoubleWord36(op2high, op2low);

        DoubleWord36 exp = new DoubleWord36(dw1._value.and(dw2._value));
        DoubleWord36 dw = dw1.logicalAnd(dw2);
        assertEquals(exp, dw);
    }

    @Test
    public void not() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        DoubleWord36 dw1 = new DoubleWord36(op1high, op1low);

        DoubleWord36 exp = new DoubleWord36(dw1._value.not());
        DoubleWord36 dw = dw1.logicalNot();
        assertEquals(exp, dw);
    }

    @Test
    public void or() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        DoubleWord36 dw1 = new DoubleWord36(op1high, op1low);

        long op2high = 0_776655_443322L;
        long op2low = 0_110077_665544L;
        DoubleWord36 dw2 = new DoubleWord36(op2high, op2low);

        DoubleWord36 exp = new DoubleWord36(dw1._value.or(dw2._value));
        DoubleWord36 dw = dw1.logicalOr(dw2);
        assertEquals(exp, dw);
    }

    @Test
    public void xor() {
        long op1high = 0_777666_555444L;
        long op1low = 0_333222_111000L;
        DoubleWord36 dw1 = new DoubleWord36(op1high, op1low);

        long op2high = 0_776655_443322L;
        long op2low = 0_110077_665544L;
        DoubleWord36 dw2 = new DoubleWord36(op2high, op2low);

        DoubleWord36 exp = new DoubleWord36(dw1._value.xor(dw2._value));
        DoubleWord36 dw = dw1.logicalXor(dw2);
        assertEquals(exp, dw);
    }


    //  Floating Point private (worker) methods ------------------------------------------------------------------------------------

    @Test
    public void checkExponent_lowest(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36.checkExponent(DoubleWord36.LOWEST_EXPONENT);
    }

    @Test
    public void checkExponent_highest(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36.checkExponent(DoubleWord36.HIGHEST_EXPONENT);
    }

    @Test
    public void checkExponent_zero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36.checkExponent(0);
    }

    @Test(expected = CharacteristOverflowException.class)
    public void checkExponent_over(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36.checkExponent(DoubleWord36.HIGHEST_EXPONENT + 1);
    }

    @Test(expected = CharacteristUnderflowException.class)
    public void checkExponent_under(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36.checkExponent(DoubleWord36.LOWEST_EXPONENT - 1);
    }

    @Test
    public void convertMantissaToIntegral_zero() {
        long mantissa64 = 0;
        int exponent = 0;
        long expIntegral = 0;
        int expExponent = 0;

        long[] result = DoubleWord36.convertMantissaToIntegral(mantissa64, exponent);
        long resultIntegral = result[0];
        int resultExponent = (int) result[1];

        assertEquals(expIntegral, resultIntegral);
        assertEquals(expExponent, resultExponent);
    }

    @Test
    public void convertMantissaToIntegral_simple() {
        long mantissa64 = 0xFFFF_0000_0000_0000L;
        int exponent = 0;
        long expIntegral = 0xFFFF;
        int expExponent = -16;

        long[] result = DoubleWord36.convertMantissaToIntegral(mantissa64, exponent);
        long resultIntegral = result[0];
        int resultExponent = (int) result[1];

        assertEquals(expIntegral, resultIntegral);
        assertEquals(expExponent, resultExponent);
    }

    @Test
    public void convertMantissaToIntegral_withExponent() {
        long mantissa64 = 0xFFFF_0000_0000_0000L;
        int exponent = 8;
        long expIntegral = 0xFFFF;
        int expExponent = -8;

        long[] result = DoubleWord36.convertMantissaToIntegral(mantissa64, exponent);
        long resultIntegral = result[0];
        int resultExponent = (int) result[1];

        assertEquals(expIntegral, resultIntegral);
        assertEquals(expExponent, resultExponent);
    }

    @Test
    public void getAbsoluteCharacteristic_pos(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x8000_0000_0000_0000L, -3, false);
        int expCharacteristic = -3 + DoubleWord36.CHARACTERISTIC_BIAS;

        int characteristic = DoubleWord36.getAbsoluteCharacteristic(bi);
        assertEquals(expCharacteristic, characteristic);
    }

    @Test
    public void getAbsoluteCharacteristic_neg(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x8000_0000_0000_0000L, 4, true);
        int expCharacteristic = 4 + DoubleWord36.CHARACTERISTIC_BIAS;

        int characteristic = DoubleWord36.getAbsoluteCharacteristic(bi);
        assertEquals(expCharacteristic, characteristic);
    }

    @Test
    public void getAbsoluteMantissa_pos(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x00FF_0000_0000_0000L, 0, false);
        long expMantissa = 0xFF00_0000_0000_0000L;

        long mantissa = DoubleWord36.getAbsoluteMantissa(bi);
        assertEquals(expMantissa, mantissa);
    }

    @Test
    public void getAbsoluteMantissa_neg(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x00FF_0000_0000_0000L, 0, true);
        long expMantissa = 0xFF00_0000_0000_0000L;

        long mantissa = DoubleWord36.getAbsoluteMantissa(bi);
        assertEquals(expMantissa, mantissa);
    }

    @Test
    public void normalizeComponents_Zero() {
        long integral = 0L;
        long fractional = 0L;
        int exponent = 077777;
        long expMantissa = 0L;
        int expExponent = 0;

        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
        long normalizedMantissa = ncResult[0];
        int normalizedExponent = (int) ncResult[1];

        assertEquals(expMantissa, normalizedMantissa);
        assertEquals(expExponent, normalizedExponent);
    }

    @Test
    public void normalizeComponents_Integer() {
        long integral = 01234L;     //  001010011100
        long fractional = 0L;
        int exponent = -6;          //  making the real number 001010.011100, or .10100111 e4
        long expMantissa = 0xA700_0000_0000_0000L;
        int expExponent = 4;

        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
        long normalizedMantissa = ncResult[0];
        int normalizedExponent = (int) ncResult[1];

        assertEquals(expMantissa, normalizedMantissa);
        assertEquals(expExponent, normalizedExponent);
    }

    @Test
    public void normalizeComponents_Fraction() {
        long integral = 0L;
        long fractional = 0x007E_0000_0000_0000L;   //  000----0000000.000000000111111000000----000  E4
        int exponent = 4;                           //  making the real number 0.00000111111 E0, or .111111 E-5
        long expMantissa = 0xFC00_0000_0000_0000L;
        int expExponent = -5;

        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
        long normalizedMantissa = ncResult[0];
        int normalizedExponent = (int) ncResult[1];

        assertEquals(expMantissa, normalizedMantissa);
        assertEquals(expExponent, normalizedExponent);
    }

    @Test
    public void normalizeComponents_Fun() {
        long integral = 0x7L;
        long fractional = 0xFFF0_0000_0000_0000L;
        int exponent = 12;
        long expMantissa = 0xFFFE_0000_0000_0000L;
        int expExponent = 15;

        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
        long normalizedMantissa = ncResult[0];
        int normalizedExponent = (int) ncResult[1];

        assertEquals(expMantissa, normalizedMantissa);
        assertEquals(expExponent, normalizedExponent);
    }

    @Test
    public void toDouble(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = new DoubleWord36(0xF000_0000_0000_0000L, 2, false);
        double expected = 3.75F;

        double result = dw.toFloat();
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToLongBits(result));
    }

    @Test
    public void toFloat(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = new DoubleWord36(0xF000_0000_0000_0000L, 2, false);
        float expected = 3.75F;

        float result = dw.toFloat();
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(result));
    }


    //  Floating Point methods -----------------------------------------------------------------------------------------------------

    @Test
    public void floatingFromInteger(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = new DoubleWord36(0777);
        DoubleWord36 expected = new DoubleWord36(0_7770_0000_0000_0000_0000L, 9, false);

        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
        assertEquals(expected._value, bi);
    }

    @Test
    public void floatingFromNegativeInteger(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = new DoubleWord36(077777).negate();
        DoubleWord36 exp = new DoubleWord36(0_7777_7000_0000_0000_0000L, 15, true);
        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
        assertEquals(exp._value, bi);
    }

    @Test
    public void floatingFromIntegerZero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = DoubleWord36.DW36_POSITIVE_ZERO;
        DoubleWord36 exp = new DoubleWord36(0L, 0, false);

        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
        assertEquals(exp._value, bi);
    }

    @Test
    public void floatingFromIntegerNegativeZero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = DoubleWord36.DW36_NEGATIVE_ZERO;
        DoubleWord36 exp = new DoubleWord36(0L, 0, true);

        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
        assertEquals(exp._value, bi);
    }

    @Test
    public void isNegativeFloating()        { assertTrue((DoubleWord36.DW36_NEGATIVE_ONE).isNegativeFloatingPoint()); }

    @Test
    public void isNegativeZeroFloating()    { assertTrue((DoubleWord36.DW36_NEGATIVE_ZERO_FLOATING).isNegativeZeroFloatingPoint()); }

    @Test
    public void isPositiveFloating()        { assertTrue((DoubleWord36.DW36_POSITIVE_ONE).isPositiveFloatingPoint()); }

    @Test
    public void isPositiveZeroFloating()    { assertTrue((DoubleWord36.DW36_POSITIVE_ZERO_FLOATING).isPositiveZeroFloatingPoint()); }

    @Test
    public void isZeroFloating_negative()   { assertTrue((DoubleWord36.DW36_NEGATIVE_ZERO_FLOATING).isZeroFloatingPoint()); }

    @Test
    public void isZeroFloating_positive()   { assertTrue((DoubleWord36.DW36_POSITIVE_ZERO_FLOATING).isZeroFloatingPoint()); }

    @Test
    public void addFloating_Zeros(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(0.0);
        DoubleWord36 dw2 = new DoubleWord36(0.0F);
        DoubleWord36 exp = new DoubleWord36(0.0);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    @Test
    public void addFloating_PosPos(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(1024.5);
        DoubleWord36 dw2 = new DoubleWord36(4096.75);
        DoubleWord36 exp = new DoubleWord36(5121.25);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    @Test
    public void addFloating_Inverses(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(1024.875F);
        DoubleWord36 dw2 = new DoubleWord36(-1024.875);
        DoubleWord36 exp = new DoubleWord36(0.0);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    @Test
    public void addFloating_PosNeg_ResultPos1(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(1000.0);
        DoubleWord36 dw2 = new DoubleWord36(-500.0);
        DoubleWord36 exp = new DoubleWord36(500.0);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    @Test
    public void addFloating_PosNeg_ResultPos2(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(0x8080, 0x8080_0000_0000_0000L, 0, false);
        DoubleWord36 dw2 = new DoubleWord36(0x8080, 0x0000_0000_0000_0000L, 0, true);
        DoubleWord36 exp = new DoubleWord36(0L, 0x8080_0000_0000_0000L, 0, false);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    @Test
    public void addFloating_PosNeg_ResultNeg(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(2.5);
        DoubleWord36 dw2 = new DoubleWord36(-5.25);
        DoubleWord36 exp = new DoubleWord36(-2.75);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    @Test
    public void addFloating_NegNeg(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(1024, 0x8000_0000_0000_0000L, 0, true);
        DoubleWord36 dw2 = new DoubleWord36(1024, 0x3000_0000_0000_0000L, 2, true);
        DoubleWord36 exp = new DoubleWord36(1024 + 4096 + 1, 0x4000_0000_0000_0000L, 0, true);
        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);

        assertFalse(afpr._overFlow);
        assertFalse(afpr._underFlow);
        assertEquals(exp, afpr._value);
    }

    //  TODO compareFloatingPoint
    @Test
    public void compareFloatingPoint_1(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(1024, 0x8000_0000_0000_0000L, 0, true);
        DoubleWord36 dw2 = new DoubleWord36(1024, 0x3000_0000_0000_0000L, 2, true);

        int result = dw1.compareFloatingPoint(dw2);
        assertEquals(-1, result);
    }

    @Test
    public void compareFloatingPoint_2(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(0.0);
        DoubleWord36 dw2 = new DoubleWord36(-0.0);

        int result = dw1.compareFloatingPoint(dw2);
        assertEquals(0, result);
    }

    @Test
    public void compareFloatingPoint_3(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(10.112);
        DoubleWord36 dw2 = new DoubleWord36(10.0999999999);

        int result = dw1.compareFloatingPoint(dw2);
        assertEquals(1, result);
    }

    //  TODO divideFloatingPoint

    //  TODO multiplyFloatingPoint
    @Test
    public void multiplyFloating_1(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(5.0);   //  .1010000 E3
        DoubleWord36 dw2 = new DoubleWord36(4.0);   //  .1000000 E3
        DoubleWord36 exp = new DoubleWord36(20.0);  //  .1010000 E5

        DoubleWord36.MultiplyFloatingPointResult mfpr = dw1.multiplyFloatingPoint(dw2);
        System.out.println(String.format("dw1:%024o", dw1._value));//TODO
        System.out.println(String.format("dw2:%024o", dw2._value));//TODO
        System.out.println(String.format("exp:%024o", exp._value));//TODO
        System.out.println(String.format("res:%024o", mfpr._value._value));//TODO
        assertEquals(exp, mfpr._value);
        assertFalse(mfpr._overflow);
        assertFalse(mfpr._underflow);
    }

    @Test
    public void multiplyFloating_2(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw1 = new DoubleWord36(-3.14159);
        DoubleWord36 dw2 = new DoubleWord36(10.0);
        DoubleWord36 exp = new DoubleWord36(-31.4159);

        DoubleWord36.MultiplyFloatingPointResult mfpr = dw1.multiplyFloatingPoint(dw2);
        System.out.println(String.format("dw1:%024o", dw1._value));//TODO
        System.out.println(String.format("dw2:%024o", dw2._value));//TODO
        System.out.println(String.format("exp:%024o", exp._value));//TODO
        System.out.println(String.format("res:%024o", mfpr._value._value));//TODO
        System.out.println(DoubleWord36.toDouble(mfpr._value._value));//TODO
        System.out.println(-3.14159 * 100000);//TODO
        assertEquals(exp, mfpr._value);
        assertFalse(mfpr._overflow);
        assertFalse(mfpr._underflow);
    }

    @Test
    public void normalize_Zero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = new DoubleWord36(0.0F);
        DoubleWord36 expected = new DoubleWord36(0L, 0L);

        DoubleWord36 result = dw.normalize();
        assertEquals(expected, result);
    }

    @Test
    public void normalize_NegativeZero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        DoubleWord36 dw = new DoubleWord36(-0.0F);
        DoubleWord36 expected = new DoubleWord36(0xFFFF_FFFF_FFFF_FFFFL, 0xFFFF_FFFF_FFFF_FFFFL);

        DoubleWord36 result = dw.normalize();
        assertEquals(expected, result);
    }

    @Test
    public void normalize_Positive(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger operand = BigInteger.valueOf(0_2011_0000_7777L).shiftLeft(36);
        BigInteger expected = BigInteger.valueOf(0_1775_7777_0000L).shiftLeft(36);

        BigInteger result = DoubleWord36.normalize(operand);
        assertEquals(expected, result);
    }

    @Test
    public void normalize_Negative(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        BigInteger operand = DoubleWord36.negate(BigInteger.valueOf(0_2011_0000_7777L).shiftLeft(36));
        BigInteger expected = DoubleWord36.negate(BigInteger.valueOf(0_1775_7777_0000L).shiftLeft(36));

        BigInteger result = DoubleWord36.normalize(operand);
        assertEquals(expected, result);
    }


    //  Display --------------------------------------------------------------------------------------------------------------------

    @Test
    public void toStringFromASCII() {
        long word1 = 0_101_102_103_104L;
        long word2 = 0_105_106_107_110L;
        BigInteger bi = BigInteger.valueOf(word1).shiftLeft(36).or(BigInteger.valueOf(word2));
        DoubleWord36 dw = new DoubleWord36(bi);
        assertEquals("ABCDEFGH", dw.toStringFromASCII());
    }

    @Test
    public void toStringFromFieldata() {
        long word1 = 0_050607101112L;
        long word2 = 0_606162636465L;
        BigInteger bi = BigInteger.valueOf(word1).shiftLeft(36).or(BigInteger.valueOf(word2));
        DoubleWord36 dw = new DoubleWord36(bi);
        assertEquals(" ABCDE012345", dw.toStringFromFieldata());
    }

    @Test
    public void toOctal() {
        long word1 = 0_050607101112L;
        long word2 = 0_606162636465L;
        BigInteger bi = BigInteger.valueOf(word1).shiftLeft(36).or(BigInteger.valueOf(word2));
        DoubleWord36 dw = new DoubleWord36(bi);
        assertEquals("050607101112606162636465", dw.toOctal());
    }
}
