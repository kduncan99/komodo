/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for IndexRegister class
 */
public class TestIndexRegister {

    @Test
    public void getXM() {
        long w = 0_112233_445566L;
        assertEquals(0_445566, IndexRegister.getXM(w));
    }

    @Test
    public void getXM24() {
        long w = 0_112233_445566L;
        assertEquals(0_000033_445566L, IndexRegister.getXM24(w));
    }

    @Test
    public void getXI() {
        long w = 0_112233_445566L;
        assertEquals(0_112233, IndexRegister.getXI(w));
    }

    @Test
    public void getXI12() {
        long w = 0_112233_445566L;
        assertEquals(01122, IndexRegister.getXI12(w));
    }

    @Test
    public void getSignedXM_positive() {
        long w = 0_112233_335566L;
        assertEquals(0_335566, IndexRegister.getSignedXM(w));
    }

    @Test
    public void getSignedXM_negative() {
        long w = 0_112233_445566L;
        assertEquals(0_777777_445566L, IndexRegister.getSignedXM(w));
    }

    @Test
    public void getSignedXM24_positive() {
        long w = 0_112233_445566L;
        assertEquals(0_000033_445566, IndexRegister.getSignedXM24(w));
    }

    @Test
    public void getSignedXM24_negative() {
        long w = 0_112273_445566L;
        assertEquals(0_777773_445566L, IndexRegister.getSignedXM24(w));
    }

    @Test
    public void getSignedXI_positive() {
        long w = 0_112233_335566L;
        assertEquals(0_112233, IndexRegister.getSignedXI(w));
    }

    @Test
    public void getSignedXI_negative() {
        long w = 0_512233_445566L;
        assertEquals(0_777777_512233L, IndexRegister.getSignedXI(w));
    }

    @Test
    public void getSignedXI12_positive() {
        long w = 0_112233_445566L;
        assertEquals(0_001122, IndexRegister.getSignedXI12(w));
    }

    @Test
    public void getSignedXI12_negative() {
        long w = 0_612273_445566L;
        assertEquals(0_777777_776122L, IndexRegister.getSignedXI12(w));
    }

    @Test
    public void setXM() {
        long w = 0_112233_445566L;
        long result = IndexRegister.setXM(w, 0_332211);
        assertEquals(0_112233_332211L, result);
    }

    @Test
    public void setXM24() {
        long w = 0_112233_445566L;
        long result = IndexRegister.setXM24(w, 0_000044_332211);
        assertEquals(0_112244_332211L, result);
    }

    @Test
    public void setXI() {
        long w = 0_112233_445566L;
        long result = IndexRegister.setXI(w, 0_776655);
        assertEquals(0_776655_445566L, result);
    }

    @Test
    public void setXI12() {
        long w = 0_112233_445566L;
        long result = IndexRegister.setXI12(w, 0_7766);
        assertEquals(0_776633_445566L, result);
    }

    @Test
    public void decrement18_1() {
        long w = 0_000010_000010L;
        long result = IndexRegister.decrementModifier18(w);
        assertEquals(010, IndexRegister.getXI(result));
        assertEquals(0, IndexRegister.getXM(result));
    }

    @Test
    public void decrement18_2() {
        long w = 0_000020_000010L;
        long result = IndexRegister.decrementModifier18(w);
        assertEquals(020, IndexRegister.getXI(result));
        assertEquals(0_777767, IndexRegister.getXM(result));
    }

    @Test
    public void increment18_pos_pos() {
        long w = 0_000010_000010L;
        long result = IndexRegister.incrementModifier18(w);
        assertEquals(010, IndexRegister.getXI(result));
        assertEquals(020, IndexRegister.getXM(result));
    }

    @Test
    public void increment18_pos_neg() {
        long w = 0_000010_777775L;
        long result = IndexRegister.incrementModifier18(w);
        assertEquals(010, IndexRegister.getXI(result));
        assertEquals(06, IndexRegister.getXM(result));
    }

    @Test
    public void increment18_neg_pos() {
        long w = 0_777767_000004L;
        long result = IndexRegister.incrementModifier18(w);
        assertEquals(0_777767, IndexRegister.getXI(result));
        assertEquals(0_777773, IndexRegister.getXM(result));
    }

    @Test
    public void increment18_neg_neg() {
        long w = 0_777776_777776L;
        long result = IndexRegister.incrementModifier18(w);
        assertEquals(0_777776, IndexRegister.getXI(result));
        assertEquals(0_777775, IndexRegister.getXM(result));
    }

    @Test
    public void increment24_pos_pos() {
        long w = 0_0010_00000010L;
        long result = IndexRegister.incrementModifier24(w);
        assertEquals(010, IndexRegister.getXI12(result));
        assertEquals(020, IndexRegister.getXM24(result));
    }

    @Test
    public void increment24_pos_neg() {
        long w = 0_0010_77777775L;
        long result = IndexRegister.incrementModifier24(w);
        assertEquals(010, IndexRegister.getXI12(result));
        assertEquals(06, IndexRegister.getXM24(result));
    }

    @Test
    public void increment24_neg_pos() {
        long w = 0_7767_00000004L;
        long result = IndexRegister.incrementModifier24(w);
        assertEquals(0_7767, IndexRegister.getXI12(result));
        assertEquals(0_77777773, IndexRegister.getXM24(result));
    }

    @Test
    public void increment24_neg_neg() {
        long w = 0_7776_77777776L;
        long result = IndexRegister.incrementModifier24(w);
        assertEquals(0_7776, IndexRegister.getXI12(result));
        assertEquals(0_77777775, IndexRegister.getXM24(result));
    }
}
