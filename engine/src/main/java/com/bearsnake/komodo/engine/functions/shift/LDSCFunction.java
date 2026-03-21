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
 * Left Double Shift Circular instruction
 * (LDSC) shifts the content of A(a)|A(a+1) left by U bits, wrapping bit 71 back to bit 0.
 */
public class LDSCFunction extends Function {

    public static final LDSCFunction INSTANCE = new LDSCFunction();

    private LDSCFunction() {
        super("LDSC");
        var fc = new FunctionCode(0_73).setJField(0_011);
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
        DoubleWord36.leftShiftCircular(aReg0.getW(), aReg1.getW(), (int)operand, result);
        aReg0.setW(result[0]);
        aReg1.setW(result[1]);

        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
