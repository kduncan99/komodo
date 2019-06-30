/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.shift;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the SSC instruction f=073 j=000
 */
public class SSCFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getExecOrUserARegister((int)iw.getA()).getW();
        int count = (int)ip.getImmediateOperand() & 0177;
        long result = OnesComplement.rightShiftCircular36(operand, count);

        ip.getExecOrUserARegister((int)iw.getA()).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SSC; }
}
