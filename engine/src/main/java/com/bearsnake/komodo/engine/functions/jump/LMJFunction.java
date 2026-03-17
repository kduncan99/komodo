/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Modifier and Jump instruction
 * (LMJ) Stores the next address after this instruction in the modifier of X(a),
 * and then jumps to the address indicated by resolving U.
 */
public class LMJFunction extends Function {

    public static final LMJFunction INSTANCE = new LMJFunction();

    private LMJFunction() {
        super("LMJ");
        var c = new FunctionCode(0_74).setJField(0_13);
        setBasicModeFunctionCode(c);
        setExtendedModeFunctionCode(c);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
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

        var pc = engine.getProgramAddressRegister().getProgramCounter();
        var ci = engine.getCurrentInstruction();
        engine.getExecOrUserXRegister(ci.getA()).setXM(pc + 1);

        doJump(engine, operand);
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
