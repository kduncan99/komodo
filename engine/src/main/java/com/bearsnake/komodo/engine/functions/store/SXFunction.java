/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Index Register instruction
 * (SX) Stores the content of X(a) to U under j-field control
 */
public class SXFunction extends Function {

    public static final SXFunction INSTANCE = new SXFunction();

    private SXFunction() {
        super("SX");
        var fc = new FunctionCode(0_06);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var operand = engine.getExecOrUserXRegister(ci.getA()).getW();
        return engine.storeOperand(true, true, true, true, operand);
    }
}
