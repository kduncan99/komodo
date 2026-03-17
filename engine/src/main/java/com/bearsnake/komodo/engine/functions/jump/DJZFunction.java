/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Jump Zero instruction
 * (DJZ) Jumps if the double-word operand is zero (positive or negative).
 */
public class DJZFunction extends Function {

    public static final DJZFunction INSTANCE = new DJZFunction();

    private DJZFunction() {
        super("DJZ");
        var fc = new FunctionCode(0_71).setJField(0_16);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var jumpTarget = engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var operands = engine.getConsecutiveOperands(true, 2);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        if (DoubleWord36.isZero(operands[0], operands[1])) {
            doJump(engine, jumpTarget);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
