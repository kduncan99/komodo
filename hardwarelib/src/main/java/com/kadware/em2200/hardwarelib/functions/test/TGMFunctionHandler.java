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
 * Handles the TGM instruction extended mode f=033 j=013
 */
public class TGMFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if |(U)| > A(a)

        long uValue = ip.getOperand(true, true, true, false);
        long uNative = OnesComplement.getNative36(uValue);
        if (uNative < 0) {
            uNative = 0 - uNative;
        }

        long aValue = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long aNative = OnesComplement.getNative36(aValue);
        if (uNative > aNative) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }
}
