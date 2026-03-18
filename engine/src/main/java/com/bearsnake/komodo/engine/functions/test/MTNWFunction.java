/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Masked Test Not Within Range instruction
 * (MTNW) Checks the following:
 *     ! ( (A(a) AND R2) < ((U) AND R2) <= (A(a+1) AND R2)) )
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class MTNWFunction extends Function {

    public static final MTNWFunction INSTANCE = new MTNWFunction();

    private MTNWFunction() {
        super("MTNW");
        var fc = new FunctionCode(071).setJField(005);
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
        var aValue0 = engine.getExecOrUserARegister(ci.getA()).getW();
        var aValue1 = engine.getExecOrUserARegister(ci.getA() + 1).getW();

        operand &= engine.getExecOrUserRRegister(2).getW();
        aValue0 &= engine.getExecOrUserRRegister(2).getW();
        aValue1 &= engine.getExecOrUserRRegister(2).getW();
        if ((aValue0 >= operand) || (operand > aValue1)) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
