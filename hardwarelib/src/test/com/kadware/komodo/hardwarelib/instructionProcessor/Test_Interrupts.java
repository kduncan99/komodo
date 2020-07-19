/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 * Most instructions are instigated and tested for in other classes, but anything not there, should be here.
 */
public class Test_Interrupts extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    @Test
    public void illegalOperation(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "$(1),START",
            "          . Set up IH for interrupt class 016",
            "          LXI,U     A0,LBDI$",
            "          LXM,U     A0,ih",
            "          SA        A0,016,,B16",
            "",
            "          . Perform the illegal operation",
            "          +0 . illegal operation",
            "          HALT      07777 . should not get here",
            "",
            "ih        . Interrupt handler",
            "          HALT      01016 . should stop here",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    //  TODO make sure maskable interrupts are prevented by PAIJ
}
