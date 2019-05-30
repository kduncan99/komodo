/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.FunctionHandler;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the DABT instruction f=073 j=015 a=06
 */
public class DABTFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
Extended Mode PP<2

Description:
The DABT instruction writes the Active_Base_Table (ABT; see 2.5) into words 0–14 of the
instruction operand starting at the address U.
         */
        long operand = ip.getOperand(false, false, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }
}
