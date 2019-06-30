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
import com.kadware.komodo.hardwarelib.misc.BaseRegister;
import com.kadware.komodo.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LBED instruction f=075 j=05
 */
public class LBEDFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getProcessorPrivilege() > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        int brIndex = (int) iw.getA() + 16;
        long[] data = new long[4];
        ip.getConsecutiveOperands(false, data);
        ip.setBaseRegister(brIndex, new BaseRegister(data));

        //  Clear any active base table entries which have a level value corresponding
        //  to the exec register being loaded (because that exec register points to the BDTable for that level).
        ActiveBaseTableEntry[] abtes = ip.getActiveBaseTableEntries();
        int level = (int) iw.getA();
        for (int abtx = 0; abtx < 15; ++abtx) {
            if (abtes[abtx].getLevel() == level) {
                ip.loadActiveBaseTableEntry(abtx + 1, new ActiveBaseTableEntry(0));
            }
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBED; }
}
