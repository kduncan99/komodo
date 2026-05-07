/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Characteristic Difference to Upper instruction
 * (CDU) Subtracts the characteristic of the magnitude of the operand from the
 * characteristic of the magnitude of the register value,
 * storing the result right-justified (into Q4) and sign-extended in A(a).
 */
public class CDUFunction extends FloatingFunction {

    public static final CDUFunction INSTANCE = new CDUFunction();

    private CDUFunction() {
        super("CDU");
        var fc = new FunctionCode(0_76).setJField(0_07);
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
        var operand = engine.getOperand(true, true, false, false, false);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister((ci.getA() + 1));
        var aValue = aReg0.getW();

        if (Word36.isNegative(operand)) {
            operand ^= Word36.BIT_MASK;
        }
        if (Word36.isNegative(aValue)) {
            aValue ^= Word36.BIT_MASK;
        }
        var opChar = (operand >> 27);
        var avChar = aValue >> 27;
        var result = avChar - opChar;
        if (result < 0) {
            result = (result - 1) | 0_777777_777000L;
        }
        aReg1.setW(result);

        return true;
    }
}
