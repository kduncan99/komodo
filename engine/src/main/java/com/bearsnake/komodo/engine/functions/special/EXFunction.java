/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Execute Instruction
 * (EX) Fetches an instruction from the developed operand address and executes it.
 */
public class EXFunction extends Function {

    public static final EXFunction INSTANCE = new EXFunction();

    private EXFunction() {
        super("EX");
        setBasicModeFunctionCode(new FunctionCode(0_72).setJField(0_10));
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_14).setAField(0_05));

        setAFieldSemantics(AFieldSemantics.A_REGISTER);// not used for BM
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, false, false, false, false);
        engine.getActivityStatePacket().setCurrentInstruction(operand);
        return false;
    }
}
