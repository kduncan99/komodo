/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.test;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the TW instruction f=056
 */
public class TWFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if A(a) < (U) <= A(a+1)

        long uValue = ip.getOperand(true, true, true, true);
        long aValueLow = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long aValueHigh = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();

        long nativeU = OnesComplement.getNative36(uValue);
        long nativeALow = OnesComplement.getNative36(aValueLow);
        long nativeAHigh = OnesComplement.getNative36(aValueHigh);

        if ((nativeALow < nativeU) && (nativeU <= nativeAHigh)) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }
}
