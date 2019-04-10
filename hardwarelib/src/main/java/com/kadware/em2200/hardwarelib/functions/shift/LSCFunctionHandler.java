/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.shift;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the LSC instruction f=073 j=006
 */
public class LSCFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Retrieve (U). Shift left circularly until bit0 != bit1, then store the result into A(a),
        //  and the number of shifts done into A(a+1).
        //  If (U) already has bit0 != bit1, then store (U) in A(a) and 0 in A(a+1)
        //  If (U) is all zeros or all ones, store it in A(a) and 35 in A(a+1)
        long operand = ip.getOperand(true, true, false, false);
        int count = 0;
        if (OnesComplement.isZero36(operand)) {
            count = 35;
        } else {
            long bits = operand & 0_600000_000000l;
            while ((bits == 0) || (bits == 0_600000_000000l)) {
                operand = OnesComplement.leftShiftCircular36(operand, 1);
                ++count;
                bits = operand & 0_600000_000000l;
            }
        }

        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(count);
    }
}
