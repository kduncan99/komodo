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
 * Handles the TVA instruction f=075 j=010
 */
public class TVAFunctionHandler extends InstructionHandler {

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
The LBN instruction determines the “true Bank_Name” (as described below) associated with the
L,BDI portion of the Virtual_Address contained in the instruction operand U. Xa bits 0–17 := true
Bank_Name and Xa bits 18–35 = 0. LBN then skips or reads the next instruction depending on the
BD.Type described by the true Bank_Name. The true Bank_Name is determined as follows:
 When the instruction operand L,BDI is in the range 0,0 to 0,31, that becomes the true
Bank_Name. The next instruction is then skipped.
 For all other values of instruction operand L,BDI, the L,BDI is used to access a
Bank_Descriptor, from which the true Bank_Name is derived. If the BD.Type = Indirect BD, or
BD.Type = Gate BD, the Indirect BD, or Gate BD is itself used directly to derive the true
Bank_Name. An Addressing_Exception interrupt occurs for a BD limits violation. The L
portion of the true Bank_Name is taken from the instruction operand L field. The BDI portion
of the true Bank_Name := BDI – BDT(BDI).BD.DISP (by subtracting the Displacement (DISP) of
the BD from the instruction operand BDI with DISP treated as unsigned binary). Instruction
results are Architecturally_Undefined if DISP > BDI. The next instruction is read if the
BD.Type = Basic_Mode, otherwise the next instruction is skipped.
See 4.6 for further clarification of addressing instructions, including a generic Base_Register
Manipulation algorithm (see 4.6.4) and a list of the restrictions placed on executive software on
the manipulation of addressing structures, allowing for model_dependent addressing instruction
acceleration schemes (see 4.6.5).
Architecturally_Undefined:
 The results are undefined if F0.a = F0.x and F0.h = 1.
 The results are undefined if DISP > BDI. This should not happen in a properly constructed BD;
see Section 11 for a discussion of the use of the DISP.
         */
        long operand = ip.getOperand(true, true, false, false);
        ip.getExecOrUserARegister((int)iw.getA()).setW(operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.TVA; }
}
