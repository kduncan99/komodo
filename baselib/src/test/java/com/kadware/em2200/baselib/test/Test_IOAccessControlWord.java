/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.kadware.em2200.baselib.IOAccessControlWord;
import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.baselib.Word36Array;
import com.kadware.em2200.baselib.Word36ArraySlice;
import com.kadware.em2200.baselib.exceptions.*;

/**
 * Unit tests for IOAccessControlWord
 */
public class Test_IOAccessControlWord {

    @Rule
    public ExpectedException _exception = ExpectedException.none();

    private static final long BUFFER_DATA[] = {
        0_111111_111111l,
        0_222222_222222l,
        0_333333_333333l,
        0_444444_444444l,
    };

    private static final Word36Array BUFFER = new Word36Array(BUFFER_DATA);

    @Test
    public void equals_false1(
    ) {
        Word36Array array1 = new Word36Array(10);
        Word36Array array2 = new Word36Array(10);
        array1.setValue(3, 0_777777_777777l);
        array2.setValue(3, 0_777777_777776l);

        IOAccessControlWord acw1 = new IOAccessControlWord(array1, 0, IOAccessControlWord.AddressModifier.Increment);
        IOAccessControlWord acw2 = new IOAccessControlWord(array2, 0, IOAccessControlWord.AddressModifier.Increment);
        assertFalse(acw1.equals(acw2));
    }

    @Test
    public void equals_false2(
    ) {
        Word36Array array = new Word36Array(10);
        array.setValue(3, 0_777777_777777l);
        array.setValue(4, 0_777777_777776l);

        IOAccessControlWord acw1 = new IOAccessControlWord(array, 0, IOAccessControlWord.AddressModifier.Increment);
        IOAccessControlWord acw2 = new IOAccessControlWord(array, 2, IOAccessControlWord.AddressModifier.Increment);
        assertFalse(acw1.equals(acw2));
    }

    @Test
    public void equals_false3(
    ) {
        Word36Array array = new Word36Array(10);
        array.setValue(3, 0_777777_777777l);
        array.setValue(4, 0_777777_777776l);

        IOAccessControlWord acw1 = new IOAccessControlWord(array, 0, IOAccessControlWord.AddressModifier.Increment);
        IOAccessControlWord acw2 = new IOAccessControlWord(array, 0, IOAccessControlWord.AddressModifier.Decrement);
        assertFalse(acw1.equals(acw2));
    }

    @Test
    public void equals_true(
    ) {
        Word36Array array = new Word36Array(10);
        array.setValue(3, 0_777777_777777l);
        array.setValue(4, 0_777777_777776l);

        IOAccessControlWord acw1 = new IOAccessControlWord(array, 0, IOAccessControlWord.AddressModifier.Increment);
        IOAccessControlWord acw2 = new IOAccessControlWord(array, 0, IOAccessControlWord.AddressModifier.Increment);
        assertTrue(acw1.equals(acw2));
    }

    @Test
    public void get_decrement(
    ) {
        //  We're going to access that sub-buffer in decreasing order for 5 accesses, which is one more than the size of the
        //  sub-buffer - the acw should give us zero for the last access.
        IOAccessControlWord acw = new IOAccessControlWord(BUFFER,
                                                          BUFFER.getArraySize() - 1,
                                                          IOAccessControlWord.AddressModifier.Decrement);

        //  Ask for the first 4 words in decreasing order
        int wordIndex = 0;
        assertEquals(0_444444_444444l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_222222_222222l, acw.getWord(wordIndex++).getW());
        assertEquals(0_111111_111111l, acw.getWord(wordIndex++).getW());
    }

    @Test
    public void get_increment(
    ) {
        //  We're going to access that sub-buffer in increasing order for 5 accesses, which is one more than the size of the
        //  sub-buffer - the acw should give us zero for the last access.
        IOAccessControlWord acw = new IOAccessControlWord(BUFFER, 0, IOAccessControlWord.AddressModifier.Increment);

        //  Ask for the first 4 words in increasing order, then ask for more, which will give us zero each time.
        int wordIndex = 0;
        assertEquals(0_111111_111111l, acw.getWord(wordIndex++).getW());
        assertEquals(0_222222_222222l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_444444_444444l, acw.getWord(wordIndex++).getW());
    }

    @Test
    public void get_noChange(
    ) {
        //  We're going to access that sub-buffer, with no increment or decrement, for 6 accesses.
        //  Start with a subset of the buffer which includes the second and third words of the main buffer.
        //  Then set up the ACW to begin at the second word of the subset (third word of the original buffer).
        Word36ArraySlice subset = new Word36ArraySlice(BUFFER, 1, 2);
        IOAccessControlWord acw = new IOAccessControlWord(subset, 1, IOAccessControlWord.AddressModifier.NoChange);

        int wordIndex = 0;
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
    }

    @Test
    public void get_outOfRange_1(
    ) {
        IOAccessControlWord acw = new IOAccessControlWord(BUFFER, 0, IOAccessControlWord.AddressModifier.Increment);

        //  Ask for a value which is out of range
        _exception.expect(InvalidArgumentRuntimeException.class);
        assertEquals(0l, acw.getWord(-1).getW());
    }

    @Test
    public void get_outOfRange_2(
    ) {
        IOAccessControlWord acw = new IOAccessControlWord(BUFFER, 0, IOAccessControlWord.AddressModifier.Increment);

        //  Ask for a value which is out of range
        _exception.expect(InvalidArgumentRuntimeException.class);
        assertEquals(0l, acw.getWord(4).getW());
    }

    @Test
    public void get_outOfRange_3(
    ) {
        IOAccessControlWord acw = new IOAccessControlWord(BUFFER, 0, IOAccessControlWord.AddressModifier.NoChange);

        //  Ask for a value which is out of range, but for NoChange it should not throw
        assertEquals(0_111111_111111l, acw.getWord(4).getW());
    }

    @Test
    public void get_skipData(
    ) {
        //  Just like increment
        IOAccessControlWord acw = new IOAccessControlWord(BUFFER, 0, IOAccessControlWord.AddressModifier.SkipData);

        int wordIndex = 0;
        assertEquals(0_111111_111111l, acw.getWord(wordIndex++).getW());
        assertEquals(0_222222_222222l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_444444_444444l, acw.getWord(wordIndex++).getW());
    }

    @Test
    public void set_decrement(
    ) {
        Word36Array buffer = new Word36Array(10);
        Word36Array comp = new Word36Array(10);

        IOAccessControlWord acw = new IOAccessControlWord(buffer, 9, IOAccessControlWord.AddressModifier.Decrement);

        for (int bx = 0; bx < buffer.getArraySize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setWord(bx, value);
            comp.setWord36(9 - bx, value);
        }

        assertTrue(comp.equals(buffer));
    }

    @Test
    public void set_increment(
    ) {
        Word36Array buffer = new Word36Array(10);
        Word36Array comp = new Word36Array(10);

        IOAccessControlWord acw = new IOAccessControlWord(buffer, 0, IOAccessControlWord.AddressModifier.Increment);

        for (int bx = 0; bx < buffer.getArraySize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setWord(bx, value);
            comp.setWord36(bx, value);
        }

        assertTrue(comp.equals(buffer));
    }

    @Test
    public void set_noChange(
    ) {
        Word36Array buffer = new Word36Array(10);
        Word36Array comp = new Word36Array(10);

        IOAccessControlWord acw = new IOAccessControlWord(buffer, 0, IOAccessControlWord.AddressModifier.NoChange);

        for (int bx = 0; bx < buffer.getArraySize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setValue(bx, value.getW());
            comp.setWord36(0, value);
        }

        assertTrue(comp.equals(buffer));
    }

    @Test
    public void set_outOfRange_1(
    ) {
        Word36Array buffer = new Word36Array(10);
        IOAccessControlWord acw = new IOAccessControlWord(buffer, 9, IOAccessControlWord.AddressModifier.Decrement);
        _exception.expect(InvalidArgumentRuntimeException.class);
        acw.setValue(10, 0);
    }

    @Test
    public void set_outOfRange_2(
    ) {
        Word36Array buffer = new Word36Array(10);
        IOAccessControlWord acw = new IOAccessControlWord(buffer, 9, IOAccessControlWord.AddressModifier.Decrement);
        _exception.expect(InvalidArgumentRuntimeException.class);
        acw.setValue(-1, 0);
    }

    @Test
    public void set_outOfRange_3(
    ) {
        Word36Array buffer = new Word36Array(10);
        IOAccessControlWord acw = new IOAccessControlWord(buffer, 9, IOAccessControlWord.AddressModifier.NoChange);
        acw.setValue(10, 0);
    }

    @Test
    public void set_skipData(
    ) {
        Word36Array buffer = new Word36Array(10);
        Word36Array comp = new Word36Array(10);

        IOAccessControlWord acw = new IOAccessControlWord(buffer, 0, IOAccessControlWord.AddressModifier.SkipData);

        for (int bx = 0; bx < buffer.getArraySize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setWord(bx, value);
            comp.setWord36(bx, value);
        }

        assertTrue(comp.equals(buffer));
    }
}
