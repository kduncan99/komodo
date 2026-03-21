/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Left Shift and Count instruction
 * (DLSC) Shifts 72-bit (U) left circularly until bit 0 is not equal to bit 1.
 * The result is then stored in A(a)|A(a+1), and the number of bit positions shifted is stored in A(a+2).
 * If the starting value is all zeros or all ones, that value is stored in A(a)|A(a+1)
 * and A(a+2) is set to 71.
 */
public class DLSCFunction extends Function {

    public static final DLSCFunction INSTANCE = new DLSCFunction();

    private DLSCFunction() {
        super("DLSC");
        var fc = new FunctionCode(0_73).setJField(0_07);
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
        var operands = engine.getConsecutiveOperands(false, 2);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        var aReg2 = engine.getExecOrUserARegister(ci.getA() + 2);
        if (DoubleWord36.isZero(operands[0], operands[1])) {
            aReg0.setW(operands[0]);
            aReg1.setW(operands[1]);
            aReg2.setW(71);
        } else {
            var result = new Long[2];
            var count = 0;
            for (;;) {
                var bits = (operands[0] >> 34) & 03;
                if ((bits == 1) || (bits == 2)) {
                    aReg0.setW(operands[0]);
                    aReg1.setW(operands[1]);
                    aReg2.setW(count);
                    break;
                }
                DoubleWord36.leftShiftCircular(operands[0], operands[1], 1, result);
                operands[0] = result[0];
                operands[1] = result[1];
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
