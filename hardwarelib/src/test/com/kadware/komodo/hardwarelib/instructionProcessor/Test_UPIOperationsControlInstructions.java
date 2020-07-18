/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_UPIOperationsControlInstructions extends BaseFunctions {

//    private static String[] CHANNEL_PROGRAM_DEFINITIONS = {
//        "CHP_IOPUPI   $EQUF 0,,S1",
//        "CHP_CHMOD    $EQUF 0,,S2",
//        "CHP_DEVNUM   $EQUF 0,,S3",
//        "CHP_FUNCTION $EQUF 0,,S4",
//        "CHP_FORMAT   $EQUF 0,,S5",
//        "CHP_ACWS     $EQUF 0,,S6",
//        "CHP_BLKADDR  $EQUF 1,,W",
//        "CHP_CHANSTAT $EQUF 2,,S1",
//        "CHP_DEVSTAT  $EQUF 2,,S2",
//        "CHP_RESBYTES $EQUF 2,,S3",
//        "CHP_WORDS    $EQUF 3,,H2",
//        "CHP_ACWS     $EQUF 4",
//    };

    //  ----------------------------------------------------------------------------------------------------------------------------

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void send_to_ourself(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'CHP$DEFS'",
            "",
            "IP_UPI    $EQU      7 . hard-coded in InventoryManager.java",
            "",
            "CODE",
            "          SEND      IP_UPI . should NOT cause an interrupt since we're not stopped",
            "          HALT      0 . should stop here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }
}
