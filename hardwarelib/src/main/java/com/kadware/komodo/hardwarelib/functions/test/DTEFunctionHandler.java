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
 * Handles the DTE instruction f=071 j=017
 */
public class DTEFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if U,U+1 == A(a),A(a+1) - for this test, -0 is not equal to +0
        long[] uOperand = new long[2];
        long[] aOperand = new long[2];
        aOperand[0] = ip.getExecOrUserARegister((int) iw.getA()).getW();
        aOperand[1] = ip.getExecOrUserARegister((int) iw.getA() + 1).getW();
        ip.getConsecutiveOperands(true, uOperand);
        if ((uOperand[0] == aOperand[0]) && (uOperand[1] == aOperand[1])) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DTE; }
}
