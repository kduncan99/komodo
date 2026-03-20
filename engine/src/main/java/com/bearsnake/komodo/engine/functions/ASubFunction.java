/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

/**
 * Handles routing of functions which have a common f field and possibly a j field,
 * and are further discriminated by an a subfield.
 */
public class ASubFunction extends SubFunction {

    protected ASubFunction(String mnemonic) {
        super(mnemonic);
    }

    //TODO remove later
    @Override
    public void debug(final String prefix) {
        for (var e : _functions.entrySet()) {
            var fc = e.getKey();
            var func = e.getValue();
            System.out.printf("%sa=%03o: %s%n", prefix, fc, func.getMnemonic());
            if (func instanceof SubFunction sf) {
                sf.debug(prefix + "  ");
            }
        }
    }

    @Override
    Function lookupFunction(
        final long instWord
    ) throws InvalidInstructionInterrupt {
        var func = _functions.get(InstructionWord.getA(instWord));
        if (func == null) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        }
        if (func instanceof SubFunction sf) {
            return sf.lookupFunction(instWord);
        }
        return func;
    }

    @Override
    boolean putFunction(
        final FunctionCode functionCode,
        final Function function
    ) {
        // There should be no subFunctions under an ASubFunction. This is easy.
        var existing = _functions.get(functionCode.getAField());
        if (existing != null) {
            throw new FunctionTable.CollisionException(existing, function);
        }
        _functions.put(functionCode.getAField(), function);
        return true;
    }
}
