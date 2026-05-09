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
 * Floating Compress and Load instruction
 * (FCL) Compresses a double precision floating point number in U:U+1
 * to a single precision floating point number storing it in A(a).
 */
public class FCLFunction extends FloatingFunction {

    public static final FCLFunction INSTANCE = new FCLFunction();

    private FCLFunction() {
        super("FCL");
        var fc = new FunctionCode(0_76).setJField(0_17);
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
        var dr = engine.getDesignatorRegister();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var neg = Word36.isNegative(operands[0]);

        // Get magnitude of 8 bit characteristic, convert to 11 bit, and ones-complement 12 bits if we are negative.
        // The resulting 11 bit characteristic will be prefixed with the sign bit.
        var characteristic11 = operands[0] >> 24;
        if (characteristic11 >= 04000) {
            characteristic11 ^= 07777;
        }

        if (characteristic11 < 01600) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicUnderflow);
            }
            aReg0.setW(0);
            dr.setCharacteristicUnderflow(true);
            return true;
        } else if (characteristic11 > 02177) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicOverflow);
            }
            aReg0.setW(0);
            dr.setCharacteristicOverflow(true);
            return true;
        }
        var characteristic8 = characteristic11 - 01600;
        if (neg) {
            characteristic8 ^= 0777;
        }

        var mantissa = ((operands[0] & 0_000077_777777L) << 3) | (operands[1] >> 33);
        aReg0.setW((characteristic8 << 27) | mantissa);
        return true;
    }
}
