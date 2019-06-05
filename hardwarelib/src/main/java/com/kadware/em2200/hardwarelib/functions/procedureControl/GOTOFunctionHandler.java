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
 * Handles the GOTO instruction f=07 j=017 a=00
 */
public class GOTOFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
Description:
The GOTO instruction loads B0 (or one of B12–B15 on a transfer to Basic_Mode) to describe the
Bank specified by the L,BDI in bits 0–17 of the instruction operand at the address U and a jump is
taken in the new environment to the address in bits 18–35 of the instruction operand at the
address U. Regardless of the value of DB17, User X0bit 0 := DB16 (0 if previous mode was
Extended_Mode or 1 if previous mode was Basic_Mode), User X0bits 1-17 := 0, and User
X0bits 18-35 := previous Access_Key. Gate processing may occur, including writing Latent
Parameter values to R0 and R1 (User R0 and User R1 if DB17 = 0 or Executive R0 and Executive R1
if DB17 = 1), if Gate.LP0I = 0 and/or Gate.LP1I = 0, respectively.

Algorithms describing GOTO to an Extended_Mode Bank and GOTO to a Basic_Mode Bank follow.

GOTO to an Extended_Mode Bank Algorithm
1. A determination is made of the Base_Register information to be loaded into B0 as described
in 4.6.4, steps 3 through 9 (including any interrupt that may be generated). Gate processing
may occur.
2. DB16 and the Access_Key from the previous environment (the environment in which the
GOTO was executed) are copied into User X0 (regardless of the value of DB17) as follows:
USER X0
DB16
Zeros Access_Key
0 1 17 18 35
3. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.BD12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 do not cause a GRS violation regardless of the level of processor
privilege in effect.
4. Hard-held PAR.L,BDI is updated as described in 4.6.4, step 18 and PAR.PC := (U)bits 18–35.
The appropriate information (as determined in step 1 above) is loaded into B0.
6. If the Target BD.G = 1 or if Target BD.GAP.E = 0 and Target BD.SAP.E = 0 (Enter access is
denied) on a nongated transfer (see 4.6.4, step 21), a Terminal_Addressing_Exception interrupt
occurs. Note: the environment stored on the Interrupt_Control_Stack reflects the
environment after steps 3 and 4 above.

GOTO to a Basic_Mode Bank (Mixed-Mode Transfer) Algorithm
1. A determination is made of the Base_Register information to be loaded as described in 4.6.4,
steps 3 through 9 (including any interrupt that may be generated). Gate processing may
occur.
2. At this time it is detected that the Target BD.Type = Basic_Mode and that a mixed-mode
(Extended_Mode to Basic_Mode) transfer is to occur. B0.V := 1 and hard-held
PAR.L,BDI := 0,0, marking B0 as void. A determination is made of which of B12–B15 is to be
loaded. For a nongated GOTO, B12 is loaded. For a gated GOTO, Gate.B + 12 determine the
Base_Register number. The only way that a GOTO to Basic_Mode can load other than B12 is
through a Gate.
3. DB16 and the Access_Key from the previous environment (the environment in which the
GOTO was executed) are copied into User X0 (regardless of the value of DB17) as follows:
USER X0
DB16
Zeros Access_Key
0 1 17 18 35
4. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.BD12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
privilege in effect.
5. The ABT is updated as described in 4.6.4, step 18 and hard-held PAR.PC := (U)bits 18-35.
Note: ABT(Target B).Offset := 0.
6. The appropriate information (as determined in step 1 above) is loaded into the Base_Register
selected in step 2 above.
7. DB31 is toggled as described in 4.4.2.3.
8. If the Target BD.G = 1 or if Target BD.GAP.E = 0 and Target BD.SAP.E = 0 (Enter access is
denied) on a nongated transfer (see 4.6.4, step 21), a Terminal_Addressing_Exception interrupt
occurs. Note: the selected Base_Register remains loaded with the BD information and that
the environment stored on the Interrupt_Control_Stack reflects the environment after
steps 4–5 above.

Base_Register Manipulation Algorithm
See 4.6 for further clarification of addressing instructions, including a generic Base_Register
Manipulation algorithm (see 4.6.4) and a list of the restrictions placed on executive software on
the manipulation of addressing structures, allowing for model_dependent addressing instruction
acceleration schemes (see 4.6.5).

Operation Note: Because User X0 need not be backed up on a fault interrupt detected on
this instruction, User X0 should not be used as the instruction operand.
         */
    }
}
