/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump on Overflow instruction
 * (JO) Jumps if the designator register overflow bit is set.
 */
public class JOFunction extends Function {

    public static final JOFunction INSTANCE = new JOFunction();

    private JOFunction() {
        super("JO");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_14).setAField(0_00));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_14).setAField(0_00));

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

        if (engine.getDesignatorRegister().isOverflow()) {
            doJump(engine, operand);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
