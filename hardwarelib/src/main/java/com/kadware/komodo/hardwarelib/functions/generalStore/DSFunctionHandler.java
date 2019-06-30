/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalStore;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DS instruction f=071 j=012
 */
public class DSFunctionHandler extends InstructionHandler {

    //  Mitigates object proliferation, but requires sync'd thread protection
    private final long operands[] = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int grsIndex = ip.getExecOrUserARegisterIndex((int)iw.getA());
        operands[0] = ip.getGeneralRegister(grsIndex).getW();
        operands[1] = ip.getGeneralRegister(grsIndex + 1).getW();
        ip.storeConsecutiveOperands(true, operands);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DS; }
}
