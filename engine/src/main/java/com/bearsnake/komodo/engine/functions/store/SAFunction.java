/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Accumulator instruction
 * (SA) Stores the content of A(a) to U under j-field control
 */
public class SAFunction extends Function {

    public SAFunction() {
        super("SA");
        var fc = new FunctionCode(0_01);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var operand = engine.getExecOrUserARegister(ci.getA()).getW();
        return engine.storeOperand(true, true, true, true, operand);
    }
}
