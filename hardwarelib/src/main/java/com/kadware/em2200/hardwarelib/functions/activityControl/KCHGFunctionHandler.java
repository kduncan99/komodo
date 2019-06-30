/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.activityControl;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the KCHG instruction f=037 j=04 a=04
 */
public class KCHGFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getProcessorPrivilege() > 1) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long operand = ip.getOperand(false, true, false, false);
        ip.setGeneralRegister(GeneralRegisterSet.X0, ip.getIndicatorKeyRegister().getAccessKey());
        ip.getIndicatorKeyRegister().setAccessKey((int) (operand >> 18));
        dr.setQuantumTimerEnabled((operand & 040) != 0);
        dr.setDeferrableInterruptEnabled((operand & 020) != 0);
        dr.setExecRegisterSetSelected((operand & 01) != 0);
    }

    @Override
    public Instruction getInstruction() { return Instruction.KCHG; }
}
