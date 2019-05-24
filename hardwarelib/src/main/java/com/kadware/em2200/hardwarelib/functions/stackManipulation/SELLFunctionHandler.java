/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.stackManipulation;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.functions.FunctionHandler;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;

/**
 * Handles the SELL instruction (f=073 j=014 a=03) extended mode only
 */
@SuppressWarnings("Duplicates")
public class SELLFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt {
        boolean longModifier = ip.getDesignatorRegister().getExecutive24BitIndexingEnabled();

        InstructionWord iWord = ip.getCurrentInstruction();
        IndexRegister xreg = ip.getExecOrUserXRegister((int)iWord.getX());
        BaseRegister breg = ip.getBaseRegister((int)iWord.getB());
        long oldModifier = longModifier ? xreg.getXM24() : xreg.getXM();

        if (breg._voidFlag
            || (oldModifier < breg._lowerLimitNormalized)
            || (oldModifier > breg._upperLimitNormalized)) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                (int) iWord.getB(),
                                                                (int) oldModifier);
        }

        long addend = iWord.getD() + (longModifier ? xreg.getXI12() : xreg.getXI());
        long newModifier = oldModifier + addend;
        if (longModifier) {
            xreg.setXM24(newModifier);
        } else {
            xreg.setXM(newModifier);
        }
    }
}
