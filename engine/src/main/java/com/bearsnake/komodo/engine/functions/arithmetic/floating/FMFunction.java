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
 * Floating Multiple instruction
 * (FM) Multiplies two 36-bit floating point numbers, storing the normalized result in A(a).
 */
public class FMFunction extends FloatingFunction {

    public static final FMFunction INSTANCE = new FMFunction();

    private FMFunction() {
        super("FM");
        var fc = new FunctionCode(0_76).setJField(0_02);
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
        var aReg = engine.getExecOrUserARegister(ci.getA());
        var aValue = aReg.getW();

        if (Word36.isZero(operand) || Word36.isZero(aValue)) {
            aReg.setW(0);
            return true;
        }

        var resultSign = 0;
        if (getSinglePrecisionSign(operand) == 1) {
            resultSign = 1;
            operand ^= Word36.BIT_MASK;
        }
        if (getSinglePrecisionSign(aValue) == 1) {
            resultSign = 1 - resultSign;
            aValue ^= Word36.BIT_MASK;
        }

        var mantissa = (getSinglePrecisionMantissa(aValue) * getSinglePrecisionMantissa(operand));
        var exponent = getSinglePrecisionExponent(aValue) + getSinglePrecisionExponent(operand);
        while (mantissa >> 53 == resultSign) {
            mantissa <<= 1;
            exponent--;
        }
        mantissa >>= 27;

        var dr = engine.getDesignatorRegister();
        if (exponent < -0200) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicUnderflow);
            }
            aReg.setW(0);
            dr.setCharacteristicUnderflow(true);
            return true;
        } else if (exponent > 0200) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicOverflow);
            }
            aReg.setW(0);
            dr.setCharacteristicOverflow(true);
            return true;
        }

        aReg.setW(constructSinglePrecision(resultSign, getSinglePrecisionCharacteristicFromExponent(exponent), mantissa));

        return true;
    }
}
