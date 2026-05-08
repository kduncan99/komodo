/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load and Unpack Floating instruction
 * (LUF) Stores the magnitude of the characteristic of the floating-point operand into A(a) right-justified and zero-filled,
 * and the mantissa of the operand into A(a+1) right-justified and sign-extended.
 */
public class LUFFunction extends FloatingFunction {

    public static final LUFFunction INSTANCE = new LUFFunction();

    private LUFFunction() {
        super("LUF");
        var fc = new FunctionCode(0_76).setJField(0_04);
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
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        if (Word36.isZero(operand)) {
            aReg1.setW(0);
            return true;
        }

        var negOp = Word36.isNegative(operand);
        var magChar = (negOp ? (operand ^ Word36.BIT_MASK) : operand) >> 27;
        var mantissa = operand & 0_000777_777777L;
        if (negOp) {
            mantissa |= 0_777000_000000L;
        }

        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        aReg0.setW(magChar);
        aReg1.setW(mantissa);

        return true;
    }
}
