/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.activityControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.BankManipulator;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LAE instruction f=073 j=015 a=012
 */
public class LAEFunctionHandler extends InstructionHandler {

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

        long[] operands = new long[15];
        ip.getConsecutiveOperands(false, operands);

        for (int opx = 0, brx = 1; opx < 15; ++opx, ++brx) {
            BankManipulator.bankManipulation(ip, Instruction.LAE, brx, operands[opx]);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.LAE; }
}
