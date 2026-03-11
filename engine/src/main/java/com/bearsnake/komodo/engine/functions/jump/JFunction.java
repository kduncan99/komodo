/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump instruction
 */
public class JFunction extends Function {

    public JFunction() {
        super("J");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_04).setAField(0_00));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_15).setAField(0_04));

        setAFieldSemantics(AFieldSemantics.UNUSED);
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
