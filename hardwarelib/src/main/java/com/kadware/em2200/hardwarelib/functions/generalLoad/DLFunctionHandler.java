/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.generalLoad;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the DL instruction f=071 j=013
 */
public class DLFunctionHandler extends InstructionHandler {

    //  Mitigates object proliferation, but requires sync'd thread protection
    private final long operands[] = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        ip.getConsecutiveOperands(true, operands);

        int grsIndex = ip.getExecOrUserARegisterIndex((int)iw.getA());
        ip.setGeneralRegister(grsIndex, operands[0]);
        ip.setGeneralRegister(grsIndex + 1, operands[1]);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DL; }
}
