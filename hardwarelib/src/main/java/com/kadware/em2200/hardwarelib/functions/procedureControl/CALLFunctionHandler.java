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
 * Handles the CALL instruction f=07 j=016 a=013
 */
public class CALLFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
The CALL instruction loads B0 (or one of B12–B15 on a transfer to Basic_Mode) to describe the
Bank specified by the L,BDI in bits 0–17 of the instruction operand at the address U and a jump is
taken in the new environment to the address in bits 18–35 of the instruction operand at the
address U. Essential activity environment is captured on the Return_Control_Stack (see 3.3.1)
such that a subsequent return to the original environment may be accomplished by a RTN
instruction (see 6.15.4). In addition, certain activity environment is written to User X0 (regardless
of the value of DB17). Gate processing may occur, including writing latent parameters to R0 and
R1 (user or executive GRS, depending on the ultimate setting of DB17), if enabled in the Gate.
Algorithms describing CALL to an Extended_Mode Bank and CALL to a Basic_Mode Bank follow.

CALL to an Extended_Mode Bank Algorithm
1. A model_dependent check must be made for a possible RCS overflow as described in 4.6.4,
in the note prior to step 1.
2. Prior L,BDI must be fetched from the hard-held PAR.L,BDI and retained as described in 4.6.4,
step 2.
3. A determination is made of the Base_Register information to be loaded into B0 as described
in 4.6.4, steps 3 through 9 (including any interrupts that may be generated). Gate processing
may occur.
4. The RCS frame is written with the following information:
 RCS.Reentry_Point_Program_Address.L,BDI := Prior PAR.L,BDI
 RCS.Reentry_Point_Program_Address.Offset := PAR.PC + 1 (points to instruction
following CALL)
 RCS.DB12-17 := current DB12–17 and RCS.Access_Key := current Access_Key
 RCS.B := 0 and RCS.Must_Be_Zero := 0
5. DB16 and the Access_Key from the previous environment (the environment in which the
CALL was executed) are copied into User X0 (regardless of the value of DB17) as follows:
USER X0
DB16
Zeros Access_Key
0 1 17 18 35
6. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
privilege in effect.
7. Hard-held PAR.L,BDI is updated as described in 4.6.4, step 18 and PAR.PC := (U)bits 18–35.
8. The appropriate information (as determined in step 3 above) is loaded into B0.
9. If the Target BD.G = 1 or if Target BD.GAP.E = 0 and Target BD.SAP.E = 0 (Enter access is
denied) on a nongated transfer (see 4.6.4, step 21), a Terminal_Addressing_Exception interrupt
occurs. Note: the environment stored on the Interrupt_Control_Stack reflects the
environment after steps 6 and 7 above.

CALL to a Basic_Mode Bank (Mixed-Mode Transfer) Algorithm
1. A model_dependent check must be made for a possible RCS overflow as described in 4.6.4,
in the note prior to step 1.
2. Prior L,BDI must be fetched from the hard-held L,BDI and retained as described in 4.6.4, step
2.
3. A determination is made of the Base_Register information to be loaded as described in 4.6.4,
steps 3 through 9 (including any interrupts that may be generated). Gate processing may
occur.
4. At this time it is detected that the Target BD.Type = Basic_Mode and that a mixed-mode
(Extended_Mode to Basic_Mode) transfer is to occur. B0.V := 1 and hard-held
PAR.L,BDI := 0,0, marking B0 as void. A determination is made of which of B12–B15 is to be
loaded. For a nongated CALL, B12 is loaded. For a gated CALL, Gate.B + 12 determine the
Base_Register number. The only way that a CALL to Basic_Mode can load other than B12 is
through a Gate.
5. The RCS frame is written with the following information:
 RCS.Reentry_Point_Program_Address.L,BDI := Prior PAR.L,BDI
 RCS.Reentry_Point_Program_Address.Offset := PAR.PC + 1 (points to instruction
following CALL)
 RCS.DB12-17 := current DB12–17 and RCS.Access_Key := current Access_Key
 RCS.B := Gate.B or, if no Gate was processed, RCS.B := 0.
 RCS.Must_Be_Zero := 0.
6. If DB17 = 0, User X11bits 4-5 := 2 or if DB17 = 1 Executive X11bits 4-5 := 2, the remaining bits
are Architecturally_Undefined. X11 is then ready to use as the Xa of an LBJ instruction (see
6.16.1). With Xa.IS = 2, the LBJ acts as a RTN.
7. DB16 and the Access_Key from the previous environment (the environment in which the
CALL was executed) are copied into User X0 (regardless of the value of DB17) as follows:
USER X0
DB16
Zeros Access_Key
0 1 17 18 35
8. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key.DB16 := 1, indicating a transfer to
Basic_Mode.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
privilege in effect.
9. The ABT is updated as described in 4.6.4, step 18 and hard-held PAR.PC := (U)bits 18-35.
Note: ABT(Target B).Offset := 0.
10. The appropriate information (as determined in step 3 above) is loaded into the Base_Register
selected in step 4 above.
11. DB31 is toggled as described in 4.4.2.3.
12. If the Target BD.G = 1 or if Target BD.GAP.E = 0 and Target BD.SAP.E = 0 (Enter access is
denied) on a nongated transfer (see 4.6.4, step 21), a Terminal_Addressing_Exception interrupt
occurs. Note: the selected Base_Register remains loaded with the BD information and that
the environment stored on the Interrupt_Control_Stack reflects the environment after
steps 8–11 above.

Base_Register Manipulation Algorithm
See 4.6 for further clarification of addressing instructions, including a generic Base_Register
Manipulation algorithm (see 4.6.4) and a list of the restrictions placed on executive software on
the manipulation of addressing structures, allowing for model_dependent addressing instruction
acceleration schemes (see 4.6.5).
Operation_Note: Because User X0 need not be backed up on a fault interrupt detected on this
instruction, User X0 should not be used as the instruction operand.

1 An RCS/Generic Stack Underflow or Overflow (Class 11) can occur.
2 User X0bit 0 := DB16.
3 DB17 of the Gate selects Executive or User R0/R1 if Gate processing occurs with Latent Parameters enabled.
4 BDR Selection is accessed on a transfer to Basic_Mode.
5 System Control Designators—Replaced by DB12–15, and 17 of a Gate if a Gate is processed. In Version E models,
DB15 is Set_to_Zero. In Version E models, if DB14 is set DB17 is Set_to_Zero.
6 Set to one on a transfer to Basic_Mode.
         */
    }
}
