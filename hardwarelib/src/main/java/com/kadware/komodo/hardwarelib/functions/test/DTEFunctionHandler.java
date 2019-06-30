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
 * Handles the DTE instruction f=071 j=017
 */
public class DTEFunctionHandler extends InstructionHandler {

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

    @Override
    public Instruction getInstruction() { return Instruction.DTE; }
}
