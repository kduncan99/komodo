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
 * Handles the JNFU instruction f=074 j=015 a=01
 */
public class JNFUFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dreg = ip.getDesignatorRegister();
        if (!dreg.getCharacteristicUnderflow()) {
            int counter = (int)ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }
        dreg.setCharacteristicUnderflow(false);
    }

    @Override
    public Instruction getInstruction() { return Instruction.JNFU; }
}
