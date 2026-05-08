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
 * Double Load and Unpack Floating instruction
 * (DFU) Stores the magnitude of the characteristic of the floating-point operand into A(a) right-justified and zero-filled,
 * and the mantissa of the operand into A(a+1)|A(a+2) right-justified and sign-extended.
 */
public class DFUFunction extends FloatingFunction {

    public static final DFUFunction INSTANCE = new DFUFunction();

    private DFUFunction() {
        super("DFU");
        var fc = new FunctionCode(0_76).setJField(0_14);
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
        var operands = engine.getConsecutiveOperands(true, 2);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        var aReg2 = engine.getExecOrUserARegister(ci.getA() + 2);

        var neg = Word36.isNegative(operands[0]);
        var characteristic = operands[0] >> 24;
        var mantissaHigh = operands[0] & 0_000077_777777L;
        if (neg) {
            characteristic ^= 0_7777;
            mantissaHigh |= 0_777700_000000L;
        }

        aReg0.setW(characteristic);
        aReg1.setW(mantissaHigh);
        aReg2.setW(operands[1]);

        return true;
    }
}
