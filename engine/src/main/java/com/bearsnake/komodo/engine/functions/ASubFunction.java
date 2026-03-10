/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

import java.util.HashMap;

/**
 * Handles routing of functions which have a common f field and possibly a j field,
 * and are further discriminated by an a subfield.
 */
public class ASubFunction extends SubFunction {

    private final HashMap<Integer, Function> _functions = new HashMap<>();

    protected ASubFunction(String mnemonic) {
        super(mnemonic);
    }

    @Override
    Function lookupFunction(
        final InstructionWord iWord
    ) throws InvalidInstructionInterrupt {
        var func = _functions.get((int)iWord.getA());
        if (func == null) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        }
        return func;
    }

    @Override
    boolean putFunction(
        final FunctionCode functionCode,
        final Function function
    ) {
        // There should be no subFunctions under an ASubFunction. This is easy.
        return _functions.put(functionCode.getAField(), function) == null;
    }
}
