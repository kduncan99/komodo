/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Add to Accumulator Upper instruction
 * (AU) Adds the content of U under j-field control to the value in A(a), and stores it in A(a+1).
 */
public class AUFunction extends FixedFunction {

    public static final AUFunction INSTANCE = new AUFunction();

    private AUFunction() {
        super("AU");
        var fc = new FunctionCode(0_20);
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
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg = engine.getExecOrUserARegister(ci.getA());
        var result = add36(engine, aReg.getW(), operand);
        engine.getExecOrUserARegister(ci.getA() + 1).setW(result);

        return true;
    }
}
