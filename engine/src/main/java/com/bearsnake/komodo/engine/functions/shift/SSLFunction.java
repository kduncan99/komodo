/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Single Shift Logical instruction
 * (SSL) shifts the content of A(a) right by U bits.
 */
public class SSLFunction extends Function {

    public static final SSLFunction INSTANCE = new SSLFunction();

    private SSLFunction() {
        super("SSL");
        var fc = new FunctionCode(0_73).setJField(0_02);
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
        aReg.rightShiftLogical((int)operand);
        return true;
    }

    @Override
    public boolean isShiftInstruction() {
        return true;
    }
}
