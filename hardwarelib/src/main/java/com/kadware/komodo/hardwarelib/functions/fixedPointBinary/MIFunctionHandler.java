/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the MI instruction f=030
 */
public class MIFunctionHandler extends InstructionHandler {

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

        ip.getExecOrUserARegister((int)iw.getA()).setW(_product[0]);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(_product[1]);
    }

    @Override
    public Instruction getInstruction() { return Instruction.MI; }
}
