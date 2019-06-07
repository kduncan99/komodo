/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.conditionalJump;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the JNDF instruction f=074 j=015 a=03
 */
public class JNDFFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dreg = ip.getDesignatorRegister();
        if (!dreg.getDivideCheck()) {
            int counter = (int)ip.getJumpOperand();
            ip.setProgramCounter(counter, true);
        } else {
            dreg.setDivideCheck(false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.JNDF; }
}
