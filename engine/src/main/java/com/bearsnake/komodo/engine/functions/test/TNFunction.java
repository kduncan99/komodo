/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Test Negative instruction
 * (TN) Checks the value of U to see if it is negative (bit 35 is 1).
 * Note that negative zero is considered negative.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TNFunction extends Function {

    public static final TNFunction INSTANCE = new TNFunction();

    private TNFunction() {
        super("TN");
        setBasicModeFunctionCode(new FunctionCode(061));
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(014));

        setAFieldSemantics(AFieldSemantics.UNUSED);
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

        if (Word36.isNegative(operand)) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
