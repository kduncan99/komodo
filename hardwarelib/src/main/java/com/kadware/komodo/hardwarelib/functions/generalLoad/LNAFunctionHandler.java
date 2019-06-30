/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalLoad;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the LNA instruction f=011
 */
public class LNAFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  we can just *not* the operand, because the following setW() will truncate the errant bits outside of the 36-bit word.
        long operand = ~(ip.getOperand(true, true, true, true));
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LNA; }
}
