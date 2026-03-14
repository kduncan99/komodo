/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Execute Repeated Instruction
 * (EXR) Fetches the instruction at the developed operand address and executes it repeatedly,
 * according to the repeat counter in R1.
 * See 6.27.2 for list of target instructions which are allowed.
 */
public class EXRFunction extends Function {

    public EXRFunction() {
        super("EXR");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_14).setAField(0_06));

        setAFieldSemantics(AFieldSemantics.FUNCTION_CODE_EXTENSION);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // TODO
        return true;
    }
}
