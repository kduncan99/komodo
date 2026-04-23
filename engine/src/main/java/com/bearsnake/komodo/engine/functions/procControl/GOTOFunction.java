/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.AccessPermissions;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.AddressingExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Go To function
 * (GOTO) Loads a bank to B0 then jumps to the address in the U field.
 */
/*
 TODO REMOVE THESE SPECIAL NOTES LATER
The GOTO instruction loads B0 (or one of B12–B15 on a transfer to Basic_Mode) to describe the
Bank specified by the L,BDI in bits 0–17 of the instruction operand at the address U and a jump is
taken in the new environment to the address in bits 18–35 of the instruction operand at the
address U. Regardless of the value of DB17, User X0bit 0 := DB16 (0 if previous mode was
Extended_Mode or 1 if previous mode was Basic_Mode), User X0bits 1-17 := 0, and User
X0bits 18-35 := previous Access_Key. Gate processing may occur, including writing Latent
Parameter values to R0 and R1 (User R0 and User R1 if DB17 = 0 or Executive R0 and Executive
R1 if DB17 = 1), if Gate.LP0I = 0 and/or Gate.LP1I = 0, respectively.

GOTO to an Extended_Mode Bank Algorithm (4.6.4) Summary:
3-10:A determination is made of the Base_Register information to be loaded into B0
        (including any interrupt that may be generated). Gate processing may occur.
14:  DB16 and the Access_Key from the previous environment (the environment in which
        the GOTO was executed) are copied into User X0 (regardless of the value of DB17) as
        follows:
            0:     DB16
            1-17:  Zeros
            18-35: Access_Key
15:  If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15
        and DB17 := Gate.DB17 and/or if Gate.AKI = 0, Indicator/Key_Register.Access_Key := Gate.Access_Key.
        If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
        R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent
        Parameter 0 Value; and/or if LP1I = 0, then if either DB17 = 0, User
        R1 := Gate.Latent_Parameter_1 Value or DB17 = 1, Executive
        R1 := Gate.Latent_Parameter_1 Value.
        Note: writing a Latent Parameter into Executive R0/R1 do not cause a GRS violation
        regardless of the level of processor privilege in effect.
17:  PAR.PC := (U)bits 18–35.
18:  PAR.L,BDI is updated.
21:  If the Target BD.G = 1 or if Target BD.GAP.E = 0 and Target BD.SAP.E = 0 (Enter access
        is denied) on a nongated transfer, a Terminal_Addressing_Exception interrupt occurs

GOTO to a Basic_Mode Bank (Mixed-Mode Transfer) Algorithm (4.6.4) Summary:
3-9:   A determination is made of the Base_Register information to be loaded into B0
        (including any interrupt that may be generated). Gate processing may occur.
10-11: At this time it is detected that the Target BD.Type = Basic_Mode and that a
        mixed-mode (Extended_Mode to Basic_Mode) transfer is to occur. B0.V := 1 and hard-
        held PAR.L,BDI := 0,0, marking B0 as void. A determination is made of which of B12–
        B15 is to be loaded. For a nongated GOTO, B12 is loaded. For a gated GOTO,
        Gate.B + 12 determine the Base_Register number. The only way that a GOTO to
        Basic_Mode can load other than B12 is through a Gate.
14:    DB16 and the Access_Key from the previous environment (the environment in which
        the GOTO was executed) are copied into User X0 (regardless of the value of DB17) as follows:
            0:     DB16
            1-17:  Zeros
            18-35: Access_Key
15:    If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15
        and DB17 := Gate.DB17 and/or if Gate.AKI = 0,
        Indicator/Key_Register.Access_Key := Gate.Access_Key.
        If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
        R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent
        Parameter 0 Value; and/or if LP1I = 0, then if either DB17 = 0, User
        R1 := Gate.Latent_Parameter_1 Value or DB17 = 1, Executive
        R1 := Gate.Latent_Parameter_1 Value.
        Note: writing a Latent Parameter into Executive R0/R1 does not cause a GRS
        violation regardless of the level of processor privilege in effect.
16:    DB.16 := 1
17:    PAR.PC := (U)bits 18-35.
18:    The ABT is updated. Note: ABT(Target B).Offset := 0.
20:    Basic_Mode DB31 toggle and Reference_Violation detection (see 4.4.6.1).
21:    If the Target BD.G = 1 or if Target BD.GAP.E = 0 and Target BD.SAP.E = 0 (Enter access
        is denied) on a nongated transfer a Terminal_Addressing_Exception interrupt occurs.
 */
public class GOTOFunction extends Function {

    public static final GOTOFunction INSTANCE = new GOTOFunction();

    private GOTOFunction() {
        super("GOTO");
        setExtendedModeFunctionCode(new FunctionCode(0_07).setJField(0_17).setAField(0_00));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, false, false, false);
        engine.bankManipulation(this, (short) 0, (short) 0, (short) 0, 0, null, operand, null);
        return true;
    }
}
