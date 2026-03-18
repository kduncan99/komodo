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
 * Test and Set and Skip instruction
 * (TSS) If U:05 is set, go to next instruction. Otherwise, set U:05 and skip.
 * This instruction is executed under storage lock for U.
 */
public class TSSFunction extends Function {
    public static final TSSFunction INSTANCE = new TSSFunction();

    private TSSFunction() {
        super("TSS");
        var fc = new FunctionCode(073).setJField(017).setAField(01);
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

        if ((Word36.getS1(operand) & 01) == 0) {
            engine.storeToCachedAddress(operand | 0_010000_000000L);
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
