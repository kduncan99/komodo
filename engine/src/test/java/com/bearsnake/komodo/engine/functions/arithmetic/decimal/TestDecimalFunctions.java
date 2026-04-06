/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.functions.arithmetic.decimal.TestDecimalFunction.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDecimalFunctions extends FunctionUnitTest {

    @Test
    public void testToBinary_Positive() {
        // 12345678+
        long operand = decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN);
        assertEquals(12345678L, DecimalFunction.toBinary(operand));
    }

    @Test
    public void testToBinary_Negative() {
        // 12345678-
        long operand = decWord(1, 2, 3, 4, 5, 6, 7, 8, NEGATIVE_SIGN);
        assertEquals(-12345678L, DecimalFunction.toBinary(operand));
    }

    @Test
    public void testToBinary_ZeroPositive() {
        // 00000000+
        long operand = decWord(0, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN);
        assertEquals(0L, DecimalFunction.toBinary(operand));
    }

    @Test
    public void testToBinary_ZeroNegative() {
        // 00000000-
        long operand = decWord(0, 0, 0, 0, 0, 0, 0, 0, NEGATIVE_SIGN);
        assertEquals(0L, DecimalFunction.toBinary(operand));
    }

    @Test
    public void testDoubleToBinary_Positive() {
        // 123456789 01234567+
        long word0 = decWord(1, 2, 3, 4, 5, 6, 7, 8, 9);
        long word1 = decWord(0, 1, 2, 3, 4, 5, 6, 7, POSITIVE_SIGN);
        assertEquals(12345678901234567L, DecimalFunction.doubleToBinary(word0, word1));
    }

    @Test
    public void testDoubleToBinary_Negative() {
        // 123456789 01234567-
        long word0 = decWord(1, 2, 3, 4, 5, 6, 7, 8, 9);
        long word1 = decWord(0, 1, 2, 3, 4, 5, 6, 7, NEGATIVE_SIGN);
        assertEquals(-12345678901234567L, DecimalFunction.doubleToBinary(word0, word1));
    }

    @Test
    public void testDoubleToBinary_Zero() {
        long word0 = decWord(0, 0, 0, 0, 0, 0, 0, 0, 0);
        long word1 = decWord(0, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN);
        assertEquals(0L, DecimalFunction.doubleToBinary(word0, word1));
    }

    @Test
    public void testDoubleToBinary_Large() {
        // Max value for 17 digits: 99,999,999,999,999,999
        long word0 = decWord(9, 9, 9, 9, 9, 9, 9, 9, 9);
        long word1 = decWord(9, 9, 9, 9, 9, 9, 9, 9, POSITIVE_SIGN);
        assertEquals(99999999999999999L, DecimalFunction.doubleToBinary(word0, word1));
    }

    @Test
    public void testDoubleToDecimal_Positive() {
        Word36 high = new Word36();
        Word36 low = new Word36();
        boolean overflow = DecimalFunction.doubleToDecimal(12345678901234567L, high, low);

        assertEquals(false, overflow);
        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, 9), high.getW());
        assertEquals(decWord(0, 1, 2, 3, 4, 5, 6, 7, POSITIVE_SIGN), low.getW());
    }

    @Test
    public void testDoubleToDecimal_Negative() {
        Word36 high = new Word36();
        Word36 low = new Word36();
        boolean overflow = DecimalFunction.doubleToDecimal(-12345678901234567L, high, low);

        assertEquals(false, overflow);
        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, 9), high.getW());
        assertEquals(decWord(0, 1, 2, 3, 4, 5, 6, 7, NEGATIVE_SIGN), low.getW());
    }

    @Test
    public void testDoubleToDecimal_Zero() {
        Word36 high = new Word36();
        Word36 low = new Word36();
        boolean overflow = DecimalFunction.doubleToDecimal(0L, high, low);

        assertEquals(false, overflow);
        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 0, 0), high.getW());
        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN), low.getW());
    }

    @Test
    public void testDoubleToDecimal_Max() {
        Word36 high = new Word36();
        Word36 low = new Word36();
        boolean overflow = DecimalFunction.doubleToDecimal(99999999999999999L, high, low);

        assertEquals(false, overflow);
        assertEquals(decWord(9, 9, 9, 9, 9, 9, 9, 9, 9), high.getW());
        assertEquals(decWord(9, 9, 9, 9, 9, 9, 9, 9, POSITIVE_SIGN), low.getW());
    }

    @Test
    public void testDoubleToDecimal_Overflow() {
        Word36 high = new Word36();
        Word36 low = new Word36();
        // 18 digits - should overflow
        boolean overflow = DecimalFunction.doubleToDecimal(100000000000000000L, high, low);

        assertEquals(true, overflow);
    }
}
