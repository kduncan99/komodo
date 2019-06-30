/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.addressSpaceManagement;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.BankManipulator;
import com.kadware.komodo.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the LBE instruction f=075 j=03
 */
public class LBEFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if ( dr.getProcessorPrivilege() > 0 ) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long operand = ip.getOperand(true, true, false, false);
        BankManipulator.bankManipulation(ip, Instruction.LBE, operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBE; }
}
