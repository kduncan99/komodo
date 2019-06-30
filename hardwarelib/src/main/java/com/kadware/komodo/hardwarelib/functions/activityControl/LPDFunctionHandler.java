/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.activityControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LPD instruction f=07 j=014 a=not-used
 */
public class LPDFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getJumpOperand(false);
        DesignatorRegister dr = new DesignatorRegister(operand & 0157);
        ip.setDesignatorRegister(dr);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LPD; }
}
