/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_GeneralLoadInstructions extends BaseFunctions {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Load Instructions
    //  ----------------------------------------------------------------------------------------------------------------------------

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    //TODO
//    @Test
//    public void generalLoadImmediate_Extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Write some load instructions starting at absolute address 01000 for the MSP's UPI
//        String[] source = {
//            "          $EXTEND .",
//            "          $INFO 10 1",
//            "          $INFO 1 3",
//            "",
//            "$(1),START$* . ",
//            "          LA,U      A0,01000     . ",
//            "          LNA,U     A1,1         . ",
//            "          LNA,XU    A2,0777776   . ",
//            "          LMA,U     A3,2         . ",
//            "          LMA,XU    A4,0777774   . ",
//            "          LNMA,U    A5,4         . ",
//            "          LNMA,XU   A6,0777772   . ",
//            "          LA,XU     A7,0777777   . make sure neg zero is eliminated",
//            "          LX,XU     X5,01234     . ",
//            "          LXI,U     X0,0100      . ",
//            "          LXM,U     X0,022020    . ",
//            "          LXSI,XU   X1,0711111   . ",
//            "          LR,XU     R8,0755332   . ",
//            "          LX,XU     X6,0777776   . ",
//            "          LSBO,U    X6,035       . ",
//            "          LX,U      X7,0444444   . ",
//            "          LSBL,U    X7,033       . ",
//            "          HALT      0            . ",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(01000, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_777777_777776L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(01, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(02, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//        Assert.assertEquals(03, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
//        Assert.assertEquals(0_777777_777773L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
//        Assert.assertEquals(0_777777_777772L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
//        Assert.assertEquals(0_000100_022020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        Assert.assertEquals(0_111100_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
//        Assert.assertEquals(01234, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
//        Assert.assertEquals(0_777777_755332L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R8).getW());
//        Assert.assertEquals(0_357777_777776L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X6).getW());
//        Assert.assertEquals(0_003300_444444L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X7).getW());
//    }
//
    //TODO
//    @Test
//    public void X_A_RegisterOverlap(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  load some values into A0-A3, and check for corresponding values in X12-X15
//        //  This will be extended mode.
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,01000",
//            "          LA,U      A1,02000",
//            "          LA,U      A2,03000",
//            "          LA,U      A3,04000",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(01000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X12).getW());
//        Assert.assertEquals(02000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X13).getW());
//        Assert.assertEquals(03000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X14).getW());
//        Assert.assertEquals(04000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X15).getW());
//    }
//
    //TODO
//    @Test
//    public void partialWordLoad_ThirdWordMode(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Tests the various third-word transfer modes in quarter-word-mode for load instructions
//        //  This will be extended mode.
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 5",
//            "          $INFO 10 1",
//            "",
//            "$(0) .",
//            "DATA_W     + 0112233445566          . for LA,W",
//            "DATA_H2   + 0112233,0445566         . for LA,H2",
//            "DATA_H1   + 0112233,0445566         . for LA,H1",
//            "DATA_XH2P + 0111111,0377777         . for LA,XH2 positive operand",
//            "DATA_XH2N + 0111111,0400000         . for LA,XH2 negative operand",
//            "DATA_XH1P + 0355555,0222222         . for LA,XH1 positive operand",
//            "DATA_XH1N + 0455555,0222222         . for LA,XH1 negative operand",
//            "DATA_T3P  + 01111,02222,03333       . for LA,T3 positive operand",
//            "DATA_T3N  + 02222,03333,06666       . for LA,T3 negative operand",
//            "DATA_T2P  + 01111,02222,03333       . for LA,T2 positive operand",
//            "DATA_T2N  + 02222,05555,01111       . for LA,T2 negative operand",
//            "DATA_T1P  + 01111,02222,03333       . for LA,T1 positive operand",
//            "DATA_T1N  + 04444,03333,02222       . for LA,T1 negative operand",
//
//            "",
//            "$(1),START$* .",
//            "          LA,W      A0,DATA_W,,B2",
//            "          LA,H2     A1,DATA_H2,,B2",
//            "          LA,H1     A2,DATA_H1,,B2",
//            "          LA,XH2    A3,DATA_XH2P,,B2",
//            "          LA,XH2    A4,DATA_XH2N,,B2",
//            "          LA,XH1    A5,DATA_XH1P,,B2",
//            "          LA,XH1    A6,DATA_XH1N,,B2",
//            "          LA,T3     A7,DATA_T3P,,B2",
//            "          LA,T3     A8,DATA_T3N,,B2",
//            "          LA,T2     A9,DATA_T2P,,B2",
//            "          LA,T2     A10,DATA_T2N,,B2",
//            "          LA,T1     A11,DATA_T1P,,B2",
//            "          LA,T1     A12,DATA_T1N,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_112233_445566L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_000000_445566L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_000000_112233L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(0_000000_377777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//        Assert.assertEquals(0_777777_400000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
//        Assert.assertEquals(0_000000_355555L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
//        Assert.assertEquals(0_777777_455555L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
//        Assert.assertEquals(0_000000_003333L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
//        Assert.assertEquals(0_777777_776666L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
//        Assert.assertEquals(0_000000_002222L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
//        Assert.assertEquals(0_777777_775555L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A10).getW());
//        Assert.assertEquals(0_000000_001111L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A11).getW());
//        Assert.assertEquals(0_777777_774444L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A12).getW());
//    }
//
    //TODO
//    @Test
//    public void partialWordLoad_QuarterWordMode(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Tests the various partial-word transfer modes in quarter-word-mode for load instructions
//        //  This will be extended mode.
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA_QW   + 0111,0222,0333,0444",
//            "DATA_SW   + 011,022,033,044,055,066",
//            "",
//            "$(1),START$*",
//            "          LR,Q2     R0,DATA_QW,,B2",
//            "          LR,Q4     R1,DATA_QW,,B2",
//            "          LR,Q3     R2,DATA_QW,,B2",
//            "          LR,Q1     R3,DATA_QW,,B2",
//            "          LR,S6     R4,DATA_SW,,B2",
//            "          LR,S5     R5,DATA_SW,,B2",
//            "          LR,S4     R6,DATA_SW,,B2",
//            "          LR,S3     R7,DATA_SW,,B2",
//            "          LR,S2     R8,DATA_SW,,B2",
//            "          LR,S1     R9,DATA_SW,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0222, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R0).getW());
//        Assert.assertEquals(0444, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R1).getW());
//        Assert.assertEquals(0333, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R2).getW());
//        Assert.assertEquals(0111, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R3).getW());
//        Assert.assertEquals(066, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R4).getW());
//        Assert.assertEquals(055, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R5).getW());
//        Assert.assertEquals(044, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R6).getW());
//        Assert.assertEquals(033, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R7).getW());
//        Assert.assertEquals(022, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R8).getW());
//        Assert.assertEquals(011, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R9).getW());
//    }

    //TODO need some various register-to-register loads

    //TODO need to minalib partial word x-fers (that they transfer the full word)

    //TODO
//    @Test
//    public void loadRegisterSet_normal(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Testing LRS with non-zero count1 and count2
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0),DATA .",
//            "          + 1",
//            "          + 2",
//            "          + 3",
//            "          + 4",
//            "          + 5",
//            "          + 6",
//            "          + 7",
//            "          + 8",
//            "          + 9",
//            "          + 10",
//            "          + 11",
//            "          + 12",
//            "DESCRIPTOR + 6,X0,4,R0",
//            "",
//            "$(1),START$*",
//            "          LA        A4,DESCRIPTOR,,B2",
//            "          LRS       A4,DATA,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(01, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R0).getW());
//        Assert.assertEquals(02, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R1).getW());
//        Assert.assertEquals(03, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R2).getW());
//        Assert.assertEquals(04, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R3).getW());
//        Assert.assertEquals(05, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        Assert.assertEquals(06, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
//        Assert.assertEquals(07, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
//        Assert.assertEquals(010, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
//        Assert.assertEquals(011, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
//        Assert.assertEquals(012, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
//    }
//
    //TODO
//    @Test
//    public void loadRegisterSet_count1Empty(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Testing LRS with non-zero count1 and count2
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0),DATA .",
//            "          + 1",
//            "          + 2",
//            "          + 3",
//            "          + 4",
//            "          + 5",
//            "          + 6",
//            "          + 7",
//            "          + 8",
//            "          + 9",
//            "          + 10",
//            "          + 11",
//            "          + 12",
//            "DESCRIPTOR + 6,X0,0,R0",
//            "",
//            "$(1),START$*",
//            "          LA        A4,DESCRIPTOR,,B2",
//            "          LRS       A4,DATA,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(01, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        Assert.assertEquals(02, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
//        Assert.assertEquals(03, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
//        Assert.assertEquals(04, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
//        Assert.assertEquals(05, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
//        Assert.assertEquals(06, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
//    }
//
    //TODO
//    @Test
//    public void loadRegisterSet_nop(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Testing LRS with non-zero count1 and count2
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "DATA",
//            "          + 1",
//            "          + 2",
//            "          + 3",
//            "          + 4",
//            "          + 5",
//            "          + 6",
//            "          + 7",
//            "          + 8",
//            "          + 9",
//            "          + 10",
//            "          + 11",
//            "          + 12",
//            "DESCRIPTOR + 0,X0,0,R0",
//            "",
//            "$(1),START$*",
//            "          LA        A4,DESCRIPTOR,,B2",
//            "          LRS       A4,DATA,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R0).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R1).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R2).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.R3).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
//    }
//
    //TODO
//    @Test
//    public void generalLoadFromStorage_Extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  Testing load instructions which cannot be tested with immediate addressing
//        //  Also, minalib loading from multiple banks, including exec banks
//        String[] source = {
//            "          $EXTEND",
//            "",
//            "$(0),DATA1 . Based on B2",
//            "          + 0",
//            "          + 0",
//            "          + 01,01",
//            "          + 01,02",
//            "",
//            "$(2),DATA2 . Based on B3",
//            "          + 01,03",
//            "          + 01,04",
//            "          + 0777777777777",
//            "          + 0777777777776",
//            "",
//            "$(4),DATA3 . Based on B4",
//            "          + 01,05",
//            "          + 01,06",
//            "          + 0777777777777",
//            "          + 0777777777775",
//            "",
//            "$(6),DATA4 . Based on B5",
//            "          + 0112233,0445566",
//            "          + 0223344,0556677",
//            "",
//            "$(8),DATA5 . Based on B6",
//            "          + 0111,0222,0333,0444",
//            "          + 0222,0333,0444,0555",
//            "          + 0333,0444,0555,0666",
//            "          + 0444,0555,0666,0777",
//            "",
//            "$(1),START$* .",
//            "          . Some special load instructions",
//            "          DL        A0,DATA1+2,,B2",
//            "          DLN       A2,DATA2,,B3",
//            "          DLN       A4,DATA2+2,,B3",
//            "          DLM       A6,DATA3,,B4",
//            "          DLM       A8,DATA3+2,,B4",
//            "          LXLM      X0,DATA4,,B5",
//            "          LXSI      X0,DATA4+1,,B5",
//            "",
//            "          . Some LAQW Silliness",
//            "          LXM,U     X5,0",
//            "          LXI,U     X5,0",
//            "          LAQW      A12,DATA5,X5,B6",
//            "          LXM,U     X5,1",
//            "          LXI,U     X5,010000",
//            "          LAQW      A13,DATA5,X5,B6",
//            "          LXM,U     X5,2",
//            "          LXI,U     X5,020000",
//            "          LAQW      A14,DATA5,X5,B6",
//            "          LXM,U     X5,3",
//            "          LXI,U     X5,030000",
//            "          LAQW      A15,DATA5,X5,B6",
//            "",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtendedMultibank(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(01_000001, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(01_000002, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0777776_777774L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(0777776_777773L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
//        Assert.assertEquals(1, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
//        Assert.assertEquals(01_000005, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
//        Assert.assertEquals(01_000006, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
//        Assert.assertEquals(2, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
//        Assert.assertEquals(0_667733_445566L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        Assert.assertEquals(0111, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A12).getW());
//        Assert.assertEquals(0333, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A13).getW());
//        Assert.assertEquals(0555, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A14).getW());
//        Assert.assertEquals(0777, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A15).getW());
//    }

    //TODO minalib generating various interrupts

    //TODO storage limits testing for load operand, store value, double load operand, LRS
}
