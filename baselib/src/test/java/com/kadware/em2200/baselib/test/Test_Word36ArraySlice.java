/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.test;

import static org.junit.Assert.*;

import com.kadware.em2200.baselib.exceptions.InvalidArgumentRuntimeException;
import org.junit.Test;

import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.baselib.Word36Array;
import com.kadware.em2200.baselib.Word36ArraySlice;

public class Test_Word36ArraySlice {

    @Test
    public void basic() {
        Word36Array array = new Word36Array(5);
        array.setWord36(0, new Word36(0));
        array.setWord36(1, new Word36(1));
        array.setWord36(2, new Word36(2));
        array.setWord36(3, new Word36(3));
        array.setWord36(4, new Word36(4));

        Word36ArraySlice subset = new Word36ArraySlice(array, 2, 2);
        assertEquals(2, subset.getArraySize());
        assertEquals(2, subset.getWord36(0).getW());
        assertEquals(3, subset.getWord36(1).getW());
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void basicLimits_1() {
        Word36Array array = new Word36Array(16);
        Word36ArraySlice slice = new Word36ArraySlice(array, 8, 16);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void basicLimits_2() {
        Word36Array array = new Word36Array(16);
        Word36ArraySlice slice = new Word36ArraySlice(array, 24, 16);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void basicLimits_3() {
        Word36Array array = new Word36Array(16);
        Word36ArraySlice slice = new Word36ArraySlice(array, -1, 5);
    }

    @Test
    public void indirect() {
        Word36Array array = new Word36Array(16);
        for (int ax = 0; ax < 16; ++ax) {
            array.setWord36(ax, new Word36((ax << 18) | ax));
        }

        Word36ArraySlice mid = new Word36ArraySlice(array, 2, 12);
        Word36ArraySlice sub = new Word36ArraySlice(mid, 2, 8);
        assertEquals(8, sub.getArraySize());
        for (int ax = 0; ax < 8; ++ax) {
            int v = ax + 4;
            assertEquals((v << 18) | v, sub.getValue(ax));
        }
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void indirectLimits_1() {
        Word36Array array = new Word36Array(16);
        Word36ArraySlice mid = new Word36ArraySlice(array,4, 8);
        Word36ArraySlice slice = new Word36ArraySlice(mid, 4, 8);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void indirectLimits_2() {
        Word36Array array = new Word36Array(16);
        Word36ArraySlice mid = new Word36ArraySlice(array,4, 8);
        Word36ArraySlice slice = new Word36ArraySlice(mid, 10, 8);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void indirectLimits_3() {
        Word36Array array = new Word36Array(16);
        Word36ArraySlice mid = new Word36ArraySlice(array,4, 8);
        Word36ArraySlice slice = new Word36ArraySlice(mid, -1, 3);
    }
}
