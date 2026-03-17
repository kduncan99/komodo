/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump instruction
 * (JK) evaluates the operand, but does not jump.
 * The assumption is that the selected jump key is present, but cleared.
 * It is not specified how the jump key is selected, but it doesn't matter.
 */
public class JKFunction extends Function {

    public static final JKFunction INSTANCE = new JKFunction();

    private JKFunction() {
        super("JK");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_04).setAField(0_01));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
