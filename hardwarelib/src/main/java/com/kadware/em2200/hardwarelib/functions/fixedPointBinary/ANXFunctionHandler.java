/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.fixedPointBinary;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.OperationTrapInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the ANX instruction f=025
 */
@SuppressWarnings("Duplicates")
public class ANXFunctionHandler extends InstructionHandler {

    private final OnesComplement.Add36Result _ar = new OnesComplement.Add36Result();

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserXRegister((int)iw.getA()).getW();
        long operand2 = OnesComplement.negate36(ip.getOperand(true, true, true, true));

        OnesComplement.add36(operand1, operand2, _ar);

        ip.getExecOrUserXRegister((int)iw.getA()).setW(_ar._sum);
        ip.getDesignatorRegister().setCarry(_ar._carry);
        ip.getDesignatorRegister().setOverflow(_ar._overflow);
        if (ip.getDesignatorRegister().getOperationTrapEnabled() && _ar._overflow) {
            throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.ANX; }
}
