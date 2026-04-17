/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.functions.Function;

/**
 * Load Bank and Jump function
 * (LBJ) Loads a bank (selected from B12:B15 for Basic Mode, B0 for Extended Mode)
 * and then jumps to the address in the U field.
 */

/*
 TODO REMOVE THESE SPECIAL NOTES LATER
LBJ,IS = 0 or 1 to a Basic_Mode Bank or a Nongated Extended_Mode Bank
without Enter Access (Acts as Normal LBJ) Algorithm (4.6.4) Summary:
2:   Prior L,BDI must be fetched from the ABT entry of the Base_Register determined by Xa.BDR + 12 and retained
3-9: Source L,BDI is translated from Xa.E,LS,BDI as described in 4.6.3.1, then a
        determination is made of the Base_Register information to be loaded (including any
        interrupts that may be generated). Gate processing may occur.
10:  Base_Register to be loaded is specified by Xa.BDR (BDR+12).
13:  Prior L,BDI from step 2 is translated to E,LS,BDI as described in 4.6.3.1 and, together
        with PAR.PC+1 (points to the instruction following the LBJ) is written to Xa as follows:
            0:     E
            1-2:   BDR
            3:     LS
            4-5:   0
            6-17:  BDI
            18-35: PAR_PC+1
14:  DB16 and the Access_Key from the previous environment (the environment in which
        the CALL was executed) are copied into User X0 (regardless of the value of DB17) as follows:
            0:     DB16
            1-17:  Zeros
            18-35: Access_Key
15:  If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15
        and DB17 := Gate.DB17 and/or if Gate.AKI = 0,
        Indicator/Key_Register.Access_Key := Gate.Access_Key.DB16 := 1, indicating a transfer
        to Basic_Mode.
        If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
        R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent
        Parameter 0 Value; and/or if LP1I = 0, then if either DB17 = 0, User
        R1 := Gate.Latent_Parameter_1 Value or DB17 = 1, Executive
        R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter into Executive
        R0/R1 does not cause a GRS violation regardless of the level of processor privilege
        in effect.
17:  PAR.PC := (U)bits 18-35.
18:  The ABT is updated. Note: ABT(Target B).Offset := 0.
20:  Basic_Mode DB31 toggle and Reference_Violation detection (see 4.4.6.1).
21:  If the Target BD.G = 1 or if a Validated Entry or Selection of Base_Register error occurs
        a Terminal_Addressing_Exception interrupt occurs.

LBJ,IS = 0 to an Extended_Mode Bank with Enter Access or Through a Gate
(LBJ/CALL) (4.6.4) Summary:
    A model_dependent check must be made for a possible RCS overflow as described in 4.6.4.1
2:   Prior L,BDI must be fetched from the ABT entry of the Base_Register determined by
        Xa.BDR + 12 and retained.
3-9: Source L,BDI is translated from Xa.E,LS,BDI as described in 4.6.3.1, then a
        determination is made of the Base_Register information to be loaded (including any
        interrupts that may be generated). Gate processing may occur.
10:  At this time it is detected that the Target BD.Type = Extended_Mode, either Gated or
        with Enter access, and that a mixed-mode (Basic_Mode to Extended_Mode) transfer is
        to occur and that B0 is to be loaded.
11:  The Base_Register (Xa.BDR + 12).V := 1 and its associated ABT.L,BDI := 0,0, marking
        that Base_Register void. The ABT.Offset is Architecturally_Undefined for void
        Base_Registers.
12:  The RCS frame is written with the following information:
        • RCS.Reentry_Point_Program_Address.L,BDI := Prior L,BDI (from the selected
            Base_Register ABT entry, as described in step 2 above)
        • RCS.Reentry_Point_Program_Address.Offset := PAR.PC + 1 (points to
            instruction following LBJ/CALL)
        • RCS.DB12-17 := current DB12–17.
        • RCS.Access_Key := current Access_Key.
        • RCS.B := Xa.BDR
        • RCS.Must_be_Zero := 0
14:  DB16 and the Access_Key from the previous environment (the environment in which
        the LBJ/CALL was executed) are copied into User X0 (regardless of the value of DB17) as follows:
            0:     DB16
            1-17:  Zeros
            18-35: Access_Key
15:  If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15
        and DB17 := Gate.DB17 and/or if Gate.AKI = 0,
        Indicator/Key_Register.Access_Key := Gate.Access_Key.DB16 := 1, indicating a transfer
        to Extended_Mode.
        If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
        R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent
        Parameter 0 Value; and/or if LP1I = 0, then if either DB17 = 0, User
        R1 := Gate.Latent_Parameter_1 Value or DB17 = 1, Executive
        R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter into Executive
        R0/R1 does not cause a GRS violation regardless of the level of processor privilege in effect.
17:  PAR.PC := (U)bits 18-35.
18:  PAR.L,BDI is updated.
21:  If the Target BD.G = 1, a Terminal_Addressing_Exception interrupt occurs.
Note: if nongated and Enter access is denied in the Target BD, processing follows the normal
LBJ algorithm performing the load of the Bank without the transfer to Extended_Mode (see
4.6.3.3).

LBJ,IS = 1 to an Extended_Mode Bank with Enter Access or Through a Gate
(LBJ/GOTO) (4.6.4) Summary:
3-9: Source L,BDI is translated from Xa.E,LS,BDI as described in 4.6.3.1, then a
        determination is made of the Base_Register information to be loaded (including any
        interrupts that may be generated). Gate processing may occur.
10:  At this time it is detected that the Target BD.Type = Extended_Mode, either Gated or
        with Enter access, and that a mixed-mode (Basic_Mode to Extended_Mode) transfer is
        to occur and that B0 is to be loaded.
11:  The Base_Register (Xa.BDR + 12).V := 1 and its associated ABT.L,BDI := 0,0, marking
        that Base_Register void. The ABT.Offset is Architecturally_Undefined for void
        Base_Registers.
14:  DB16 and the Access_Key from the previous environment (the environment in which
        the LBJ/GOTO was executed) are copied into User X0 (regardless of the value of DB17) as follows:
            0:     DB16
            1-17:  Zeros
            18-35: Access_Key
15:  If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15
        and DB17 := Gate.DB17 and/or if Gate.AKI = 0,
        Indicator/Key_Register.Access_Key := Gate.Access_Key. DB16 := 1, indicating a
        transfer to Extended_Mode.
        If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
        R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent
        Parameter 0 Value; and/or if LP1I = 0, then if either DB17 = 0, User
        R1 := Gate.Latent_Parameter_1 Value or DB17 = 1, Executive
        R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter into Executive
        R0/R1 does not cause a GRS violation regardless of the level of processor privilege in effect.
17:  PAR.PC := (U)bits 18-35.
18:  PAR.L,BDI is updated.
21:  If the Target BD.G = 1, a Terminal_Addressing_Exception interrupt occurs.
Note: if nongated and Enter access is denied in the Target BD, processing follows the normal
LBJ algorithm performing the load of the Bank without the transfer to Extended_Mode (see
4.6.3.3).

LBJ,IS = 2 with DB16 = 1 on RCS (LBJ/RTN to Basic_Mode) (4.6.4) Summary:
A model_dependent check must be made for a possible RCS overflow as described in 4.6.4.1.
3-9: A determination is made of the Base_Register information to be loaded (including any
        interrupts that may be generated). RCS.L,BDI is the Source L,BDI.
10:  Because this is a RTN to Basic_Mode (RCS.DB16 = 1), one of B12–B15 is to be loaded,
        determined by RCS.B + 12.
16:  Access_Key := RCS.Access_Key.
        DB12–17 := RCS.DB12-17.
17:  PAR.PC := RCS.Offset.
18:  ABT(RCS.B + 12).L,BDI := RCS.L,BDI.
        ABT(RCS.B + 12).Offset := 0.
20:  Basic_Mode DB31 toggle and Reference_Violation detection (see 4.4.6.1).
21:  If BD.G = 1 a Terminal_Addressing_Exception interrupt occurs. No check for Enter
        Access, Validated Entry, or Selection of Base_Register is made on LBJ/RTN to Basic_Mode.
Note: When Xa.IS = 2, Xa bits 0-3, 6-35 are ignored and Xa is not modified.

LBJ,IS = 2 with DB16 = 0 on RCS (LBJ/RTN to Extended_Mode) (4.6.4) Summary:
A model_dependent check must be made for a possible RCS overflow as described in
4.6.4.1.
3-9: A determination is made of the Base_Register information to be loaded into B0
        (including any interrupts that may be generated). RCS.L,BDI is the Source L,BDI.
10:  Because DB16 = 0 in the RCS, this is a mixed-mode (Basic_Mode to Extended_Mode)
        transfer and B0 is to be loaded.
11:  B(RCS.B + 12).V := 1 and ABT(RCS.B + 12).L,BDI := 0,0, marking that Base_Register void.
        The Offset is Architecturally_Undefined for void Base_Registers.
16:  Access_Key := RCS.Access_Key.
        DB12–17 := RCS.DB12-17.
17:  PAR.PC := RCS.Offset.
18:  PAR.L,BDI := RCS.L,BDI
21:  If the BD.G = 1 a Terminal_Addressing_Exception interrupt occurs. No check for Enter
        access is made on LBJ/RTN to Extended_Mode.
Note: When Xa.IS = 2, Xa bits 0-3, 6-35 are ignored and Xa is not modified.
 */
public abstract class LXJFunction extends Function {

    protected LXJFunction(
        final String mnemonic
    ) {
        super(mnemonic);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }
}
