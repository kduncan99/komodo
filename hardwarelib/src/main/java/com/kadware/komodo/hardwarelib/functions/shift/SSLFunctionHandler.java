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
 * Handles the SSL instruction f=073 j=002
 */
@SuppressWarnings("Duplicates")
public class SSLFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getExecOrUserARegister((int)iw.getA()).getW();
        int count = (int) ip.getImmediateOperand() & 0177;
        Word36 w36 = new Word36(operand);
        Word36 result = w36.rightShiftLogical(count);
        ip.getExecOrUserARegister((int) iw.getA()).setW(result.getW());
    }

    @Override
    public Instruction getInstruction() { return Instruction.SSL; }
}
