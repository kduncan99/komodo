/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.test;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the MTE instruction extended mode f=071 j=00
 */
public class MTEFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if (U) AND R2 == A(a) AND R2
        long op1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long op2 = ip.getOperand(true, true, false, false);
        long opMask = ip.getExecOrUserRRegister(2).getW();

        if ((op1 & opMask) == (op2 & opMask)) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.MTE; }
}
