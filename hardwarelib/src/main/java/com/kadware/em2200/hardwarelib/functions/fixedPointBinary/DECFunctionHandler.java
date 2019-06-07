/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.fixedPointBinary;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.OperationTrapInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the DEC instruction f=005, a=011
 */
@SuppressWarnings("Duplicates")
public class DECFunctionHandler extends InstructionHandler {

    private static final long NEGATIVE_ONE_36 = 0_777777_777776l;

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        boolean twosComplement = chooseTwosComplementBasedOnJField(iw, ip.getDesignatorRegister());
        boolean skip = ip.incrementOperand(true, true, NEGATIVE_ONE_36, twosComplement);

        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getOperationTrapEnabled() && dr.getOverflow()) {
            throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
        }

        if (skip) {
            ip.skipNextInstruction();
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DEC; }
}
