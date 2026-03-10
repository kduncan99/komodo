/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for InstructionWord class
 */
public class TestInstructionWord {
    @Test
    public void testConstructor() {
        InstructionWord iw = new InstructionWord();
        assertEquals(0, iw.getW());
    }

    @Test
    public void testComposeU() {
        InstructionWord iw = new InstructionWord();
        int f = 0_12; // 001010 binary
        int j = 0_13; // 1011 binary
        int a = 0_14; // 1100 binary
        int x = 0_15; // 1101 binary
        int h = 1;
        int i = 0;
        int u = 0_123456; // 010 100 111 001 011 110 binary (16 bits)
        iw.compose(f, j, a, x, h, i, u);

        // Expected bits: f(6) j(4) a(4) x(4) h(1) i(1) u(16)
        // 001010 1011 1100 1101 1 0 0101001110010110 (this is not 16 bits, it's 16 digits?)
        // u = 0_123456 octal = 0x0A72E (16 bits)
        // Expected value:
        long expected = ((long)f << 30) | ((long)j << 26) | ((long)a << 22) | ((long)x << 18) | ((long)h << 17) | ((long)i << 16) | u;
        assertEquals(expected, iw.getW());
        assertEquals(f, iw.getF());
        assertEquals(j, iw.getJ());
        assertEquals(a, iw.getA());
        assertEquals(x, iw.getX());
        assertEquals(h, iw.getH());
        assertEquals(i, iw.getI());
        assertEquals(u, iw.getU());
    }

    @Test
    public void testComposeBD() {
        InstructionWord iw = new InstructionWord();
        int f = 0_12;
        int j = 0_13;
        int a = 0_14;
        int x = 0_15;
        int h = 1;
        int i = 0;
        int b = 0_16; // 1110 (4 bits)
        int d = 0_1234; // 001 010 011 100 (12 bits)
        iw.compose(f, j, a, x, h, i, b, d);

        long expected = ((long)f << 30) | ((long)j << 26) | ((long)a << 22) | ((long)x << 18) | ((long)h << 17) | ((long)i << 16) | ((long)b << 12) | d;
        assertEquals(expected, iw.getW());
        assertEquals(f, iw.getF());
        assertEquals(j, iw.getJ());
        assertEquals(a, iw.getA());
        assertEquals(x, iw.getX());
        assertEquals(h, iw.getH());
        assertEquals(i, iw.getI());
        assertEquals(b, iw.getB());
        assertEquals(d, iw.getD());
    }

    @Test
    public void testSetters() {
        InstructionWord iw = new InstructionWord();
        iw.setF(0_77);
        assertEquals(0_770000_000000L, iw.getW());
        iw.setJ(0_17);
        assertEquals(0_777400_000000L, iw.getW());
        iw.setA(0_17);
        assertEquals(0_777760_000000L, iw.getW());
        iw.setX(0_17);
        assertEquals(0_777777_000000L, iw.getW());
        iw.setH(1);
        assertEquals(0_777777_400000L, iw.getW());
        iw.setI(1);
        assertEquals(0_777777_600000L, iw.getW());
        iw.setU(0_177777);
        assertEquals(0_777777_777777L, iw.getW());

        iw.setW(0);
        iw.setB(0_17);
        assertEquals(0_000000_170000L, iw.getW());
        iw.setD(0_7777);
        assertEquals(0_000000_177777L, iw.getW());
    }

    @Test
    public void testCompositeSetters() {
        InstructionWord iw = new InstructionWord();
        iw.setHIU(0_777777L);
        assertEquals(0_777777L, iw.getW());
        assertEquals(0_777777L, iw.getHIU());

        iw.setW(0);
        iw.setXHIU(0_17777777L);
        assertEquals(0_17777777L, iw.getW());
    }

    @Test
    public void testMasking() {
        InstructionWord iw = new InstructionWord();
        iw.setF(0_177); // should be masked to 0_77
        assertEquals(0_77, iw.getF());

        iw.setJ(0_37); // should be masked to 0_17
        assertEquals(0_17, iw.getJ());

        iw.setA(0_37); // should be masked to 0_17
        assertEquals(0_17, iw.getA());

        iw.setX(0_37); // should be masked to 0_17
        assertEquals(0_17, iw.getX());

        iw.setH(2); // should be masked to 0
        assertEquals(0, iw.getH());
        iw.setH(3); // should be masked to 1
        assertEquals(1, iw.getH());

        iw.setI(2); // should be masked to 0
        assertEquals(0, iw.getI());

        iw.setU(0_377777); // should be masked to 0_177777
        assertEquals(0_177777, iw.getU());

        iw.setB(0_37); // should be masked to 0_17
        assertEquals(0_17, iw.getB());

        iw.setD(0_17777); // should be masked to 0_7777
        assertEquals(0_7777, iw.getD());
    }

    @Test
    public void testAllOnes() {
        InstructionWord iw = new InstructionWord();
        iw.setW(0_777777_777777L);
        assertEquals(0_77, iw.getF());
        assertEquals(0_17, iw.getJ());
        assertEquals(0_17, iw.getA());
        assertEquals(0_17, iw.getX());
        assertEquals(1, iw.getH());
        assertEquals(1, iw.getI());
        assertEquals(0_177777, iw.getU());
        assertEquals(0_17, iw.getB());
        assertEquals(0_7777, iw.getD());
        assertEquals(0_777777, iw.getHIU());
    }

    @Test
    public void testSetW() {
        InstructionWord iw = new InstructionWord();
        iw.setW(0xFFFFFFFFFFFFFFFFL); // all ones, 64-bit
        assertEquals(0_777777_777777L, iw.getW()); // should be masked to 36 bits
    }
}
