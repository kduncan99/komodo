/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Integer to Decimal instruction
 * (IDE) Converts the single-precision one's complement binary operand
 * to two-word decimal operand. If the binary is negative zero, the result is positive zero.
 */
public class IDEFunction extends DecimalFunction {

    public static final IDEFunction INSTANCE = new IDEFunction();

    private IDEFunction() {
        super("IDE");
        var fc = new FunctionCode(0_07).setJField(0_10);
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
        var operand = engine.getOperand(true, true, true, false, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);

        if (Word36.isZero(operand)) {
            aReg0.setW(0);
            aReg1.setW(POSITIVE_SIGN);
        } else {
            var sign = Word36.isNegative(operand) ? NEGATIVE_SIGN : POSITIVE_SIGN;
            var magnitude = Word36.isNegative(operand) ? Math.abs(Word36.getTwosComplement(operand)) : operand;
            long value0 = 0; // top 9 decimal digits
            long value1 = (sign); // bottom 8 decimal digits and sign digit

            var wx = 1;
            var shift = 4;
            while (magnitude != 0) {
                var digit = magnitude % 10;
                magnitude /= 10;
                if (wx == 1) {
                    value1 |= (digit & 017) << shift;
                } else {
                    value0 |= (digit & 017) << shift;
                }

                shift += 4;
                if (shift == 36) {
                    shift = 0;
                    wx = 0;
                }
            }

            aReg0.setW(value0);
            aReg1.setW(value1);
        }

        return true;
    }
}
