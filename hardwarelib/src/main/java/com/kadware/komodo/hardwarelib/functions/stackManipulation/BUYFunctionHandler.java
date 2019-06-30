/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.stackManipulation;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.baselib.IndexRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import com.kadware.komodo.hardwarelib.misc.BaseRegister;

/**
 * Handles the BUY instruction (f=073 j=014 a=02) extended mode only
 */
@SuppressWarnings("Duplicates")
public class BUYFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt {
        boolean longModifier = ip.getDesignatorRegister().getExecutive24BitIndexingEnabled();
        InstructionWord iWord = ip.getCurrentInstruction();
        IndexRegister xreg = ip.getExecOrUserXRegister((int)iWord.getX());
        BaseRegister breg = ip.getBaseRegister((int)iWord.getB());

        long subtrahend = iWord.getD() + (longModifier ? xreg.getXI12() : xreg.getXI());
        long newModifier = (longModifier ? xreg.getXM24() : xreg.getXM()) - subtrahend;
        if (breg._voidFlag
            || (newModifier < breg._lowerLimitNormalized)
            || (newModifier > breg._upperLimitNormalized)) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                (int) iWord.getB(),
                                                                (int) newModifier);
        }

        if (longModifier) {
            xreg.setXM24(newModifier);
        } else {
            xreg.setXM(newModifier);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.BUY; }
}
