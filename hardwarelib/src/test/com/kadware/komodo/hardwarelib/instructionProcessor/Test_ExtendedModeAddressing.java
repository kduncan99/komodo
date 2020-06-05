/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_ExtendedModeAddressing extends BaseFunctions {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests for addressing modes
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void immediateUnsigned_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,U      A0,01000",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(01000, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());

        clear();
    }

    @Test
    public void immediateSignedExtended_Positive_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,XU     A0,01000",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(01000, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());

        clear();
    }

    @Test
    public void immediateSignedExtended_NegativeZero_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,XU     A0,0777777",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(0, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());

        clear();
    }

    @Test
    public void immediateSignedExtended_Negative_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,XU     A0,-1",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(0_777777_777776L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());

        clear();
    }

    @Test
    public void grs_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LR,U      R5,01234",
            "          LA        A0,R5",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(01234, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());

        clear();
    }

    @Test
    public void storage_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2),DATA",
            "          01122,03344,05566",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LBU       B2,DATABDI",
            "          LA        A0,DATA,,B2",
            "          HALT      0",
            "",
            "DESREG    + 0,0     . PP=0, ExtMode, Normal Regs",
            "DATABDI   + LBDIREF$+DATA,0",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(0_112233_445566L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());

        clear();
    }

    @Test
    public void grs_indexed_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LR,U      R5,01234",
            "          LXM,U     X1,4",
            "          LXI,U     X1,2",
            "          LA        A0,R1,*X1",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(01234, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000006L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());

        clear();
    }

    @Test
    public void storage_indexed_18BitModifier_ExtendedMode(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA1     0",
            "          01",
            "          0",
            "          0",
            "          02",
            "          0",
            "          0",
            "          03",
            "          0",
            "          0",
            "          05",
            "          0",
            "          0",
            "          010",
            "",
            "$(2),DATA2 . for auto-increment testing",
            "          $RES 8",
            "",
            "$(4),DATA3 . for X0 testing",
            "          $RES 8",
            "",
            "$(6),DATA4 . for non-auto-increment testing",
            "          $RES 8",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LBU       B2,DATA1BDI",
            "          LBU       B3,DATA2BDI",
            "          LBU       B4,DATA3BDI",
            "          LBU       B5,DATA4BDI",
            "",
            "          LXM,U     X5,1",
            "          LXI,U     X5,3",
            "          LXM,U     X7,0",
            "          LXI,U     X7,1",
            "          LXM,U     X0,1 . should do nothing",
            "          LXI,U     X0,1 . as above",
            "          LXM,U     X1,1",
            "          LXI,U     X1,0",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          HALT      0",
            "",
            "DESREG    + 0,0     . PP=0, ExtMode, Normal Regs",
            "DATA1BDI  + LBDIREF$+DATA1,0",
            "DATA2BDI  + LBDIREF$+DATA2,0",
            "DATA3BDI  + LBDIREF$+DATA3,0",
            "DATA4BDI  + LBDIREF$+DATA4,0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, true);
        ipl(true);
        showDebugInfo();

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());

        assertEquals(0_000001_000001L, _instructionProcessor.getExecOrUserXRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0_000000_000001L, _instructionProcessor.getExecOrUserXRegister(GeneralRegisterSet.X1).getW());
        assertEquals(0_000003_000020L, _instructionProcessor.getExecOrUserXRegister(GeneralRegisterSet.X5).getW());
        assertEquals(0_000001_000005L, _instructionProcessor.getExecOrUserXRegister(GeneralRegisterSet.X7).getW());

        long[] bank3Data = getBankByBaseRegister(3);
        assertEquals(01, bank3Data[0]);
        assertEquals(02, bank3Data[1]);
        assertEquals(03, bank3Data[2]);
        assertEquals(05, bank3Data[3]);
        assertEquals(010, bank3Data[4]);

        long[] bank4Data = getBankByBaseRegister(4);
        assertEquals(010, bank4Data[0]);
        assertEquals(0, bank4Data[1]);
        assertEquals(0, bank4Data[2]);
        assertEquals(0, bank4Data[3]);

        long[] bank5Data = getBankByBaseRegister(5);
        assertEquals(0, bank5Data[0]);
        assertEquals(010, bank5Data[1]);
        assertEquals(0, bank5Data[2]);
        assertEquals(0, bank5Data[3]);

        clear();
    }

//    @Test
//    public void storage_indexed_24BitModifier_ExtendedMode(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     0",
//            "          01",
//            "          0",
//            "          0",
//            "          02",
//            "          0",
//            "          0",
//            "          03",
//            "          0",
//            "          0",
//            "          05",
//            "          0",
//            "          0",
//            "          010",
//            "",
//            "$(2),DATA2",
//            "          $RES 8",
//            "",
//            "$(1),START$*",
//            "          LXM,U     X5,1",
//            "          LXI,U     X5,0300",
//            "          LXM,U     X7,0",
//            "          LXI,U     X7,0100",
//            "          LA        A3,DATA1,*X5,B2",
//            "          SA        A3,DATA2,*X7,B3",
//            "          LA        A3,DATA1,*X5,B2",
//            "          SA        A3,DATA2,*X7,B3",
//            "          LA        A3,DATA1,*X5,B2",
//            "          SA        A3,DATA2,*X7,B3",
//            "          LA        A3,DATA1,*X5,B2",
//            "          SA        A3,DATA2,*X7,B3",
//            "          LA        A3,DATA1,*X5,B2",
//            "          SA        A3,DATA2,*X7,B3",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtendedMultibank(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setExecutive24BitIndexingEnabled(true);
//        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(1);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        long[] bankData = getBank(processors._instructionProcessor, 3);
//        assertEquals(01, bankData[0]);
//        assertEquals(02, bankData[1]);
//        assertEquals(03, bankData[2]);
//        assertEquals(05, bankData[3]);
//        assertEquals(010, bankData[4]);
//    }
//
//    @Test
//    public void execRegisterSelection_ExtendedMode(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      EA5,01",
//            "          LX,U      EX5,05",
//            "          LR,U      ER5,077",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setExecRegisterSetSelected(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        Assert.assertEquals(01, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EA5).getW());
//        Assert.assertEquals(05, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EX5).getW());
//        Assert.assertEquals(077, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.ER5).getW());
//    }
//
//    //TODO read reference violation GAP
//
//    //TODO write reference violation GAP
//
//    //TODO execute reference violation GAP
//
//    //TODO read reference violation SAP
//
//    //TODO write reference violation SAP
//
//    //TODO execute reference violation SAP
//
//    //TODO reference out of limits EXTENDED mode
//
//    //TODO unbased Base Register reference EXTENDED mode

}
