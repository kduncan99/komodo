/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for InstructionWord class
 */
public class Test_InstructionWord {

    //  Getters --------------------------------------------------------------------------------------------------------------------

    @Test
    public void getF(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(017, iw.getF());
    }

    @Test
    public void getJ(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(015, iw.getJ());
    }

    @Test
    public void getA(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(013, iw.getA());
    }

    @Test
    public void getX(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(011, iw.getX());
    }

    @Test
    public void getH(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(0, iw.getH());
    }

    @Test
    public void getI(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(01, iw.getI());
    }

    @Test
    public void getU(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(01777, iw.getU());
    }

    @Test
    public void getHIU(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 01777);
        assertEquals(0201777, iw.getHIU());
    }

    @Test
    public void getB(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 07, 01532);
        assertEquals(07, iw.getB());
    }

    @Test
    public void getD(
    ) {
        InstructionWord iw = new InstructionWord(017, 015, 013, 011, 0, 1, 07, 01532);
        assertEquals(01532, iw.getD());
    }

    //  Setters --------------------------------------------------------------------------------------------------------------------

    @Test
    public void setF_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setF(023);
        assertEquals(0_230000_000000l, result.getW());
    }

    @Test
    public void setF_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setF(0);
        assertEquals(0_007777_777777, result.getW());
    }

    @Test
    public void setJ_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setJ(03);
        assertEquals(0_001400_000000l, result.getW());
    }

    @Test
    public void setJ_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setJ(0);
        assertEquals(0_770377_777777l, result.getW());
    }

    @Test
    public void setA_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setA(013);
        assertEquals(0_000260_000000l, result.getW());
    }

    @Test
    public void setA_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setA(013);
        assertEquals(0_777677_777777l, result.getW());
    }

    @Test
    public void setX_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setX(014);
        assertEquals(0_000014_000000l, result.getW());
    }

    @Test
    public void setX_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setX(014);
        assertEquals(0_777774_777777l, result.getW());
    }

    @Test
    public void setH_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setH(1);
        assertEquals(0_000000_400000l, result.getW());
    }

    @Test
    public void setH_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setH(0);
        assertEquals(0_777777_377777l, result.getW());
    }

    @Test
    public void setI_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setH(1);
        assertEquals(0_000000_400000l, result.getW());
    }

    @Test
    public void setI_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setI(0);
        assertEquals(0_777777_577777l, result.getW());
    }

    @Test
    public void setU(
    ) {
        InstructionWord iw = new InstructionWord(0_112233_645566l);
        InstructionWord result = iw.setU(0_102030);
        assertEquals(0_112233_702030l, result.getW());
    }

    @Test
    public void setHIU(
    ) {
        InstructionWord iw = new InstructionWord(0_112233_145566l);
        InstructionWord result = iw.setHIU(0_600012l);
        assertEquals(0_112233_600012l, result.getW());
    }

    @Test
    public void setXHIU_over0(
    ) {
        InstructionWord iw = new InstructionWord(0);
        InstructionWord result = iw.setXHIU(0_012_334455l);
        assertEquals(0_012_334455l, result.getW());
    }

    @Test
    public void setXHIU_over7(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setXHIU(0_002_334455l);
        assertEquals(0_777762_334455l, result.getW());
    }

    @Test
    public void setB_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setB(013);
        assertEquals(0_000000_130000l, result.getW());
    }

    @Test
    public void setB_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setB(013);
        assertEquals(0_777777_737777l, result.getW());
    }

    @Test
    public void setD_0(
    ) {
        InstructionWord iw = new InstructionWord();
        InstructionWord result = iw.setD(01723);
        assertEquals(0_000000_001723l, result.getW());
    }

    @Test
    public void setD_077(
    ) {
        InstructionWord iw = new InstructionWord(0_777777_777777l);
        InstructionWord result = iw.setD(01723);
        assertEquals(0_777777_771723l, result.getW());
    }


    //  Interpret ------------------------------------------------------------------------------------------------------------------

    private static class InterpretInfo {
        public final InstructionWord _instructionWord;
        public final boolean _extendedModeFlag;
        public final String _mnemonic;
        public final String _expectedString;

        public InterpretInfo(
            final InstructionWord instructionWord,
            final boolean extendedModeFlag,
            final String mnemonic,
            final String expectedString
        ) {
            _instructionWord = instructionWord;
            _extendedModeFlag = extendedModeFlag;
            _mnemonic = mnemonic;
            _expectedString = expectedString;
        }
    }

    @Test
    public void interpretBasicMode(
    ) {
        InterpretInfo infoArray[] = {
            new InterpretInfo(new InstructionWord(),                                false, "",   "00 00 00 00 0 0 000000"),
            new InterpretInfo(new InstructionWord(001, 0, 5, 0, 0, 0, 01000),       false, "SA", "SA          A5,01000"),
            new InterpretInfo(new InstructionWord(010, 2, 5, 010, 0, 1, 017777),    false, "LA", "LA,H1       A5,*017777,X8"),
            new InterpretInfo(new InstructionWord(027, 5, 5, 010, 1, 0, 031),       false, "LX", "LX,T3/Q4    X5,031,*X8"),
            new InterpretInfo(new InstructionWord(023, 0, 7, 010, 1, 1, 01000),     false, "LR", "LR          R7,*01000,*X8"),
            new InterpretInfo(new InstructionWord(014, 0, 2, 0, 0, 1, 031),         false, "AA", "AA          A2,A13"),
            new InterpretInfo(new InstructionWord(014, 017, 2, 0, 0_203100),        false, "AA", "AA,XU       A2,0203100"),
        };

        for (InterpretInfo info : infoArray) {
            assertEquals(info._mnemonic, info._instructionWord.getMnemonic(info._extendedModeFlag));
            assertEquals(info._expectedString, info._instructionWord.interpret(info._extendedModeFlag, false, 3));
        }
    }

    @Test
    public void interpretExtendedMode(
    ) {
        InterpretInfo infoArray[] = {
            new InterpretInfo(new InstructionWord(),                              true, "",   "00 00 00 00 0 0 00 0000"),
            new InterpretInfo(new InstructionWord(040, 016, 04, 0, 0, 0, 020133), true, "OR", "OR,U        A4,020133"),
            new InterpretInfo(new InstructionWord(040, 014, 04, 0, 0, 0, 020133), true, "OR", "OR,S2       A4,0133,,B2"),
        };

        for (InterpretInfo info : infoArray) {
            assertEquals(info._mnemonic, info._instructionWord.getMnemonic(info._extendedModeFlag));
            assertEquals(info._expectedString, info._instructionWord.interpret(info._extendedModeFlag, false, 3));
        }
    }

    @Test
    public void interpretSpecial(
    ) {
        InterpretInfo infoArray[] = {
            new InterpretInfo(new InstructionWord(022, 015, 01, 0, 0, 0, 010655), true,  "BT",  "BT,S1       X1,B3,X0,B1,055"),
            new InterpretInfo(new InstructionWord(022, 0, 01, 02, 1, 1, 010637),  true,  "BT",  "BT          X1,B3,*X2,B1,*037"),
            new InterpretInfo(new InstructionWord(072, 011, 0, 0, 0, 0, 017),     false, "ER",  "ER          CSF$"),
            new InterpretInfo(new InstructionWord(070, 01, 04, 05, 1, 0, 013325), false, "JGD", "JGD         A8,013325,*X5"),
            new InterpretInfo(new InstructionWord(070, 01, 04, 05, 0, 0, 023325), true,  "JGD", "JGD         A8,023325,X5"),
        };

        for (InterpretInfo info : infoArray) {
            assertEquals(info._mnemonic, info._instructionWord.getMnemonic(info._extendedModeFlag));
            assertEquals(info._expectedString, info._instructionWord.interpret(info._extendedModeFlag, false, 3));
        }
    }
}
