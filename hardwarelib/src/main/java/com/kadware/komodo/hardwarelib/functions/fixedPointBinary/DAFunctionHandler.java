/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.OperationTrapInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DA instruction f=071 j=010
 */
@SuppressWarnings("Duplicates")
public class DAFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long[] operand1 = {
            ip.getExecOrUserARegister((int)iw.getA()).getW(),
            ip.getExecOrUserARegister((int)iw.getA() + 1).getW()
        };
        DoubleWord36 dwOperand1 = new DoubleWord36(operand1[0], operand1[1]);

        long[] operand2 = new long[2];
        ip.getConsecutiveOperands(true, operand2);
        DoubleWord36 dwOperand2 = new DoubleWord36(operand2[0], operand2[1]);

        DoubleWord36.AdditionResult ar = dwOperand1.add(dwOperand2);

        long[] result = {
            ar._value.get().shiftRight(36).longValue() & Word36.BIT_MASK,
            ar._value.get().longValue() & Word36.BIT_MASK
        };

        ip.getExecOrUserARegister((int) iw.getA()).setW(result[0]);
        ip.getExecOrUserARegister((int) iw.getA() + 1).setW(result[1]);

        ip.getDesignatorRegister().setCarry(ar._carry);
        ip.getDesignatorRegister().setOverflow(ar._overflow);
        if (ip.getDesignatorRegister().getOperationTrapEnabled() && ar._overflow) {
            throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DA; }
}
