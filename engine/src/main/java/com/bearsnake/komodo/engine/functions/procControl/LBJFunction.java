/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Bank and Jump function
 * (LBJ) Loads a bank (selected from B12:B15 for Basic Mode, B0 for Extended Mode)
 * and then jumps to the address in the U field.
 */
public class LBJFunction extends Function {

    public static final LBJFunction INSTANCE = new LBJFunction();

    private LBJFunction() {
        super("LBJ");
        setBasicModeFunctionCode(new FunctionCode(0_07).setJField(0_17));

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
