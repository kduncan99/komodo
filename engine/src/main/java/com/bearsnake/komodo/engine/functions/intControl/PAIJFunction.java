/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.intControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Prevent All Interrupts and Jump instruction
 * (PAIJ) Clears DB13 (prevents deferrable interrupts from processing)
 * then loads the program counter from the U field - assumes no bank switching
 */
public class PAIJFunction extends Function {

    public static final PAIJFunction INSTANCE = new PAIJFunction();

    private PAIJFunction() {
        super("PAIJ");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_14).setAField(0_07).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_14).setAField(0_07).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        // Indirect Addressing doesn't work with PP < 2, but we'll check this anyway in case something changes.
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        doJump(engine, operand);
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
