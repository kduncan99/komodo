/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.ArithmeticExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load and Convert to Floating instruction
 * (DLCF) Normalizes the fixed point number in U|U+1 to bit 12, adjusting the characteristic in A(a) accordingly.
 * Stores the resulting floating point number in A(a+1)|A(a+2).
 */
public class DLCFFunction extends FloatingFunction {

    public static final DLCFFunction INSTANCE = new DLCFFunction();

    private DLCFFunction() {
        super("DLCF");
        var fc = new FunctionCode(0_76).setJField(0_15);
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
        var aReg2 = engine.getExecOrUserARegister(ci.getA() + 2);
        if (DoubleWord36.isZero(operands[0], operands[1])) {
            aReg1.setW(0);
            aReg2.setW(0);
            return true;
        }

        var sign = operands[0] & 0_400000_000000L;
        var bias = aReg0.getW() & 0_3777;
        var normCount = 0;

        if (sign != 0) {
            operands[0] ^= Word36.BIT_MASK;
            operands[1] ^= Word36.BIT_MASK;
        }

        // need to normalize the positive value such that the 1st 15 bits are 000_000_000_000_1xx
        while ((operands[0] & 0_777700_000000L) != 0) {
            DoubleWord36.rightShiftLogical(operands, 1);
            normCount++;
        }
        while ((operands[0] & 0_000040_000000L) == 0) {
            DoubleWord36.leftShiftLogical(operands, 1);
            normCount--;
        }

        var characteristic = bias + normCount;
        if (characteristic - 02000 < 0) {
            engine.getDesignatorRegister().setCharacteristicUnderflow(true);
            if (engine.getDesignatorRegister().isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicUnderflow);
            }
        } else if (characteristic - 02000 > 01777) {
            engine.getDesignatorRegister().setCharacteristicOverflow(true);
            if (engine.getDesignatorRegister().isArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.CharacteristicOverflow);
            }
        }

        if (sign != 0) {
            characteristic ^= 0_3777;
            operands[0] ^= Word36.BIT_MASK;
            operands[1] ^= Word36.BIT_MASK;
        }

        aReg1.setW(sign | (characteristic << 24) | (operands[0] & 0_000077_777777L));
        aReg2.setW(operands[1]);
        return true;
    }
}
