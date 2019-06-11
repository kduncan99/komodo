/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.test;

import com.kadware.em2200.baselib.ArraySlice;
import com.kadware.em2200.baselib.exceptions.InvalidArgumentRuntimeException;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Test_ArraySlice {

    @Test
    public void testConstructor1() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice = new ArraySlice(base);
        for (int sx = 0; sx < 8; ++sx) {
            assertEquals(sx, slice.get(sx));
        }
    }

    @Test
    public void testConstructor2_okay1() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice = new ArraySlice(base, 2, 4);
        for (int sx = 0; sx < 4; ++sx) {
            assertEquals(sx + 2, slice.get(sx));
        }
    }

    @Test
    public void testConstructor2_okay2() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice = new ArraySlice(base, 2, 6);
        for (int sx = 0; sx < 6; ++sx) {
            assertEquals(sx + 2, slice.get(sx));
        }
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor2_error1() {
        long[] base = new long[8];
        ArraySlice slice = new ArraySlice(base, -2, 4);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor2_error2() {
        long[] base = new long[8];
        ArraySlice slice = new ArraySlice(base, 2, 8);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor2_error3() {
        long[] base = new long[8];
        ArraySlice slice = new ArraySlice(base, 256, 8);
    }

    @Test
    public void testConstructor3() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        ArraySlice slice2 = new ArraySlice(slice1, 1, 2);
        for (int sx = 0; sx < 2; ++sx) {
            assertEquals(sx + 3, slice2.get(sx));
        }
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor3_error1() {
        long[] base = new long[8];
        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        ArraySlice slice2 = new ArraySlice(slice1, -2, 4);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor3_error2() {
        long[] base = new long[8];
        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        ArraySlice slice2 = new ArraySlice(slice1, 2, 4);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor3_error3() {
        long[] base = new long[8];
        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        ArraySlice slice2 = new ArraySlice(slice1, 29999, 4);
    }

    @Test
    public void testCoherency() {
        //  Ensure the base array acted upon by one slice shows updated values in a different slice.
        //  Also tests get() and set()
        long[] base = new long[16];
        ArraySlice slice1 = new ArraySlice(base, 4, 8);
        ArraySlice slice2 = new ArraySlice(base, 2, 10);
        for (int ax = 0; ax < 8; ++ax) {
            slice1.set(ax, ax);
        }
        for (int ax = 2; ax < 10; ++ax) {
            assertEquals(ax - 2, slice2.get(ax));
        }
    }

    @Test
    public void testGetAll() {
        long[] base = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
        long[] comp = { 30, 40, 50, 60, 70 };

        ArraySlice slice1 = new ArraySlice(base, 1, 8);
        ArraySlice slice2 = new ArraySlice(slice1, 1, 5);
        assertArrayEquals(comp, slice2.getAll());
    }

    @Test
    public void testLoad() {
        long[] base = { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20 };
        long[] comp = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 10, 12, 14, 16,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        ArraySlice slice = new ArraySlice(new long[32]);
        slice.load(base, 2, 6, 10);
        long[] view = slice.getAll();
        assertArrayEquals(comp, view);
    }
}
