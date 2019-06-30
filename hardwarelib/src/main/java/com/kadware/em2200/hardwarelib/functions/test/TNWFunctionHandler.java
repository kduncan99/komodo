/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.test;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the TNW instruction f=057
 */
@SuppressWarnings("Duplicates")
public class TNWFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if (U) <= A(a) or (U) > A(a+1)

        long uValue = ip.getOperand(true, true, true, true);
        long aValueLow = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long aValueHigh = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();

        long nativeU = OnesComplement.getNative36(uValue);
        long nativeALow = OnesComplement.getNative36(aValueLow);
        long nativeAHigh = OnesComplement.getNative36(aValueHigh);

        if ((nativeU <= nativeALow) || (nativeU > nativeAHigh)) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.TNW; }
}
