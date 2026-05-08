/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.DesignatorRegister;
import com.bearsnake.komodo.engine.functions.actControl.LAEFunction;
import com.bearsnake.komodo.engine.functions.actControl.LDFunction;
import com.bearsnake.komodo.engine.functions.actControl.SDFunction;
import com.bearsnake.komodo.engine.functions.actControl.URFunction;
import com.bearsnake.komodo.engine.functions.addrSpace.DABTFunction;
import com.bearsnake.komodo.engine.functions.addrSpace.LBEFunction;
import com.bearsnake.komodo.engine.functions.addrSpace.LBUFunction;
import com.bearsnake.komodo.engine.functions.arithmetic.decimal.*;
import com.bearsnake.komodo.engine.functions.arithmetic.fixed.*;
import com.bearsnake.komodo.engine.functions.arithmetic.floating.*;
import com.bearsnake.komodo.engine.functions.intControl.AAIJFunction;
import com.bearsnake.komodo.engine.functions.intControl.ERFunction;
import com.bearsnake.komodo.engine.functions.intControl.PAIJFunction;
import com.bearsnake.komodo.engine.functions.intControl.SGNLFunction;
import com.bearsnake.komodo.engine.functions.jump.*;
import com.bearsnake.komodo.engine.functions.load.*;
import com.bearsnake.komodo.engine.functions.logical.*;
import com.bearsnake.komodo.engine.functions.procControl.*;
import com.bearsnake.komodo.engine.functions.shift.*;
import com.bearsnake.komodo.engine.functions.special.*;
import com.bearsnake.komodo.engine.functions.stack.BUYFunction;
import com.bearsnake.komodo.engine.functions.stack.SELLFunction;
import com.bearsnake.komodo.engine.functions.system.IPCFunction;
import com.bearsnake.komodo.engine.functions.system.SPIDFunction;
import com.bearsnake.komodo.engine.functions.system.SYSCFunction;
import com.bearsnake.komodo.engine.functions.test.*;
import com.bearsnake.komodo.engine.functions.store.*;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.functions.system.IARFunction;

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

    public static final Function[] ALL_FUNCTIONS = new Function[]{
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

        // fixed
        AAFunction.INSTANCE,
        ADD1Function.INSTANCE,
        AHFunction.INSTANCE,
        AMAFunction.INSTANCE,
        ANAFunction.INSTANCE,
        ANHFunction.INSTANCE,
        ANMAFunction.INSTANCE,
        ANTFunction.INSTANCE,
        ANUFunction.INSTANCE,
        ANXFunction.INSTANCE,
        ATFunction.INSTANCE,
        AUFunction.INSTANCE,
        AXFunction.INSTANCE,
        // BAOFunction.INSTANCE,
        DAFunction.INSTANCE,
        DANFunction.INSTANCE,
        DECFunction.INSTANCE,// TODO needs unit tests
        DEC2Function.INSTANCE,// TODO needs unit tests
        DFFunction.INSTANCE,// TODO needs unit tests
        DIFunction.INSTANCE,// TODO needs unit tests
        DSFFunction.INSTANCE,// TODO needs unit tests
        ENZFunction.INSTANCE,// TODO needs unit tests
        INCFunction.INSTANCE,
        INC2Function.INSTANCE,
        MFFunction.INSTANCE,// TODO needs unit tests
        MIFunction.INSTANCE,// TODO needs unit tests
        MSIFunction.INSTANCE,// TODO needs unit tests
        SUB1Function.INSTANCE,

        // float
        CDUFunction.INSTANCE,
        // DFAFunction.INSTANCE,
        // DFANFunction.INSTANCE,
        // DFDFunction.INSTANCE,
        DFMFunction.INSTANCE,
        DFUFunction.INSTANCE,
        DLCFFunction.INSTANCE, // (also DFP)
        // FAFunction.INSTANCE,
        // FANFunction.INSTANCE,
        // FCLFunction.INSTANCE,
        // FDFunction.INSTANCE,
        // FELFunction.INSTANCE,
        FMFunction.INSTANCE,
        LCFFunction.INSTANCE,
        LUFFunction.INSTANCE,
        MCDUFunction.INSTANCE,

        // decimal
        ADEFunction.INSTANCE,
        BDEFunction.INSTANCE,
        DADEFunction.INSTANCE,
        DDEIFunction.INSTANCE,
        // DEBFunction.INSTANCE,
        DEIFunction.INSTANCE,
        DIDEFunction.INSTANCE,
        DSDEFunction.INSTANCE,
        // EDDEFunction.INSTANCE,
        IDEFunction.INSTANCE,
        SDEFunction.INSTANCE,

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
        TCSFunction.INSTANCE,
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
        TSFunction.INSTANCE,
        TSKPFunction.INSTANCE,
        TSSFunction.INSTANCE,
        TWFunction.INSTANCE,
        TZFunction.INSTANCE,
        UNLKFunction.INSTANCE,

        // shift
        DLSCFunction.INSTANCE,
        DSAFunction.INSTANCE,
        DSCFunction.INSTANCE,
        DSLFunction.INSTANCE,
        LDSCFunction.INSTANCE,
        LDSLFunction.INSTANCE,
        LSCFunction.INSTANCE,
        LSSCFunction.INSTANCE,
        LSSLFunction.INSTANCE,
        SSAFunction.INSTANCE,
        SSCFunction.INSTANCE,
        SSLFunction.INSTANCE,

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
        // BBNFunction.INSTANCE,
        // BICFunction.INSTANCE,
        // BICLFunction.INSTANCE,
        // BIMFunction.INSTANCE,
        // BIMLFunction.INSTANCE,
        // BIMTFunction.INSTANCE,
        // BNFunction.INSTANCE,
        // BTFunction.INSTANCE,

        // string
        // LSFunction.INSTANCE,
        // LSAFunction.INSTANCE,
        // SSFunction.INSTANCE,
        // TESFunction.INSTANCE,
        // TNESFunction.INSTANCE,

        // addressSpace
        DABTFunction.INSTANCE,// TODO needs unit tests
        LBEFunction.INSTANCE,// TODO needs unit tests
        // LBEDFunction.INSTANCE,   PP=0
        // LBNFunction.INSTANCE,
        LBUFunction.INSTANCE,// TODO needs unit tests
        // LBUDFunction.INSTANCE,   PP=0
        // SBEDFunction.INSTANCE,   PP=0
        // SBUFunction.INSTANCE,
        // SBUDFunction.INSTANCE,   PP=0
        // TRAFunction.INSTANCE,
        // TRARSFunction.INSTANCE,  PP=0
        // TVAFunction.INSTANCE,
        // We do not support VIEW

        // procedureControl
        CALLFunction.INSTANCE,// TODO needs unit tests
        GOTOFunction.INSTANCE,// TODO needs unit tests
        LBJFunction.INSTANCE,// TODO needs unit tests
        LDJFunction.INSTANCE,// TODO needs unit tests
        LIJFunction.INSTANCE,// TODO needs unit tests
        LOCLFunction.INSTANCE,
        RTNFunction.INSTANCE,// TODO needs more unit tests

        // queueing
        // DEPOSITQBFFunction.INSTANCE,
        // DEQFunction.INSTANCE,
        // DEQWFunction.INSTANCE,
        // ENQFunction.INSTANCE,
        // ENQFFunction.INSTANCE,
        // WITHDRAWQBFunction.INSTANCE,

        // activityControl
        // ACELFunction.INSTANCE,   PP=0,1,2
        // DCELFunction.INSTANCE,   PP=0,1,2
        // KCHGFunction.INSTANCE,   PP=0
        LAEFunction.INSTANCE,// TODO needs unit tests
        LDFunction.INSTANCE,
        // LPDFunction.INSTANCE,
        // LUDFunction.INSTANCE,
        SDFunction.INSTANCE,
        // SKQTFunction.INSTANCE,   PP=0,1
        // SPDFunction.INSTANCE,
        // SUDFunction.INSTANCE,
        URFunction.INSTANCE,// TODO needs unit tests

        // stack
        BUYFunction.INSTANCE,
        SELLFunction.INSTANCE,

        // interruptControl
        AAIJFunction.INSTANCE,
        ERFunction.INSTANCE,
        PAIJFunction.INSTANCE,
        SGNLFunction.INSTANCE,

        // system
        IARFunction.INSTANCE,
        IPCFunction.INSTANCE,// TODO needs unit tests
        SPIDFunction.INSTANCE,// TODO needs unit tests
        SYSCFunction.INSTANCE,// TODO needs unit tests

        // dayclock
        // LMCFunction.INSTANCE,    PP=0
        // LRDFunction.INSTANCE,    PP=0
        // RDCFunction.INSTANCE,    PP=0
        // RMDFunction.INSTANCE,
        // SDMFFunction.INSTANCE,   PP=0
        // SDMNFunction.INSTANCE,   PP=0
        // SDMSFunction.INSTANCE,   PP=0
        // SMDFunction.INSTANCE,    PP=0

        // upi
        // ACKFunction.INSTANCE,    PP=0
        // SENDFunction.INSTANCE,   PP=0

        // instrumentation
        // CJHEFunction.INSTANCE,   PP=0
        // LBRXFunction.INSTANCE,   PP=0
        // SJHFunction.INSTANCE,    PP=0

        // special
        DCBFunction.INSTANCE,
        EXFunction.INSTANCE,
        EXRFunction.INSTANCE,
        NOPFunction.INSTANCE,
        RNGBFunction.INSTANCE,
        RNGIFunction.INSTANCE
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
            for (var fx = 0; fx < ALL_FUNCTIONS.length; fx++) {
                var func = ALL_FUNCTIONS[fx];
                func.setFunctionTableIndex(fx);

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
        final long instWord
    ) throws InvalidInstructionInterrupt {
        synchronized (FunctionTable.class) {
            if (!_isInitialized) {
                initializeLookups();
            }

            var topLevel = dReg.isBasicModeEnabled() ? BASIC_MODE_TOP_LEVEL : EXTENDED_MODE_TOP_LEVEL;
            var func = topLevel.get(InstructionWord.getF(instWord));
            if (func == null) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
            }

            if (func instanceof SubFunction sf) {
                return sf.lookupFunction(instWord);
            }
            return func;
        }
    }
}
