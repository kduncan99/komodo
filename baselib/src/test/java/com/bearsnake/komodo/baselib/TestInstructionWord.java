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

    //  Getters --------------------------------------------------------------------------------------------------------------------

//    @Test
//    public void getF() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(017, InstructionWord.getF(iw));
//    }
//
//    @Test
//    public void getJ() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(015, InstructionWord.getJ(iw));
//    }
//
//    @Test
//    public void getA() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(013, InstructionWord.getA(iw));
//    }
//
//    @Test
//    public void getX() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(011, InstructionWord.getX(iw));
//    }
//
//    @Test
//    public void getH() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(0, InstructionWord.getH(iw));
//    }
//
//    @Test
//    public void getI() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(01, InstructionWord.getI(iw));
//    }
//
//    @Test
//    public void getU() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(01777, InstructionWord.getU(iw));
//    }
//
//    @Test
//    public void getHIU() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 01777);
//        assertEquals(0201777, InstructionWord.getHIU(iw));
//    }
//
//    @Test
//    public void getB() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 07, 01532);
//        assertEquals(07, InstructionWord.getB(iw));
//    }
//
//    @Test
//    public void getD() {
//        long iw = InstructionWord.compose(017, 015, 013, 011, 0, 1, 07, 01532);
//        assertEquals(01532, InstructionWord.getD(iw));
//    }
//
//    //  Setters --------------------------------------------------------------------------------------------------------------------
//
//    @Test
//    public void setF_0(
//    ) {
//        assertEquals(0_230000_000000L, InstructionWord.setF(0, 023));
//    }
//
//    @Test
//    public void setF_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setF(iw, 0);
//        assertEquals(0_007777_777777L, iw);
//    }
//
//    @Test
//    public void setJ_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setJ(iw, 03);
//        assertEquals(0_001400_000000L, iw);
//    }
//
//    @Test
//    public void setJ_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setJ(iw, 0);
//        assertEquals(0_770377_777777L, iw);
//    }
//
//    @Test
//    public void setA_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setA(iw, 013);
//        assertEquals(0_000260_000000L, iw);
//    }
//
//    @Test
//    public void setA_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setA(iw, 013);
//        assertEquals(0_777677_777777L, iw);
//    }
//
//    @Test
//    public void setX_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setX(iw, 014);
//        assertEquals(0_000014_000000L, iw);
//    }
//
//    @Test
//    public void setX_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setX(iw, 014);
//        assertEquals(0_777774_777777L, iw);
//    }
//
//    @Test
//    public void setH_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setH(iw, 1);
//        assertEquals(0_000000_400000L, iw);
//    }
//
//    @Test
//    public void setH_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setH(iw, 0);
//        assertEquals(0_777777_377777L, iw);
//    }
//
//    @Test
//    public void setI_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setH(iw, 1);
//        assertEquals(0_000000_400000L, iw);
//    }
//
//    @Test
//    public void setI_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setI(iw, 0);
//        assertEquals(0_777777_577777L, iw);
//    }
//
//    @Test
//    public void setU(
//    ) {
//        long iw = 0_112233_645566L;
//        iw = InstructionWord.setU(iw, 0_102030);
//        assertEquals(0_112233_702030L, iw);
//    }
//
//    @Test
//    public void setHIU(
//    ) {
//        long iw = 0_112233_145566L;
//        iw = InstructionWord.setHIU(iw, 0_600012L);
//        assertEquals(0_112233_600012L, iw);
//    }
//
//    @Test
//    public void setXHIU_over0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setXHIU(iw, 0_012_334455L);
//        assertEquals(0_012_334455L, iw);
//    }
//
//    @Test
//    public void setXHIU_over7(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setXHIU(iw, 0_002_334455L);
//        assertEquals(0_777762_334455L, iw);
//    }
//
//    @Test
//    public void setB_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setB(iw, 013);
//        assertEquals(0_000000_130000L, iw);
//    }
//
//    @Test
//    public void setB_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setB(iw, 013);
//        assertEquals(0_777777_737777L, iw);
//    }
//
//    @Test
//    public void setD_0(
//    ) {
//        long iw = 0;
//        iw = InstructionWord.setD(iw, 01723);
//        assertEquals(0_000000_001723L, iw);
//    }
//
//    @Test
//    public void setD_077(
//    ) {
//        long iw = 0_777777_777777L;
//        iw = InstructionWord.setD(iw, 01723);
//        assertEquals(0_777777_771723L, iw);
//    }
}
