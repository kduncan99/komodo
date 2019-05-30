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
 * Handles the SBU instruction f=075 j=02
 */
public class SBUFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
Extended Mode unrestricted
Basic Mode PP==0

Description:
The SBU instruction retrieves the name of the Bank, in the form of a Virtual_Address
(L,BDI,Offset), which currently resides in the user Base_Register specified by Ba.
The Bank_Name is stored at the instruction operand address U.
The Bank_Name is taken directly from the corresponding Active_Base_Table entry (ABT; see
2.5): U.L,BDI,Offset := ABT(F0.a).L,BDI,Offset when F0.a > 0. For F0.a = 0 (B0),
U.L,BDI := PAR.L,BDI and the U.Offset := 0
         */
        long operand = ip.getOperand(true, true, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }
}
