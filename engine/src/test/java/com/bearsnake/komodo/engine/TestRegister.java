/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestRegister {

    @Test
    public void testGetValue() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        assertEquals(0_123456_765432L, reg.getW());
    }

    @Test
    public void testSetValue() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        assertEquals(0_123456_765432L, reg.getW());
        reg.setW(0_777777_000000L);
        assertEquals(0_777777_000000L, reg.getW());
    }

    @Test
    public void testGetW() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        assertEquals(0_123456_765432L, reg.getW());
    }

    @Test
    public void testGetXI() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        assertEquals(0_123456L, reg.getXI());
    }

    @Test
    public void testGetXI12() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        // _value = 001 010 011 100 101 110 111 110 101 100 011 010
        // _value >> 24 = 001 010 011 100 = 01234
        assertEquals(01234L, reg.getXI12());
    }

    @Test
    public void testGetXM() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        assertEquals(0_765432L, reg.getXM());
    }

    @Test
    public void testGetXM24() {
        Register reg = new Register();
        reg.setW(0_123456_765432L);
        // XM24 is bits 0-23
        // _value = 0_123456_765432L = 01 010 011 100 101 110 111 110 101 100 011 010 (binary)
        // 0-23 bits: 01 101 110 111 110 101 100 011 010 (binary)
        // 01 10 1110 1111 1010 1100 0110 10 -> Wait, octal is easier.
        // 0_123456_765432L is octal.
        // 012 345 676 543 2 (not quite)
        // 01 23 45 67 65 43 2 (not quite)
        // In octal, each digit is 3 bits. 12 digits = 36 bits.
        // 0_1 2 3 4 5 6 _ 7 6 5 4 3 2
        //   H1          H2
        // 0_1 2 3 4 5 6 | 7 6 5 4 3 2
        // bits 0-17: 765432
        // bits 18-23: 56 (from 123456)
        // so bits 0-23: 56765432 (octal)
        assertEquals(0_56765432L, reg.getXM24());
    }

    @Test
    public void testGetSignedXI() {
        Register reg = new Register();
        reg.setXI(0_777777L); // -0 in 1's complement 18-bit
        assertEquals(0_777777777777L, reg.getSignedXI());
        reg.setXI(0_400000L); // -MAX in 1's complement 18-bit
        assertEquals(0_777777400000L, reg.getSignedXI());
    }

    @Test
    public void testGetSignedXI12() {
        Register reg = new Register();
        reg.setXI12(0_7777L); // -0 in 1's complement 12-bit
        assertEquals(0_777777777777L, reg.getSignedXI12());
        reg.setXI12(0_4000L); // -MAX in 1's complement 12-bit
        assertEquals(0_777777774000L, reg.getSignedXI12());
    }

    @Test
    public void testGetSignedXM() {
        Register reg = new Register();
        reg.setXM(0_777777L);
        assertEquals(0_777777777777L, reg.getSignedXM());
    }

    @Test
    public void testGetSignedXM24() {
        Register reg = new Register();
        reg.setXM24(0_7777_7777L);
        assertEquals(0_777777777777L, reg.getSignedXM24());
    }

    @Test
    public void testSetXI() {
        Register reg = new Register();
        reg.setW(0);
        reg.setXI(0_123456L);
        assertEquals(0_123456_000000L, reg.getW());
    }

    @Test
    public void testSetXI12() {
        Register reg = new Register();
        reg.setW(0);
        reg.setXI12(0_1234L);
        assertEquals(0_123400_000000L, reg.getW());
    }

    @Test
    public void testSetXM() {
        Register reg = new Register();
        reg.setW(0);
        reg.setXM(0_765432L);
        assertEquals(0_000000_765432L, reg.getW());
    }

    @Test
    public void testSetXM24() {
        Register reg = new Register();
        reg.setW(0);
        reg.setXM24(0_56765432L);
        assertEquals(0_000056_765432L, reg.getW());
    }

    @Test
    public void testDecrementCounter18() {
        Register reg = new Register();
        reg.setW(0_000000_000005L);
        reg.decrementCounter18();
        assertEquals(0_000004L, reg.getW());
        
        reg.setW(0);
        reg.decrementCounter18();
        // --0 is -1, & 0_777777 is 0_777777
        assertEquals(0_777777L, reg.getW());
    }

    @Test
    public void testDecrementCounter24() {
        Register reg = new Register();
        reg.setW(5);
        reg.decrementCounter24();
        assertEquals(4, reg.getW());

        reg.setW(0);
        reg.decrementCounter24();
        assertEquals(0_7777_7777L, reg.getW());
    }

    @Test
    public void testDecrementModifier18() {
        Register reg = new Register();
        reg.setXI(1);
        reg.setXM(10);
        reg.decrementModifier18();
        assertEquals(9, reg.getXM());
    }

    @Test
    public void testIncrementModifier18() {
        Register reg = new Register();
        reg.setXI(1);
        reg.setXM(10);
        reg.incrementModifier18();
        assertEquals(11, reg.getXM());
    }

    @Test
    public void testIncrementModifier24() {
        Register reg = new Register();
        reg.setXI12(1);
        reg.setXM24(10);
        reg.incrementModifier24();
        assertEquals(11, reg.getXM24());
    }
}
