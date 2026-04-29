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
 * Add Negative Thirds to Accumulator instruction
 * (AT) Adds the arithmetic inversed of the individual T1, T2, and T2 values of the content of U
 * to the corresponding thirds of the value in A(a), and storing the composite result in A(a).
 */
public class ANTFunction extends FixedFunction {

    public static final ANTFunction INSTANCE = new ANTFunction();

    private ANTFunction() {
        super("AT");
        var fc = new FunctionCode(0_72).setJField(0_07);
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

        var resT1 = add12(engine, (short) aReg.getT1(), (short) (Word36.getT1(operand) ^ 0_7777));
        var resT2 = add12(engine, (short) aReg.getT2(), (short) (Word36.getT2(operand) ^ 0_7777));
        var resT3 = add12(engine, (short) aReg.getT3(), (short) (Word36.getT3(operand) ^ 0_7777));
        var result = (long)resT1 << 24 | (long)resT2 << 12 | resT3;
        aReg.setW(result);

        return true;
    }
}
