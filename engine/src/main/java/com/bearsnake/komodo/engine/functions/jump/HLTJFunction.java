/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Halt Jump instruction
 * (HLTJ) Loads the program counter for the U field then stops the processor
 */
public class HLTJFunction extends Function {

    public static final HLTJFunction INSTANCE = new HLTJFunction();

    private HLTJFunction() {
        super("HLTJ");
        var fc = new FunctionCode(0_74).setJField(0_15).setAField(0_05).setProcessorPrivilege(0);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        doJump(engine, operand);
        engine.halt(Engine.HaltCode.HLTJ_INSTRUCTION);
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
