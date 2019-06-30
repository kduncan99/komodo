/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.OperationTrapInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the ANA instruction f=016
 */
@SuppressWarnings("Duplicates")
public class AMAFunctionHandler extends InstructionHandler {

    private final OnesComplement.Add36Result _ar = new OnesComplement.Add36Result();

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, true, true);
        if (OnesComplement.isNegative36(operand2)) {
            operand2 = OnesComplement.negate36(operand2);
        }

        OnesComplement.add36(operand1, operand2, _ar);

        ip.getExecOrUserARegister((int)iw.getA()).setW(_ar._sum);
        ip.getDesignatorRegister().setCarry(_ar._carry);
        ip.getDesignatorRegister().setOverflow(_ar._overflow);
        if (ip.getDesignatorRegister().getOperationTrapEnabled() && _ar._overflow) {
            throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.AMA; }
}
