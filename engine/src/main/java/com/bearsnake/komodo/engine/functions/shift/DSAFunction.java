/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Shift Algebraic instruction
 * (DSA) shifts the content of A(a)|A(a+1) right by U bits, preserving the sign bit (bit 71).
 */
public class DSAFunction extends Function {

    public static final DSAFunction INSTANCE = new DSAFunction();

    private DSAFunction() {
        super("DSA");
        var fc = new FunctionCode(0_73).setJField(0_05);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getImmediateOperand() & 0177;
        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister(ci.getA() + 1);

        var result = new Long[2];
        DoubleWord36.rightShiftAlgebraic(aReg0.getW(), aReg1.getW(), (int)operand, result);
        aReg0.setW(result[0]);
        aReg1.setW(result[1]);

        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
