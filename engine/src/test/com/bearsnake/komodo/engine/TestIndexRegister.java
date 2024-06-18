/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for IndexRegister class
 */
public class TestIndexRegister {

    @Test
    public void getXM(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(0_445566, w.getXM());
    }

    @Test
    public void getXM24(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(0_000033_445566l, w.getXM24());
    }

    @Test
    public void getXI(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(0_112233, w.getXI());
    }

    @Test
    public void getXI12(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(01122, w.getXI12());
    }

    @Test
    public void getSignedXM_positive(
    ) {
        IndexRegister w = new IndexRegister(0_112233_335566l);
        assertEquals(0_335566, w.getSignedXM());
    }

    @Test
    public void getSignedXM_negative(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(0_777777_445566l, w.getSignedXM());
    }

    @Test
    public void getSignedXM24_positive(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(0_000033_445566, w.getSignedXM24());
    }

    @Test
    public void getSignedXM24_negative(
    ) {
        IndexRegister w = new IndexRegister(0_112273_445566l);
        assertEquals(0_777773_445566l, w.getSignedXM24());
    }

    @Test
    public void getSignedXI_positive(
    ) {
        IndexRegister w = new IndexRegister(0_112233_335566l);
        assertEquals(0_112233, w.getSignedXI());
    }

    @Test
    public void getSignedXI_negative(
    ) {
        IndexRegister w = new IndexRegister(0_512233_445566l);
        assertEquals(0_777777_512233l, w.getSignedXI());
    }

    @Test
    public void getSignedXI12_positive(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        assertEquals(0_001122, w.getSignedXI12());
    }

    @Test
    public void getSignedXI12_negative(
    ) {
        IndexRegister w = new IndexRegister(0_612273_445566l);
        assertEquals(0_777777_776122l, w.getSignedXI12());
    }

    @Test
    public void setXM(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        IndexRegister result = w.setXM(0_332211);
        assertEquals(0_112233_332211l, result.getW());
    }

    @Test
    public void setXM24(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        IndexRegister result = w.setXM24(0_000044_332211);
        assertEquals(0_112244_332211l, result.getW());
    }

    @Test
    public void setXI(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        IndexRegister result = w.setXI(0_776655);
        assertEquals(0_776655_445566l, result.getW());
    }

    @Test
    public void setXI12(
    ) {
        IndexRegister w = new IndexRegister(0_112233_445566l);
        IndexRegister result = w.setXI12(0_7766);
        assertEquals(0_776633_445566l, result.getW());
    }

    @Test
    public void decrement18_1(
    ) {
        IndexRegister w = new IndexRegister(0_000010_000010l);
        IndexRegister result = w.decrementModifier18();
        assertEquals(010, result.getXI());
        assertEquals(0, result.getXM());
    }

    @Test
    public void decrement18_2(
    ) {
        IndexRegister w = new IndexRegister(0_000020_000010l);
        IndexRegister result = w.decrementModifier18();
        assertEquals(020, result.getXI());
        assertEquals(0_777767, result.getXM());
    }

    @Test
    public void increment18_pos_pos(
    ) {
        IndexRegister w = new IndexRegister(0_000010_000010l);
        IndexRegister result = w.incrementModifier18();
        assertEquals(010, result.getXI());
        assertEquals(020, result.getXM());
    }

    @Test
    public void increment18_pos_neg(
    ) {
        IndexRegister w = new IndexRegister(0_000010_777775l);
        IndexRegister result = w.incrementModifier18();
        assertEquals(010, result.getXI());
        assertEquals(06, result.getXM());
    }

    @Test
    public void increment18_neg_pos(
    ) {
        IndexRegister w = new IndexRegister(0_777767_000004l);
        IndexRegister result = w.incrementModifier18();
        assertEquals(0_777767, result.getXI());
        assertEquals(0_777773, result.getXM());
    }

    @Test
    public void increment18_neg_neg(
    ) {
        IndexRegister w = new IndexRegister(0_777776_777776l);
        IndexRegister result = w.incrementModifier18();
        assertEquals(0_777776, result.getXI());
        assertEquals(0_777775, result.getXM());
    }

    @Test
    public void increment24_pos_pos(
    ) {
        IndexRegister w = new IndexRegister(0_0010_00000010l);
        IndexRegister result = w.incrementModifier24();
        assertEquals(010, result.getXI12());
        assertEquals(020, result.getXM24());
    }

    @Test
    public void increment24_pos_neg(
    ) {
        IndexRegister w = new IndexRegister(0_0010_77777775l);
        IndexRegister result = w.incrementModifier24();
        assertEquals(010, result.getXI12());
        assertEquals(06, result.getXM24());
    }

    @Test
    public void increment24_neg_pos(
    ) {
        IndexRegister w = new IndexRegister(0_7767_00000004l);
        IndexRegister result = w.incrementModifier24();
        assertEquals(0_7767, result.getXI12());
        assertEquals(0_77777773, result.getXM24());
    }

    @Test
    public void increment24_neg_neg(
    ) {
        IndexRegister w = new IndexRegister(0_7776_77777776l);
        IndexRegister result = w.incrementModifier24();
        assertEquals(0_7776, result.getXI12());
        assertEquals(0_77777775, result.getXM24());
    }
}
