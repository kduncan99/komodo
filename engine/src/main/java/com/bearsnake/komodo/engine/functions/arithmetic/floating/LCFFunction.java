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
 * Load and Convert to Floating instruction
 * (LCF) Normalizes the fixed point number in U to bit 9, adjusting the characteristic in A(a) accordingly.
 * Stores the resulting floating point number in A(a+1).
 */
public class LCFFunction extends FloatingFunction {

    public static final LCFFunction INSTANCE = new LCFFunction();

    private LCFFunction() {
        super("LCF");
        var fc = new FunctionCode(0_76).setJField(0_05);
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
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);
        if (Word36.isZero(operand)) {
            aReg1.setW(0);
            return true;
        }

        var sign = operand & 0_400000_000000L;
        var bias = aReg0.getW() & 0_377;
        var normCount = 0;

        if (sign != 0) {
            operand ^= Word36.BIT_MASK;
        }

        // need to normalize the positive value such that the 1st 12 bits are 000_000_000_1xx
        while ((operand & 0_777000_000000L) != 0) {
            operand >>= 1;
            normCount++;
        }
        while ((operand & 0_000400_000000L) == 0) {
            operand <<= 1;
            normCount--;
        }

        var characteristic = bias + normCount;
        if (characteristic - 0200 < 0) {
            engine.getDesignatorRegister().setCharacteristicUnderflow(true);
            if (engine.getDesignatorRegister().isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicUnderflow);
            }
        } else if (characteristic - 0200 > 0177) {
            engine.getDesignatorRegister().setCharacteristicOverflow(true);
            if (engine.getDesignatorRegister().isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicOverflow);
            }
        }

        if (sign != 0) {
            characteristic ^= 0_377;
            operand ^= Word36.BIT_MASK;
        }

        aReg1.setW(sign | (characteristic << 27) | (operand & 0_000777_777777L));
        return true;
    }
}
