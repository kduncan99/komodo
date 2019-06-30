/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.test;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the MTLE / MTNG instruction extended mode f=071 j=02
 */
public class MTLEFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if ((U) AND R2) <= (A(a) AND R2)

        long aValue = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long uValue = ip.getOperand(true, true, false, false);
        long opMask = ip.getExecOrUserRRegister(2).getW();

        if (OnesComplement.compare36(uValue & opMask, aValue & opMask) <= 0) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.MTLE; }
}
