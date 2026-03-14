/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Double Count Bits instruction
 * (DCB) Counts the number of bits set in the double-word at U,U+1,
 * storing the result in A(a)
 */
public class DCBFunction extends Function {

    public DCBFunction() {
        super("DCB");
        setExtendedModeFunctionCode(new FunctionCode(0_33).setJField(0_15));

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

        var count = Long.bitCount(operands[0]) + Long.bitCount(operands[1]);
        engine.getExecOrUserARegister(engine.getCurrentInstruction().getA()).setW(count);
        return true;
    }
}
