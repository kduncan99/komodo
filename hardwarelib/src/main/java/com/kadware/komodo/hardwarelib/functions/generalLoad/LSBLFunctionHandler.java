/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalLoad;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the LSBL instruction f=061, extended mode only
 */
public class LSBLFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getOperand(true, true, true, true);
        ip.getExecOrUserXRegister((int)iw.getA()).setS2(operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LSBL; }
}