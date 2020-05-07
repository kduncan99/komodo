/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.minalib.AbsoluteModule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_Interrupts extends BaseFunctions {

    @Test
    public void illegalOperation(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {

        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "$(1),START$*",
            "          +0 . illegal operation",
            "          HALT      07777",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
    }

    //  TODO make sure maskable interrupts are prevented by PAIJ

}
