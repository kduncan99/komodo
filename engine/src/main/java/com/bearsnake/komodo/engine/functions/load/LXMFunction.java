/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load X Modifier instruction
 * (LXM) loads the content of U under j-field control, and stores it in LX(a)[18-35]
 */
public class LXMFunction extends Function {

    public static final LXMFunction INSTANCE = new LXMFunction();

    private LXMFunction() {
        super("LXM");
        var fc = new FunctionCode(0_26);
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
        engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserXRegisterIndex(ci.getA())).setXM(operand);
        return true;
    }
}
