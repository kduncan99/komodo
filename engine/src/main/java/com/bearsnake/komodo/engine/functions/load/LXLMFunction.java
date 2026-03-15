/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load X Long Modifier instruction
 * (LXLM) loads the content of U under j-field control, and stores it in LX(a)[12-35]
 */
public class LXLMFunction extends Function {

    public LXLMFunction() {
        super("LXLM");
        setBasicModeFunctionCode(new FunctionCode(0_75).setJField(013).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_75).setJField(013));

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(true, true, true, false, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        engine.getExecOrUserXRegister(ci.getA()).setXM24(operand);
        return true;
    }
}
