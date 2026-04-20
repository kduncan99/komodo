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
        var par = engine.getProgramAddressRegister();
        engine.allocateAndPopulateRCSFrame(par.getBankLevel(),
                                           par.getBankDescriptorIndex(),
                                           par.getProgramCounter() + 1,
                                           0,
                                           (int)(dr.getCompositeValue() >> 18) & 077,
                                           ik.getAccessKey());

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
