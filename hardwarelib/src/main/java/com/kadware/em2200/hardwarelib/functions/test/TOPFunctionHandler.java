/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.test;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the TOP instruction f=045
 */
public class TOPFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  count the bits in the logical AND of A(a) and U.
        //  If there are an odd number of them, skip the next instruction.
        long op1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long op2 = ip.getOperand(true, true, true, true);
        int bitCount = Long.bitCount(op1 & op2);
        if ((bitCount & 0x01) != 0) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.TOP; }
}
