/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Add Negative Halves to Accumulator instruction
 * (ANH) Adds the arithmetic inverse of the individual H1 and H2 values of the content of U
 * to the corresponding halves of the value in A(a), and storing the composite result in A(a).
 */
public class ANHFunction extends FixedFunction {

    public static final ANHFunction INSTANCE = new ANHFunction();

    private ANHFunction() {
        super("ANH");
        var fc = new FunctionCode(0_72).setJField(0_05);
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
        var operand = engine.getOperand(false, true, false, false, false);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg = engine.getExecOrUserARegister(ci.getA());

        var resH1 = add18(engine, aReg.getH1(), Word36.getH1(operand) ^ 0_777777);
        var resH2 = add18(engine, aReg.getH2(), Word36.getH2(operand) ^ 0_777777);
        var result = (long)resH1 << 18 | resH2;
        aReg.setW(result);

        return true;
    }
}
