/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.activityControl;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the DCEL instruction f=073 j=015 a=004
 */
public class DCELFunctionHandler extends InstructionHandler {

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
        int opx = 0;

        for (int grsx = GeneralRegisterSet.X0; grsx <= GeneralRegisterSet.X11; ++grsx) {
            ops[opx++] = ip.getGeneralRegister(grsx).getW();
        }

        for (int grsx = GeneralRegisterSet.A0; grsx <= GeneralRegisterSet.A15 + 4; ++grsx) {
            ops[opx++] = ip.getGeneralRegister(grsx).getW();
        }

        for (int grsx = GeneralRegisterSet.R0; grsx <= GeneralRegisterSet.R15; ++grsx) {
            ops[opx++] = ip.getGeneralRegister(grsx).getW();
        }

        ip.storeConsecutiveOperands(false, ops);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DCEL; }
}
