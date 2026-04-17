/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Call function
 * (CALL) Loads a bank to B0 then jumps to the address in the U field
 * after preserving essential portions of the current environment.
 */
/*
TODO REMOVE THESE SPECIAL NOTES LATER


 */
public class CALLFunction extends Function {

    public static final CALLFunction INSTANCE = new CALLFunction();

    private CALLFunction() {
        super("CALL");
        setExtendedModeFunctionCode(new FunctionCode(0_07).setJField(0_16).setAField(0_13));

        setAFieldSemantics(AFieldSemantics.UNUSED);
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
