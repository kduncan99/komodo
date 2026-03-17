/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Load Negative A instruction
 * (DLN) loads the arithmetic negative of the content of U and U+1, storing the values in Aa and Aa+1
 */
public class DLNFunction extends Function {

    public static final DLNFunction INSTANCE = new DLNFunction();

    private DLNFunction() {
        super("DLN");
        var fc = new FunctionCode(0_71).setJField(014);
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
        if (operands == null) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var ax = engine.getExecOrUserARegisterIndex(ci.getA());

        operands[0] = Word36.negate(operands[0]);
        operands[1] = Word36.negate(operands[1]);

        engine.getGeneralRegister(ax).setW(operands[0]);
        engine.getGeneralRegister(ax + 1).setW(operands[1]);
        return true;
    }
}
