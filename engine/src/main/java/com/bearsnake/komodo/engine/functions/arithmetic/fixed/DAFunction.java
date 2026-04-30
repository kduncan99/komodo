/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Add to Accumulator instruction
 * (DA) Adds the 72-bit content of U:U+1 to the 72-bit value in A(a):A(a+1) and stores it in A(a).
 */
public class DAFunction extends FixedFunction {

    public static final DAFunction INSTANCE = new DAFunction();

    private DAFunction() {
        super("DA");
        var fc = new FunctionCode(0_71).setJField(0_10);
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
        var aReg1 = engine.getExecOrUserARegister((ci.getA() + 1));

        add72(engine, aReg0, aReg1, operands[0], operands[1]);
        return true;
    }
}
