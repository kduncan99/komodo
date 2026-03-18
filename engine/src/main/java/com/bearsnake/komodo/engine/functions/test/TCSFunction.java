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
 * Test and Clear and Skip instruction
 * (TCS) If U:05 is set, clear U:S1 and skip. Otherwise, go to next instruction.
 * This instruction is executed under storage lock for U.
 */
public class TCSFunction extends Function {
    public static final TCSFunction INSTANCE = new TCSFunction();

    private TCSFunction() {
        super("TCS");
        var fc = new FunctionCode(073).setJField(017).setAField(02);
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
            engine.storeToCachedAddress(operand & 0_007777_777777L);
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
