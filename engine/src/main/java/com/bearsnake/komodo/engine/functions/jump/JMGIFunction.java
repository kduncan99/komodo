/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Jump Modifier Greater and Increment instruction
 * (JMGI) Compare the modifier portion of X(a) to zero, and if it is greater,
 * jump to the address indicated by resolving U.
 * In any case, apply index register incrementation, except that if the index register
 * is also specified as X(x) and the increment bit (h-field) is set, it will already be
 * increment so we do not do it twice.
 * The architecture guide does not specify whether the comparison to zero occurs
 * before or after incrementation in the case where fieldA == fieldX and fieldH == 1,
 * so we wait until after U resolution (which would do the increment) to do the comparison.
 * NOTE: We FAIL the spec in that when X(a) is incremented due to address resolution,
 * we do 24-bit modification for extended mode with 24-bit index modifier flag set...
 * The spec specifically states that the increment for this instruction should always use 18-bits.
 * There is a messy way to fix this. We'll do it if someone complains.
 */
public class JMGIFunction extends Function {

    public static final JMGIFunction INSTANCE = new JMGIFunction();

    private JMGIFunction() {
        super("JMGI");
        var fc = new FunctionCode(0_74).setJField(0_012);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

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

        var ci = engine.getCurrentInstruction();
        var xaReg = engine.getExecOrUserXRegister(ci.getA());
        var modifier = xaReg.getXM();

        if ((modifier > 0) && (modifier < 0400000)) {
            doJump(engine, operand);
        }

        if ((ci.getA() != ci.getX()) || (ci.getH() == 0))
            xaReg.incrementModifier18();
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
