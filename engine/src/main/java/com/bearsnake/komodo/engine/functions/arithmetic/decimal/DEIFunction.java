/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Decimal to Integer instruction
 * (DEI) Converts the one-word BCD operand to one's complement binary.
 */
public class DEIFunction extends DecimalFunction {

    public static final DEIFunction INSTANCE = new DEIFunction();

    private DEIFunction() {
        super("DEI");
        var fc = new FunctionCode(0_07).setJField(0_06);
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

        var binary = toBinary(operand);
        var result = Word36.getOnesComplement(binary);
        var ci = engine.getCurrentInstruction();
        engine.getExecOrUserARegister(ci.getA()).setW(result);
        return true;
    }
}
