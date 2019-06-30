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
 * Handles the MATL instruction extended mode f=071 j=06
 */
public class MATLFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if ((U) AND R2) <= (A(a) AND R2) - unsigned

        long uValue = ip.getOperand(true, true, false, false);
        long aValue = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long opMask = ip.getExecOrUserRRegister(2).getW();

        if ((uValue & opMask) <= (aValue & opMask)) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.MATL; }
}
