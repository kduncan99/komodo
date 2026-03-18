/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Masked Alphanumeric Test Less Than or Equal instruction
 * (MATL) Checks the logical AND of U AND R2 to see if the result is
 * alphanumerically less than or equal to the logical AND of A(a) AND R2.
 * Alphanumeric comparisons treat bit 0 as a data bit rather than a sign bit.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class MATLFunction extends Function {

    public static final MATLFunction INSTANCE = new MATLFunction();

    private MATLFunction() {
        super("MATL");
        var fc = new FunctionCode(071).setJField(006);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, false, false, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aValue = engine.getExecOrUserARegister(ci.getA()).getW();

        operand &= engine.getExecOrUserRRegister(2).getW();
        aValue &= engine.getExecOrUserRRegister(2).getW();
        if (operand <= aValue) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
