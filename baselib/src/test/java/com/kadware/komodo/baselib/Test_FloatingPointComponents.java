/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.CharacteristicOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristicUnderflowException;
import com.kadware.komodo.baselib.exceptions.DivideByZeroException;
import java.math.BigInteger;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class Test_FloatingPointComponents {

    private String padZero(
        final String input,
        final int digits
    ) {
        StringBuilder sb = new StringBuilder();
        while (input.length() + sb.length() < digits) {
            sb.append("0");
        }
        sb.append(input);
        return sb.toString();
    }

    private String decompose(final double value) {
        long bits = Double.doubleToRawLongBits(value);
        long sign = bits >>> 63;
        long characteristic = (bits >>> 52) & 0x7FF;
        long mantissa = bits & 0xF_FFFF_FFFF_FFFFL;
        long unbiasedExponent = characteristic - FloatingPointComponents.IEEE754_DOUBLE_EXPONENT_BIAS;
        long actualMantissa = mantissa | 0x10_0000_0000_0000L;

        StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------------------\n");
        sb.append(String.format("double value:    %s\n", String.valueOf(value)));
        sb.append(String.format("raw bits:        %s\n", padZero(Long.toBinaryString(bits), 64)));
        sb.append(String.format("sign:            %s\n", Long.toBinaryString(sign)));
        sb.append(String.format("characteristic:  %s\n", padZero(Long.toBinaryString(characteristic), 11)));
        sb.append(String.format("mantissa:        %s\n", padZero(Long.toBinaryString(mantissa), 52)));
        sb.append(String.format("exponent:        %d\n", unbiasedExponent));
        sb.append(String.format("adjustedMantissa:%s   (note first bit is left of decimal)\n", Long.toBinaryString(actualMantissa)));
        return sb.toString();
    }

    private String decompose(
        final FloatingPointComponents fpc
    ) throws CharacteristicOverflowException, CharacteristicUnderflowException {
        DoubleWord36 dw36 = fpc.toDoubleWord36();
        BigInteger bits = dw36._value;
        long sign = bits.shiftRight(71).longValue();
        long characteristic = bits.shiftRight(60).and(BigInteger.valueOf(03777)).longValue();
        long mantissa = bits.and(BigInteger.valueOf(0_7777_7777).shiftLeft(36).or(BigInteger.valueOf(0_777777_777777L))).longValue();
        long unbiasedExponent = characteristic - FloatingPointComponents.DW36_EXPONENT_BIAS;

        StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------------------\n");
        sb.append(String.format("FPC value:       %s\n", fpc.toString()));
        sb.append(String.format("DW36 value:      %s\n", dw36.toOctal()));
        sb.append(String.format("raw bits:        %s\n", padZero(bits.toString(2), 72)));
        sb.append(String.format("sign:            %s\n", Long.toBinaryString(sign)));
        sb.append(String.format("characteristic:  %s\n", padZero(Long.toBinaryString(characteristic), 11)));
        sb.append(String.format("mantissa:        %s\n", padZero(Long.toBinaryString(mantissa), 60)));
        sb.append(String.format("exponent:        %d\n", unbiasedExponent));
        sb.append(String.format("toDouble():      %f\n", fpc.toDouble()));
        return sb.toString();
    }

    //  constructors ---------------------------------------------------------------------------------------------------------------

    //  TODO
    @Test
    public void fromDouble_1(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value = 256.9375;
        FloatingPointComponents fpc = new FloatingPointComponents(value).normalize();
        assertFalse(fpc._isNegative);
        assertEquals(9, fpc._exponent);
        assertEquals(0, fpc._integral);
        assertEquals(0_40074_00000_00000_00000L, fpc._mantissa);
    }

    //  add ------------------------------------------------------------------------------------------------------------------------

    @Test
    public void add_simple(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value1 = 5.5;
        double value2 = 23.335;
        double expValue = value1 + value2;
        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.add(operand2);
        assertEquals(expValue, result.toDouble(), 0);
    }

    @Test
    public void add_negative(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value1 = 5.5;
        double value2 = -23.335;
        double expValue = value1 + value2;
        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.add(operand2);
        assertEquals(expValue, result.toDouble(), 0);
    }

    @Test
    public void add_inverse(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value1 = 3.14159;
        double value2 = -3.14159;
        double expValue = value1 + value2;
        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.add(operand2);
        assertEquals(expValue, result.toDouble(), 0);
    }

    //  checkExponent --------------------------------------------------------------------------------------------------------------

    @Test
    public void checkExponent_lowest(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.LOWEST_EXPONENT);
    }

    @Test
    public void checkExponent_highest(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.HIGHEST_EXPONENT);
    }

    @Test
    public void checkExponent_zero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents.checkExponent(0);
    }

    @Test(expected = CharacteristicUnderflowException.class)
    public void checkExponent_under(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.LOWEST_EXPONENT -1);
    }

    @Test(expected = CharacteristicOverflowException.class)
    public void checkExponent_over(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.HIGHEST_EXPONENT + 1);
    }

    //  compare --------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  divide ---------------------------------------------------------------------------------------------------------------------

    @Test
    public void divide_simple(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = 32.5;
        double value2 = 2.5;
        double expectedValue = value1 / value2;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents expected = new FloatingPointComponents(expectedValue).normalize();

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expected, result);
    }

    @Test
    public void divide_bigger(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = -47166.560546875;
        double value2 = -11.515625;
        double expectedValue = value1 / value2;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents expected = new FloatingPointComponents(expectedValue).normalize();

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expected, result);
    }

    @Test
    public void divide_one_by_one(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = 1.0;
        double value2 = 1.0;
        double expectedValue = value1 / value2;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents expected = new FloatingPointComponents(expectedValue);

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expected, result);
    }

    @Test
    public void divide_small(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = -1.0/128.0;
        double value2 = -1.0/4.0;
        double expectedValue = value1 / value2;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents expected = new FloatingPointComponents(expectedValue);

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expected, result);
    }

    @Test
    public void divide_randomExcercise(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        for (int c = 0; c < 100; ++c) {
            Random r = new Random(System.currentTimeMillis());
            double value1 = r.nextDouble();
            double value2 = r.nextDouble();
            double expectedValue = value1 / value2;

            FloatingPointComponents operand1 = new FloatingPointComponents(value1).normalize();
            FloatingPointComponents operand2 = new FloatingPointComponents(value2).normalize();
            FloatingPointComponents expected = new FloatingPointComponents(expectedValue).normalize();

            FloatingPointComponents result = operand1.divide(operand2).normalize();
            assertEquals(expected.toDouble(), result.toDouble(), 0.000001);
        }
    }

    @Test
    public void divide_zero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = 0.0;
        double value2 = 11.515625;
        double expectedValue = 0.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expectedValue, result.toDouble(), 0.000001);
    }

    @Test
    public void divide_negative_zero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = -0.0;
        double value2 = 11.515625;
        double expectedValue = -0.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expectedValue, result.toDouble(), 0.000001);
    }

    @Test(expected = DivideByZeroException.class)
    public void divide_byZero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = 1002.378475;
        double value2 = 0.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.divide(operand2);
    }

    //  equals ---------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  hashCode -------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  invert ---------------------------------------------------------------------------------------------------------------------

    @Test(expected = DivideByZeroException.class)
    public void invertZero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        FloatingPointComponents fpZero = new FloatingPointComponents(false, 0, 0, 0);
        fpZero.invert();
    }

    @Test
    public void invertOne(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        FloatingPointComponents fpOne = new FloatingPointComponents(false, 0, 1, 0);
        FloatingPointComponents expected =
            new FloatingPointComponents(false, 0, 1, 0).normalizeNoThrow();

        FloatingPointComponents result = fpOne.invert();
        assertEquals(expected, result);
    }

    //  is**** ---------------------------------------------------------------------------------------------------------------------

    @Test
    public void isNegativeZero() {
        assertTrue(FloatingPointComponents.COMP_NEGATIVE_ZERO.isNegativeZero());
        assertFalse(FloatingPointComponents.COMP_POSITIVE_ZERO.isNegativeZero());
        assertFalse(new FloatingPointComponents(0.5).isNegativeZero());
    }

    @Test
    public void isNormalized() {
        assertTrue(FloatingPointComponents.COMP_POSITIVE_ZERO.isNormalized());
        assertTrue(FloatingPointComponents.COMP_NEGATIVE_ZERO.isNormalized());

        FloatingPointComponents not_1 = new FloatingPointComponents(false, 5, 128, 0);
        FloatingPointComponents not_2 = new FloatingPointComponents(false,
                                                                    5,
                                                                    128,
                                                                    0_77000_00000_00000_00000L);
        FloatingPointComponents not_3 = new FloatingPointComponents(true,
                                                                    3,
                                                                    0,
                                                                    0_37777_00000_00000_00000L);
        FloatingPointComponents is = new FloatingPointComponents(true,
                                                                 12,
                                                                 0,
                                                                 0_40000_00000_00000_00000L);

        assertFalse(not_1.isNormalized());
        assertFalse(not_2.isNormalized());
        assertFalse(not_3.isNormalized());
        assertTrue(is.isNormalized());
    }

    @Test
    public void isPositiveZero() {
        assertFalse(FloatingPointComponents.COMP_NEGATIVE_ZERO.isPositiveZero());
        assertTrue(FloatingPointComponents.COMP_POSITIVE_ZERO.isPositiveZero());
        assertFalse(new FloatingPointComponents(0.5).isPositiveZero());
    }

    @Test
    public void isZero() {
        assertTrue(FloatingPointComponents.COMP_NEGATIVE_ZERO.isZero());
        assertTrue(FloatingPointComponents.COMP_POSITIVE_ZERO.isZero());
        assertFalse(new FloatingPointComponents(0.5).isZero());
    }

    //  multiply -------------------------------------------------------------------------------------------------------------------

    //  TODO more? some overflows maybe
    @Test
    public void multiply_zero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value1 = 0.0;
        double value2 = 3.14159;
        double expectedValue = 0.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents result = operand1.multiply(operand2);

        assertEquals(expectedValue, result.toDouble(), 0);
    }

    @Test
    public void multiply_negativeZero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value1 = -0.0F;
        double value2 = 3.14159F;
        double expectedValue = 0.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents result = operand1.multiply(operand2);

        assertEquals(expectedValue, result.toDouble(), 0);
    }

    @Test
    public void multiply_simple(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        double value1 = 1000.0;
        double value2 = 3.14159;
        double expectedValue = 3141.59;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.multiply(operand2);
        assertEquals(expectedValue, result.toDouble(), 0.000001);
    }


    //  normalize ------------------------------------------------------------------------------------------------------------------

    @Test
    public void normalize_NegativeZero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents result = FloatingPointComponents.COMP_NEGATIVE_ZERO.normalize();
        assertFalse(result.isPositiveZero());
        assertTrue(result.isNegativeZero());
        assertTrue(result.isZero());
    }

    @Test
    public void normalize_PositiveZero(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents result = FloatingPointComponents.COMP_POSITIVE_ZERO.normalize();
        assertTrue(result.isPositiveZero());
        assertFalse(result.isNegativeZero());
        assertTrue(result.isZero());
    }

    @Test
    public void normalize_Integer(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false, -6, 01234, 0L);
        FloatingPointComponents expected = new FloatingPointComponents(false, 4, 0L, 0_5160_0000_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
        assertEquals(expected, result);
    }

    @Test
    public void normalize_Fraction(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false, 4, 0, 0_0000_7700_0000_0000_0000L);
        FloatingPointComponents expected = new FloatingPointComponents(false, -8, 0L, 0_7700_0000_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
        assertEquals(expected, result);
    }

    @Test
    public void normalize_IntegralAndFraction(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(true, 12, 07L, 0_7777_0000_0000_0000_0000L);
        FloatingPointComponents expected = new FloatingPointComponents(true, 15, 0L, 0_7777_7000_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
        assertEquals(expected, result);
    }

    @Test
    public void normalize_more(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        //  actual value in binary is 100000100011.1
        //  result mantissa in binary is 10000010001110...0 - in octal is 404340...0
        //  real exponent in decimal is 12
        FloatingPointComponents expected = new FloatingPointComponents(false,
                                                                       12,
                                                                       0,
                                                                       0_40434_00000_00000_00000L);

        assertEquals(expected, operand.normalize());
    }

    @Test(expected = CharacteristicUnderflowException.class)
    public void normalize_Underflow(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      FloatingPointComponents.LOWEST_EXPONENT,
                                                                      0L,
                                                                      0_0000_7777_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
    }

    @Test(expected = CharacteristicOverflowException.class)
    public void normalize_Overflow(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      FloatingPointComponents.HIGHEST_EXPONENT,
                                                                      01L,
                                                                      0L);

        FloatingPointComponents result = operand.normalize();
    }

    //  toDouble -------------------------------------------------------------------------------------------------------------------

    @Test
    public void toDouble_1(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        assertEquals(2083.5, operand.toDouble(), 0);
    }

    @Test
    public void toDouble_2(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(true,
                                                                      0,
                                                                      1000000,
                                                                      0_6000_0000_0000_0000_0000L);
        assertEquals(-1000000.75, operand.toDouble(), 0);
    }

    //  toDoubleWord36 -------------------------------------------------------------------------------------------------------------

    @Test
    public void toDoubleWord36_1(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        //  actual value in binary is 100000100011.1
        //  result mantissa in binary is 10000010001110...0 - in octal is 404340...0
        //  real exponent in decimal is 12, biased in octal is 2014
        Word36[] expected = {
            new Word36(0_2014_40434000L),
            new Word36(0L),
        };

        assertArrayEquals(expected, operand.toDoubleWord36().getWords());
    }

    //  toFloat --------------------------------------------------------------------------------------------------------------------

    @Test
    public void toFloat_1(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        assertEquals(2083.5F, operand.toFloat(), 0);
    }

    @Test
    public void toFloat_2(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(true,
                                                                      0,
                                                                      1000000,
                                                                      0_6000_0000_0000_0000_0000L);
        assertEquals(-1000000.75F, operand.toFloat(), 0);
    }

    //  toWord36 -------------------------------------------------------------------------------------------------------------------

    @Test
    public void toWord36_1(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        //  actual value in binary is 100000100011.1
        //  result mantissa in binary is 10000010001110...0 - in octal is 404340...0
        //  real exponent in decimal is 12, biased in octal is 214
        Word36 expected = new Word36(0_214_404340000L);

        assertEquals(expected, operand.toWord36());
    }

    //  toString -------------------------------------------------------------------------------------------------------------------

    //TODO
}
