/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump Non-Zero instruction
 * (JNZ) Jumps if A(a) is non-zero.
 */
public class JNZFunction extends Function {

    public static final JNZFunction INSTANCE = new JNZFunction();

    private JNZFunction() {
        super("JNZ");
        var fc = new FunctionCode(0_74).setJField(0_01);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        if (!engine.getExecOrUserARegister(ci.getA()).isZero()) {
            doJump(engine, operand);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
