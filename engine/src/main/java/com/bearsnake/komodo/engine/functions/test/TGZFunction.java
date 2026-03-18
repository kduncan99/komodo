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
 * Test Greater Than Zero instruction
 * (TGZ) Checks the value of U to see if it is greater than positive zero.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * Extended Mode only.
 */
public class TGZFunction extends Function {

    public static final TGZFunction INSTANCE = new TGZFunction();

    private TGZFunction() {
        super("TGZ");
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(01));

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

        if (Word36.compare(operand, 0L) > 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
