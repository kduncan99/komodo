/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.ArithmeticExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Floating Expand and Load instruction
 * (FEL) Expands a single precision floating point number in U
 * to a double precision floating point number storing it in A(a)|A(a+1).
 */
public class FELFunction extends FloatingFunction {

    public static final FELFunction INSTANCE = new FELFunction();

    private FELFunction() {
        super("FEL");
        var fc = new FunctionCode(0_76).setJField(0_16);
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
        var aReg1 = engine.getExecOrUserARegister((ci.getA() + 1) & 017);
        var neg = Word36.isNegative(operand);

        // Get magnitude of 8 bit characteristic, convert to 11 bit, and ones-complement 12 bits if we are negative.
        // The resulting 11 bit characteristic will be prefixed with the sign bit.
        var characteristic8 = operand >> 27;
        if (characteristic8 >= 0400) {
            characteristic8 ^= 0777;
        }
        var characteristic12 = characteristic8 + 01600;
        if (neg) {
            characteristic12 ^= 07777;
        }

        var mantissa27 = operand & 0_000777_777777L;
        aReg0.setW((characteristic12 << 24) | (mantissa27 >> 3));
        aReg1.setW(mantissa27 << 33);
        return true;
    }
}
