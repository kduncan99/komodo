/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.addrSpace;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Base Register, User Function
 * (LBU) Loads the base register (2 through 15) with the bank indicated by the virtual address in U.
 * U:H1 contains L,BDI of the bank, while U:H2 contains an offset (for subsetting purposes).
 */
public class LBUFunction extends Function {

    public static final LBUFunction INSTANCE = new LBUFunction();

    private LBUFunction() {
        super("LBU");
        setBasicModeFunctionCode(new FunctionCode(0_75).setJField(0_00).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_75).setJField(0_00));

        setAFieldSemantics(AFieldSemantics.B_REGISTER);
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
