/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.baselib.GeneralRegisterSet;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_Addressing_BasicMode extends Test_InstructionProcessor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests for addressing modes
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void immediateUnsigned_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(1),START$* .",
            "          LA,U      A0,01000 .",
            "          HALT      0 .",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Positive_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(1),START$* .",
            "          LA,XU     A0,01000",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_NegativeZero_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(1),START$* .",
            "          LA,XU     A0,0777777",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Negative_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(1),START$* .",
            "          LA,XU     A0,-1",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_777777_777776L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 5",
            "",
            "$(1),START$* .",
            "          LR,U      R5,01234",
            "          LA        A0,R5",
            "          HALT      0"
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01234, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_indexed_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 5",
            "",
            "$(1),START$* .",
            "          LR,U      R5,01234    . Put the test value in R5",
            "          LXM,U     X1,4        . Set X modifier to 4 and increment to 2",
            "          LXI,U     X1,2",
            "          LA        A0,R1,*X1   . Use X-reg modifying R1 GRS to get to R5",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01234, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000006L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
    }

    @Test
    public void grs_indirect_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(2)      $LIT",
            "INDIRECT* +R5                    . Only using the x,h,i, and u fields",
            "",
            "$(1),START$* .",
            "          LR,U      R5,01234      . Put the test value in R5",
            "          LA        A0,*INDIRECT  . Indirection through INDIRECT",
            "                                  .   will transfer content from R5 to A0",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01234, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void storage_indexed_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     +0",
            "          +01",
            "          +0",
            "          +0",
            "          +02",
            "          +0",
            "          +0",
            "          +03",
            "          +0",
            "          +0",
            "          +05",
            "          +0",
            "          +0",
            "          +010",
            "",
            "$(2)",
            "DATA2     $res      8",
            "",
            "$(1),START$*",
            "          LXM,U     X5,1",
            "          LXI,U     X5,3",
            "          LXM,U     X7,0",
            "          LXI,U     X7,1",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bankData = getBank(processors._instructionProcessor, 15);
        showDebugInfo(processors);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void storage_indirect_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1",
            "          NOP       *DATA2",
            "          NOP       *DATA1+2",
            "          NOP       *DATA1+3",
            "          NOP       *DATA1+4",
            "          NOP       DATA2+1",
            "",
            "$(2)",
            "DATA2",
            "          NOP       *DATA1+1",
            "          +         011,022,033,044,055,066",
            "",
            "$(1),START$*",
            "          LA        A0,*DATA1",
            "          HALT      0"
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_112233_445566L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void execRegisterSelection_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC .",
            "          $INFO 1 5",
            "",
            "$(1),START$* .",
            "          LA,U      EA5,01              . ",
            "          LX,U      EX5,05              . ",
            "          LR,U      ER5,077             . ",
            "          HALT      0                   . "
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setExecRegisterSetSelected(true);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EA5).getW());
        assertEquals(05, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EX5).getW());
        assertEquals(077, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.ER5).getW());
    }

    @Test
    public void storage_BasicMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC . ",
            "          $INFO 1 5",
            "",
            "$(0),DATA +0112233,0445566",
            "",
            "$(1),START$*",
            "          LA        A0,DATA",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_112233_445566L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    //TODO read reference violation GAP

    //TODO write reference violation GAP

    //TODO execute reference violation GAP

    //TODO read reference violation SAP

    //TODO write reference violation SAP

    //TODO execute reference violation SAP

    //TODO reference out of limits BASIC mode
}
