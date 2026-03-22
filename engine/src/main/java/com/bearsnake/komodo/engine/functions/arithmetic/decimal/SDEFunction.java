/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Subtract Decimal instruction
 * (SDE) Subtracts decimal (U) from decimal A(a) and stores the result in A(a).
 */
public class SDEFunction extends DecimalFunction {

    public static final SDEFunction INSTANCE = new SDEFunction();

    private SDEFunction() {
        super("SDE");
        var fc = new FunctionCode(0_07).setJField(0_02);
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
        var operand = engine.getOperand(true, true, true, false, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aReg = engine.getExecOrUserARegister(ci.getA());

        var binaryU = toBinary(operand);
        var binaryA = toBinary(aReg.getW());
        var over = DecimalFunction.toDecimal(binaryA - binaryU, aReg);

        var dr = engine.getDesignatorRegister();
        dr.setCarry(false);
        dr.setOverflow(over);

        return true;
    }
}
