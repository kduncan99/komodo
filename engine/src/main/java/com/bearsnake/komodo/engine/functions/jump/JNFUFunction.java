/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump on No Characteristic Underflow instruction
 * (JNFU) Jumps if characteristic underflow is clear.
 */
public class JNFUFunction extends Function {

    public JNFUFunction() {
        super("JNFU");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_15).setAField(0_01));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_15).setAField(0_01));

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

        if (!engine.getDesignatorRegister().isCharacteristicUnderflow()) {
            doJump(engine, operand);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
