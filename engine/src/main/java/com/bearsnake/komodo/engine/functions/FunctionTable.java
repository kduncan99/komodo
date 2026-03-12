/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.DesignatorRegister;
import com.bearsnake.komodo.engine.functions.load.*;
import com.bearsnake.komodo.engine.interrupts.HardwareDefaultInterrupt;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

import java.util.HashMap;

public abstract class FunctionTable {

    private static final Function[] ALL_FUNCTIONS = new Function[]{
        new DLFunction(),
        new DLMFunction(),
        new DLNFunction(),
        new LAFunction(),
        new LAQWFunction(),
        new LMAFunction(),
        new LNAFunction(),
        new LNMAFunction(),
        new LRFunction(),
        new LRSFunction(),
        new LSBLFunction(),
        new LSBOFunction(),
        new LXFunction(),
        new LXIFunction(),
        new LXLMFunction(),
        new LXMFunction(),
        new LXSIFunction(),
    };

    private static boolean _isInitialized = false;
    private static final HashMap<Integer, Function> BASIC_MODE_TOP_LEVEL = new HashMap<>();
    private static final HashMap<Integer, Function> EXTENDED_MODE_TOP_LEVEL = new HashMap<>();

    private static boolean ingestFunction(
        final HashMap<Integer, Function> topLevel,
        final Function function,
        final FunctionCode functionCode
    ) {
        Integer f = functionCode.getFField();
        Integer j = functionCode.getJField();
        Integer a = functionCode.getAField();

        if ((j == null) && (a == null)) {
            // This is f-field sensitive, with no reliance on j or a fields.
            return topLevel.put(f, function) == null;
        }

        if (j != null) {
            // function code is f|j field sensitive (and maybe |a-field).
            // Look for a function with the given f-field to see if it exists,
            // and if so, if it is a j-subfunction.
            var existing = topLevel.get(f);
            if (existing == null) {
                // Nothing at the targeted f location - create a new JSubFunction and pass it this function we're processing.
                // If the function is f|j|a sensitive, the JSubFunction will handle the recursion down to an ASubFunction.
                var jSub = new JSubFunction(String.format("f%03o", f));
                topLevel.put(f, existing);
                return jSub.putFunction(functionCode, function);
            }

            // A function already exists at the f coordinate - is it a JSubFunction? It better be...
            if (existing instanceof JSubFunction jSub) {
                return jSub.putFunction(functionCode, function);
            }

            return false;
        }

        // Function code is f|a sensitive. Algorithm is basically the same as above for f|j.
        var existing = topLevel.get(f);
        if (existing == null) {
            // Nothing at the targeted f location - create a new JSubFunction and pass it this function we're processing.
            // If the function is f|j|a sensitive, the JSubFunction will handle the recursion down to an ASubFunction.
            var aSub = new ASubFunction(String.format("f%03o", f));
            topLevel.put(f, existing);
            return aSub.putFunction(functionCode, function);
        }

        // A function already exists at the f coordinate - is it a JSubFunction? It better be...
        if (existing instanceof JSubFunction jSub) {
            return jSub.putFunction(functionCode, function);
        }

        return false;
    }

    private static boolean initializeLookups() {
        var error = false;
        for (var func : ALL_FUNCTIONS) {
            for (var fc : func.getBasicModeFunctionCodes()) {
                if (!ingestFunction(BASIC_MODE_TOP_LEVEL, func, fc)) {
                    error = true;
                }
            }
            for (var fc : func.getExtendedModeFunctionCodes()) {
                if (!ingestFunction(EXTENDED_MODE_TOP_LEVEL, func, fc)) {
                    error = true;
                }
            }
        }
        _isInitialized = !error;
        return _isInitialized;
    }

    public static Function lookupFunction(
        final DesignatorRegister dReg,
        final InstructionWord iWord
    ) throws InvalidInstructionInterrupt,
             HardwareDefaultInterrupt {
        synchronized (FunctionTable.class) {
            if (!_isInitialized) {
                if (!initializeLookups()) {
                    throw new HardwareDefaultInterrupt();
                }
            }

            var topLevel = dReg.isBasicModeEnabled() ? BASIC_MODE_TOP_LEVEL : EXTENDED_MODE_TOP_LEVEL;
            var func = topLevel.get(iWord.getF());
            if (func == null) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
            } else if (func instanceof SubFunction sf) {
                func = sf.lookupFunction(iWord);
            }
            return func;
        }
    }
}
