/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import static com.bearsnake.komodo.engine.Constants.GRS_X0;

/**
 * Local function
 * (LOCL) does not load a bank, but it does preserve the current environment
 * and then jumps to the address in the U field.
 */
/*
TODO REMOVE THESE SPECIAL NOTES LATER

1 A model_dependent check must be made for a possible RCS overflow as described in 4.6.4.1
2 An RCS frame is bought as described in 3.3.1 and written with the following information:
• RCS.Reentry_Point_Program_Address.L,BDI := PAR.L,BDI
• RCS.Reentry_Point_Program_Address.Offset := PAR.PC + 1 (points to instruction following LOCL)
• RCS.DB12-17 := current DB12–17
• RCS.Access_Key := current Access_Key
• RCS.B := 0
• RCS.Must_be_Zero := 0
3 DB16 and the Access_Key are copied into User X0 (regardless of the value of DB17) as follows:
        0:     DB16
        1-17:  Zeros
        18-35: Access_Key
4 Hard-held PAR.PC := (U)bits 18-35.
 */
public class LOCLFunction extends Function {

    public static final LOCLFunction INSTANCE = new LOCLFunction();

    private LOCLFunction() {
        super("LOCL");
        setExtendedModeFunctionCode(new FunctionCode(0_07).setJField(0_16).setAField(0_00));

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        var dr = engine.getDesignatorRegister();
        var ik = engine.getActivityStatePacket().getIndicatorKeyRegister();

        // TODO buy an RCS frame
        // TODO save RCS data (see temporary TODO above)

        // Preserve DB16 and access key to User X0
        var x0Value = (dr.isBasicModeEnabled() ? 0_400000_000000L : 0L) | ik.getAccessKey().toComposite();
        engine.getGeneralRegister(GRS_X0, true).setW(x0Value);

        // Set new address in PAR.PC
        doJump(engine, operand);
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
