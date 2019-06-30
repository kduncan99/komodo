/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.unconditionalJump;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.misc.ProgramAddressRegister;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

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
