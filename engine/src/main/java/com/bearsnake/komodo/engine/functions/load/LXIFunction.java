/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load X Increment instruction
 * (LXI) loads the content of U under j-field control, and stores it in LX(a)[0-17]
 */
public class LXIFunction extends Function {

    public static final LXIFunction INSTANCE = new LXIFunction();

    private LXIFunction() {
        super("LXI");
        var fc = new FunctionCode(0_46);
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
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserXRegisterIndex(ci.getA())).setXI(operand);
        return true;
    }
}
