/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump on No Characteristic Overflow instruction
 * (JNFO) Jumps if characteristic overflow is clear.
 */
public class JNFOFunction extends Function {

    public static final JNFOFunction INSTANCE = new JNFOFunction();

    private JNFOFunction() {
        super("JNFO");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_15).setAField(0_02));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_15).setAField(0_02));

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

        if (!engine.getDesignatorRegister().isCharacteristicOverflow()) {
            doJump(engine, operand);
        }
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
