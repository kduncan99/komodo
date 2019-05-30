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
 * Handles the LBED instruction f=075 j=05
 */
public class LBEDFunctionHandler extends FunctionHandler {

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
The LBED instruction loads one of the executive Base_Registers B16–B31, specified by Ba+16,
with the 4-word instruction operand starting at the instruction operand address U. This is a direct
load, with no Bank_Descriptor access. A Version H model_dependent undetected Base_Register
conflict may occur on Ba+16, that is, Ba+16 may not actually be loaded until a number of
instructions beyond the LBED have been executed. The instruction operand format is as
described in 2.1.1.
When LBED loads a BDTP (one of B16–23), accelerated BD information for the Level being loaded
is invalidated by the following actions:
1. Hardware is required to invalidate any BD acceleration information for BDs at the Level being
loaded (see 4.6.5).
2. User Base_Registers and their ABT entries are voided if the BD currently loaded in the
Base_Register matches the Level being loaded (that is, if ABT.Level = LBED F0.a). The ABT
entry is voided by writing ABT(F0.a).L,BDI := 0,0; the contents of the ABT(F0.a).Offset are
Architecturally_Undefined. This operation is required for B2–B15; it is model_dependent
whether B1 is included.
Operation_Notes:
 Software must ensure that only the number of Absolute_Address bits supported by the
specific model is utilized in the Base_Address fields since those are the only bits transferred
from the instruction operand to the Base_Register. Any other Base_Address bits that are set
are ignored by hardware.
 See Section 8 for special addressing rules for the instruction operand.
 When the Base_Register is set void (B.V := 1), the contents of all other Base_Register fields
are Architecturally_Undefined.
         */
        long operand = ip.getOperand(false, false, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }
}
