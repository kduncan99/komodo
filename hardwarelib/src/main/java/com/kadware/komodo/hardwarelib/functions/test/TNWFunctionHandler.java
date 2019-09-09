/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.test;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

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
        long aValueLow = ip.getExecOrUserARegister((int) iw.getA()).getW();
        long aValueHigh = ip.getExecOrUserARegister((int) iw.getA() + 1).getW();

        if ((Word36.compare(uValue, aValueLow) <= 0) || (Word36.compare(uValue, aValueHigh) > 0)) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.TNW; }
}
