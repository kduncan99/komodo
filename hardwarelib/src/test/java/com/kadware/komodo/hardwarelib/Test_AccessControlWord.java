/*
 * Copyright (c) 20182019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.exceptions.InvalidArgumentRuntimeException;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

/**
 * Unit tests for Test_AccessControlWord
 */
public class Test_AccessControlWord {

    @Rule
    public ExpectedException _exception = ExpectedException.none();

    private static final long BUFFER_DATA[] = {
        0_111111_111111l,
        0_222222_222222l,
        0_333333_333333l,
        0_444444_444444l,
    };

    private static final ArraySlice BUFFER = new ArraySlice(BUFFER_DATA);

    @Test
    public void equals_false1(
    ) {
        ArraySlice array1 = new ArraySlice(new long[10]);
        ArraySlice array2 = new ArraySlice(new long[10]);
        array1.set(3, 0_777777_777777l);
        array2.set(3, 0_777777_777776l);

        com.kadware.komodo.hardwarelib.AccessControlWord acw1 = new com.kadware.komodo.hardwarelib.AccessControlWord(array1, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        com.kadware.komodo.hardwarelib.AccessControlWord acw2 = new com.kadware.komodo.hardwarelib.AccessControlWord(array2, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        assertFalse(acw1.equals(acw2));
    }

    @Test
    public void equals_false2(
    ) {
        ArraySlice array = new ArraySlice(new long[10]);
        array.set(3, 0_777777_777777l);
        array.set(4, 0_777777_777776l);

        com.kadware.komodo.hardwarelib.AccessControlWord acw1 = new com.kadware.komodo.hardwarelib.AccessControlWord(array, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        com.kadware.komodo.hardwarelib.AccessControlWord acw2 = new com.kadware.komodo.hardwarelib.AccessControlWord(array, 2, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        assertFalse(acw1.equals(acw2));
    }

    @Test
    public void equals_false3(
    ) {
        ArraySlice array = new ArraySlice(new long[10]);
        array.set(3, 0_777777_777777l);
        array.set(4, 0_777777_777776l);

        com.kadware.komodo.hardwarelib.AccessControlWord acw1 = new com.kadware.komodo.hardwarelib.AccessControlWord(array, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        com.kadware.komodo.hardwarelib.AccessControlWord acw2 = new com.kadware.komodo.hardwarelib.AccessControlWord(array, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Decrement);
        assertFalse(acw1.equals(acw2));
    }

    @Test
    public void equals_true(
    ) {
        ArraySlice array = new ArraySlice(new long[10]);
        array.set(3, 0_777777_777777l);
        array.set(4, 0_777777_777776l);

        com.kadware.komodo.hardwarelib.AccessControlWord acw1 = new com.kadware.komodo.hardwarelib.AccessControlWord(array, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        com.kadware.komodo.hardwarelib.AccessControlWord acw2 = new com.kadware.komodo.hardwarelib.AccessControlWord(array, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);
        assertTrue(acw1.equals(acw2));
    }

    @Test
    public void get_decrement(
    ) {
        //  We're going to access that sub-buffer in decreasing order for 5 accesses, which is one more than the size of the
        //  sub-buffer - the acw should give us zero for the last access.
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(BUFFER,
                                                          BUFFER.getSize() - 1,
                                                                                                                    com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Decrement);

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
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(BUFFER, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);

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
        ArraySlice subset = new ArraySlice(BUFFER, 1, 2);
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(subset, 1, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.NoChange);

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
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(BUFFER, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);

        //  Ask for a value which is out of range
        _exception.expect(InvalidArgumentRuntimeException.class);
        assertEquals(0l, acw.getWord(-1).getW());
    }

    @Test
    public void get_outOfRange_2(
    ) {
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(BUFFER, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);

        //  Ask for a value which is out of range
        _exception.expect(InvalidArgumentRuntimeException.class);
        assertEquals(0l, acw.getWord(4).getW());
    }

    @Test
    public void get_outOfRange_3(
    ) {
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(BUFFER, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.NoChange);

        //  Ask for a value which is out of range, but for NoChange it should not throw
        assertEquals(0_111111_111111l, acw.getWord(4).getW());
    }

    @Test
    public void get_skipData(
    ) {
        //  Just like increment
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(BUFFER, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.SkipData);

        int wordIndex = 0;
        assertEquals(0_111111_111111l, acw.getWord(wordIndex++).getW());
        assertEquals(0_222222_222222l, acw.getWord(wordIndex++).getW());
        assertEquals(0_333333_333333l, acw.getWord(wordIndex++).getW());
        assertEquals(0_444444_444444l, acw.getWord(wordIndex++).getW());
    }

    @Test
    public void set_decrement(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        ArraySlice comp = new ArraySlice(new long[10]);

        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 9, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Decrement);

        for (int bx = 0; bx < buffer.getSize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setWord(bx, value);
            comp.set(9 - bx, value.getW());
        }

        assertTrue(comp.equals(buffer));
    }

    @Test
    public void set_increment(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        ArraySlice comp = new ArraySlice(new long[10]);

        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Increment);

        for (int bx = 0; bx < buffer.getSize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setWord(bx, value);
            comp.set(bx, value.getW());
        }

        assertTrue(comp.equals(buffer));
    }

    @Test
    public void set_noChange(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        ArraySlice comp = new ArraySlice(new long[10]);

        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.NoChange);

        for (int bx = 0; bx < buffer.getSize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setValue(bx, value.getW());
            comp.set(0, value.getW());
        }

        assertTrue(comp.equals(buffer));
    }

    @Test
    public void set_outOfRange_1(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 9, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Decrement);
        _exception.expect(InvalidArgumentRuntimeException.class);
        acw.setValue(10, 0);
    }

    @Test
    public void set_outOfRange_2(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 9, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.Decrement);
        _exception.expect(InvalidArgumentRuntimeException.class);
        acw.setValue(-1, 0);
    }

    @Test
    public void set_outOfRange_3(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 9, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.NoChange);
        acw.setValue(10, 0);
    }

    @Test
    public void set_skipData(
    ) {
        ArraySlice buffer = new ArraySlice(new long[10]);
        ArraySlice comp = new ArraySlice(new long[10]);

        com.kadware.komodo.hardwarelib.AccessControlWord acw = new com.kadware.komodo.hardwarelib.AccessControlWord(buffer, 0, com.kadware.komodo.hardwarelib.AccessControlWord.AddressModifier.SkipData);

        for (int bx = 0; bx < buffer.getSize(); ++bx) {
            Word36 value = new Word36();
            value.setQ1(bx);
            value.setQ2(bx);
            value.setQ3(bx);
            value.setQ4(bx);
            acw.setWord(bx, value);
            comp.set(bx, value.getW());
        }

        assertTrue(comp.equals(buffer));
    }
}
