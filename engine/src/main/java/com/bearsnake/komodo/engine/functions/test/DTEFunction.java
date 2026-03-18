/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double-Precision Test Equal instruction
 * (DTE) Checks the value of U | U+1 to see if it is equal to A(a) | A(a+1).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class DTEFunction extends Function {

    public static final DTEFunction INSTANCE = new DTEFunction();

    private DTEFunction() {
        super("DTE");
        var fc = new FunctionCode(071).setJField(017);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getConsecutiveOperands(true, 2);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aValue0 = engine.getExecOrUserARegister(ci.getA()).getW();
        var aValue1 = engine.getExecOrUserARegister(ci.getA() + 1).getW();

        if (DoubleWord36.compare(operand[0], operand[1], aValue0, aValue1) == 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
