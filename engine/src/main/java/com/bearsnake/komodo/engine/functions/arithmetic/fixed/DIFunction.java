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
 * Divide Integer instruction
 * (DI) Divides the 72-bit signed integer in A(a):A(a+1) by the 36-bit signed content in U,
 * storing the result in A(a) with the remainder in A(a+1).
 */
public class DIFunction extends FixedFunction {

    public static final DIFunction INSTANCE = new DIFunction();

    private DIFunction() {
        super("DI");
        var fc = new FunctionCode(0_34);
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

        System.out.printf("divisor=%012o negDivisor=%b biDivisor=%s\n", divisor, negDivisor, biDivisor);//TODO remove

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister((ci.getA() + 1));
        var dividendMSW = aReg0.getW();
        var dividendLSW = aReg1.getW();
        var negDividend = aReg0.isNegative();
        if (negDividend) {
            dividendLSW ^= Word36.BIT_MASK;
            dividendMSW ^= Word36.BIT_MASK;
        }
        var biDividend = BigInteger.valueOf(dividendMSW).shiftLeft(36).or(BigInteger.valueOf(dividendLSW));

        System.out.printf("dividend=%012o:%012o negDividend=%b, biDividend=%s\n", dividendMSW, dividendLSW, negDividend, biDividend);

        var negResult = negDividend ^ negDivisor;

        if (biDivisor.equals(BigInteger.ZERO) || (biDividend.compareTo(biDivisor.shiftLeft(35)) >= 0)) {
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
