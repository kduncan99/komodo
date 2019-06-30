/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.OperationTrapInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the MSI instruction f=031
 */
public class MSIFunctionHandler extends InstructionHandler {

    private final long[] _product = { 0, 0 };

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, true, true);
        OnesComplement.multiply36(operand1, operand2, _product);

        ip.getExecOrUserARegister((int)iw.getA()).setW(_product[1]);

        //  check for overflow conditions.
        //  result[0] must be positive or negative zero, and the signs of result[0] and result[1] must match.
        if ((!OnesComplement.isZero36(_product[0]))
            || (OnesComplement.isNegative36(_product[0]) != OnesComplement.isNegative36(_product[1]))) {
            throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.MultiplySingleIntegerOverflow);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.MSI; }
}
