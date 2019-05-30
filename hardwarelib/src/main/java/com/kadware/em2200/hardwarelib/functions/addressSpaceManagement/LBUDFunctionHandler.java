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
 * Handles the LBUD instruction f=075 j=07
 */
public class LBUDFunctionHandler extends FunctionHandler {

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
The LBUD instruction loads one of the user Base_Registers B1–B15, specified by Ba, with the
4-word instruction operand starting at the instruction operand address U. This is a direct load,
with no Bank_Descriptor access.
The instruction operand format is as described in 2.1.1. See Section 8 for special addressing rules
for the instruction operand.
The LBUD instruction sets ABT(F0.a) := 0 to insure that a subsequent LBDI compare is always
unsuccessful.
Operation_Notes:
 An Invalid_Instruction interrupt is generated if F0.a = 0 (Ba designating Base_Register 0).
 In Version 3E models, an Invalid_Instruction interrupt is generated if F0.a (Ba) specifies the
same Basic_Mode Base_Register from which the LBUD is being executed.
 In Version 4E models, an Invalid_Instruction interrupt is generated if F0.a (Ba) specifies
Base_Registers B12-B15.
 In Version H models, Ba may not be loaded until a number of model_dependent instructions
beyond the LBUD have been executed.
 When the Base_Register is set void (B.V := 1), the contents of all other Base_Register fields
are Architecturally_Undefined.
Software_Notes:
 Software must ensure that only the number of Absolute_Address bits supported by the
specific model is utilized in the Base_Address fields since those are the only bits transferred
from the instruction operand to the Base_Register. Any other Base_Address bits that are set
are ignored by hardware.
 If an LBUD instruction is executed to establish an activity local stack, there is no ABT entry;
therefore, the LAE instruction cannot reestablish the activity local stack. While executing an
LBUD instruction to establish a Base_Register is not architecturally prohibited, it does not
make sense for an operational environment.
         */
        long operand = ip.getOperand(false, false, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }
}
