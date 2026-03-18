/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Test NOP instruction
 * (TNOP) Retrieves the operand, but does nothing with it and never skips.
 * Extended Mode only.
 */
public class TNOPFunction extends Function {

    public static final TNOPFunction INSTANCE = new TNOPFunction();

    private TNOPFunction() {
        super("TNOP");
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(00));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // TNOP retrieves the operand, but does nothing with it and never skips.
        engine.getOperand(false, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        return true;
    }
}
