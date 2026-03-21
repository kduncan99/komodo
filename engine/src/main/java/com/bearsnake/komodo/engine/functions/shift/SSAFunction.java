/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Single Shift Algebraic instruction
 * (SSA) shifts the content of A(a) right by U bits, preserving the sign bit.
 */
public class SSAFunction extends Function {

    public static final SSAFunction INSTANCE = new SSAFunction();

    private SSAFunction() {
        super("SSA");
        var fc = new FunctionCode(0_73).setJField(0_04);
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
        aReg.rightShiftAlgebraic((int)operand);
        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
