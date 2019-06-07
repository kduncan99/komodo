/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.procedureControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the LBJ instruction f=07 j=017
 */
public class LBJFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        /*
The LBJ instruction loads a Bank into one of the four Basic_Mode Base_Registers, B12–B15 (or B0
on a transfer to Extended_Mode), and jumps to the address specified by U. It is a Basic_Mode
only instruction used to provide Basic_Mode procedure control and to provide an interface from
Basic_Mode to Extended_Mode. With the Interface Specification (Xa.IS), an LBJ can provide the
functionality of a CALL, GOTO or RTN instruction.
Basic_Mode procedures must retain all 36 bits of Xa to ensure compatibility.
The Xa register has the following format at execution time:
Xa E BDR
LS
IS BDI Reserved
0 1 2 3 4 5 6 17 18 35
Field Description
E Executive Level, along with the LS, translates to the level of a Bank as follows:
E,LS
0,0
0,1
1,0
1,1
L
4
6
2
0
Xa.E is ignored when LBJ,IS = 2. See 4.6.3.1 for more information on this translation.
BDR Bank_Descriptor_Register, BDR + 12 specifies which of B12–B15 is to be manipulated. Xa.BDR
is ignored when LBJ, IS = 2.
LS Level Specification—See E definition.
IS Interface Specification, used to provide Basic_Mode to Extended_Mode interfacing as
follows:
IS Meaning
0 Acts as LBJ/CALL, causing a mixed-mode transfer to Extended_Mode, for the
following cases:
• The Target BD.Type = Extended_Mode and Enter access is allowed.
• The Target BD.Type = Extended_Mode and a gate was encountered.
Otherwise, follows normal LBJ processing.
1 Acts as LBJ/GOTO, causing a mixed-mode transfer to Extended_Mode, for the
following cases:
• The Target BD.Type = Extended_Mode and Enter access is allowed.
• The Target BD.Type = Extended_Mode and a gate was encountered.
Otherwise, follows normal LBJ processing, except that GOTO_Inhibit is checked if a
Gate is encountered.
2 Acts as LBJ/RTN, regardless of the Target BD.Type.
3 Illegal, generates an Addressing_Exception interrupt.
BDI Bank_Descriptor_Index of Bank to be loaded. Xa.BDI is ignored when LBJ,IS = 2.

Xa may specify any X-Register except X0, in which case an Invalid_Instruction interrupt occurs. In
some cases, as described below, Xa is altered by LBJ. In addition, User X0 (regardless of the
value of DB17) may be written to contain certain environment states and R0 and/or R1 (user or
executive GRS, depending on the setting of DB17) may be written to contain Latent Parameters if
Gate processing occurs.
Five LBJ algorithms are described to outline the handling of certain situations. These algorithms
are:
1. LBJ,IS = 0 or LBJ,IS = 1 to a Basic_Mode Bank or a nongated Extended_Mode Bank without
Enter access (acts as normal LBJ)
2. LBJ,IS = 0 to an Extended_Mode Bank with Enter access or through a Gate (LBJ/CALL)
3. LBJ,IS = 1 to an Extended_Mode Bank with Enter access or through a Gate (LBJ/GOTO)
4. LBJ,IS = 2 with DB16 = 1 on RCS (LBJ/RTN to Basic_Mode)
5. LBJ,IS = 2 with DB16 = 0 on RCS (LBJ/RTN to Extended_Mode).
Note: LBJ,IS = 3 generates an Addressing_Exception interrupt.

LBJ,IS = 0 or 1 to a Basic_Mode Bank or a Nongated Extended_Mode Bank without
Enter Access (Acts as Normal LBJ) Algorithm
1. Prior L,BDI must be fetched from the ABT entry of the Base_Register determined by
Xa.BDR + 12 and retained as described in 4.6.4, step 2.
2. Source L,BDI is translated from Xa.E,LS,BDI as described in 4.6.3.1, then a determination is
made of the Base_Register information to be loaded as described in 4.6.4, steps 3 through 9
(including any interrupts that may be generated). Gate processing may occur.
3. Prior L,BDI from step 1 is translated to E,LS,BDI as described in 4.6.3.1 and, together with
PAR.PC+1 (points to the instruction following the LBJ) is written to Xa as follows:
Xa E BDR
LS
0 BDI PAR.PC + 1
0 1 2 3 4 5 6 17 18 35
BDR reflects the Base_Register that was loaded. Note: IS is overwritten.
4. DB16 and the Access_Key from the previous environment (the environment in which the LBJ
was executed) are copied into User X0 (regardless of the value of DB17) as follows:
USER
X0
DB16
Zeros Access_Key
0 1 18 17 35
5. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.BD12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key.DB16 := 1, indicating a transfer to
Basic_Mode.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
privilege in effect.
6. The ABT is updated as described in 4.6.4, step 18 and hard-help PAR.PC := (U)bits 18-35.
ABT.Offset := 0.
7. The appropriate information (as determined in step 2 above) is loaded into the
Base_Register(Xa.BDR + 12).
8. DB31 is toggled as described in 4.4.2.3.
9. If the Target BD.G = 1 or if a Validated Entry or Selection of Base_Register error occurs (see
4.6.4, step 21), a Terminal_Addressing_Exception interrupt occurs. Note: the selected
Base_Register remains loaded with the BD information and that the environment stored on
the Interrupt_Control_Stack reflects the environment after steps 6 and 7 above.

LBJ,IS = 0 to an Extended_Mode Bank with Enter Access or Through a Gate
(LBJ/CALL) Algorithm
1. A model_dependent check must be made for a possible RCS overflow as described in 4.6.4,
in the note prior to step 1.
2. Prior L,BDI must be fetched from the ABT entry of the Base_Register determined by
Xa.BDR + 12 and retained as described in 4.6.4, step 2.
3. Source L,BDI is translated from Xa.E,LS,BDI as described in 4.6.3.1, then a determination is
made of the Base_Register information to be loaded as described in 4.6.4, steps 3 through 9
(including any interrupts that may be generated). Gate processing may occur.
4. At this time it is detected that the Target BD.Type = Extended_Mode, either Gated or with
Enter access, and that a mixed-mode (Basic_Mode to Extended_Mode) transfer is to occur
and that B0 is to be loaded. The Base_Register (Xa.BDR + 12).V := 1 and its associated
ABT.L,BDI := 0,0, marking that Base_Register void. The ABT.Offset is
Architecturally_Undefined for void Base_Registers.
5. The RCS frame is written with the following information:
 RCS.Reentry_Point_Program_Address.L,BDI := Prior L,BDI (from the selected
Base_Register ABT entry, as described in step 2 above)
 RCS.Reentry_Point_Program_Address.Offset := PAR.PC + 1 (points to instruction
following LBJ/CALL)
 RCS.DB12-17 := current DB12–17 and RCS.Access_Key := current Access_Key
 RCS.B := Xa.BDR
 RCS.Must_Be_Zero := 0
6. DB16 and the Access_Key from the previous environment (the environment in which the
LBJ/CALL was executed) are copied into User X0 (regardless of the value of DB17) as follows:
USER X0
DB16
Zeros Access_Key
0 1 17 18 35
7. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key.DB16 := 1, indicating a transfer to
Extended_Mode.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
privilege in effect.
8. Hard-held PAR.L,BDI is updated as described in 4.6.4, step 18 and hard-held
PAR.PC := (U)bits 18-35.
9. The appropriate BD information (as determined in step 3 above) is loaded into B0.
10. If the Target BD.G = 1, a Terminal_Addressing_Exception interrupt occurs. Note: the
environment stored on the Interrupt_Control_Stack reflects the environment after steps 7
and 8 above.
Note: if nongated and Enter access is denied in the Target BD, processing follows the normal
LBJ algorithm performing the load of the Bank without the transfer to Extended_Mode (see
4.6.3.3).

LBJ,IS = 1 to an Extended_Mode Bank with Enter Access or Through a Gate
(LBJ/GOTO) Algorithm
1. Source L,BDI is translated from Xa.E,LS,BDI as described in 4.6.3.1, then a determination is
made of the Base_Register information to be loaded as described in 4.6.4, steps 3 through 9
(including any interrupts that may be generated). Gate processing may occur.
2. At this time it is detected that the Target BD.Type = Extended_Mode, either Gated or with
Enter access, and that a mixed-mode (Basic_Mode to Extended_Mode) transfer is to occur
and that B0 is to be loaded. The Base_Register(Xa.BDR + 12).V := 1 and its associated
ABT(Xa.BDR + 12).L,BDI := 0,0, marking that Base_Register void. The Offset is
Architecturally_Undefined for void Base_Registers.
3. DB16 and the Access_Key from the previous environment (the environment in which the
LBJ/GOTO was executed) are copied into User X0 (regardless of the value of DB17) as
follows:
USER X0
DB16
Zeros Access_Key
0 1 17 18 35
4. If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.BD12-15 and
DB17 := Gate.DB17 and/or if Gate.AKI = 0,
Indicator/Key_Register.Access_Key := Gate.Access_Key. DB16 := 1, indicating a transfer to
Extended_Mode.
If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
privilege in effect.
5. Hard-held PAR.L,BDI is updated as described in 4.6.4, step 18 and PAR.PC := (U)bits 18-35.
6. The appropriate information (as determined in step 1 above) is loaded into B0.
7. If the Target BD.G = 1, a Terminal_Addressing_Exception interrupt occurs. The environment
stored on the Interrupt_Control_Stack reflects the environment after steps 4 and 5 above.
Note: if nongated and Enter access is denied in the Target BD, processing follows the normal
LBJ algorithm performing the load of the Bank without the transfer to Extended_Mode (see
4.6.3.3).

LBJ,IS = 2 with DB16 = 1 on RCS (LBJ/RTN to Basic_Mode) Algorithm
1. A model_dependent check must be made for a possible RCS underflow as described in 4.6.4,
in the note prior to step 1.
2. A determination is made of the Base_Register information to be loaded as described in 4.6.4,
steps 3 through 9 (including any interrupts that may be generated); RCS.L,BDI is the Source
L,BDI.
3. Because this is a RTN to Basic_Mode (RCS.DB16 = 1), one of B12–B15 is to be loaded,
determined by RCS.B + 12.
4. The hard-held Access_Key := RCS.Access_Key and DB12–17 := RCS.DB12-17.
5. The ABT is updated as described in 4.6.4, step 18. ABT(RCS.B + 12).L,BDI := RCS.L,BDI.
Hard-held PAR.PC := RCS.Offset. ABT(RCS.B + 12).Offset := 0.
6. The appropriate information (as determined in step 2 above) is loaded into the Base_Register
selected in step 3.
7. DB31 is toggled as described in 4.4.2.3.
8. If the BD.G = 1 or the RCS.Trap = 1, a Terminal_Addressing_Exception interrupt occurs. Note:
the selected Base_Register remains loaded with the BD information and that the
environment stored on the Interrupt_Control_Stack reflects the environment after steps 4
and 5 above. No check for Enter Access, Validated Entry, or Selection of Base_Register is
made on LBJ/RTN to Basic_Mode.
Note: When Xa.IS = 2, Xa bits 0-3, 6-35 are ignored and Xa is not modified.

LBJ,IS = 2 with DB16 = 0 on RCS (LBJ/RTN to Extended_Mode) Algorithm
1. A model_dependent check must be made for a possible RCS underflow as described in 4.6.4,
in the note prior to step 1.
2. A determination is made of the Base_Register information to be loaded as described in 4.6.4,
steps 3 through 10 (including any interrupts that may be generated); RCS.L,BDI is the Source
L,BDI.
3. Because DB16 = 0 in the RCS, this is a mixed-mode (Basic_Mode to Extended_Mode) transfer
and B0 is to be loaded. B(RCS.B + 12).V := 1 and ABT(RCS.B + 12).L,BDI := 0,0, marking that
Base_Register void. The Offset is Architecturally_Undefined for void Base_Registers.
4. The hard-held Access_Key := RCS.Access_Key and the hard-held DB12–17 := RCS.DB12-17.
5. The hard-held PAR.L,BDI := RCS.L,BDI and hard-held PAR.PC := RCS.Offset.
6. The appropriate information (as determined in step 2 above) is loaded into B0.
7. If the BD.G = 1 or the RCS.Trap = 1, a Terminal_Addressing_Exception interrupt occurs. Note:
the environment stored on the Interrupt_Control_Stack reflects the environment after
steps 4 and 5 above. No check for Enter access is made on LBJ/RTN to Extended_Mode.
Note: When Xa.IS = 2, Xa bits 0-3, 6-35 are ignored and Xa is not modified.

Base_Register Manipulation Algorithm
See 4.6 for further clarification of addressing instructions, including a generic Base_Register
Manipulation algorithm (see 4.6.4) and a list of the restrictions placed on executive software on
the manipulation of addressing structures, allowing for model_dependent addressing instruction
acceleration schemes (see 4.6.5).
Operation_Note: When the next instruction is fetched, PAR.PC < 0200 is not tested;
instructions are always fetched from storage.
Architecturally_Undefined: The instruction F0.a and F0.x may specify the same X-Register;
however, in this case, the resultant value in Xx is undefined if index incrementation is specified
(F0.h = 1)

1 An RCS/Generic Stack Underflow or Overflow (Class 11) can occur.
2 Invalid_Instruction (Class 14) interrupt occurs if the F0.a = 0 (specifies X0).
3 Written to user X0 on LBJ, LBJ/GOTO, and LBJ/CALL.
4 DB17 from the Gate selects Executive or user R0/R1 if Gate processing occurs with Latent Parameters enabled.
5 DB12-17 := RCS.DB12-17 on a LBJ/RTN. In Version E models, DB15 is Set_to_Zero. In Version E models, if DB14 is set
DB17 is Set_to_Zero.
6 DB12-15 := Gate.DB12-15 and DB17 := Gate.DB17 if a Gate is processed on a LBJ. In Version E models, DB15 is
Set_to_Zero. In Version E models, if DB14 is set DB17 is Set_to_Zero.
7 DB16 := 0 on a transfer to Extended_Mode.
         */
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBJ; }
}
