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
 * Allow All Interrupts and Jump instruction
 * (AAIJ) Sets DB13 (allows deferrable interrupts to be processed)
 * then loads the program counter from the U field - assumes no bank switching
 */
public class AAIJFunction extends Function {

    public static final AAIJFunction INSTANCE = new AAIJFunction();

    private AAIJFunction() {
        super("AAIJ");
        setBasicModeFunctionCode(new FunctionCode(0_74).setJField(0_07));
        setExtendedModeFunctionCode(new FunctionCode(0_74).setJField(0_14).setAField(0_06).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        engine.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        doJump(engine, operand);
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
