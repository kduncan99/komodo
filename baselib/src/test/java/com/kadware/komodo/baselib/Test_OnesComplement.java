/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * Unit tests for OnesComplement class
 */
public class Test_OnesComplement {

//    //  Tests for sign and for zero ------------------------------------------------------------------------------------------------
//
//    @Test
//    public void isNegative72_PositiveZero(
//    ) {
//        assertFalse(OnesComplement.isNegative72(POSITIVE_ZERO_72));
//    }
//
//    @Test
//    public void isNegative72_PositiveInteger(
//    ) {
//        assertFalse(OnesComplement.isNegative72(LARGEST_POSITIVE_INTEGER_72));
//    }
//
//    @Test
//    public void isNegative72_NegativeZero(
//    ) {
//        assertTrue(OnesComplement.isNegative72(NEGATIVE_ZERO_72));
//    }
//
//    @Test
//    public void isNegative72_NegativeInteger(
//    ) {
//        assertTrue(OnesComplement.isNegative72(SMALLEST_NEGATIVE_INTEGER_72));
//    }
//
//    @Test
//    public void isNegativeZero36_PositiveZero(
//    ) {
//        assertFalse(OnesComplement.isNegativeZero36(POSITIVE_ZERO_36));
//    }
//
//    @Test
//    public void isNegativeZero36_NegativeOne(
//    ) {
//        assertFalse(OnesComplement.isNegativeZero36(NEGATIVE_ONE_36));
//    }
//
//    @Test
//    public void isNegativeZero36_True(
//    ) {
//        assertTrue(OnesComplement.isNegativeZero36(NEGATIVE_ZERO_36));
//    }
//
//    @Test
//    public void isNegativeZero72_PositiveZero(
//    ) {
//        assertFalse(OnesComplement.isNegativeZero72(POSITIVE_ZERO_72));
//    }
//
//    @Test
//    public void isNegativeZero72_NegativeOne(
//    ) {
//        assertFalse(OnesComplement.isNegativeZero72(NEGATIVE_ONE_72));
//    }
//
//    @Test
//    public void isNegativeZero72_True(
//    ) {
//        assertTrue(OnesComplement.isNegativeZero72(NEGATIVE_ZERO_72));
//    }
//
//    @Test
//    public void isPositive36_NegativeZero(
//    ) {
//        assertFalse(OnesComplement.isPositive36(NEGATIVE_ZERO_36));
//    }
//
//    @Test
//    public void isPositive36_NegativeInteger(
//    ) {
//        assertFalse(OnesComplement.isPositive36(SMALLEST_NEGATIVE_INTEGER_36));
//    }
//
//    @Test
//    public void isPositive36_PositiveZero(
//    ) {
//        assertTrue(OnesComplement.isPositive36(POSITIVE_ZERO_36));
//    }
//
//    @Test
//    public void isPositive36_PositiveInteger(
//    ) {
//        assertTrue(OnesComplement.isPositive36(LARGEST_POSITIVE_INTEGER_36));
//    }
//
//    @Test
//    public void isPositive72_NegativeZero(
//    ) {
//        assertFalse(OnesComplement.isPositive72(NEGATIVE_ZERO_72));
//    }
//
//    @Test
//    public void isPositive72_NegativeInteger(
//    ) {
//        assertFalse(OnesComplement.isPositive72(SMALLEST_NEGATIVE_INTEGER_72));
//    }
//
//    @Test
//    public void isPositive72_PositiveZero(
//    ) {
//        assertTrue(OnesComplement.isPositive72(POSITIVE_ZERO_72));
//    }
//
//    @Test
//    public void isPositive72_PositiveInteger(
//    ) {
//        assertTrue(OnesComplement.isPositive72(LARGEST_POSITIVE_INTEGER_72));
//    }
//
//    @Test
//    public void isPositiveZero36_NegativeZero(
//    ) {
//        assertFalse(OnesComplement.isPositiveZero36(NEGATIVE_ZERO_36));
//    }
//
//    @Test
//    public void isPositiveZero36_PositiveOne(
//    ) {
//        assertFalse(OnesComplement.isZero36(POSITIVE_ONE_36));
//    }
//
//    @Test
//    public void isPositiveZero36_True(
//    ) {
//        assertTrue(OnesComplement.isZero36(POSITIVE_ZERO_36));
//    }
//
//    @Test
//    public void isPositiveZero72_NegativeZero(
//    ) {
//        assertFalse(OnesComplement.isPositiveZero72(NEGATIVE_ZERO_72));
//    }
//
//    @Test
//    public void isPositiveZero72_PositiveOne(
//    ) {
//        assertFalse(OnesComplement.isZero72(POSITIVE_ONE_72));
//    }
//
//    @Test
//    public void isPositiveZero72_True(
//    ) {
//        assertTrue(OnesComplement.isZero72(POSITIVE_ZERO_72));
//    }
//
//    @Test
//    public void isZero36_PositiveOne(
//    ) {
//        assertFalse(OnesComplement.isZero36(POSITIVE_ONE_36));
//    }
//
//    @Test
//    public void isZero36_PositiveInteger(
//    ) {
//        assertFalse(OnesComplement.isZero36(LARGEST_POSITIVE_INTEGER_36));
//    }
//
//    @Test
//    public void isZero36_NegativeOne(
//    ) {
//        assertFalse(OnesComplement.isZero36(NEGATIVE_ONE_36));
//    }
//
//    @Test
//    public void isZero36_NegativeInteger(
//    ) {
//        assertFalse(OnesComplement.isZero36(SMALLEST_NEGATIVE_INTEGER_36));
//    }
//
//    @Test
//    public void isZero36_PositiveZero(
//    ) {
//        assertTrue(OnesComplement.isZero36(POSITIVE_ZERO_36));
//    }
//
//    @Test
//    public void isZero36_NegativeZero(
//    ) {
//        assertTrue(OnesComplement.isZero36(NEGATIVE_ZERO_36));
//    }
//
//    @Test
//    public void isZero72_PositiveOne(
//    ) {
//        assertFalse(OnesComplement.isZero72(POSITIVE_ONE_72));
//    }
//
//    @Test
//    public void isZero72_PositiveInteger(
//    ) {
//        assertFalse(OnesComplement.isZero72(LARGEST_POSITIVE_INTEGER_72));
//    }
//
//    @Test
//    public void isZero72_NegativeOne(
//    ) {
//        assertFalse(OnesComplement.isZero72(NEGATIVE_ONE_72));
//    }
//
//    @Test
//    public void isZero72_NegativeInteger(
//    ) {
//        assertFalse(OnesComplement.isZero72(SMALLEST_NEGATIVE_INTEGER_72));
//    }
//
//    @Test
//    public void isZero72_PositiveZero(
//    ) {
//        assertTrue(OnesComplement.isZero72(POSITIVE_ZERO_72));
//    }
//
//    @Test
//    public void isZero72_NegativeZero(
//    ) {
//        assertTrue(OnesComplement.isZero72(NEGATIVE_ZERO_72));
//    }
//
//
//    //  Conversions ----------------------------------------------------------------------------------------------------------------
//
//    @Test
//    public void getNative36_NegativeMinimum(
//    ) {
//        long value = SMALLEST_NEGATIVE_INTEGER_36;
//        long result = OnesComplement.getNative36(value);
//        assertEquals(-LARGEST_POSITIVE_INTEGER_36, result);
//    }
//
//    @Test
//    public void getNative36_NegativeOne(
//    ) {
//        long value = NEGATIVE_ONE_36;
//        long result = OnesComplement.getNative36(value);
//        assertEquals(-1, result);
//    }
//
//    @Test
//    public void getNative36_NegativeZero(
//    ) {
//        long value = NEGATIVE_ZERO_36;
//        long result = OnesComplement.getNative36(value);
//        assertEquals(0, result);
//    }
//
//    @Test
//    public void getNative36_PositiveOne(
//    ) {
//        long value = POSITIVE_ONE_36;
//        long result = OnesComplement.getNative36(value);
//        assertEquals(1, result);
//    }
//
//    @Test
//    public void getNative36_PositiveMaximum(
//    ) {
//        long value = LARGEST_POSITIVE_INTEGER_36;
//        long result = OnesComplement.getNative36(value);
//        assertEquals(LARGEST_POSITIVE_INTEGER_36, result);
//    }
//
//    @Test
//    public void getNative36_PositiveZero(
//    ) {
//        long value = POSITIVE_ZERO_36;
//        long result = OnesComplement.getNative36(value);
//        assertEquals(0, result);
//    }
//
//    @Test
//    public void getNative72_NegativeMinimum(
//    ) {
//        long[] value = SMALLEST_NEGATIVE_INTEGER_72;
//        BigInteger result = OnesComplement.getNative72(value);
//        assertEquals(OnesComplement.BI_SMALLEST_NEGATIVE_INTEGER_72, result);
//    }
//
//    @Test
//    public void getNative72_NegativeOne(
//    ) {
//        long[] value = NEGATIVE_ONE_72;
//        BigInteger result = OnesComplement.getNative72(value);
//        assertEquals(BigInteger.ONE.negate(), result);
//    }
//
//    @Test
//    public void getNative72_NegativeZero(
//    ) {
//        long[] value = NEGATIVE_ZERO_72;
//        BigInteger result = OnesComplement.getNative72(value);
//        assertEquals(BigInteger.ZERO, result);
//    }
//
//    @Test
//    public void getNative72_PositiveOne(
//    ) {
//        long[] value = POSITIVE_ONE_72;
//        BigInteger result = OnesComplement.getNative72(value);
//        assertEquals(BigInteger.ONE, result);
//    }
//
//    @Test
//    public void getNative72_PositiveMaximum(
//    ) {
//        long[] value = LARGEST_POSITIVE_INTEGER_72;
//        BigInteger result = OnesComplement.getNative72(value);
//        assertEquals(OnesComplement.BI_LARGEST_POSITIVE_INTEGER_72, result);
//    }
//
//    @Test
//    public void getNative72_PositiveZero(
//    ) {
//        long[] value = POSITIVE_ZERO_72;
//        BigInteger result = OnesComplement.getNative72(value);
//        assertEquals(BigInteger.ZERO, result);
//    }
//
//    @Test
//    public void getOnesComplement36_LargestPositive(
//    ) {
//        long value = 0_377777_777777l;
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(LARGEST_POSITIVE_INTEGER_36, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_PositiveOne(
//    ) {
//        long value = 1;
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(POSITIVE_ONE_36, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_PositiveOverflow_1(
//    ) {
//        long value = LARGEST_POSITIVE_INTEGER_36 + 1;
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(OnesComplement.SMALLEST_NEGATIVE_INTEGER_36, ocr._result);
//        assertTrue(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_PositiveOverflow_2(
//    ) {
//        //  Test a value bigger than 36 bits
//        long value = 0x7fff_ffff_ffff_ffffl;
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        //  Don't minalib the actual value - it doesn't really matter
//        assertTrue(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_NegativeOne(
//    ) {
//        long value = -1;
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(NEGATIVE_ONE_36, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_SmallestNegative(
//    ) {
//        long value = -(0_377777_777777l);
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(SMALLEST_NEGATIVE_INTEGER_36, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_NegativeOverflow_1(
//    ) {
//        long value = -(0_400000_000000l);
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(OnesComplement.LARGEST_POSITIVE_INTEGER_36, ocr._result);
//        assertTrue(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement36_Zero(
//    ) {
//        long value = 0;
//        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
//        OnesComplement.getOnesComplement36(value, ocr);
//        assertEquals(POSITIVE_ZERO_36, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_LargestPositive(
//    ) {
//        BigInteger value = OnesComplement.BI_LARGEST_POSITIVE_INTEGER_72;
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(LARGEST_POSITIVE_INTEGER_72, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_PositiveOne(
//    ) {
//        BigInteger value = BigInteger.valueOf(1);
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(POSITIVE_ONE_72, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_PositiveOverflow_1(
//    ) {
//        BigInteger value = OnesComplement.BI_LARGEST_POSITIVE_INTEGER_72.add(BigInteger.ONE);
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(OnesComplement.SMALLEST_NEGATIVE_INTEGER_72, ocr._result);
//        assertTrue(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_PositiveOverflow_2(
//    ) {
//        //  Test a value bigger than 72 bits
//        BigInteger value = new BigInteger("FFFFFFFFFFFFFFFFFFFF", 16);
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        //  Don't minalib the actual value - it doesn't really matter
//        assertTrue(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_NegativeOne(
//    ) {
//        BigInteger value = BigInteger.ONE.negate();
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(NEGATIVE_ONE_72, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_SmallestNegative(
//    ) {
//        BigInteger value = OnesComplement.BI_SMALLEST_NEGATIVE_INTEGER_72;
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(SMALLEST_NEGATIVE_INTEGER_72, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_NegativeOverflow_1(
//    ) {
//        BigInteger value = new BigInteger("-0400000000000000000000000", 8);
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(OnesComplement.LARGEST_POSITIVE_INTEGER_72, ocr._result);
//        assertTrue(ocr._overflow);
//    }
//
//    @Test
//    public void getOnesComplement72_Zero(
//    ) {
//        BigInteger value = BigInteger.ZERO;
//        OnesComplement.OnesComplement72Result ocr = new OnesComplement.OnesComplement72Result();
//        OnesComplement.getOnesComplement72(value, ocr);
//        assertArrayEquals(POSITIVE_ZERO_72, ocr._result);
//        assertFalse(ocr._overflow);
//    }
//
//    @Test
//    public void absoluteValue36_Positive(
//    ) {
//        long parameter = LARGEST_POSITIVE_INTEGER_36;
//        assertEquals(LARGEST_POSITIVE_INTEGER_36, OnesComplement.absoluteValue36(parameter));
//    }
//
//    @Test
//    public void absoluteValue36_Negative(
//    ) {
//        long parameter = NEGATIVE_ONE_36;
//        assertEquals(1, OnesComplement.absoluteValue36(parameter));
//    }
//
//    @Test
//    public void absoluteValue72_Positive(
//    ) {
//        long[] value = LARGEST_POSITIVE_INTEGER_72;
//        long[] result = new long[2];
//        OnesComplement.absoluteValue72(value, result);
//        assertArrayEquals(LARGEST_POSITIVE_INTEGER_72, result);
//    }
//
//    @Test
//    public void absoluteValue72_Negative(
//    ) {
//        long[] value = NEGATIVE_ONE_72;
//        long[] result = new long[2];
//        OnesComplement.absoluteValue72(value, result);
//        assertArrayEquals(POSITIVE_ONE_72, result);
//    }
//
//    @Test
//    public void add12_NegativeOnes(
//    ) {
//        long addend1 = NEGATIVE_ONE_12;
//        long addend2 = NEGATIVE_ONE_12;
//        long expected = 0_7775l;
//        long result = OnesComplement.add12Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add12_NegativeZeros(
//    ) {
//        long addend1 = NEGATIVE_ZERO_12;
//        long addend2 = NEGATIVE_ZERO_12;
//        long expected = NEGATIVE_ZERO_12;
//        long result = OnesComplement.add12Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add12_OppositeSigns(
//    ) {
//        long addend1 = 07723l;
//        long addend2 = 00054l;
//        long expected = POSITIVE_ZERO_12;
//        long result = OnesComplement.add12Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add12_OppositeZeros(
//    ) {
//        long addend1 = NEGATIVE_ZERO_12;
//        long addend2 = POSITIVE_ZERO_12;
//        long expected = POSITIVE_ZERO_12;
//        long result = OnesComplement.add12Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add12_PositiveOnes(
//    ) {
//        long addend1 = POSITIVE_ONE_12;
//        long addend2 = POSITIVE_ONE_12;
//        long expected = 02l;
//        long result = OnesComplement.add12Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add18_NegativeOnes(
//    ) {
//        long addend1 = NEGATIVE_ONE_18;
//        long addend2 = NEGATIVE_ONE_18;
//        long expected = 0_777775l;
//        long result = OnesComplement.add18Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add18_NegativeZeros(
//    ) {
//        long addend1 = NEGATIVE_ZERO_18;
//        long addend2 = NEGATIVE_ZERO_18;
//        long expected = NEGATIVE_ZERO_18;
//        long result = OnesComplement.add18Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add18_OppositeSigns(
//    ) {
//        long addend1 = 0777723l;
//        long addend2 = 00054l;
//        long expected = POSITIVE_ZERO_18;
//        long result = OnesComplement.add18Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add18_OppositeZeros(
//    ) {
//        long addend1 = NEGATIVE_ZERO_18;
//        long addend2 = POSITIVE_ZERO_18;
//        long expected = POSITIVE_ZERO_18;
//        long result = OnesComplement.add18Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add18_PositiveOnes(
//    ) {
//        long addend1 = POSITIVE_ONE_18;
//        long addend2 = POSITIVE_ONE_18;
//        long expected = 02l;
//        long result = OnesComplement.add18Simple(addend1, addend2);
//        assertEquals(expected, result);
//    }
//
//    @Test
//    public void add36_NegativeOnes(
//    ) {
//        long addend1 = NEGATIVE_ONE_36;
//        long addend2 = NEGATIVE_ONE_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(0_777777_777775l, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_NegativeOverflow(
//    ) {
//        long addend1 = SMALLEST_NEGATIVE_INTEGER_36;
//        long addend2 = NEGATIVE_ONE_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(OnesComplement.LARGEST_POSITIVE_INTEGER_36, ar._sum);
//        assertTrue(ar._carry);
//        assertTrue(ar._overflow);
//    }
//
//    @Test
//    public void add36_NegativeZeros(
//    ) {
//        long addend1 = NEGATIVE_ZERO_36;
//        long addend2 = NEGATIVE_ZERO_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(NEGATIVE_ZERO_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_OppositeSigns_PosNeg_NegResult(
//    ) {
//        long addend1 = POSITIVE_ONE_36;
//        long addend2 = 0_777777_777775L;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(NEGATIVE_ONE_36, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_OppositeSigns_PosNeg_PosResult(
//    ) {
//        long addend1 = 02L;
//        long addend2 = NEGATIVE_ONE_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ONE_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_OppositeSigns_PosNeg_ZeroResult(
//    ) {
//        long addend1 = 0_777777_777772L;
//        long addend2 = 05L;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ZERO_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_OppositeSigns_NegPos_NegResult(
//    ) {
//        long addend1 = 0_777777_777774L;
//        long addend2 = 02L;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(NEGATIVE_ONE_36, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_OppositeSigns_NegPos_PosResult(
//    ) {
//        long addend1 = 0_777777_777775L;
//        long addend2 = 03L;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ONE_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_OppositeSigns_NegPos_ZeroResult(
//    ) {
//        long addend1 = 0_777777_777770L;
//        long addend2 = 07L;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ZERO_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_PositiveOnes(
//    ) {
//        long addend1 = POSITIVE_ONE_36;
//        long addend2 = POSITIVE_ONE_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(02, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_PositiveOverflow(
//    ) {
//        long addend1 = LARGEST_POSITIVE_INTEGER_36;
//        long addend2 = POSITIVE_ONE_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(OnesComplement.SMALLEST_NEGATIVE_INTEGER_36, ar._sum);
//        assertFalse(ar._carry);
//        assertTrue(ar._overflow);
//    }
//
//    @Test
//    public void add36_PositiveZeros(
//    ) {
//        long addend1 = POSITIVE_ZERO_36;
//        long addend2 = POSITIVE_ZERO_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ZERO_36, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_Zeros_NegPos(
//    ) {
//        long addend1 = NEGATIVE_ZERO_36;
//        long addend2 = POSITIVE_ZERO_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ZERO_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add36_Zeros_PosNeg(
//    ) {
//        long addend1 = POSITIVE_ZERO_36;
//        long addend2 = NEGATIVE_ZERO_36;
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        OnesComplement.add36(addend1, addend2, ar);
//        assertEquals(POSITIVE_ZERO_36, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_NegativeOnes(
//    ) {
//        long[] addend1 = NEGATIVE_ONE_72;
//        long[] addend2 = NEGATIVE_ONE_72;
//        long[] expected = { 0_777777_777777l, 0_777777_777775l };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(expected, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_NegativeOverflow(
//    ) {
//        long[] addend1 = SMALLEST_NEGATIVE_INTEGER_72;
//        long[] addend2 = NEGATIVE_ONE_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(OnesComplement.LARGEST_POSITIVE_INTEGER_72, ar._sum);
//        assertTrue(ar._carry);
//        assertTrue(ar._overflow);
//    }
//
//    @Test
//    public void add72_NegativeZeros(
//    ) {
//        long[] addend1 = NEGATIVE_ZERO_72;
//        long[] addend2 = NEGATIVE_ZERO_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(NEGATIVE_ZERO_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_OppositeSigns_PosNeg_NegResult(
//    ) {
//        long[] addend1 = POSITIVE_ONE_72;
//        long[] addend2 = { 0_777777_777777l, 0_777777_777775L };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(NEGATIVE_ONE_72, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_OppositeSigns_PosNeg_PosResult(
//    ) {
//        long[] addend1 = { 0l, 02l };
//        long[] addend2 = NEGATIVE_ONE_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ONE_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_OppositeSigns_PosNeg_ZeroResult(
//    ) {
//        long[] addend1 = { 0_777777_777777l, 0_777777_777772l };
//        long[] addend2 = { 0, 05l };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ZERO_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_OppositeSigns_NegPos_NegResult(
//    ) {
//        long[] addend1 = { 0_777777_777777l, 0_777777_777774l };
//        long[] addend2 = { 0, 02l };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(NEGATIVE_ONE_72, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_OppositeSigns_NegPos_PosResult(
//    ) {
//        long[] addend1 = { 0_777777_777777l, 0_777777_777775l };
//        long[] addend2 = { 0l, 03l };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ONE_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_OppositeSigns_NegPos_ZeroResult(
//    ) {
//        long[] addend1 = { 0_777777_777777l, 0_777777_777770l };
//        long[] addend2 = { 0l, 07l };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ZERO_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_PositiveOnes(
//    ) {
//        long[] addend1 = POSITIVE_ONE_72;
//        long[] addend2 = POSITIVE_ONE_72;
//        long[] expected = { 0l, 02l };
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(expected, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_PositiveOverflow(
//    ) {
//        long[] addend1 = LARGEST_POSITIVE_INTEGER_72;
//        long[] addend2 = POSITIVE_ONE_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(OnesComplement.SMALLEST_NEGATIVE_INTEGER_72, ar._sum);
//        assertFalse(ar._carry);
//        assertTrue(ar._overflow);
//    }
//
//    @Test
//    public void add72_PositiveZeros(
//    ) {
//        long[] addend1 = POSITIVE_ZERO_72;
//        long[] addend2 = POSITIVE_ZERO_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ZERO_72, ar._sum);
//        assertFalse(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_Zeros_NegPos(
//    ) {
//        long[] addend1 = NEGATIVE_ZERO_72;
//        long[] addend2 = POSITIVE_ZERO_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ZERO_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test
//    public void add72_Zeros_PosNeg(
//    ) {
//        long[] addend1 = POSITIVE_ZERO_72;
//        long[] addend2 = NEGATIVE_ZERO_72;
//
//        OnesComplement.Add72Result ar = new OnesComplement.Add72Result();
//        OnesComplement.add72(addend1, addend2, ar);
//        assertArrayEquals(POSITIVE_ZERO_72, ar._sum);
//        assertTrue(ar._carry);
//        assertFalse(ar._overflow);
//    }
//
//    @Test(expected = DivideByZeroException.class)
//    public void divide72_byZero(
//    ) throws DivideByZeroException {
//        long[] dividend = { 10l, 10l };
//        long[] divisor = POSITIVE_ZERO_72;
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//    }
//
//    @Test(expected = DivideByZeroException.class)
//    public void divide72_byNegativeZero(
//    ) throws DivideByZeroException {
//        long[] dividend = { 10l, 10l };
//        long[] divisor = NEGATIVE_ZERO_72;
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//    }
//
//    @Test
//    public void divide72_pos_pos_noremainder(
//    ) throws DivideByZeroException {
//        long[] dividend = { 0l, 250l };
//        long[] divisor = { 0l, 25l };
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//
//        long[] expQuotient = { 0l, 10l };
//        assertArrayEquals(expQuotient, dr._quotient);
//        assertArrayEquals(POSITIVE_ZERO_72, dr._remainder);
//        assertFalse(dr._overflow);
//    }
//
//    @Test
//    public void divide72_pos_pos_remainder(
//    ) throws DivideByZeroException {
//        long[] dividend = { 0l, 12345l };
//        long[] divisor = { 0l, 1000l };
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//
//        long[] expQuotient = { 0l, 12l };
//        long[] expRemainder = { 0l, 345l };
//        assertArrayEquals(expQuotient, dr._quotient);
//        assertArrayEquals(expRemainder, dr._remainder);
//        assertFalse(dr._overflow);
//    }
//
//    @Test
//    public void divide72_neg_pos(
//    ) throws DivideByZeroException {
//        long[] dividend = { 0_777777_777777l, 0_777777_747706l };   //  -12345
//        long[] divisor = { 0l, 1000l };
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//
//        long[] expQuotient = { 0_777777_777777l, 0_777777_777763l };    //  -12
//        long[] expRemainder = { 0_777777_777777l, 0_777777_777246l };   //  -345
//        assertArrayEquals(expQuotient, dr._quotient);
//        assertArrayEquals(expRemainder, dr._remainder);
//        assertFalse(dr._overflow);
//    }
//
//    @Test
//    public void divide72_pos_neg(
//    ) throws DivideByZeroException {
//        long[] dividend = { 0l, 12345l };
//        long[] divisor = { 0_777777_777777l, 0_777777_776027l };    //  -1000
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//
//        long[] expQuotient = { 0_777777_777777l, 0_777777_777763l };    //  -12
//        long[] expRemainder = { 0l, 345l };
//        assertArrayEquals(expQuotient, dr._quotient);
//        assertArrayEquals(expRemainder, dr._remainder);
//        assertFalse(dr._overflow);
//    }
//
//    @Test
//    public void divide72_neg_neg(
//    ) throws DivideByZeroException {
//        long[] dividend = { 0_777777_777777l, 0_777777_747706l };   //  -12345
//        long[] divisor = { 0_777777_777777l, 0_777777_776027l };    //  -1000
//        OnesComplement.DivideResult dr = new OnesComplement.DivideResult();
//        OnesComplement.divide72(dividend, divisor, dr);
//
//        long[] expQuotient = { 0l, 12l };
//        long[] expRemainder = { 0_777777_777777l, 0_777777_777246l };   //  -345
//        assertArrayEquals(expQuotient, dr._quotient);
//        assertArrayEquals(expRemainder, dr._remainder);
//        assertFalse(dr._overflow);
//    }
//
//    //???? we should have a unit minalib for divide overflow
//
//    @Test
//    public void leftShiftCircular72_by0(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] expected = parameter;
//        long[] result = new long[2];
//        OnesComplement.leftShiftCircular72(parameter, 0, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void leftShiftCircular72_by3(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] expected = { 0_112223_334445l, 0_556667_770001l };
//        long[] result = new long[2];
//        OnesComplement.leftShiftCircular72(parameter, 3, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void leftShiftCircular72_by36(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] expected = { 0_555666_777000l, 0_111222_333444l };
//        long[] result = new long[2];
//        OnesComplement.leftShiftCircular72(parameter, 36, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void leftShiftCircular72_by72(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] expected = parameter;
//        long[] result = new long[2];
//        OnesComplement.leftShiftCircular72(parameter, 72, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void leftShiftCircular72_byNeg(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] expected = parameter;
//        long[] result = new long[2];
//        OnesComplement.leftShiftCircular72(parameter, -4, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void leftShiftLogical72_by3(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] expected = { 0_112223_334445l, 0_556667_770000l };
//        long[] result = new long[2];
//        OnesComplement.leftShiftLogical72(parameter, 3, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void leftShiftLogical72_by72(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] result = new long[2];
//        OnesComplement.leftShiftLogical72(parameter, 72, result);
//        assertArrayEquals(OnesComplement.POSITIVE_ZERO_72, result);
//    }
//
//    @Test
//    public void leftShiftLogical72_negCount(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] result = new long[2];
//        OnesComplement.leftShiftLogical72(parameter, -5, result);
//        assertArrayEquals(parameter, result);
//    }
//
//    @Test
//    public void leftShiftLogical72_zeroCount(
//    ) {
//        long[] parameter = { 0_111222_333444l, 0_555666_777000l };
//        long[] result = new long[2];
//        OnesComplement.leftShiftLogical72(parameter, 0, result);
//        assertArrayEquals(parameter, result);
//    }
//
//    @Test
//    public void multiply36_zeros(
//    ) {
//        long factor1 = POSITIVE_ZERO_36;
//        long factor2 = POSITIVE_ZERO_36;
//        long[] expected = POSITIVE_ZERO_72;
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void multiply36_negative_zeros(
//    ) {
//        long factor1 = NEGATIVE_ZERO_36;
//        long factor2 = NEGATIVE_ZERO_36;
//        long[] expected = POSITIVE_ZERO_72;
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void multiply36_small(
//    ) {
//        long factor1 = 194885;
//        long factor2 = 59938;
//        long[] expected = { 0l, factor1 * factor2 };
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void multiply36_large(
//    ) {
//        long factor1 = LARGEST_POSITIVE_INTEGER_36;
//        long factor2 = LARGEST_POSITIVE_INTEGER_36;
//        //  0_377777_777777 * 0_377777_777777 = 0177777_777777_000000_000001
//        long[] expected = { 0_177777_777777l, 0_000000_000001l };
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void multiply36_pos_neg_small(
//    ) {
//        long factor1 = 0_777777_777772l;    //  -5
//        long factor2 = 17;
//        long[] expected = { 0_777777_777777l, 0_777777_777652l };
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void multiply36_pos_neg_large(
//    ) {
//        long factor1 = LARGEST_POSITIVE_INTEGER_36;
//        long factor2 = SMALLEST_NEGATIVE_INTEGER_36;
//        long[] expected = { 0_600000_000000l, 0_777777_777776l };
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void multiply36_neg_neg(
//    ) {
//        long factor1 = SMALLEST_NEGATIVE_INTEGER_36;
//        long factor2 = SMALLEST_NEGATIVE_INTEGER_36;
//        long[] expected = { 0_177777_777777l, 0_000000_000001l };
//        long[] result = new long[2];
//
//        OnesComplement.multiply36(factor1, factor2, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void negate18_PositiveOne(
//    ) {
//        assertEquals(NEGATIVE_ONE_18, OnesComplement.negate18(POSITIVE_ONE_18));
//    }
//
//    @Test
//    public void negate18_PositiveZero(
//    ) {
//        assertEquals(NEGATIVE_ZERO_18, OnesComplement.negate18(POSITIVE_ZERO_18));
//    }
//
//    @Test
//    public void negate18_NegativeOne(
//    ) {
//        assertEquals(POSITIVE_ONE_18, OnesComplement.negate18(NEGATIVE_ONE_18));
//    }
//
//    @Test
//    public void negate18_NegativeZero(
//    ) {
//        assertEquals(POSITIVE_ZERO_18, OnesComplement.negate18(NEGATIVE_ZERO_18));
//    }
//
//    @Test
//    public void negate12_PositiveOne(
//    ) {
//        assertEquals(NEGATIVE_ONE_12, OnesComplement.negate12(POSITIVE_ONE_12));
//    }
//
//    @Test
//    public void negate12_PositiveZero(
//    ) {
//        assertEquals(NEGATIVE_ZERO_12, OnesComplement.negate12(POSITIVE_ZERO_12));
//    }
//
//    @Test
//    public void negate12_NegativeOne(
//    ) {
//        assertEquals(POSITIVE_ONE_12, OnesComplement.negate12(NEGATIVE_ONE_12));
//    }
//
//    @Test
//    public void negate12_NegativeZero(
//    ) {
//        assertEquals(POSITIVE_ZERO_12, OnesComplement.negate12(NEGATIVE_ZERO_12));
//    }
//
//    @Test
//    public void negate72_PositiveOne(
//    ) {
//        long[] result = new long[2];
//        OnesComplement.negate72(POSITIVE_ONE_72, result);
//        assertArrayEquals(NEGATIVE_ONE_72, result);
//    }
//
//    @Test
//    public void negate72_PositiveZero(
//    ) {
//        long[] result = new long[2];
//        OnesComplement.negate72(POSITIVE_ZERO_72, result);
//        assertArrayEquals(NEGATIVE_ZERO_72, result);
//    }
//
//    @Test
//    public void negate72_NegativeOne(
//    ) {
//        long[] result = new long[2];
//        OnesComplement.negate72(NEGATIVE_ONE_72, result);
//        assertArrayEquals(POSITIVE_ONE_72, result);
//    }
//
//    @Test
//    public void negate72_NegativeZero(
//    ) {
//        long[] result = new long[2];
//        OnesComplement.negate72(NEGATIVE_ZERO_72, result);
//        assertArrayEquals(POSITIVE_ZERO_72, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_negCount(
//    ) {
//        long parameter[] = { 0, 033225l };
//        long expected[] = parameter;
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, -3, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_neg_3Count(
//    ) {
//        long[] parameter = { 0_400000_000031l, 0_200000_112233l };
//        long[] expected = { 0_740000_000003l, 0_120000_011223l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 3, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_neg_70Count(
//    ) {
//        long[] parameter = { 0_421456_321456l, 0_376436_276112l };
//        long[] expected = { 0_777777_777777l, 0_777777_777776l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 70, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_neg_71Count(
//    ) {
//        long[] parameter = { 0_421456_321456l, 0_376436_276112l };
//        long[] expected = { 0_777777_777777l, 0_777777_777777l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 71, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_neg_72Count(
//    ) {
//        long[] parameter = { 0_421456_321456l, 0_376436_276112l };
//        long[] expected = { 0_777777_777777l, 0_777777_777777l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 72, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_pos_3Count(
//    ) {
//        long[] parameter = {0_000000_000123l, 0_000000_033225l };
//        long[] expected = {0_000000_000012l, 0_300000_003322l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 3, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_pos_70Count(
//    ) {
//        long[] parameter = { 0_321456_321456l, 0_223344_556677l };
//        long[] expected = { 0l, 01l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 70, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_pos_71Count(
//    ) {
//        long[] parameter = { 0_321456_321456l, 0_223344_556677l };
//        long[] expected = { 0l, 0l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 71, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_pos_72Count(
//    ) {
//        long[] parameter = { 0_321456_321456l, 0_223344_556677l };
//        long[] expected = { 0l, 0l };
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 72, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftAlgebraic72_zeroCount(
//    ) {
//        long[] parameter = { 0_722555_333444l, 0_334322_033225l };
//        long[] expected = parameter;
//        long result[] = new long[2];
//
//        OnesComplement.rightShiftAlgebraic72(parameter, 0, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftCircular72_1(
//    ) {
//        long operand[] = { 0_000111_222333l, 0_444555_666777l };
//        long result[] = new long[2];
//        long expected[] = { 0_770001_112223l, 0_334445_556667l };
//        OnesComplement.rightShiftCircular72(operand, 6, result);
//        assertArrayEquals(expected, result);
//    }
//
//    @Test
//    public void rightShiftCircular72_2(
//    ) {
//        long operand[] = { 0_000111_222000l, 0_000333_444000l };
//        long result[] = new long[2];
//        long expected[] = { 0_000001_112220l, 0_000003_334440l };
//        OnesComplement.rightShiftCircular72(operand, 6, result);
//        assertArrayEquals(expected, result);
//    }
}
