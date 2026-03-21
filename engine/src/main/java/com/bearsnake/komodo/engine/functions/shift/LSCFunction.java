/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Left Shift and Count instruction
 * (LSC) Shifts (U) left circularly until bit 0 is not equal to bit 1.
 * The result is then stored in A(a), and the number of bit positions shifted is stored in A(a+1).
 * If the starting value is all zeros or all ones, that value is stored in A(a) and A(a+1) is set to 35.
 */
public class LSCFunction extends Function {

    public static final LSCFunction INSTANCE = new LSCFunction();

    private LSCFunction() {
        super("LSC");
        var fc = new FunctionCode(0_73).setJField(0_06);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
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
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        if (Word36.isZero(operand) || (operand == 0_777777777777L)) {
            aReg0.setW(operand);
            aReg1.setW(35);
        } else {
            var count = 0;
            var current = operand;
            for (;;) {
                var bits = (current >> 34) & 03;
                if ((bits == 1) || (bits == 2)) {
                    aReg0.setW(current);
                    aReg1.setW(count);
                    break;
                }
                current = Word36.leftShiftCircular(current, 1);
                count++;
            }
        }

        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
