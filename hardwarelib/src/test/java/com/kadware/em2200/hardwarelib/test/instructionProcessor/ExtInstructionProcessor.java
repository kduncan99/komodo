/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;

/**
 * Extended subclass of InstructionProcessor class, suitably instrumented for special testing
 */
public class ExtInstructionProcessor extends InstructionProcessor {

    public ExtInstructionProcessor(
        final String name,
        final short upi
    ) {
        super(name, upi);
    }

    @Override
    protected void executeInstruction(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        System.out.println(String.format("Executing Instruction at %012o --> %s",
                                         getProgramAddressRegister().getProgramCounter(),
                                         getCurrentInstruction().interpret(!getDesignatorRegister().getBasicModeEnabled(),
                                                                           getDesignatorRegister().getExecRegisterSetSelected())));
        super.executeInstruction();
    }
}
