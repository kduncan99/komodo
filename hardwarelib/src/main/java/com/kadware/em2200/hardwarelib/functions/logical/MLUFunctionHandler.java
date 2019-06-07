/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.logical;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the MLU instruction f=043
 */
public class MLUFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long opAa = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long opR2 = ip.getExecOrUserRRegister(2).getW();
        long compR2 = OnesComplement.negate36(opR2);
        long opU = ip.getOperand(true, true, true, true);
        long result = (opU & opR2) | (opAa & compR2);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.MLU; }
}
