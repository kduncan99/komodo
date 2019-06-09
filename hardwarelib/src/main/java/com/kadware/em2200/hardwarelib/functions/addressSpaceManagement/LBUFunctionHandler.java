/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LBU instruction f=075 j=00
 */
public class LBUFunctionHandler extends InstructionHandler {

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
        if (brIndex < 2) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidBaseRegister);
        }

        loadBank(ip, brIndex);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBU; }
}
