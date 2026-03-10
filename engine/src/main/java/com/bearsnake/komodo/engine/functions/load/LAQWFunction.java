/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load A Quarter Word instruction
 */
public class LAQWFunction extends Function {

    public LAQWFunction() {
        super("LAQW");
        var fc = new FunctionCode(0_07).setJField(0_04);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public void execute(
        ActivityStatePacket activityState
    ) throws MachineInterrupt {
        // TODO
    }
}
