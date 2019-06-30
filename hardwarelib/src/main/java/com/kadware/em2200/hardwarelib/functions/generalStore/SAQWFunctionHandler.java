/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.generalStore;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the SAQW instruction f=07 j=05
 * Operates much like SA,[q1-q4], except that the quarter-word designation is taken from bits 4-5 of X(x).
 * X-incrementation is not supported - results are undefined.
 */
public class SAQWFunctionHandler extends InstructionHandler {

    //  Array of j-field values indexed by the quarter-word designator where
    //  des==0 -> Q1, des==1 -> Q2, etc.
    private static final int J_FIELDS[] = { 7, 4, 6, 5 };

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int designator = (int)ip.getExecOrUserXRegister((int)iw.getX()).getS1() & 03;
        int jField = J_FIELDS[designator];
        long value = ip.getGeneralRegister(ip.getExecOrUserARegisterIndex((int)iw.getA())).getW();
        ip.storePartialOperand(value, jField, true);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SAQW; }
}
