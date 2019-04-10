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
 * Handles the DTE instruction f=071 j=017
 */
public class DTEFunctionHandler extends FunctionHandler {

    private final long[] _operand1 = new long[2];
    private final long[] _operand2 = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if (U) > == A(a)
        _operand1[0] = ip.getExecOrUserARegister((int)iw.getA()).getW();
        _operand1[1] = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();
        ip.getConsecutiveOperands(true, _operand2);
        if (OnesComplement.isEqual72(_operand1, _operand2)) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }
}
