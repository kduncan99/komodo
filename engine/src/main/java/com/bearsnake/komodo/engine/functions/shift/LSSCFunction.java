/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Left Shift Single Circular instruction
 * (LSSC) shifts the content of A(a) left by U bits, wrapping bit 35 back to bit 0.
 */
public class LSSCFunction extends Function {

    public static final LSSCFunction INSTANCE = new LSSCFunction();

    private LSSCFunction() {
        super("LSSC");
        var fc = new FunctionCode(0_73).setJField(0_10);
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
        var aReg = engine.getExecOrUserARegister(ci.getA());
        aReg.leftShiftCircular((int)operand);
        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
