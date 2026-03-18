/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Word36 class
 */
public class TestDoubleWord36 {

    @Test
    public void compare_PositiveZero_NegativeZero() {
        assertEquals(1, DoubleWord36.compare(0L, 0L, 0_777777_777777L, 0_777777_777777L));
    }

    @Test
    public void compare_NegativeZero_PositiveZero() {
        assertEquals(-1, DoubleWord36.compare(0_777777_777777L, 0_777777_777777L, 0L, 0L));
    }

    @Test
    public void compare_NegativeZero_NegativeZero() {
        assertEquals(0, DoubleWord36.compare(0_777777_777777L, 0_777777_777777L, 0_777777_777777L, 0_777777_777777L));
    }

    @Test
    public void compare_PositiveZero_PositiveZero() {
        assertEquals(0, DoubleWord36.compare(0L, 0L, 0L, 0L));
    }

    @Test
    public void compare_Positive_Negative() {
        assertEquals(1, DoubleWord36.compare(0L, 10L, 0_777777_777777L, 0_777777_777765L)); // +10 vs -10
    }

    @Test
    public void compare_Negative_Positive() {
        assertEquals(-1, DoubleWord36.compare(0_777777_777777L, 0_777777_777765L, 0L, 10L)); // -10 vs +10
    }

    @Test
    public void compare_Positive_Positive() {
        assertEquals(1, DoubleWord36.compare(0L, 20L, 0L, 10L));
        assertEquals(-1, DoubleWord36.compare(0L, 10L, 0L, 20L));
        assertEquals(0, DoubleWord36.compare(0L, 10L, 0L, 10L));
    }

    @Test
    public void compare_Negative_Negative() {
        assertEquals(1, DoubleWord36.compare(0_777777_777777L, 0_777777_777765L, 0_777777_777777L, 0_777777_777753L)); // -10 vs -20
        assertEquals(-1, DoubleWord36.compare(0_777777_777777L, 0_777777_777753L, 0_777777_777777L, 0_777777_777765L)); // -20 vs -10
        assertEquals(0, DoubleWord36.compare(0_777777_777777L, 0_777777_777765L, 0_777777_777777L, 0_777777_777765L)); // -10 vs -10
    }

    @Test
    public void compare_LargeValues() {
        // Largest positive: 0377777777777 777777777777
        // Smallest negative: 0400000000000 000000000000
        assertEquals(1, DoubleWord36.compare(0_377777_777777L, 0_777777_777777L, 0_400000_000000L, 0L));
        assertEquals(-1, DoubleWord36.compare(0_400000_000000L, 0L, 0_377777_777777L, 0_777777_777777L));
    }

    @Test
    public void testIsNegative() {
        assertTrue(DoubleWord36.isNegative(0_400000_000000L, 0L));
        assertTrue(DoubleWord36.isNegative(0_777777_777777L, 0_777777_777777L));
        assertFalse(DoubleWord36.isNegative(0_377777_777777L, 0_777777_777777L));
        assertFalse(DoubleWord36.isNegative(0L, 0L));
    }

    @Test
    public void testIsNegativeZero() {
        assertTrue(DoubleWord36.isNegativeZero(0_777777_777777L, 0_777777_777777L));
        assertFalse(DoubleWord36.isNegativeZero(0L, 0L));
        assertFalse(DoubleWord36.isNegativeZero(0_777777_777777L, 0L));
        assertFalse(DoubleWord36.isNegativeZero(0L, 0_777777_777777L));
    }

    @Test
    public void testIsPositive() {
        assertFalse(DoubleWord36.isPositive(0_400000_000000L, 0L));
        assertFalse(DoubleWord36.isPositive(0_777777_777777L, 0_777777_777777L));
        assertTrue(DoubleWord36.isPositive(0_377777_777777L, 0_777777_777777L));
        assertTrue(DoubleWord36.isPositive(0L, 0L));
    }

    @Test
    public void testIsPositiveZero() {
        assertTrue(DoubleWord36.isPositiveZero(0L, 0L));
        assertFalse(DoubleWord36.isPositiveZero(0_777777_777777L, 0_777777_777777L));
        assertFalse(DoubleWord36.isPositiveZero(0L, 1L));
        assertFalse(DoubleWord36.isPositiveZero(1L, 0L));
    }

    @Test
    public void testIsZero() {
        assertTrue(DoubleWord36.isZero(0L, 0L));
        assertTrue(DoubleWord36.isZero(0_777777_777777L, 0_777777_777777L));
        assertFalse(DoubleWord36.isZero(0L, 1L));
        assertFalse(DoubleWord36.isZero(0_777777_777777L, 0_777777_777776L));
    }

    @Test
    public void testAdd() {
        long[] dest = new long[2];
        Word36.Flags flags;

        // 1 + 1 = 2
        flags = DoubleWord36.add(new long[]{0L, 1L}, 0, new long[]{0L, 1L}, 0, dest, 0);
        assertEquals(0L, dest[0]);
        assertEquals(2L, dest[1]);
        assertFalse(flags._carry);
        assertFalse(flags._overflow);

        // -1 + 1 = 0
        flags = DoubleWord36.add(new long[]{0_777777_777777L, 0_777777_777776L}, 0, new long[]{0L, 1L}, 0, dest, 0);
        assertEquals(0L, dest[0]);
        assertEquals(0L, dest[1]);
        assertTrue(flags._carry);
        assertFalse(flags._overflow);

        // -0 + -0 = -0
        flags = DoubleWord36.add(new long[]{0_777777_777777L, 0_777777_777777L}, 0, new long[]{0_777777_777777L, 0_777777_777777L}, 0, dest, 0);
        assertEquals(0_777777_777777L, dest[0]);
        assertEquals(0_777777_777777L, dest[1]);
        assertFalse(flags._carry);
        assertFalse(flags._overflow);

        // Max positive + 1 = overflow
        flags = DoubleWord36.add(new long[]{0_377777_777777L, 0_777777_777777L}, 0, new long[]{0L, 1L}, 0, dest, 0);
        assertEquals(0_400000_000000L, dest[0]);
        assertEquals(0L, dest[1]);
        assertFalse(flags._carry);
        assertTrue(flags._overflow);
    }

    @Test
    public void testMultiply() {
        long[] dest = new long[2];

        // 2 * 3 = 6
        DoubleWord36.multiply(2L, 3L, dest, 0);
        assertEquals(0L, dest[0]);
        assertEquals(6L, dest[1]);

        // -2 * 3 = -6
        // -2 is 0_777777_777775L
        // Expected result -6 is 0_777777_777777L, 0_777777_777771L
        DoubleWord36.multiply(0_777777_777775L, 3L, dest, 0);
        assertEquals(0_777777_777777L, dest[0]);
        assertEquals(0_777777_777771L, dest[1]);

        // -2 * -3 = 6
        // -3 is 0_777777_777774L
        DoubleWord36.multiply(0_777777_777775L, 0_777777_777774L, dest, 0);
        assertEquals(0L, dest[0]);
        assertEquals(6L, dest[1]);
    }

    @Test
    public void testDivide() {
        long[] quot = new long[2];
        long[] rem = new long[2];

        // 7 / 3 = 2 rem 1
        DoubleWord36.divide(new long[]{0L, 7L}, 0, new long[]{0L, 3L}, 0, quot, 0, rem, 0);
        assertEquals(0L, quot[0]);
        assertEquals(2L, quot[1]);
        assertEquals(0L, rem[0]);
        assertEquals(1L, rem[1]);

        // -7 / 3 = -2 rem -1
        // -7 is MSW=0_777777_777777L, LSW=0_777777_777770L
        // Expected quot -2 is MSW=0_777777_777777L, LSW=0_777777_777775L
        // Expected rem -1 is MSW=0_777777_777777L, LSW=0_777777_777776L
        DoubleWord36.divide(new long[]{0_777777_777777L, 0_777777_777770L}, 0, new long[]{0L, 3L}, 0, quot, 0, rem, 0);
        assertEquals(0_777777_777777L, quot[0]);
        assertEquals(0_777777_777775L, quot[1]);
        assertEquals(0_777777_777777L, rem[0]);
        assertEquals(0_777777_777776L, rem[1]);
    }

    @Test
    public void testDivideShort() {
        long[] quot = new long[1];
        long[] rem = new long[1];

        // 7 / 3 = 2 rem 1
        DoubleWord36.divideShort(new long[]{0L, 7L}, 0, new long[]{0L, 3L}, 0, quot, 0, rem, 0);
        assertEquals(2L, quot[0]);
        assertEquals(1L, rem[0]);

        // -7 / 3 = -2 rem -1
        DoubleWord36.divideShort(new long[]{0_777777_777777L, 0_777777_777770L}, 0, new long[]{0L, 3L}, 0, quot, 0, rem, 0);
        assertEquals(0_777777_777775L, quot[0]);
        assertEquals(0_777777_777776L, rem[0]);
    }

    @Test
    public void testGetOnesComplement() {
        long[] dest = new long[2];
        DoubleWord36.getOnesComplement(BigInteger.valueOf(10), dest, 0);
        assertEquals(0L, dest[0]);
        assertEquals(10L, dest[1]);

        DoubleWord36.getOnesComplement(BigInteger.valueOf(-10), dest, 0);
        assertEquals(0_777777_777777L, dest[0]);
        assertEquals(0_777777_777765L, dest[1]);
    }

    @Test
    public void testGetTwosComplement() {
        BigInteger bi;
        bi = DoubleWord36.getTwosComplement(0L, 10L);
        assertEquals(BigInteger.valueOf(10), bi);

        bi = DoubleWord36.getTwosComplement(0_777777_777777L, 0_777777_777765L);
        assertEquals(BigInteger.valueOf(-10), bi);
    }

    @Test
    public void testGetOnesComplementShort() {
        long[] dest = new long[1];
        DoubleWord36.getOnesComplementShort(BigInteger.valueOf(10), dest, 0);
        assertEquals(10L, dest[0]);

        DoubleWord36.getOnesComplementShort(BigInteger.valueOf(-10), dest, 0);
        assertEquals(0_777777_777765L, dest[0]);
    }

    @Test
    public void testIsNegativeArray() {
        assertTrue(DoubleWord36.isNegative(new long[]{0_400000_000000L, 0L}, 0));
        assertFalse(DoubleWord36.isNegative(new long[]{0_377777_777777L, 0_777777_777777L}, 0));
    }

    @Test
    public void testIsNegativeZeroArray() {
        assertTrue(DoubleWord36.isNegativeZero(new long[]{0_777777_777777L, 0_777777_777777L}, 0));
        assertFalse(DoubleWord36.isNegativeZero(new long[]{0L, 0L}, 0));
    }
}
