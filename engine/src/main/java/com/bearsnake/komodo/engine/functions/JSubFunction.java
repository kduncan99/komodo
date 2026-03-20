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
        final long instWord
    ) throws InvalidInstructionInterrupt {
        var j = InstructionWord.getJ(instWord);
        var func = _functions.get(j);
        if (func == null) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        }

        if (func instanceof SubFunction sf) {
            return sf.lookupFunction(instWord);
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

        if (a == null) {
            if (existing == null) {
                _functions.put(j, function);
                return true;
            }
            if (existing instanceof ASubFunction asf) {
                // If we have an ASubFunction, we can't put an f|j function there
                // unless we move it to a=0? No, that's not right.
                throw new FunctionTable.CollisionException(existing, function);
            }
            throw new FunctionTable.CollisionException(existing, function);
        }

        // New function is f|j|a sensitive.
        ASubFunction asf;
        if (existing == null) {
            asf = new ASubFunction(String.format("j%03oa", j));
            _functions.put(j, asf);
        } else if (existing instanceof ASubFunction existingAsf) {
            asf = existingAsf;
        } else {
            // Collision: existing is a Function, but we need an ASubFunction
            // Special handling: if existing is indeed the function for a=0, we could convert.
            // But let's assume registration order is consistent.
            throw new FunctionTable.CollisionException(existing, function);
        }

        return asf.putFunction(functionCode, function);
    }
}
