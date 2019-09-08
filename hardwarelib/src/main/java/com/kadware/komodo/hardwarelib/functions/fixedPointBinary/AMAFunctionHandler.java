/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
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

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, true, true);
        if (Word36.isNegative(operand2)) {
            operand2 = Word36.negate(operand2);
        }

        Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

        ip.getExecOrUserARegister((int)iw.getA()).setW(sar._value);
        ip.getDesignatorRegister().setCarry(sar._flags._carry);
        ip.getDesignatorRegister().setOverflow(sar._flags._overflow);
        if (ip.getDesignatorRegister().getOperationTrapEnabled() && sar._flags._overflow) {
            throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.AMA; }
}
