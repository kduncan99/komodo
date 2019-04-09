/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.test;

import static org.junit.Assert.*;
import org.junit.Test;

import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.baselib.Word36Array;
import com.kadware.em2200.baselib.Word36ArraySlice;

public class Test_Word36ArraySlice {

    @Test
    public void basic(
    ) {
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
}
