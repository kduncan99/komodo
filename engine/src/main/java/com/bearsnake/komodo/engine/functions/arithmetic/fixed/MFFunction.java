/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.math.BigInteger;

/**
 * Multiply Fractional instruction
 * (MF) Multiplies two signed 36-bit integers, then shifts the result circularly to the left,
 * storing the result in A(a):A(a+1).
 * Assumes the factors are unsigned fractions with the decimal point between bits 0 and 1,
 * thus the resulting product is also a fraction with the decimal point between bits 0 and 1.
 */
public class MFFunction extends FixedFunction {

    public static final MFFunction INSTANCE = new MFFunction();
    private static final BigInteger MASK_36 = BigInteger.valueOf(Word36.BIT_MASK);
    private static final BigInteger MASK_72 = MASK_36.shiftLeft(36).or(MASK_36);

    private MFFunction() {
        super("MF");
        var fc = new FunctionCode(0_32);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand1 = engine.getOperand(true, true, true, true, false);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var op1Neg = Word36.isNegative(operand1);
        if (op1Neg) {
            operand1 ^= Word36.BIT_MASK;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        var operand2 = aReg0.getW();
        var op2Neg = Word36.isNegative(operand2);
        if (op2Neg) {
            operand2 ^= Word36.BIT_MASK;
        }

        var biOp1 = BigInteger.valueOf(operand1);
        var biOp2 = BigInteger.valueOf(operand2);
        var biResult = biOp1.multiply(biOp2);
        if (op1Neg != op2Neg) {
            biResult = biResult.xor(MASK_72);
        }
        biResult = biResult.shiftLeft(1);
        if (op1Neg != op2Neg) {
            biResult = biResult.or(BigInteger.ONE);
        }

        var resultLSW = biResult.longValue() & Word36.BIT_MASK;
        var resultMSW = biResult.shiftRight(36).longValue() & Word36.BIT_MASK;
        aReg0.setW(resultMSW);
        aReg1.setW(resultLSW);

        return true;
    }
}
