/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Left Shift Single Logical instruction
 * (LSSL) shifts the content of A(a) left by U bits.
 */
public class LSSLFunction extends Function {

    public static final LSSLFunction INSTANCE = new LSSLFunction();

    private LSSLFunction() {
        super("LSSL");
        var fc = new FunctionCode(0_73).setJField(0_12);
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
        aReg.leftShiftLogical((int)operand);
        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
