/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.procedureControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.BankManipulator;

/**
 * Handles the GOTO instruction f=07 j=017 a=00
 */
public class GOTOFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getOperand(false, true, false, false);
        BankManipulator.bankManipulation(ip, Instruction.GOTO, operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.GOTO; }
}

