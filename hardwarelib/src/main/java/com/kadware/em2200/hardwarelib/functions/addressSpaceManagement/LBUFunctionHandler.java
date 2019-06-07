/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the LBU instruction f=075 j=00
 */
public class LBUFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
Extended Mode unrestricted
Basic Mode PP==0

The LBU instruction loads one of the user Base_Registers B2–B15, specified by Ba, to describe
the Bank (and subset, if applicable) specified by (U), which is in the form of a Virtual_Address
(L,BDI,Offset).
The LBU instruction algorithm is described in the following steps.
1. If Ba specifies B0 or B1, an Invalid_Instruction interrupt occurs. If Ba specifies the same
Basic_Mode Base_Register from which the LBU is being executed, see 4.4.6.3 (Basic_Mode
Base_Register Selection for Instruction Fetches).
2. A determination is made of the Base_Register information to be loaded into Ba as described
in 4.6.4, steps 3 through 9.
Operation_Notes:
 When the Source L,BDI is 0,0, Ba is set void (B.V := 1), regardless of which Base_Register
is designated by Ba.
 When the Source L,BDI is in the range 0,1 to 0,31, an Addressing_Exception interrupt
occurs.
 When operating at a PP < 2 and Ba designates one of B2 through B15 and the Target L,BDI
refers to either a Basic_Mode, Extended_Mode, or Queue Bank, the Target BD is loaded.
 When operating at a PP > 1 and Ba designates one of B2 through B15 and the Target L,BDI
refers to a Basic_Mode BD with either (or both) GAP.E = 1 or SAP.E = 1, the Target BD is
loaded.
 When operating at a PP > 1 and the Target L,BDI refers to a Basic_Mode BD with both
GAP.E = 0 and SAP.E = 0, Ba is set void.
 When the Target L,BDI refers to a Gate Bank, Gate processing does not occur. The Gate
Bank becomes the Target BD and it is loaded.
3. The ABT is updated as described in 4.6.4, step 18.
4. Ba is loaded, describing the Bank determined in step 2 above. Subsetting, if applicable,
occurs (see 4.6.6).
5. Unless Ba.V := 1, the Target BD.G is checked and, if Target BD.G = 1, the condition is reported
through a Terminal_Addressing_Exception interrupt. If the void resulted from a calculation of
a negative Upper_Limit while subsetting, it is Architecturally_Undefined whether these checks
are made. Note: that Ba remains loaded with the BD information.
         */
        long operand = ip.getOperand(true, true, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBU; }
}
