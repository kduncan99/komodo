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
