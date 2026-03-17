/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Magnitude Accumulator instruction
 * (LMA) loads the content of U under j-field control, takes its magnitude, and stores it in A(a)
 */
public class LMAFunction extends Function {

    public static final LMAFunction INSTANCE = new LMAFunction();

    private LMAFunction() {
        super("LMA");
        var fc = new FunctionCode(0_12);
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
        var operand = engine.getOperand(true, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        if (Word36.isNegative(operand)) {
            operand = Word36.negate(operand);
        }
        engine.getExecOrUserARegister(ci.getA()).setW(operand);
        return true;
    }
}
