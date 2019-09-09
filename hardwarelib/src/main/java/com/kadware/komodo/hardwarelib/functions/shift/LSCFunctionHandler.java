/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.shift;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the LSC instruction f=073 j=006 (Load shift and count)
 * Left shift operand circularly unti bit 0 != bit1, then store it in Aa.
 * Store the shift count in A(a+1).
 */
@SuppressWarnings("Duplicates")
public class LSCFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getOperand(true, true, false, false);
        int count = 0;
        if (Word36.isZero(operand)) {
            count = 35;
        } else {
            long bits = operand & 0_600000_000000L;
            while ((bits == 0) || (bits == 0_600000_000000L)) {
                operand = Word36.leftShiftCircular(operand, 1);
                ++count;
                bits = operand & 0_600000_000000L;
            }
        }

        ip.getExecOrUserARegister((int) iw.getA()).setW(operand);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(count);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LSC; }
}
