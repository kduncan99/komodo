/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.InventoryManager;
import com.kadware.em2200.hardwarelib.exceptions.MaxNodesException;
import com.kadware.em2200.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.em2200.hardwarelib.exceptions.UPIConflictException;
import com.kadware.em2200.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;
import com.kadware.em2200.hardwarelib.misc.ProgramAddressRegister;
import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_MiscTests extends Test_InstructionProcessor {

    @Test
    public void illegalOperation(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {

        String[] source = {
            "          $EXTEND",
            "$(1),START*",
            "          LA,U      A5,1",
            "          +0 . illegal operation",
            "          LA,U      A5,2",
            "          HALT      07777",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);
        showDebugInfo(ip, msp);

        InstructionProcessor.StopReason reason = ip.getLatestStopReason();
        long detail = ip.getLatestStopDetail();
        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, reason);
        assertEquals(01016, detail);
    }
}
