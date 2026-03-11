/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Modifier and Jump instruction
 */
public class LMJFunction extends Function {

    public LMJFunction() {
        super("LMJ");
        var c = new FunctionCode(0_74).setJField(0_13);
        setBasicModeFunctionCode(c);
        setExtendedModeFunctionCode(c);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        ActivityStatePacket activityState
    ) throws MachineInterrupt {
        // TODO
        return false;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
