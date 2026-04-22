/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.addrSpace;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Base Register, Exec Function
 * (LBE) Loads the base register (16 through 31) with the bank indicated by the virtual address in U.
 * U:H1 contains L,BDI of the bank, while U:H2 contains an offset (for subsetting purposes).
 * Register selection is taken from the sum of 16 with the A field.
 */
public class LBEFunction extends Function {

    public static final LBEFunction INSTANCE = new LBEFunction();

    private LBEFunction() {
        super("LBE");
        setBasicModeFunctionCode(new FunctionCode(0_75).setJField(0_03).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_75).setJField(0_03).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.B_REGISTER_USER);
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
