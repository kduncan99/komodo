/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

import java.util.HashMap;

/**
 * Handles routing of functions which have a common f field,
 * and are further discriminated by a j subfield.
 */
public abstract class SubFunction extends Function {

    private final HashMap<Integer, Function> _functions = new HashMap<>();

    protected SubFunction(String mnemonic) {
        super(mnemonic);
    }

    @Override
    public final boolean execute(
        ActivityStatePacket activityState
    ) throws InvalidInstructionInterrupt {
        throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
    }

    abstract Function lookupFunction(
        final InstructionWord iWord
    ) throws InvalidInstructionInterrupt;

    abstract boolean putFunction(
        final FunctionCode functionCode,
        final Function function
    );
}
