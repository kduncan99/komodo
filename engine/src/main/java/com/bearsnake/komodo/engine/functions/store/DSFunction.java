/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Store Accumulator instruction
 * (DS) Stores the content of Aa and Aa+1 to U and U+1
 */
public class DSFunction extends Function {

    public static final DSFunction INSTANCE = new DSFunction();

    private DSFunction() {
        super("DS");
        var fc = new FunctionCode(0_71).setJField(012);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var ax = engine.getExecOrUserARegisterIndex(ci.getA());
        var operands = new long[2];
        operands[0] = engine.getGeneralRegisterSet().getRegister(ax).getW();
        operands[1] = engine.getGeneralRegisterSet().getRegister(ax + 1).getW();
        return engine.storeConsecutiveOperands(true, operands);
    }
}
