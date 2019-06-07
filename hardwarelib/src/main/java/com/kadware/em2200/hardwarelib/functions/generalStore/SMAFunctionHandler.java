/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.generalStore;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the SMA instruction f=003
 */
public class SMAFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long op = ip.getExecOrUserARegister((int)iw.getA()).getW();
        if (OnesComplement.isNegative36(op)) {
            op = OnesComplement.negate36(op);
        }
        ip.storeOperand(true, true, true, true, op);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SMA; }
}
