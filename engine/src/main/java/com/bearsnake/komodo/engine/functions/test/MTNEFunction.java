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
 * Masked Test Not Equal instruction
 * (MTNE) Checks the logical AND of U AND R2 to see if the result is NOT equal to
 * the logical AND of U AND A(a).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class MTNEFunction extends Function {

    public static final MTNEFunction INSTANCE = new MTNEFunction();

    private MTNEFunction() {
        super("MTNE");
        var fc = new FunctionCode(071).setJField(001);
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
        if (Word36.compare(operand, aValue) != 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
