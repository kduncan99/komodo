/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump Positive and Shift instruction
 * (JPS) Jumps if A(a) is positive (bit 0 is clear).
 * Additionally, A(a) is shifted left circular by one position.
 */
public class JPSFunction extends Function {

    public static final JPSFunction INSTANCE = new JPSFunction();

    private JPSFunction() {
        super("JPS");
        var fc = new FunctionCode(0_72).setJField(0_02);
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
        var operand = engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var a = engine.getCurrentInstruction().getA();
        var reg = engine.getExecOrUserARegister(a);
        if (reg.isPositive()) {
            doJump(engine, operand);
        }

        // Shift happens regardless of jump
        long value = reg.getW();
        reg.setW(Word36.leftShiftCircular(value, 1));

        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
