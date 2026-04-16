/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load I-Bank and Jump function
 * (LIJ) Loads a bank (selected from B12:B13) and then jumps to the address in the U field.
 */
public class LIJFunction extends Function {

    public static final LIJFunction INSTANCE = new LIJFunction();

    private LIJFunction() {
        super("LIJ");
        setBasicModeFunctionCode(new FunctionCode(0_07).setJField(0_13));

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
