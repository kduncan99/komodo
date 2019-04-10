/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.generalLoad;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the DLN instruction f=071 j=015
 */
public class DLMFunctionHandler extends FunctionHandler {

    //  Mitigates object proliferation, but requires sync'd thread protection
    private final long operands[] = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        ip.getConsecutiveOperands(true, operands);
        if (OnesComplement.isNegative36(operands[0])) {
            operands[0] = (~operands[0]) & OnesComplement.BIT_MASK_36;
            operands[1] = (~operands[1]) & OnesComplement.BIT_MASK_36;
        }

        int grsIndex = ip.getExecOrUserARegisterIndex((int)iw.getA());
        ip.setGeneralRegister(grsIndex, operands[0]);
        ip.setGeneralRegister(grsIndex + 1, operands[1]);
    }
}
