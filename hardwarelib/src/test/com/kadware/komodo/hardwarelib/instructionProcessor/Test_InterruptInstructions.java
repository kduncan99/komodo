/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
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
    public void after() {
        clear();
    }

    @Test
    public void erBasic(
    ) throws Exception {
        String[] source = {
            "          ER        077",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01014, ip.getLatestStopDetail());
        MachineInterrupt macInt = ip.getLastInterrupt();
        assertEquals(0L, macInt.getShortStatusField());
        assertEquals(077L, macInt.getInterruptStatusWord0().getW());
    }

    @Test
    public void signalBasic(
    ) throws Exception {
        String[] source = {
            "          SGNL      077",
            "          HALT      0",
        };

        buildMultiBank(wrapForBasicMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01014, ip.getLatestStopDetail());
        MachineInterrupt macInt = ip.getLastInterrupt();
        assertEquals(01L, macInt.getShortStatusField());
        assertEquals(077L, macInt.getInterruptStatusWord0().getW());
    }

    @Test
    public void signalExtended(
    ) throws Exception {
        String[] source = {
            "          SGNL      077",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01014, ip.getLatestStopDetail());
        MachineInterrupt macInt = ip.getLastInterrupt();
        assertEquals(01L, macInt.getShortStatusField());
        assertEquals(077L, macInt.getInterruptStatusWord0().getW());
    }

    @Test
    public void allowInterruptsAndJumpBasic(
    ) throws Exception {
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

        buildMultiBank(wrapForBasicMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        assertTrue(ip.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void allowInterruptsAndJumpExtended(
    ) throws Exception {
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

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        assertTrue(ip.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void allowInterruptsAndJumpExtendedBadPP(
    ) throws Exception {
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(1, ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void preventInterruptsAndJumpBasic(
    ) throws Exception {
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
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        assertFalse(ip.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void preventInterruptsAndJumpBasicBadPP(
    ) throws Exception {
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
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(1, ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void preventInterruptsAndJumpExtended(
    ) throws Exception {
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        assertFalse(ip.getDesignatorRegister().getDeferrableInterruptEnabled());
    }

    @Test
    public void preventInterruptsAndJumpExtendedBadPP(
    ) throws Exception {
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(1, ip.getLastInterrupt().getShortStatusField());
    }
}
