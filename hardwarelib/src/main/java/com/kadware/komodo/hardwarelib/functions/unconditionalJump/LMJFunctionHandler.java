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
 * Handles the LMJ instruction basic mode f=074 j=013
 */
public class LMJFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Increment PAR.PC and store it in X(a)Modifier, then set PAR.PC to U
        ProgramAddressRegister par = ip.getProgramAddressRegister();
        ip.getExecOrUserXRegister((int)iw.getA()).setH2(par.getProgramCounter() + 1);
        ip.setProgramCounter(ip.getJumpOperand(true), true);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LMJ; }
}
