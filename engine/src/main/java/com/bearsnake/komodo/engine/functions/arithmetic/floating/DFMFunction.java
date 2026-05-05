/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.ArithmeticExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.math.BigInteger;

/**
 * Double Floating Multiple instruction
 * (DFM) Multiplies two 72-bit floating point numbers, storing the normalized result in A(a):A(a+1).
 */
public class DFMFunction extends FloatingFunction {

    public static final DFMFunction INSTANCE = new DFMFunction();

    private DFMFunction() {
        super("DFM");
        var fc = new FunctionCode(0_76).setJField(0_12);
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
        var aValue0 = aReg0.getW();
        var aValue1 = aReg1.getW();

        if (DoubleWord36.isZero(operands[0], operands[1]) || DoubleWord36.isZero(aValue0, aValue1)) {
            aReg0.setW(0);
            aReg1.setW(0);
            return true;
        }

        var aValues = new long[]{aValue0, aValue1};
        var mantissa = BigInteger.valueOf(getDoublePrecisionMantissa(aValues))
                                 .multiply(BigInteger.valueOf(getDoublePrecisionMantissa(operands)));
        var exponent = getDoublePrecisionExponent(aValues) + getDoublePrecisionExponent(operands);
        var sign = getDoublePrecisionSign(aValues) ^ getDoublePrecisionSign(operands);

        while (mantissa.shiftRight(119).intValue() == sign) {
            mantissa = mantissa.shiftLeft(1);
            exponent--;
        }
        mantissa = mantissa.shiftRight(60);

        var dr = engine.getDesignatorRegister();
        if (exponent < -02000) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicUnderflow);
            }
            aReg0.setW(0);
            aReg1.setW(0);
            dr.setCharacteristicUnderflow(true);
            return true;
        } else if (exponent > 02000) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicOverflow);
            }
            aReg0.setW(0);
            aReg1.setW(0);
            dr.setCharacteristicOverflow(true);
            return true;
        }

        constructDoublePrecision(aValues, sign, getDoublePrecisionCharacteristicFromExponent(exponent), mantissa.longValue());
        aReg0.setW(aValues[0]);
        aReg1.setW(aValues[1]);

        return true;
    }
}
