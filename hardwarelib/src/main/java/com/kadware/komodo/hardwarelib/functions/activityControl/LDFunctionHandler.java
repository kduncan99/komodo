/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.activityControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LD instruction f=073 j=015 a=014
 */
public class LDFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (ip.getDesignatorRegister().getProcessorPrivilege() > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long operand = ip.getOperand(false, true, false, false);
        DesignatorRegister dr = new DesignatorRegister(operand);
        ip.setDesignatorRegister(dr);
        if (dr.getBasicModeEnabled()) {
            ip.findBasicModeBank(ip.getProgramAddressRegister().getProgramCounter(), true);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.LD; }
}
