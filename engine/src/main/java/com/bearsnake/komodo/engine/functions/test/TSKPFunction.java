/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Test and Skip instruction
 * (TSKIP) Retrieves the operand, then always skips.
 * Extended Mode only.
 */
public class TSKPFunction extends Function {

    public static final TSKPFunction INSTANCE = new TSKPFunction();

    private TSKPFunction() {
        super("TSKIP");
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(017));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // TSKIP retrieves the operand, then always skips.
        engine.getOperand(false, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        engine.getProgramAddressRegister().incrementProgramCounter();

        return true;
    }
}
