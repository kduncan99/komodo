/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Return function
 * (RTN) uses the RCS to determine how to re-establish a previously saved environment,
 * loading the appropriate bank and then jumping to the address in the U field.
 */
/*
TODO REMOVE THESE SPECIAL NOTES LATER

RTN to an Extended_Mode (RCS DB16 = 0) Algorithm (4.6.4) Summary:
A model_dependent check must be made for a possible RCS overflow as described in 4.6.4.1.
3-10:   A determination is made of the Base_Register information to be loaded into B0
        (including any interrupts that may be generated). RCS.L,BDI is the Source L,BDI.
16:     Access_Key := RCS.Access_Key
        DB12–17 := RCS.DB12-17.
17:     PAR.PC := RCS.Offset
18:     PAR.L,BDI := RCS.L,BDI
21:     If BD.G = 1 a Terminal_Addressing_Exception interrupt occurs. No check for Enter
        access is made on RTN.

RTN to Basic_Mode (RCS DB16 = 1, Mixed-Mode Transfer) Algorithm (4.6.4) Summary:
3-9:    A determination is made of the Base_Register information to be loaded (including any
        interrupts that may be generated). RCS.L,BDI is the Source L,BDI.
10:     Because this is a RTN to Basic_Mode (RCS.DB16 = 1), one of Base_Register 12–15 is to
        be loaded, decided by RCS.B + 12.
11:     B0.V := 1 and hard-held PAR.L,BDI := 0,0, marking B0 as void.
16:     Access_Key := RCS.Access_Key
        DB12–17 := RCS.DB12-17.
17:     PAR.PC := (U)bits 18–35.
18:     The ABT is updated. Note: ABT(Target B).Offset := 0.
20:     Basic_Mode DB31 toggle and Reference_Violation detection (see 4.4.6.1).
21:     If BD.G = 1 a Terminal_Addressing_Exception interrupt occurs. No check for Enter Access,
        Validated Entry or Selection of Base_Register is made on RTN.
 */
public class RTNFunction extends Function {

    public static final RTNFunction INSTANCE = new RTNFunction();

    private RTNFunction() {
        super("RTN");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_03));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        return true;//TODO
    }
}
