/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Register instruction
 * (SR) Stores the content of R(a) to U under j-field control
 */
public class SRFunction extends Function {

    public static final SRFunction INSTANCE = new SRFunction();

    private SRFunction() {
        super("SR");
        var fc = new FunctionCode(0_04);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.R_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var operand = engine.getExecOrUserRRegister(ci.getA()).getW();
        return engine.storeOperand(true, true, true, true, operand);
    }
}
