/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump Low Bit instruction
 * (JB) Jumps if the lowest bit of A(a) is set.
 */
public class JBFunction extends Function {

    public static final JBFunction INSTANCE = new JBFunction();

    private JBFunction() {
        super("JB");
        var fc = new FunctionCode(0_74).setJField(0_11);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
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
        if ((engine.getExecOrUserARegister(ci.getA()).getW() & 01) == 01) {
            doJump(engine, operand);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
