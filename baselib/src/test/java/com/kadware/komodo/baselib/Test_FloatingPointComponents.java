/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.CharacteristOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristUnderflowException;
import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class Test_FloatingPointComponents {

    //  constructors ---------------------------------------------------------------------------------------------------------------

    //  TODO

    //  add ------------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  checkExponent --------------------------------------------------------------------------------------------------------------

    @Test
    public void checkExponent_lowest(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.LOWEST_EXPONENT);
    }

    @Test
    public void checkExponent_highest(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.HIGHEST_EXPONENT);
    }

    @Test
    public void checkExponent_zero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents.checkExponent(0);
    }

    @Test(expected = CharacteristUnderflowException.class)
    public void checkExponent_under(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.LOWEST_EXPONENT -1);
    }

    @Test(expected = CharacteristOverflowException.class)
    public void checkExponent_over(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents.checkExponent(FloatingPointComponents.HIGHEST_EXPONENT + 1);
    }

    //  compare --------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  divide ---------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  equals ---------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  hashCode -------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  is**** ---------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  multiply -------------------------------------------------------------------------------------------------------------------

    //  TODO

    //  normalize ------------------------------------------------------------------------------------------------------------------

    @Test
    public void normalize_NegativeZero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents result = FloatingPointComponents.COMP_NEGATIVE_ZERO.normalize();
        assertFalse(result.isPositiveZero());
        assertTrue(result.isNegativeZero());
        assertTrue(result.isZero());
    }

    @Test
    public void normalize_PositiveZero(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents result = FloatingPointComponents.COMP_POSITIVE_ZERO.normalize();
        assertTrue(result.isPositiveZero());
        assertFalse(result.isNegativeZero());
        assertTrue(result.isZero());
    }

    @Test
    public void normalize_Integer(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false, -6, 01234, 0L);
        FloatingPointComponents expected = new FloatingPointComponents(false, 4, 0L, 0_5160_0000_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
//        System.out.println(operand.toString());//TODO
//        System.out.println(expected.toString());//TODO
//        System.out.println(result.toString());//TODO
        assertEquals(expected, result);
    }

    @Test
    public void normalize_Fraction(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false, 4, 0, 0_0000_7700_0000_0000_0000L);
        FloatingPointComponents expected = new FloatingPointComponents(false, -8, 0L, 0_7700_0000_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
//        System.out.println(operand.toString());//TODO
//        System.out.println(expected.toString());//TODO
//        System.out.println(result.toString());//TODO
        assertEquals(expected, result);
    }

    @Test
    public void normalize_IntegralAndFraction(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(true, 12, 07L, 0_7777_0000_0000_0000_0000L);
        FloatingPointComponents expected = new FloatingPointComponents(true, 15, 0L, 0_7777_7000_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
//        System.out.println(operand.toString());//TODO
//        System.out.println(expected.toString());//TODO
//        System.out.println(result.toString());//TODO
        assertEquals(expected, result);
    }

    @Test(expected = CharacteristUnderflowException.class)
    public void normalize_Underflow(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      FloatingPointComponents.LOWEST_EXPONENT,
                                                                      0L,
                                                                      0_0000_7777_0000_0000_0000L);

        FloatingPointComponents result = operand.normalize();
//        System.out.println(operand.toString());//TODO
//        System.out.println(expected.toString());//TODO
//        System.out.println(result.toString());//TODO
    }

    @Test(expected = CharacteristOverflowException.class)
    public void normalize_Overflow(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      FloatingPointComponents.HIGHEST_EXPONENT,
                                                                      01L,
                                                                      0L);

        FloatingPointComponents result = operand.normalize();
//        System.out.println(operand.toString());//TODO
//        System.out.println(expected.toString());//TODO
//        System.out.println(result.toString());//TODO
    }

    //  toDouble -------------------------------------------------------------------------------------------------------------------

    //TODO need more
    @Test
    public void toDouble(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        assertEquals(2083.5, operand.toDouble(), 0);
    }

    //  toDoubleWord36 -------------------------------------------------------------------------------------------------------------

    //TODO

    //  toFloat --------------------------------------------------------------------------------------------------------------------

    //TODO need more
    @Test
    public void toFloat(
    ) throws CharacteristOverflowException,
             CharacteristUnderflowException {
        FloatingPointComponents operand = new FloatingPointComponents(false,
                                                                      2,
                                                                      01010L,
                                                                      0_7000_0000_0000_0000_0000L);
        assertEquals(2083.5F, operand.toFloat(), 0);
    }

    //  toWord36 -------------------------------------------------------------------------------------------------------------------

    //TODO

    //  toString -------------------------------------------------------------------------------------------------------------------

    //TODO


//    @Test
//    public void toDouble(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(0xF000_0000_0000_0000L, 2, false);
//        double expected = 3.75F;
//
//        double result = dw.toFloat();
//        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToLongBits(result));
//    }
//
//    @Test
//    public void toFloat(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(0xF000_0000_0000_0000L, 2, false);
//        float expected = 3.75F;
//
//        float result = dw.toFloat();
//        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(result));
//    }
//
//    @Test
//    public void floatingFromInteger(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(0777);
//        DoubleWord36 expected = new DoubleWord36(0_7770_0000_0000_0000_0000L, 9, false);
//
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(expected._value, bi);
//    }
//
//    @Test
//    public void floatingFromNegativeInteger(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(077777).negate();
//        DoubleWord36 exp = new DoubleWord36(0_7777_7000_0000_0000_0000L, 15, true);
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(exp._value, bi);
//    }
//
//    @Test
//    public void floatingFromIntegerZero(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = DoubleWord36.DW36_POSITIVE_ZERO;
//        DoubleWord36 exp = new DoubleWord36(0L, 0, false);
//
//        BigInteger bi = DoubleWord36.floatingPointFromInteger(dw);
//        assertEquals(exp._value, bi);
//    }
//
//    @Test
//    public void floatingFromIntegerNegativeZero(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(1024, 0x8000_0000_0000_0000L, 0, true);
//        DoubleWord36 dw2 = new DoubleWord36(1024, 0x3000_0000_0000_0000L, 2, true);
//
//        int result = dw1.compareFloatingPoint(dw2);
//        assertEquals(-1, result);
//    }
//
//    @Test
//    public void compareFloatingPoint_2(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw1 = new DoubleWord36(0.0);
//        DoubleWord36 dw2 = new DoubleWord36(-0.0);
//
//        int result = dw1.compareFloatingPoint(dw2);
//        assertEquals(0, result);
//    }
//
//    @Test
//    public void compareFloatingPoint_3(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
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
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(0.0F);
//        DoubleWord36 expected = new DoubleWord36(0L, 0L);
//
//        DoubleWord36 result = dw.normalize();
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void normalize_NegativeZero(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36 dw = new DoubleWord36(-0.0F);
//        DoubleWord36 expected = new DoubleWord36(0xFFFF_FFFF_FFFF_FFFFL, 0xFFFF_FFFF_FFFF_FFFFL);
//
//        DoubleWord36 result = dw.normalize();
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void normalize_Positive(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        BigInteger operand = BigInteger.valueOf(0_2011_0000_7777L).shiftLeft(36);
//        BigInteger expected = BigInteger.valueOf(0_1775_7777_0000L).shiftLeft(36);
//
//        BigInteger result = DoubleWord36.normalize(operand);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void normalize_Negative(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        BigInteger operand = DoubleWord36.negate(BigInteger.valueOf(0_2011_0000_7777L).shiftLeft(36));
//        BigInteger expected = DoubleWord36.negate(BigInteger.valueOf(0_1775_7777_0000L).shiftLeft(36));
//
//        BigInteger result = DoubleWord36.normalize(operand);
//        assertEquals(expected, result);
//    }
}
