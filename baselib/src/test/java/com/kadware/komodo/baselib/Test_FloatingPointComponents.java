/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.CharacteristicOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristicUnderflowException;
import com.kadware.komodo.baselib.exceptions.DivideByZeroException;
import java.math.BigInteger;
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

    //  add ------------------------------------------------------------------------------------------------------------------------

    //  TODO
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

    //  TODO
    @Test
    public void divide_simple(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = 32.5;
        double value2 = 2.5;
        double expectedValue = 13.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expectedValue, result.toDouble(), 0.000001);
    }

    @Test
    public void divide_bigger(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = -47166.560546875;
        double value2 = -11.515625;
        double expectedValue = 4095.875;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expectedValue, result.toDouble(), 0.000001);
    }

    @Test
    public void divide_small(
    ) throws CharacteristicOverflowException,
             CharacteristicUnderflowException,
             DivideByZeroException {
        double value1 = -1.0/128.0;
        double value2 = -1.0/4.0;
        double expectedValue = 1.0/32.0;

        FloatingPointComponents operand1 = new FloatingPointComponents(value1);
        FloatingPointComponents operand2 = new FloatingPointComponents(value2);
        FloatingPointComponents exp = new FloatingPointComponents(expectedValue);//TODO

        FloatingPointComponents result = operand1.divide(operand2);
        assertEquals(expectedValue, result.toDouble(), 0.000001);
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

    //  is**** ---------------------------------------------------------------------------------------------------------------------

    @Test
    public void isNegativeZero() {
        assertTrue(FloatingPointComponents.COMP_NEGATIVE_ZERO.isNegativeZero());
        assertFalse(FloatingPointComponents.COMP_POSITIVE_ZERO.isNegativeZero());
        assertFalse(new FloatingPointComponents(0.5).isNegativeZero());
    }

    //  TODO isNormalized

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

    //  TODO
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

    //TODO

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

    //TODO

    //  toString -------------------------------------------------------------------------------------------------------------------

    //TODO

//    @Test
//    public void floatingFromInteger(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(0777);
//        DoubleWord36 expected = new DoubleWord36(0_7770_0000_0000_0000_0000L, 9, false);
//
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(expected._value, bi);
//    }
//
//    @Test
//    public void floatingFromNegativeInteger(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(077777).negate();
//        DoubleWord36 exp = new DoubleWord36(0_7777_7000_0000_0000_0000L, 15, true);
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(exp._value, bi);
//    }
//
//    @Test
//    public void floatingFromIntegerZero(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw = DoubleWord36.DW36_POSITIVE_ZERO;
//        DoubleWord36 exp = new DoubleWord36(0L, 0, false);
//
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(exp._value, bi);
//    }
//
//    @Test
//    public void floatingFromIntegerNegativeZero(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw = DoubleWord36.DW36_NEGATIVE_ZERO;
//        DoubleWord36 exp = new DoubleWord36(0L, 0, true);
//
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(exp._value, bi);
//    }
//
//    @Test
//    public void isNegativeFloating()        { assertTrue((DoubleWord36.DW36_NEGATIVE_ONE).isNegativeFloatingPoint()); }
//
//    @Test
//    public void isNegativeZeroFloating()    { assertTrue((DoubleWord36.DW36_NEGATIVE_ZERO_FLOATING).isNegativeZeroFloatingPoint()); }
//
//    @Test
//    public void isPositiveFloating()        { assertTrue((DoubleWord36.DW36_POSITIVE_ONE).isPositiveFloatingPoint()); }
//
//    @Test
//    public void isPositiveZeroFloating()    { assertTrue((DoubleWord36.DW36_POSITIVE_ZERO_FLOATING).isPositiveZeroFloatingPoint()); }
//
//    @Test
//    public void isZeroFloating_negative()   { assertTrue((DoubleWord36.DW36_NEGATIVE_ZERO_FLOATING).isZeroFloatingPoint()); }
//
//    @Test
//    public void isZeroFloating_positive()   { assertTrue((DoubleWord36.DW36_POSITIVE_ZERO_FLOATING).isZeroFloatingPoint()); }
//
//    @Test
//    public void addFloating_Zeros(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(0.0);
//        DoubleWord36 dw2 = new DoubleWord36(0.0F);
//        DoubleWord36 exp = new DoubleWord36(0.0);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    @Test
//    public void addFloating_PosPos(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(1024.5);
//        DoubleWord36 dw2 = new DoubleWord36(4096.75);
//        DoubleWord36 exp = new DoubleWord36(5121.25);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    @Test
//    public void addFloating_Inverses(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(1024.875F);
//        DoubleWord36 dw2 = new DoubleWord36(-1024.875);
//        DoubleWord36 exp = new DoubleWord36(0.0);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    @Test
//    public void addFloating_PosNeg_ResultPos1(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(1000.0);
//        DoubleWord36 dw2 = new DoubleWord36(-500.0);
//        DoubleWord36 exp = new DoubleWord36(500.0);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    @Test
//    public void addFloating_PosNeg_ResultPos2(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(0x8080, 0x8080_0000_0000_0000L, 0, false);
//        DoubleWord36 dw2 = new DoubleWord36(0x8080, 0x0000_0000_0000_0000L, 0, true);
//        DoubleWord36 exp = new DoubleWord36(0L, 0x8080_0000_0000_0000L, 0, false);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    @Test
//    public void addFloating_PosNeg_ResultNeg(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(2.5);
//        DoubleWord36 dw2 = new DoubleWord36(-5.25);
//        DoubleWord36 exp = new DoubleWord36(-2.75);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    @Test
//    public void addFloating_NegNeg(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(1024, 0x8000_0000_0000_0000L, 0, true);
//        DoubleWord36 dw2 = new DoubleWord36(1024, 0x3000_0000_0000_0000L, 2, true);
//        DoubleWord36 exp = new DoubleWord36(1024 + 4096 + 1, 0x4000_0000_0000_0000L, 0, true);
//        DoubleWord36.AddFloatingPointResult afpr = dw1.addFloatingPoint(dw2);
//
//        assertFalse(afpr._overFlow);
//        assertFalse(afpr._underFlow);
//        assertEquals(exp, afpr._value);
//    }
//
//    //  TODO compareFloatingPoint
//    @Test
//    public void compareFloatingPoint_1(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(1024, 0x8000_0000_0000_0000L, 0, true);
//        DoubleWord36 dw2 = new DoubleWord36(1024, 0x3000_0000_0000_0000L, 2, true);
//
//        int result = dw1.compareFloatingPoint(dw2);
//        assertEquals(-1, result);
//    }
//
//    @Test
//    public void compareFloatingPoint_2(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(0.0);
//        DoubleWord36 dw2 = new DoubleWord36(-0.0);
//
//        int result = dw1.compareFloatingPoint(dw2);
//        assertEquals(0, result);
//    }
//
//    @Test
//    public void compareFloatingPoint_3(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(10.112);
//        DoubleWord36 dw2 = new DoubleWord36(10.0999999999);
//
//        int result = dw1.compareFloatingPoint(dw2);
//        assertEquals(1, result);
//    }
//
//    //  TODO divideFloatingPoint
//
//    //  TODO multiplyFloatingPoint
//    @Test
//    public void multiplyFloating_1(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(5.0);   //  .1010000 E3
//        DoubleWord36 dw2 = new DoubleWord36(4.0);   //  .1000000 E3
//        DoubleWord36 exp = new DoubleWord36(20.0);  //  .1010000 E5
//
//        DoubleWord36.MultiplyFloatingPointResult mfpr = dw1.multiplyFloatingPoint(dw2);
//        System.out.println(String.format("dw1:%024o", dw1._value));//TODO
//        System.out.println(String.format("dw2:%024o", dw2._value));//TODO
//        System.out.println(String.format("exp:%024o", exp._value));//TODO
//        System.out.println(String.format("res:%024o", mfpr._value._value));//TODO
//        assertEquals(exp, mfpr._value);
//        assertFalse(mfpr._overflow);
//        assertFalse(mfpr._underflow);
//    }
//
//    @Test
//    public void multiplyFloating_2(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(-3.14159);
//        DoubleWord36 dw2 = new DoubleWord36(10.0);
//        DoubleWord36 exp = new DoubleWord36(-31.4159);
//
//        DoubleWord36.MultiplyFloatingPointResult mfpr = dw1.multiplyFloatingPoint(dw2);
//        System.out.println(String.format("dw1:%024o", dw1._value));//TODO
//        System.out.println(String.format("dw2:%024o", dw2._value));//TODO
//        System.out.println(String.format("exp:%024o", exp._value));//TODO
//        System.out.println(String.format("res:%024o", mfpr._value._value));//TODO
//        System.out.println(DoubleWord36.toDouble(mfpr._value._value));//TODO
//        System.out.println(-3.14159 * 100000);//TODO
//        assertEquals(exp, mfpr._value);
//        assertFalse(mfpr._overflow);
//        assertFalse(mfpr._underflow);
//    }
//
//    @Test
//    public void normalize_Zero(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(0.0F);
//        DoubleWord36 expected = new DoubleWord36(0L, 0L);
//
//        DoubleWord36 result = dw.normalize();
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void normalize_NegativeZero(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(-0.0F);
//        DoubleWord36 expected = new DoubleWord36(0xFFFF_FFFF_FFFF_FFFFL, 0xFFFF_FFFF_FFFF_FFFFL);
//
//        DoubleWord36 result = dw.normalize();
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void normalize_Positive(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        BigInteger operand = BigInteger.valueOf(0_2011_0000_7777L).shiftLeft(36);
//        BigInteger expected = BigInteger.valueOf(0_1775_7777_0000L).shiftLeft(36);
//
//        BigInteger result = DoubleWord36.normalize(operand);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void normalize_Negative(
//    ) throws CharacteristicOverflowException,
//             CharacteristicUnderflowException {
//        BigInteger operand = DoubleWord36.negate(BigInteger.valueOf(0_2011_0000_7777L).shiftLeft(36));
//        BigInteger expected = DoubleWord36.negate(BigInteger.valueOf(0_1775_7777_0000L).shiftLeft(36));
//
//        BigInteger result = DoubleWord36.normalize(operand);
//        assertEquals(expected, result);
//    }
}
