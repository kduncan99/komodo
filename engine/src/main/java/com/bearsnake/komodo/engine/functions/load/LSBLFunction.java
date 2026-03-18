/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load String Bit Length instruction
 * (LSBL) Copies the right-most 6 bits of U into X(a)[6:11]
 */
public class LSBLFunction extends Function {

    public static final LSBLFunction INSTANCE = new LSBLFunction();

    private LSBLFunction() {
        super("LSBL");
        var fc = new FunctionCode(0_61);
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
        engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserXRegisterIndex(ci.getA())).setS2((int)operand);
        return true;
    }
}
