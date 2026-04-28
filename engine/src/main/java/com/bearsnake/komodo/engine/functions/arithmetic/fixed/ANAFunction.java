/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Add Negative to Accumulator instruction
 * (ANA) Adds the arithmetic inverse of the content of U under j-field control
 * to the value in A(a), and stores it in A(a).
 */
public class ANAFunction extends FixedFunction {

    public static final ANAFunction INSTANCE = new ANAFunction();

    private ANAFunction() {
        super("ANA");
        var fc = new FunctionCode(0_15);
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
        operand ^= 0_777777_777777L;
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg = engine.getExecOrUserARegister(ci.getA());
        var result = add36(engine, aReg.getW(), operand);
        aReg.setW(result);

        return true;
    }
}
