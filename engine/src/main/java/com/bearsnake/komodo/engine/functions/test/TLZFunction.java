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
 * Test Less Than Zero instruction
 * (TLZ) Checks the value of U to see if it is less than negative zero.
 * This skips if (U) < -0.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * Extended Mode only.
 */
public class TLZFunction extends Function {

    public static final TLZFunction INSTANCE = new TLZFunction();

    private TLZFunction() {
        super("TLZ");
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(010));

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

        if (Word36.compare(operand, Word36.NEGATIVE_ZERO) < 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
