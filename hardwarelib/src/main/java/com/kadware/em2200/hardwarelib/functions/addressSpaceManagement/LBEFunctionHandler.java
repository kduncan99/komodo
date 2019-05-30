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
 * Handles the LBE instruction f=075 j=03
 */
public class LBEFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
Extended Mode PP==0
Basic Mode PP==0

The LBE instruction loads one of the executive Base_Registers B16–B31, specified by Ba+16, to
describe the Bank (and subset, if applicable) specified by (U), which is in the form of a
Virtual_Address (L,BDI,Offset).
The LBE instruction algorithm is described in the following steps.
1. A determination is made of the Base_Register information to be loaded into Ba+16 as
described in 4.6.4, steps 3 through 9.
Operation_Notes:
 When the Source L,BDI is 0,0, Ba+16 is set void (B.V := 1).
 When the Target L,BDI refers to either a Basic_Mode or Extended_Mode bank, the Target
BD is loaded.
 When the Target L,BDI refers to a Gate Bank, Gate processing does not occur. The Gate
Bank becomes the target BD and it is loaded.
2. Ba+16 is loaded, describing the Bank determined in step 1 above. Subsetting, if applicable,
occurs (see 4.6.6).
3. Unless Ba.V := 1, the Target BD.G is checked and, if Target BD.G = 1, the condition is reported
through a Terminal_Addressing_Exception interrupt. If the void resulted from the calculation
of a negative Upper_Limit while subsetting, it is Architecturally_Undefined whether these
checks are made. Note: Ba+16 remains loaded with the BD information.
See 4.6 for further clarification of addressing instructions, including a generic Base_Register
Manipulation algorithm (see 4.6.4) and a list of the restrictions placed on executive software on
the manipulation of addressing structures, allowing for model_dependent addressing instruction
acceleration schemes (see 4.6.5).
         */
        long operand = ip.getOperand(true, true, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }
}
