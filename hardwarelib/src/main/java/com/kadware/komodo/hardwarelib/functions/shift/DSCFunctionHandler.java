/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.shift;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DSC instruction f=073 j=001
 */
@SuppressWarnings("Duplicates")
public class DSCFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long[] operand = new long[2];
        operand[0] = ip.getExecOrUserARegister((int)iw.getA()).getW();
        operand[1] = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();

        int count = (int) ip.getImmediateOperand() & 0177;
        DoubleWord36 dw36 = new DoubleWord36(operand[0], operand[1]);
        DoubleWord36 result = dw36.rightShiftCircular(count);
        Word36[] components = result.getWords();

        ip.getExecOrUserARegister((int) iw.getA()).setW(components[0].getW());
        ip.getExecOrUserARegister((int) iw.getA() + 1).setW(components[1].getW());
    }

    @Override
    public Instruction getInstruction() { return Instruction.DSC; }
}
