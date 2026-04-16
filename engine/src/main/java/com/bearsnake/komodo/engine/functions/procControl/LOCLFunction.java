/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Local function
 * (LOCL) does not load a bank, but it does preserve the current environment,
 * and then jumps to the address in the U field.
 */
public class LOCLFunction extends Function {

    public static final LOCLFunction INSTANCE = new LOCLFunction();

    private LOCLFunction() {
        super("LOCL");
        setBasicModeFunctionCode(new FunctionCode(0_07).setJField(0_16).setAField(0_00));

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
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
