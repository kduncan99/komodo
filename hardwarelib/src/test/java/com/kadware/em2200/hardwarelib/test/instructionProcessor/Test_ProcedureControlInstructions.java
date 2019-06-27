/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.baselib.GeneralRegisterSet;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.InventoryManager;
import com.kadware.em2200.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.em2200.hardwarelib.exceptions.UPIConflictException;
import com.kadware.em2200.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_ProcedureControlInstructions extends BaseFunctions {

    @Test
    public void callNormal(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT . will be based on B2",
            "",
            "$(2) . will be initially based on B3",
            "          $RES 8",
            "",
            "$(4) . will be initially based on B4",
            "          $RES 8",
            "",
            "$(1),START$* . will be initially based on B0",
            "          CALL      (LBDICALL$+TARGET3,TARGET3),,B2",
            "          HALT      077 . should not get here",
            "",
            "$(3),TARGET3* . won't be initially based",
            "          CALL      (LBDICALL$+TARGET5,TARGET5),,B2",
            "          HALT      076 . or here",
            "",
            "$(5),TARGET5* . also won't be initially based",
            "          HALT      0   . should land here"
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank2(source, false);
        assert (absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        assertEquals(255, processors._instructionProcessor.getBaseRegister(25)._upperLimitNormalized);
        assertEquals(256 - 4, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EX0).getW());
    }

    @Test
    public void gotoNormal(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT . will be based on B2",
            "",
            "$(2) . will be initially based on B3",
            "          $RES 8",
            "",
            "$(4) . will be initially based on B4",
            "          $RES 8",
            "",
            "$(1),START$* . will be initially based on B0",
            "          GOTO      (LBDICALL$+TARGET3,TARGET3),,B2",
            "          HALT      077 . should not get here",
            "",
            "$(3),TARGET3* . won't be initially based",
            "          GOTO      (LBDICALL$+TARGET5,TARGET5),,B2",
            "          HALT      076 . or here",
            "",
            "$(5),TARGET5* . also won't be initially based",
            "          HALT      0   . should land here"
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank2(source, false);
        assert (absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loclNormal(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "",
            "$(1),START$*",
            "          LOCL      TARGET1",
            "          HALT      077 . should not get here",
            "",
            "TARGET1 .",
            "          LXM,U     X5,TARGET2",
            "          LOCL      0,X5",
            "          HALT      076 . or here",
            "",
            "TARGET2 . ",
            "          HALT      0"
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert (absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        assertEquals(255, processors._instructionProcessor.getBaseRegister(25)._upperLimitNormalized);
        assertEquals(256 - 4, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EX0).getW());
    }

    @Test
    public void rtnToCall(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "",
            "$(1),START$*",
            "          LA,U      A5,5",
            "          CALL      (LBDICALL$+TARGETSUB,TARGETSUB),,B2",
            "          LA,U      A7,7",
            "          HALT      0 . should stop here",
            "",
            "$(3),TARGETSUB* .",
            "          LA,U      A6,6",
            "          RTN       0,,B2",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank2(source, false);
        assert (absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        assertEquals(255, processors._instructionProcessor.getBaseRegister(25)._upperLimitNormalized);
        assertEquals(256, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EX0).getW());
        assertEquals(5, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(6, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(7, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
    }

    @Test
    public void rtnToLocl(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "",
            "$(1),START$*",
            "          LA,U      A5,5",
            "          LOCL      TARGETSUB",
            "          LA,U      A7,7",
            "          HALT      0 . should stop here",
            "",
            "TARGETSUB .",
            "          LA,U      A6,6",
            "          RTN       0",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert (absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        assertEquals(255, processors._instructionProcessor.getBaseRegister(25)._upperLimitNormalized);
        assertEquals(256, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.EX0).getW());
        assertEquals(5, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(6, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(7, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A7).getW());
    }

    @Test
    public void rtnNoFrame(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "",
            "$(1),START$*",
            "          RTN       0",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert (absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01013, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }
}
