/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

/**
 * Handles routing of functions which have a common f field,
 * and are further discriminated by a j subfield.
 */
public class JSubFunction extends SubFunction {

    protected JSubFunction(String mnemonic) {
        super(mnemonic);
    }

    //TODO remove later
    @Override
    public void debug(final String prefix) {
        for (var e : _functions.entrySet()) {
            var fc = e.getKey();
            var func = e.getValue();
            System.out.printf("%sj=%03o: %s%n", prefix, fc, func.getMnemonic());
            if (func instanceof SubFunction sf) {
                sf.debug(prefix + "  ");
            }
        }
    }

    @Override
    Function lookupFunction(
        final InstructionWord iWord
    ) throws InvalidInstructionInterrupt {
        var func = _functions.get(iWord.getJ());
        if (func == null) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        } else if (func instanceof SubFunction sf) {
            func = sf.lookupFunction(iWord);
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
            var asf = new ASubFunction(String.format("j%03oa", j));
            asf.putFunction(functionCode, function);
            _functions.put(j, asf);
            return true;
        }

        // Something already exists for the given j-field value. If we are f|j|a sensitive this might still work.
        if ((a != null) && (existing instanceof ASubFunction asf)) {
            return asf.putFunction(functionCode, function);
        }

        // No, there is a collision of some kind here. Stop.
        throw new FunctionTable.CollisionException(existing, function);
    }
}
