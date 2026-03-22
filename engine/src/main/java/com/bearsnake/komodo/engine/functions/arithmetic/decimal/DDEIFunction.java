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
 * Double Decimal to Integer instruction
 * (DDEI) Converts the two-word BCD operand to one's complement double-word binary.
 */
public class DDEIFunction extends DecimalFunction {

    public static final DDEIFunction INSTANCE = new DDEIFunction();

    private DDEIFunction() {
        super("DDEI");
        var fc = new FunctionCode(0_07).setJField(0_07);
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

        var valueHigh = ((operands[0] >> 32) & 017) * 100000000L + ((operands[0] >> 28) & 017) * 10000000L
                        + ((operands[0] >> 24) & 017) * 1000000L + ((operands[0] >> 20) & 017) * 100000L
                        + ((operands[0] >> 16) & 017) * 10000L + ((operands[0] >> 12) & 017) * 1000L
                        + ((operands[0] >> 8) & 017) * 100 + ((operands[0] >> 4) & 017) * 10
                        + (operands[0] & 017);
        var valueLow = ((operands[1] >> 32) & 017) * 10000000L + ((operands[1] >> 28) & 017) * 1000000L
                + ((operands[1] >> 24) & 017) * 100000L + ((operands[1] >> 20) & 017) * 10000L
                + ((operands[1] >> 16) & 017) * 1000L + ((operands[1] >> 12) & 017) * 100L
                + ((operands[1] >> 8) & 017) * 10 + ((operands[1] >> 4) & 017);

        var value = BigInteger.valueOf(valueHigh).multiply(BigInteger.valueOf(100000000L)).add(BigInteger.valueOf(valueLow));
        if (isNegative(operands[1]) && !value.equals(BigInteger.ZERO)) {
            value = value.negate();
        }

        var interim = new long[2];
        DoubleWord36.getOnesComplement(value, interim, 0);
        var ci = engine.getCurrentInstruction();
        engine.getExecOrUserARegister(ci.getA()).setW(interim[0]);
        engine.getExecOrUserARegister(ci.getA() + 1).setW(interim[1]);

        return true;
    }
}
