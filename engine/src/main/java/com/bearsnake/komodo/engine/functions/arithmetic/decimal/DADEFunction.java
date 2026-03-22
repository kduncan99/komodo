/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Add Decimal instruction
 * (DADE) Adds decimal (U)|(U+1) to decimal A(a)|A(a+1) and stores the result in A(a)|A(a+1).
 */
public class DADEFunction extends DecimalFunction {

    public static final DADEFunction INSTANCE = new DADEFunction();

    private DADEFunction() {
        super("DADE");
        var fc = new FunctionCode(0_07).setJField(0_01);
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
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg0 = engine.getExecOrUserARegister(ci.getA());
        var aReg1 = engine.getExecOrUserARegister((ci.getA() + 1) & 017);

        var binaryU = doubleToBinary(operands[0], operands[1]);
        var binaryA = doubleToBinary(aReg0.getW(), aReg1.getW());
        var over = DecimalFunction.doubleToDecimal(binaryA + binaryU, aReg0, aReg1);

        var dr = engine.getDesignatorRegister();
        dr.setCarry(false);
        dr.setOverflow(over);

        return true;
    }
}
