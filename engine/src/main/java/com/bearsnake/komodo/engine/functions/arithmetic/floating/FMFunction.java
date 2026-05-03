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

        var mantissa = (getMantissa(aValue) * getMantissa(operand)) >> 27;
        var exponent = getCharacteristic(aValue) + getCharacteristic(operand);
        var sign = getSign(aValue) ^ getSign(operand);
        while (mantissa >> 26 == sign) {
            mantissa <<= 1;
            exponent--;
        }

        var dr = engine.getDesignatorRegister();
        if (exponent < -0200) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicUnderflow);
            } else {
                dr.setCharacteristicUnderflow(true);
            }
        } else if (exponent > 0200) {
            if (dr.isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicOverflow);
            } else {
                dr.setCharacteristicOverflow(true);
            }
        }

        aReg.setW(construct(sign, getSinglePrecisionCharacteristicFromExponent(exponent), mantissa));

        return true;
    }
}
