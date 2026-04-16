/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load D-Bank and Jump function
 * (LDJ) Loads a bank (selected from B14:B15) and then jumps to the address in the U field.
 */
public class LDJFunction extends Function {

    public static final LDJFunction INSTANCE = new LDJFunction();

    private LDJFunction() {
        super("LDJ");
        setBasicModeFunctionCode(new FunctionCode(0_07).setJField(0_12));

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
