/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.unconditionalJump;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.misc.ProgramAddressRegister;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the SLJ instruction basic mode f=072 j=001
 */
public class SLJFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Increment PAR.PC, and store it in U, then update PAR.PC to reference U+1
        ProgramAddressRegister par = ip.getProgramAddressRegister();
        int returnPC = par.getProgramCounter() + 1;
        ip.storePartialOperand(returnPC, InstructionWord.H2, true);
        int newPC = ip.getJumpOperand(true);
        ip.setProgramCounter(newPC + 1, true);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SLJ; }
}
