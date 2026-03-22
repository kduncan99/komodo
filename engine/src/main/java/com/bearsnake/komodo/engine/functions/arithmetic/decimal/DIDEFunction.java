/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.math.BigInteger;

/**
 * Double Integer to Decimal instruction
 * (DIDE) Converts the double-precision one's complement binary operand
 * to three-word decimal operand. If the binary is negative zero, the result is positive zero.
 */
public class DIDEFunction extends DecimalFunction {

    public static final DIDEFunction INSTANCE = new DIDEFunction();

    private DIDEFunction() {
        super("DIDE");
        var fc = new FunctionCode(0_07).setJField(0_11);
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
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        var aReg2 = engine.getExecOrUserARegister(ci.getA() + 2);

        if (DoubleWord36.isZero(operands[0], operands[1])) {
            aReg0.setW(0);
            aReg1.setW(0);
            aReg2.setW(POSITIVE_SIGN);
        } else {
            var sign = DoubleWord36.isNegative(operands[0], operands[1]) ? NEGATIVE_SIGN : POSITIVE_SIGN;
            var magnitude = DoubleWord36.getTwosComplement(operands[0], operands[1]).abs();

            long value0 = 0; // top 9 decimal digits
            long value1 = 0; // next 9 decimal digits
            long value2 = (sign); // bottom 8 decimal digits and sign digit

            var wx = 2;
            var shift = 4;
            while (magnitude.compareTo(BigInteger.ZERO) > 0) {
                var results = magnitude.divideAndRemainder(BigInteger.TEN);
                magnitude = results[0];
                long digit = results[1].intValue();

                if (wx == 2) {
                    value2 |= (digit & 017) << shift;
                } else if (wx == 1) {
                    value1 |= (digit & 017) << shift;
                } else {
                    value0 |= (digit & 017) << shift;
                }

                shift += 4;
                if (shift == 36) {
                    shift = 0;
                    wx--;
                }
            }

            aReg0.setW(value0);
            aReg1.setW(value1);
            aReg2.setW(value2);
        }

        return true;
    }
}
