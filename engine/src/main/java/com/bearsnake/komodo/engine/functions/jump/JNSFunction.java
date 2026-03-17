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
 * Jump Negative and Shift instruction
 * (JNS) Jumps if A(a) is negative (bit 0 is set).
 * Additionally, A(a) is shifted left circular by one position.
 */
public class JNSFunction extends Function {

    public static final JNSFunction INSTANCE = new JNSFunction();

    private JNSFunction() {
        super("JNS");
        var fc = new FunctionCode(0_72).setJField(0_03);
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

        var ci = engine.getCurrentInstruction();
        var reg = engine.getGeneralRegister(ci.getA());
        boolean isNegative = reg.isNegative();

        // Shift happens regardless of jump
        long value = reg.getW();
        reg.setW(Word36.leftShiftCircular(value, 1));

        if (isNegative) {
            doJump(engine, operand);
        }
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
