/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.TestAndSetInterrupt;

/**
 * Test and Set instruction
 * (TS) If U:05 is set, take an interrupt. Otherwise, set U:05 and continue.
 * This instruction is executed under storage lock for U.
 */
public class TSFunction extends Function {

    public static final TSFunction INSTANCE = new TSFunction();

    private TSFunction() {
        super("TS");
        var fc = new FunctionCode(073).setJField(017).setAField(00);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.FUNCTION_CODE_EXTENSION);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, false, false, false, true);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        if ((Word36.getS1(operand) & 01) == 1) {
            throw new TestAndSetInterrupt(engine.getCachedBaseRegisterIndex(),
                                          engine.getCachedRelativeAddress());
        }

        engine.storeToCachedAddress(operand | 0_010000_000000L);
        return true;
    }
}
