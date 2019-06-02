/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.FunctionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.ActiveBaseTableEntry;
import com.kadware.em2200.hardwarelib.misc.BaseRegister;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LBUD instruction f=075 j=07
 */
public class LBUDFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getBasicModeEnabled() && (dr.getProcessorPrivilege() > 0)) {
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
}
