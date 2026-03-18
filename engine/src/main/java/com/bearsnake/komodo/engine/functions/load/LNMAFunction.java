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
 * Load Negative Magnitude Accumulator instruction
 * (LNMAA) loads the content of U under j-field control,
 * and stores the arithmetic inverse of its magnitude it in A(a)
 */
public class LNMAFunction extends Function {

    public static final LNMAFunction INSTANCE = new LNMAFunction();

    private LNMAFunction() {
        super("LNMA");
        var fc = new FunctionCode(0_13);
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
        if (Word36.isPositive(operand)) {
            operand = Word36.negate(operand);
        }
        engine.getExecOrUserARegister(ci.getA()).setW(operand);
        return true;
    }
}
