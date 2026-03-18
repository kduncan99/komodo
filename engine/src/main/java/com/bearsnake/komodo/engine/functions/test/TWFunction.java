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
 * Test Within Range instruction
 * (TW) Checks if A(a) < (U) <= A(a+1).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TWFunction extends Function {

    public static final TWFunction INSTANCE = new TWFunction();

    private TWFunction() {
        super("TW");
        var fc = new FunctionCode(056);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        int a = ci.getA();
        var aValue = engine.getExecOrUserARegister(a).getW();
        var aPlus1Value = engine.getExecOrUserARegister((a + 1) & 017).getW();

        if (Word36.compare(aValue, operand) < 0 && Word36.compare(operand, aPlus1Value) <= 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
