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
import com.kadware.komodo.hardwarelib.ActiveBaseTableEntry;
import com.kadware.komodo.hardwarelib.BaseRegister;
import com.kadware.komodo.hardwarelib.DesignatorRegister;

/**
 * Handles the LBUD instruction f=075 j=07
 */
public class LBUDFunctionHandler extends InstructionHandler {

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

        int brIndex = (int) iw.getA();
        if ((brIndex < 1) || (brIndex > 11)) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidBaseRegister);
        }

        if (!dr.getBasicModeEnabled() && (brIndex == (int) iw.getB())) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidBaseRegister);
        }

        long[] data = new long[4];
        ip.getConsecutiveOperands(false, data);
        ip.setBaseRegister(brIndex, new BaseRegister(data));
        ip.loadActiveBaseTableEntry(brIndex + 1, new ActiveBaseTableEntry(0));
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBUD; }
}
