/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump instruction
 */
public class JKFunction extends Function {

    // This is actually valid for f=074, j=04, and a=01 through 017.
    // However, implementing that in our structure here would be annoying.
    // So... we just use a=01 for JK. It has no function anyway.
    // If the day ever comes when we need to support a > 01, we can do something here to make it happen.
    public JKFunction() {
        super("JK");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_04).setAField(0_01));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // TODO
        return false;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
