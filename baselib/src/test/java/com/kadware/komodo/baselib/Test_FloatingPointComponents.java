package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.CharacteristOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristUnderflowException;
import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class Test_FloatingPointComponents {

//    @Test
//    public void checkExponent_lowest(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36.checkExponent(DoubleWord36.LOWEST_EXPONENT);
//    }
//
//    @Test
//    public void checkExponent_highest(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36.checkExponent(DoubleWord36.HIGHEST_EXPONENT);
//    }
//
//    @Test
//    public void checkExponent_zero(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36.checkExponent(0);
//    }
//
//    @Test(expected = CharacteristOverflowException.class)
//    public void checkExponent_over(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36.checkExponent(DoubleWord36.HIGHEST_EXPONENT + 1);
//    }
//
//    @Test(expected = CharacteristUnderflowException.class)
//    public void checkExponent_under(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        DoubleWord36.checkExponent(DoubleWord36.LOWEST_EXPONENT - 1);
//    }
//
//    @Test
//    public void convertMantissaToIntegral_zero() {
//        long mantissa64 = 0;
//        int exponent = 0;
//        long expIntegral = 0;
//        int expExponent = 0;
//
//        long[] result = DoubleWord36.convertMantissaToIntegral(mantissa64, exponent);
//        long resultIntegral = result[0];
//        int resultExponent = (int) result[1];
//
//        assertEquals(expIntegral, resultIntegral);
//        assertEquals(expExponent, resultExponent);
//    }
//
//    @Test
//    public void convertMantissaToIntegral_simple() {
//        long mantissa64 = 0xFFFF_0000_0000_0000L;
//        int exponent = 0;
//        long expIntegral = 0xFFFF;
//        int expExponent = -16;
//
//        long[] result = DoubleWord36.convertMantissaToIntegral(mantissa64, exponent);
//        long resultIntegral = result[0];
//        int resultExponent = (int) result[1];
//
//        assertEquals(expIntegral, resultIntegral);
//        assertEquals(expExponent, resultExponent);
//    }
//
//    @Test
//    public void convertMantissaToIntegral_withExponent() {
//        long mantissa64 = 0xFFFF_0000_0000_0000L;
//        int exponent = 8;
//        long expIntegral = 0xFFFF;
//        int expExponent = -8;
//
//        long[] result = DoubleWord36.convertMantissaToIntegral(mantissa64, exponent);
//        long resultIntegral = result[0];
//        int resultExponent = (int) result[1];
//
//        assertEquals(expIntegral, resultIntegral);
//        assertEquals(expExponent, resultExponent);
//    }
//
//    @Test
//    public void getAbsoluteCharacteristic_pos(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x8000_0000_0000_0000L, -3, false);
//        int expCharacteristic = -3 + DoubleWord36.CHARACTERISTIC_BIAS;
//
//        int characteristic = DoubleWord36.getAbsoluteCharacteristic(bi);
//        assertEquals(expCharacteristic, characteristic);
//    }
//
//    @Test
//    public void getAbsoluteCharacteristic_neg(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x8000_0000_0000_0000L, 4, true);
//        int expCharacteristic = 4 + DoubleWord36.CHARACTERISTIC_BIAS;
//
//        int characteristic = DoubleWord36.getAbsoluteCharacteristic(bi);
//        assertEquals(expCharacteristic, characteristic);
//    }
//
//    @Test
//    public void getAbsoluteMantissa_pos(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x00FF_0000_0000_0000L, 0, false);
//        long expMantissa = 0xFF00_0000_0000_0000L;
//
//        long mantissa = DoubleWord36.getAbsoluteMantissa(bi);
//        assertEquals(expMantissa, mantissa);
//    }
//
//    @Test
//    public void getAbsoluteMantissa_neg(
//    ) throws CharacteristOverflowException,
//             CharacteristUnderflowException {
//        BigInteger bi = DoubleWord36.floatingPointFromComponents(0x00FF_0000_0000_0000L, 0, true);
//        long expMantissa = 0xFF00_0000_0000_0000L;
//
//        long mantissa = DoubleWord36.getAbsoluteMantissa(bi);
//        assertEquals(expMantissa, mantissa);
//    }
//
//    @Test
//    public void normalizeComponents_Zero() {
//        long integral = 0L;
//        long fractional = 0L;
//        int exponent = 077777;
//        long expMantissa = 0L;
//        int expExponent = 0;
//
//        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
//        long normalizedMantissa = ncResult[0];
//        int normalizedExponent = (int) ncResult[1];
//
//        assertEquals(expMantissa, normalizedMantissa);
//        assertEquals(expExponent, normalizedExponent);
//    }
//
//    @Test
//    public void normalizeComponents_Integer() {
//        long integral = 01234L;     //  001010011100
//        long fractional = 0L;
//        int exponent = -6;          //  making the real number 001010.011100, or .10100111 e4
//        long expMantissa = 0xA700_0000_0000_0000L;
//        int expExponent = 4;
//
//        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
//        long normalizedMantissa = ncResult[0];
//        int normalizedExponent = (int) ncResult[1];
//
//        assertEquals(expMantissa, normalizedMantissa);
//        assertEquals(expExponent, normalizedExponent);
//    }
//
//    @Test
//    public void normalizeComponents_Fraction() {
//        long integral = 0L;
//        long fractional = 0x007E_0000_0000_0000L;   //  000----0000000.000000000111111000000----000  E4
//        int exponent = 4;                           //  making the real number 0.00000111111 E0, or .111111 E-5
//        long expMantissa = 0xFC00_0000_0000_0000L;
//        int expExponent = -5;
//
//        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
//        long normalizedMantissa = ncResult[0];
//        int normalizedExponent = (int) ncResult[1];
//
//        assertEquals(expMantissa, normalizedMantissa);
//        assertEquals(expExponent, normalizedExponent);
//    }
//
//    @Test
//    public void normalizeComponents_Fun() {
//        long integral = 0x7L;
//        long fractional = 0xFFF0_0000_0000_0000L;
//        int exponent = 12;
//        long expMantissa = 0xFFFE_0000_0000_0000L;
//        int expExponent = 15;
//
//        long[] ncResult = DoubleWord36.normalizeComponents(integral, fractional, exponent);
//        long normalizedMantissa = ncResult[0];
//        int normalizedExponent = (int) ncResult[1];
//
//        assertEquals(expMantissa, normalizedMantissa);
//        assertEquals(expExponent, normalizedExponent);
//    }
//
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
//
//    //  Floating Point methods -----------------------------------------------------------------------------------------------------
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
