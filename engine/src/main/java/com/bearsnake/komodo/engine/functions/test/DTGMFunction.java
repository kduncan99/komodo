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
 * Double-Precision Test Greater Magnitude instruction
 * (DTGM) Checks the magnitude of U | U+1 to see if it is greater than A(a) | A(a+1).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class DTGMFunction extends Function {

    public static final DTGMFunction INSTANCE = new DTGMFunction();

    private DTGMFunction() {
        super("DTGM");
        var fc = new FunctionCode(033).setJField(014);
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

        if (Word36.isNegative(operand[0])) {
            operand[0] = Word36.logicalNot(operand[0]);
            operand[1] = Word36.logicalNot(operand[1]);
        }

        var ci = engine.getCurrentInstruction();
        var aValue0 = engine.getExecOrUserARegister(ci.getA()).getW();
        var aValue1 = engine.getExecOrUserARegister(ci.getA() + 1).getW();

        if (Word36.isNegative(aValue0)) {
            aValue0 = Word36.logicalNot(aValue0);
            aValue1 = Word36.logicalNot(aValue1);
        }

        if (DoubleWord36.compare(operand[0], operand[1], aValue0, aValue1) > 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
