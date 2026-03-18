/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load String Bit Offset instruction
 * (LSBO) Copies the right-most 6 bits of U into X(a)[0:5]
 */
public class LSBOFunction extends Function {

    public static final LSBOFunction INSTANCE = new LSBOFunction();

    private LSBOFunction() {
        super("LSBO");
        var fc = new FunctionCode(0_60);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserXRegisterIndex(ci.getA())).setS1((int)operand);
        return true;
    }
}
