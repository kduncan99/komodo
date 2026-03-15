/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Location and Jump instruction
 * (SLJ) Stores the next address after this instruction into U, then jumps to U+1.
 */
public class SLJFunction extends Function {

    public SLJFunction() {
        super("SLJ");
        var c = new FunctionCode(0_72).setJField(0_01);
        setBasicModeFunctionCode(c);

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
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
