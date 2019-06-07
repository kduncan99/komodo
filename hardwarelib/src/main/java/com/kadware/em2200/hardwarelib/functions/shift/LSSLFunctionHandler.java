/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.shift;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the LSSL instruction f=073 j=012
 */
public class LSSLFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getExecOrUserARegister((int)iw.getA()).getW();
        int count = (int)ip.getImmediateOperand() & 0177;
        long result = OnesComplement.leftShiftLogical36(operand, count);

        ip.getExecOrUserARegister((int)iw.getA()).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LSSL; }
}
