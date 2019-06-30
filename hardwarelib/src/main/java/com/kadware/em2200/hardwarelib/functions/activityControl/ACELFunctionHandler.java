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

/**
 * Handles the ACEL instruction f=073 j=015 a=003
 */
public class ACELFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (ip.getDesignatorRegister().getProcessorPrivilege() > 2) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long[] ops = new long[48];
        ip.getConsecutiveOperands(false, ops);
        int opx = 0;

        for (int grsx = GeneralRegisterSet.X0; grsx <= GeneralRegisterSet.X11; ++grsx) {
            ip.setGeneralRegister(grsx, ops[opx++]);
        }

        for (int grsx = GeneralRegisterSet.A0; grsx <= GeneralRegisterSet.A15 + 4; ++grsx) {
            ip.setGeneralRegister(grsx, ops[opx++]);
        }

        for (int grsx = GeneralRegisterSet.R0; grsx <= GeneralRegisterSet.R15; ++grsx) {
            ip.setGeneralRegister(grsx, ops[opx++]);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.ACEL; }
}
