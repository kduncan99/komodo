/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_Interrupts extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void illegalOperation(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {

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
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(01016, _instructionProcessor.getLatestStopDetail());
    }

    //  TODO make sure maskable interrupts are prevented by PAIJ

}
