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
 * Test Less Than or Equal instruction
 * (TLE) Checks if the value of U is less than or equal to A(a).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TLEFunction extends Function {

    public static final TLEFunction INSTANCE = new TLEFunction();

    private TLEFunction() {
        super("TLE");
        var fc = new FunctionCode(054);
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
        var aValue = engine.getExecOrUserARegister(ci.getA()).getW();

        if (Word36.compare(operand, aValue) <= 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
