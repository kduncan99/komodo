/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalLoad;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DLN instruction f=071 j=014
 */
public class DLNFunctionHandler extends InstructionHandler {

    //  Mitigates object proliferation, but requires sync'd thread protection
    private final long operands[] = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        ip.getConsecutiveOperands(true, operands);
        operands[0] = (~operands[0]) & OnesComplement.BIT_MASK_36;
        operands[1] = (~operands[1]) & OnesComplement.BIT_MASK_36;

        int grsIndex = ip.getExecOrUserARegisterIndex((int)iw.getA());
        ip.setGeneralRegister(grsIndex, operands[0]);
        ip.setGeneralRegister(grsIndex + 1, operands[1]);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DLN; }
}
