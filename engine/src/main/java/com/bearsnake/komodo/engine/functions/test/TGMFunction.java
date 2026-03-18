/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Test Greater Magnitude instruction
 * (TGM) Checks if the magnitude of (U) is greater than A(a).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * Extended Mode only, f=033, j=013.
 */
public class TGMFunction extends Function {

    public static final TGMFunction INSTANCE = new TGMFunction();

    private TGMFunction() {
        super("TGM");
        setExtendedModeFunctionCode(new FunctionCode(033).setJField(013));

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, false, false, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aValue = engine.getExecOrUserARegister(ci.getA()).getW();

        long uMag = Word36.isNegative(operand) ? Word36.negate(operand) : operand;

        if (Word36.compare(uMag, aValue) > 0) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
