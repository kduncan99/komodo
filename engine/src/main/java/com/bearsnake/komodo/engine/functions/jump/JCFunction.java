/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump on Carry instruction
 * (JC) Jumps if the carry bit is set.
 */
public class JCFunction extends Function {

    public static final JCFunction INSTANCE = new JCFunction();

    private JCFunction() {
        super("JC");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_16));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_14).setAField(0_04));

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

        if (engine.getDesignatorRegister().isCarry()) {
            doJump(engine, operand);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
