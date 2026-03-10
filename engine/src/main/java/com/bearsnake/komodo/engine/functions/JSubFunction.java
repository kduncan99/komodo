/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

import java.util.HashMap;

/**
 * Handles routing of functions which have a common f field,
 * and are further discriminated by a j subfield.
 */
public class JSubFunction extends SubFunction {

    private final HashMap<Integer, Function> _functions = new HashMap<>();

    protected JSubFunction(String mnemonic) {
        super(mnemonic);
    }

    @Override
    Function lookupFunction(
        final InstructionWord iWord
    ) throws InvalidInstructionInterrupt {
        var func = _functions.get((int)iWord.getJ());
        if (func == null) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        }
        return func;
    }

    boolean putFunction(
        final FunctionCode functionCode,
        final Function function
    ) {
        var j = functionCode.getJField();
        var a = functionCode.getAField();
        var existing = _functions.get(j);
        if (existing == null) {
            // There is nothing currently for the given j-field value.
            // Is the function insensitive to a-field? If so, we're just about done.
            if (a == null) {
                _functions.put(j, function);
                return true;
            }

            // No, this is f|j|a sensitive. We need an ASubFunction
            var asf = new ASubFunction(String.format("j%02o", j));
            asf.putFunction(functionCode, function);
            _functions.put(j, asf);
            return true;
        }

        // Something already exists for the given j-field value. If we are f|j|a sensitive this might still work.
        if ((a != null) && (existing instanceof ASubFunction asf)) {
            return asf.putFunction(functionCode, function);
        }

        // No, there is a collision of some kind here. Stop.
        return false;
    }
}
