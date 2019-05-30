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
 * Handles the SBED instruction f=075 j=04
 */
public class SBEDFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
Extended Mode PP==0
Basic Mode PP==0

Description:
The SBED instruction stores the contents of one of the executive Base_Registers B16–B31,
specified by Ba+16, into the 4-word instruction operand starting at the instruction operand
address U. The instruction operand format is as described in 2.1.1.
Operation_Notes:
 See Section 8 for special addressing rules for the instruction operand.
 Unimplemented bits of Absolute_Address are written as 0.
         */
        long operand = ip.getOperand(false, false, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }
}
