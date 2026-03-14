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
 * (J) Loads the program counter from the U field - assumes no bank switching
 */
public class JFunction extends Function {

    public JFunction() {
        super("J");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_04).setAField(0_00));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_15).setAField(0_04));

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

        doJump(engine, operand);
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
