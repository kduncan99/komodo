/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.DesignatorRegister;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the JDF instruction f=074 j=014 a=03
 */
public class JDFFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dreg = ip.getDesignatorRegister();
        if (dreg.getDivideCheck()) {
            int counter = (int)ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
            dreg.setDivideCheck(false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.JDF; }
}
