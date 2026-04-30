/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.OperationTrapInterrupt;

import java.math.BigInteger;

/**
 * Multiply Single Integer instruction
 * (MSI) Multiplies two signed 36-bit integers storing the (possibly truncated) result in A(a).
 */
public class MSIFunction extends FixedFunction {

    public static final MSIFunction INSTANCE = new MSIFunction();

    private MSIFunction() {
        super("MSI");
        var fc = new FunctionCode(0_31);
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
        var aReg = engine.getExecOrUserARegister(ci.getA());
        var operand2 = aReg.getW();
        var op2Neg = Word36.isNegative(operand2);
        if (op2Neg) {
            operand2 ^= Word36.BIT_MASK;
        }

        // We have to do goofy things involving BigInteger to make sure we capture overflows correctly,
        // since Java cannot do 72-bit native integers.
        var biOp1 = BigInteger.valueOf(operand1);
        var biOp2 = BigInteger.valueOf(operand2);
        var biResult = biOp1.multiply(biOp2);
        if (biResult.bitLength() > 35) {
            var dr = engine.getDesignatorRegister();
            dr.setOverflow(true);
            if (dr.isOperationTrapEnabled()) {
                engine.postInterrupt(new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow));
            }
        }

        var result = biResult.longValue() & Word36.BIT_MASK;
        if (op1Neg != op2Neg) {
            result ^= Word36.BIT_MASK;
        }
        aReg.setW(result);

        return true;
    }
}
