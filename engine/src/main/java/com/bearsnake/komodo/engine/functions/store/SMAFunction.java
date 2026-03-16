/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Magnitude Accumulator instruction
 * (SMA) Stores the magnitude of the content of A(a) to U under j-field control
 */
public class SMAFunction extends Function {

    public SMAFunction() {
        super("SMA");
        var fc = new FunctionCode(0_03);
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
        if (Word36.isNegative(operand)) {
            operand = Word36.negate(operand);
        }
        return engine.storeOperand(true, true, true, true, operand);
    }
}
