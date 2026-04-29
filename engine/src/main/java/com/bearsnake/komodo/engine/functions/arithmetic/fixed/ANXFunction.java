/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Add Negative to Index Register instruction
 * (ANX) Adds the arithmetic inverse of the content of U under j-field control
 * to the value in X(a), and stores it in X(a).
 */
public class ANXFunction extends FixedFunction {

    public static final ANXFunction INSTANCE = new ANXFunction();

    private ANXFunction() {
        super("ANX");
        var fc = new FunctionCode(0_25);
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
        var operand = engine.getOperand(true, true, true, true, false);
        operand ^= 0_777777_777777L;
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var xReg = engine.getExecOrUserXRegister(ci.getA());
        var result = add36(engine, xReg.getW(), operand);
        xReg.setW(result);

        return true;
    }
}
