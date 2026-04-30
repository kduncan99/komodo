/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.ArithmeticExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.math.BigInteger;

/**
 * Divide Single Fractional instruction
 * (DSF) Divides the 72-bit signed integer created by sign-extending A(a) and shifting that result right by 1,
 * by the 36-bit signed divisor contained in U, storing the resulting quotient in A(a+1) and
 * discarding the remainder.
 */
public class DSFFunction extends FixedFunction {

    public static final DSFFunction INSTANCE = new DSFFunction();

    private DSFFunction() {
        super("DSF");
        var fc = new FunctionCode(0_35);
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
        var divisor = engine.getOperand(true, true, true, true, false);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }
        var negDivisor = Word36.isNegative(divisor);
        if (negDivisor) {
            divisor ^= Word36.BIT_MASK;
        }
        var biDivisor = BigInteger.valueOf(divisor);

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var dividendMSW = aReg0.getW();
        var negDividend = aReg0.isNegative();
        if (negDividend) {
            dividendMSW ^= Word36.BIT_MASK;
        }
        var biDividend = BigInteger.valueOf(dividendMSW).shiftLeft(36).shiftRight(1);

        var negResult = negDividend ^ negDivisor;

        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        if ((biDivisor.equals(BigInteger.ZERO)) || (biDividend.compareTo(biDivisor.shiftLeft(35)) >= 0)) {
            var dr = engine.getDesignatorRegister();
            dr.setDivideCheck(true);
            if (dr.isArithmeticExceptionEnabled()) {
                engine.postInterrupt(new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck));
            }
            aReg0.setW(0);
            aReg1.setW(0);
        } else {
            var quotient = biDividend.divide(biDivisor).longValue();
            var remainder = biDividend.remainder(biDivisor).longValue();
            if (negResult) {
                quotient ^= Word36.BIT_MASK;
                remainder ^= Word36.BIT_MASK;
            }
            aReg0.setW(quotient);
            aReg1.setW(remainder);
        }

        return true;
    }
}
