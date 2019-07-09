/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.minalib.AbsoluteModule;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InterruptInstructions extends BaseFunctions {

    @Test
    public void erBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          ER        077",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
        showDebugInfo(processors);//TODO

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01014, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0L, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void signalBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          SGNL      077",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01014, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01L, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void signalExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          SGNL      077",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01014, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01L, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void allowInterruptsAndJumpBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          AAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertTrue(processors._instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void allowInterruptsAndJumpExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          AAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertTrue(processors._instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void allowInterruptsAndJumpExtendedBadPP(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          AAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(1, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void preventInterruptsAndJumpBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void preventInterruptsAndJumpBasicBadPP(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(1, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void preventInterruptsAndJumpExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void preventInterruptsAndJumpExtendedBadPP(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(1, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }
}
