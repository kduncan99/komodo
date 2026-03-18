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
 * Test Neither Zero instruction
 * (TNZ) Checks the value of U to see if it is neither positive nor negative zero.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TNZFunction extends Function {

    public static final TNZFunction INSTANCE = new TNZFunction();

    private TNZFunction() {
        super("TNZ");
        setBasicModeFunctionCode(new FunctionCode(051));
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(011));

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

        if (!Word36.isZero(operand)) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
