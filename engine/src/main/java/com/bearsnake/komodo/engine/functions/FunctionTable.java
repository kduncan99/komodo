/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.DesignatorRegister;
import com.bearsnake.komodo.engine.functions.jump.*;
import com.bearsnake.komodo.engine.functions.load.*;
import com.bearsnake.komodo.engine.functions.logical.*;
import com.bearsnake.komodo.engine.functions.special.*;
import com.bearsnake.komodo.engine.functions.test.*;
import com.bearsnake.komodo.engine.functions.store.*;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;

import java.util.HashMap;

public abstract class FunctionTable {

    public static class CollisionException extends RuntimeException {

        public CollisionException(
            final Function f1,
            final Function f2
        ) {
            super("Function code collision between " + f1.getMnemonic() + " and " + f2.getMnemonic());
        }
    }

    private static final Function[] ALL_FUNCTIONS = new Function[]{
        // load
        DLFunction.INSTANCE,
        DLMFunction.INSTANCE,
        DLNFunction.INSTANCE,
        LAFunction.INSTANCE,
        LAQWFunction.INSTANCE,
        LMAFunction.INSTANCE,
        LNAFunction.INSTANCE,
        LNMAFunction.INSTANCE,
        LRFunction.INSTANCE,
        LRSFunction.INSTANCE,
        LSBLFunction.INSTANCE,
        LSBOFunction.INSTANCE,
        LXFunction.INSTANCE,
        LXIFunction.INSTANCE,
        LXLMFunction.INSTANCE,
        LXMFunction.INSTANCE,
        LXSIFunction.INSTANCE,

        // store
        DSFunction.INSTANCE,
        SAQWFunction.INSTANCE,
        SAFunction.INSTANCE,
        SASFunction.INSTANCE,
        SAZFunction.INSTANCE,
        SFSFunction.INSTANCE,
        SFZFunction.INSTANCE,
        SN1Function.INSTANCE,
        SMAFunction.INSTANCE,
        SNAFunction.INSTANCE,
        SNZFunction.INSTANCE,
        SP1Function.INSTANCE,
        SRFunction.INSTANCE,
        SRSFunction.INSTANCE,
        SXFunction.INSTANCE,
        SZFunction.INSTANCE,

        // test
        CRFunction.INSTANCE,
        DTEFunction.INSTANCE,
        DTGMFunction.INSTANCE,
        MATGFunction.INSTANCE,
        MATLFunction.INSTANCE,
        MTEFunction.INSTANCE,
        MTGFunction.INSTANCE,
        MTLEFunction.INSTANCE,
        MTNEFunction.INSTANCE,
        MTNWFunction.INSTANCE,
        MTWFunction.INSTANCE,
        // TCSFunction.INSTANCE,
        TEFunction.INSTANCE,
        TEPFunction.INSTANCE,
        TGFunction.INSTANCE,
        TGMFunction.INSTANCE,
        TGZFunction.INSTANCE,
        TLEFunction.INSTANCE,
        TLEMFunction.INSTANCE,
        TLZFunction.INSTANCE,
        TMZFunction.INSTANCE,
        TMZGFunction.INSTANCE,
        TNFunction.INSTANCE,
        TNEFunction.INSTANCE,
        TNGZFunction.INSTANCE,
        TNLZFunction.INSTANCE,
        TNMZFunction.INSTANCE,
        TNOPFunction.INSTANCE,
        TNPZFunction.INSTANCE,
        TNWFunction.INSTANCE,
        TNZFunction.INSTANCE,
        TOPFunction.INSTANCE,
        TPFunction.INSTANCE,
        TPZFunction.INSTANCE,
        TPZLFunction.INSTANCE,
        // TSFunction.INSTANCE,
        TSKPFunction.INSTANCE,
        // TSSFunction.INSTANCE,
        TWFunction.INSTANCE,
        TZFunction.INSTANCE,
        // UNLKFunction.INSTANCE,

        // shift
        // jump
        DJZFunction.INSTANCE,
        HJFunction.INSTANCE,
        HLTJFunction.INSTANCE,
        JFunction.INSTANCE,
        JBFunction.INSTANCE,
        JCFunction.INSTANCE,
        JDFFunction.INSTANCE,
        JFOFunction.INSTANCE,
        JFUFunction.INSTANCE,
        JGDFunction.INSTANCE,
        JKFunction.INSTANCE,
        JMGIFunction.INSTANCE,
        JNBFunction.INSTANCE,
        JNCFunction.INSTANCE,
        JNDFFunction.INSTANCE,
        JNFunction.INSTANCE,
        JNFOFunction.INSTANCE,
        JNFUFunction.INSTANCE,
        JNOFunction.INSTANCE,
        JNSFunction.INSTANCE,
        JNZFunction.INSTANCE,
        JOFunction.INSTANCE,
        JPFunction.INSTANCE,
        JPSFunction.INSTANCE,
        JZFunction.INSTANCE,
        LMJFunction.INSTANCE,
        SLJFunction.INSTANCE,
        // logical
        ANDFunction.INSTANCE,
        MLUFunction.INSTANCE,
        ORFunction.INSTANCE,
        XORFunction.INSTANCE,
        // storage
        // string
        // addressSpace
        // procedureControl
        // queueing
        // activity
        // stack
        // interrupt
        // system
        // dayclock
        // upi
        // instrumentation
        // special
        DCBFunction.INSTANCE,
        EXFunction.INSTANCE,// TODO implement
        EXRFunction.INSTANCE,// TODO implement
        NOPFunction.INSTANCE,
        RNGBFunction.INSTANCE,// TODO implement
        RNGIFunction.INSTANCE,// TODO implement
    };


    private static boolean _isInitialized = false;
    private static final HashMap<Integer, Function> BASIC_MODE_TOP_LEVEL = new HashMap<>();
    private static final HashMap<Integer, Function> EXTENDED_MODE_TOP_LEVEL = new HashMap<>();

    public static void clear() {
        synchronized (FunctionTable.class) {
            BASIC_MODE_TOP_LEVEL.clear();
            EXTENDED_MODE_TOP_LEVEL.clear();
            _isInitialized = false;
        }
    }

    private static void ingestFunction(
        final HashMap<Integer, Function> topLevel,
        final Function function,
        final FunctionCode functionCode
    ) {
        Integer f = functionCode.getFField();
        Integer j = functionCode.getJField();
        Integer a = functionCode.getAField();

        var existing = topLevel.get(f);
        if (j == null && a == null) {
            // f-only sensitive
            if (existing != null) {
                if (existing instanceof SubFunction) {
                    throw new CollisionException(existing, function);
                }
                if (existing != function) {
                    throw new CollisionException(existing, function);
                }
            }
            topLevel.put(f, function);
            return;
        }

        // Must be JSub or ASub
        if (j != null) {
            JSubFunction jSub;
            if (existing == null) {
                jSub = new JSubFunction(String.format("f%03oj", f));
                topLevel.put(f, jSub);
            } else if (existing instanceof JSubFunction existingJSub) {
                jSub = existingJSub;
            } else {
                throw new CollisionException(existing, function);
            }
            jSub.putFunction(functionCode, function);
        } else if (a != null) {
            ASubFunction aSub;
            if (existing == null) {
                aSub = new ASubFunction(String.format("f%03oa", f));
                topLevel.put(f, aSub);
            } else if (existing instanceof ASubFunction existingASub) {
                aSub = existingASub;
            } else {
                throw new CollisionException(existing, function);
            }
            aSub.putFunction(functionCode, function);
        }
    }

    private static void initializeLookups() {
        try {
            for (var func : ALL_FUNCTIONS) {
                if (func.getBasicModeFunctionCode() != null) {
                    ingestFunction(BASIC_MODE_TOP_LEVEL, func, func.getBasicModeFunctionCode());
                }
                if (func.getExtendedModeFunctionCode() != null) {
                    ingestFunction(EXTENDED_MODE_TOP_LEVEL, func, func.getExtendedModeFunctionCode());
                }
            }
            _isInitialized = true;
        } catch (FunctionTable.CollisionException ex) {
            for (var e : BASIC_MODE_TOP_LEVEL.entrySet()) {
                var fc = e.getKey();
                var func = e.getValue();
                System.out.printf("BM:%03o: %s%n", fc, func.getMnemonic());
                if (func instanceof SubFunction sf) {
                    sf.debug("  ");
                }
            }
            for (var e : EXTENDED_MODE_TOP_LEVEL.entrySet()) {
                var fc = e.getKey();
                var func = e.getValue();
                System.out.printf("BM:%03o: %s%n", fc, func.getMnemonic());
                if (func instanceof SubFunction sf) {
                    sf.debug("  ");
                }
            }
            throw ex;
        }
    }

    public static Function lookupFunction(
        final DesignatorRegister dReg,
        final InstructionWord iWord
    ) throws InvalidInstructionInterrupt {
        synchronized (FunctionTable.class) {
            if (!_isInitialized) {
                initializeLookups();
            }

            var topLevel = dReg.isBasicModeEnabled() ? BASIC_MODE_TOP_LEVEL : EXTENDED_MODE_TOP_LEVEL;
            var func = topLevel.get(iWord.getF());
            if (func == null) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
            }

            if (func instanceof SubFunction sf) {
                return sf.lookupFunction(iWord);
            }
            return func;
        }
    }
}
