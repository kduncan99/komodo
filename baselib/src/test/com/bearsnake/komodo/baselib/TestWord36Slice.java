/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestWord36Slice {

    @Test
    public void testBasic() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array);
        assertEquals(10, slice.getLength());
        assertEquals(0, slice.get(0));
        assertEquals(3, slice.get(3));
        assertEquals(9, slice.get(9));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIndexNegative() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array);
        slice.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIndexOutOfRange() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array);
        slice.get(10);
    }

    @Test
    public void testGetWord36() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array);
        assertEquals(6, slice.getWord36(6).getW());
    }

    @Test
    public void testBasicWithOffset() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array, 2, 5);
        assertEquals(5, slice.getLength());
        assertEquals(2, slice.get(0));
        assertEquals(6, slice.get(4));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBasicWithNegativeOffset() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array, -5, 5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBasicWithInvalidOffset() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array, 999, 5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBasicWithOffsetIndexOutOfRange() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var slice = new Word36Slice(array, 2, 5);
        slice.get(5);
    }

    @Test
    public void testSliceOfSlice() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        var base = new Word36Slice(array, 2, 15);
        var slice = new Word36Slice(base, 2, 6);
        assertEquals(6, slice.getLength());
        assertEquals(4, slice.get(0));
        assertEquals(9, slice.get(5));
    }

    @Test
    public void getArraySimple() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        var slice = new Word36Slice(array);
        assertArrayEquals(array, slice.getArray());
    }

    @Test
    public void getArraySliced() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        var slice = new Word36Slice(array, 5, 7);
        var chkArray = new long[]{5, 6, 7, 8, 9, 10, 11};
        assertArrayEquals(chkArray, slice.getArray());
    }

    @Test
    public void getArraySlicedSliced() {
        var array = new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        var slice = new Word36Slice(array, 2, 15);
        var chkArray = new long[]{5, 6, 7, 8, 9, 10};
        assertArrayEquals(chkArray, slice.getArray(3, 6));
    }
}
