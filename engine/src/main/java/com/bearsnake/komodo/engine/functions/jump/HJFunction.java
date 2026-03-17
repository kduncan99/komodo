/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Halt Jump instruction
 * (HJ or HKJ) Loads the program counter for the U field. No halt occurs.
 * Effectively just a Jump instruction.
 */
public class HJFunction extends Function {

    public static final HJFunction INSTANCE = new HJFunction();

    private HJFunction() {
        super("HJ");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_05));

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
