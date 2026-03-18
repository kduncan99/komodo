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
 * Test Zero instruction
 * (TZ) Checks the value of U to see if it is positive or negative zero.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TZFunction extends Function {

    public static final TZFunction INSTANCE = new TZFunction();

    private TZFunction() {
        super("TZ");
        setBasicModeFunctionCode(new FunctionCode(050));// A is not used
        setExtendedModeFunctionCode(new FunctionCode(050).setAField(06));

        setAFieldSemantics(AFieldSemantics.UNUSED); // or FUNCTION_CODE_EXTENSION
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

        if (Word36.isZero(operand)) {
            // increment once here. In any case, the normal cycle() operation will increment as well.
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
