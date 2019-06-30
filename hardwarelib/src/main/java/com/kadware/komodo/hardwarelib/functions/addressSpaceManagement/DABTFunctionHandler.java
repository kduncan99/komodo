/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.addressSpaceManagement;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.ActiveBaseTableEntry;

/**
 * Handles the DABT instruction f=073 j=015 a=06
 */
public class DABTFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (ip.getDesignatorRegister().getProcessorPrivilege() > 1) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  Store all active base table entries to storage
        ActiveBaseTableEntry[] entries = ip.getActiveBaseTableEntries();
        long[] values = new long[15];
        for (int ax = 0; ax < 15; ++ax) {
            values[ax] = entries[ax].getW();
        }

        ip.storeConsecutiveOperands(false, values);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DABT; }
}
