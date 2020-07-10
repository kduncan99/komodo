/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InterruptInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void erBasic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          ER        077",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01014, _instructionProcessor.getLatestStopDetail());
        MachineInterrupt macInt = _instructionProcessor.getLastInterrupt();
        assertEquals(0L, macInt.getShortStatusField());
        assertEquals(077L, macInt.getInterruptStatusWord0().getW());
    }

    @Test
    public void signalBasic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          SGNL      077",
            "          HALT      0",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01014, _instructionProcessor.getLatestStopDetail());
        MachineInterrupt macInt = _instructionProcessor.getLastInterrupt();
        assertEquals(01L, macInt.getShortStatusField());
        assertEquals(077L, macInt.getInterruptStatusWord0().getW());
    }

    @Test
    public void signalExtended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          SGNL      077",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01014, _instructionProcessor.getLatestStopDetail());
        MachineInterrupt macInt = _instructionProcessor.getLastInterrupt();
        assertEquals(01L, macInt.getShortStatusField());
        assertEquals(077L, macInt.getInterruptStatusWord0().getW());
    }

    @Test
    public void allowInterruptsAndJumpBasic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . clear deferrable interrupt enable (bit 13)",
            "          SD        A8",
            "          AND       A8,(0777757,0777777)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          AAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertTrue(_instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void allowInterruptsAndJumpExtended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . clear deferrable interrupt enable (bit 13)",
            "          SD        A8",
            "          AND       A8,(0777757,0777777)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          AAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertTrue(_instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void allowInterruptsAndJumpExtendedBadPP(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . clear deferrable interrupt enable (bit 13), set pp=3",
            "          SD        A7",
            "          AND       A7,(0777757,0777777)",
            "          OR        A8,(014,0)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          AAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(1, _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void preventInterruptsAndJumpBasic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . set deferrable interrupt enable (bit 13)",
            "          SD        A8",
            "          OR        A8,(020,0)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertFalse(_instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void preventInterruptsAndJumpBasicBadPP(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . set deferrable interrupt enable (bit 13), pp=3",
            "          SD        A8",
            "          OR        A8,(034,0)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(1, _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void preventInterruptsAndJumpExtended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . set deferrable interrupt enable (bit 13)",
            "          SD        A8",
            "          OR        A8,(020,0)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertFalse(_instructionProcessor.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void preventInterruptsAndJumpExtendedBadPP(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          . set deferrable interrupt enable (bit 13), pp=3",
            "          SD        A8",
            "          OR        A8,(034,0)",
            "          LD        A9",
            "",
            "          LXM,U     X4,1",
            "          PAIJ      TARGET,X4",
            "          HALT      077",
            "TARGET",
            "          HALT      076",
            "          HALT      0",
            };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(1, _instructionProcessor.getLastInterrupt().getShortStatusField());
    }
}
