/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.procedureControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.FunctionHandler;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the RTN instruction f=073 j=017 a=03
 */
public class RTNFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
The RTN instruction is the complementary instruction to the CALL (see 6.15.2) and LOCL (see
6.15.3) instructions and is used to return to the environment stored on the most recently written
frame of the Return_Control_Stack (see 3.3.1). A RTN to Basic_Mode may load a void,
Basic_Mode, Extended_Mode or Gate (treated as Extended_Mode) Bank into one of B12–B15.
RTN to Extended_Mode may only load B0. For RTN, the F0.x, F0.h, F0.i, F0.b and F0.d are
Reserved_for_Software.
Algorithms describing RTN to Extended_Mode and RTN to Basic_Mode follow.

RTN to Extended_Mode (RCS DB16 = 0) Algorithm
1. A model_dependent check must be made for a possible RCS underflow as described in 4.6.4,
in the note prior to step 1.
2. A determination is made of the Base_Register information to be loaded into B0 as described
in 4.6.4, steps 3 through 9 (including any interrupts that may be generated); RCS.L,BDI is the
Source L,BDI.
3. The hard-held Access_Key := RCS.Access_Key and DB12–17 := RCS.DB12-17.
4. Hard-held PAR.L,BDI := RCS.L,BDI and hard-held PAR.PC := RCS.Offset.
5. The appropriate information (as determined in step 2 above) is loaded in B0.
6. If the BD.G = 1 or the RCS.Trap = 1, a Terminal_Addressing_Exception interrupt occurs. Note:
the environment stored on the Interrupt_Control_Stack reflects the environment after
steps 3 and 4 above. No check for Enter access is made on RTN.

RTN to Basic_Mode (RCS DB16 = 1, Mixed-Mode Transfer) Algorithm
1. A determination is made of the Base_Register information to be loaded as described in 4.6.4,
steps 3 through 9 (including any interrupts that may be generated); RCS.L,BDI is the Source
L,BDI.
2. Because this is a RTN to Basic_Mode (RCS.DB16 = 1), one of Base_Register 12–15 is to be
loaded, decided by RCS.B + 12. B0.V := 1 and hard-held PAR.L,BDI := 0,0, marking B0 as void.
3. Hard-held Access_Key := RCS.Access_Key and DB12–17 := RCS.DB12-17.
4. The ABT is updated as described in 4.6.4, step 18 and hard-held PAR.PC := RCS.Offset. Note:
ABT.Offset := 0.
5. The appropriate information (as determined in step 1 above) is loaded into the Base_Register
selected in step 2.
6. DB31 is toggled as described in 4.4.2.3.
7. If the BD.G = 1 or the RCS.Trap = 1, a Terminal_Addressing_Exception interrupt occurs. Note:
the selected Base_Register remains loaded with the BD information and that the
environment stored on the Interrupt_Control_Stack reflects the environment after steps 4–
6 above. No check for Enter Access, Validated Entry or Selection of Base_Register is made
on RTN.

Base_Register Manipulation Algorithm
See 4.6 for further clarification of addressing instructions, including a generic Base_Register
Manipulation algorithm (see 4.6.4) and a list of the restrictions placed on executive software on
the manipulation of addressing structures, allowing for model_dependent addressing instruction
acceleration schemes (see 4.6.5).

1 An RCS/Generic Stack Underflow or Overflow (Class 11) can occur.
2 BDR Selection is accessed on a transfer to Basic_Mode. Used to determine the primary pair for a possible DB31
toggle as described in 4.4.2.3.
3 System Control Designators—Replaced by DB12–17 of the RCS frame. In Version E models, DB15 is Set_to_Zero. In
Version E models, if DB14 is set DB17 is Set_to_Zero.
         */
    }
}
