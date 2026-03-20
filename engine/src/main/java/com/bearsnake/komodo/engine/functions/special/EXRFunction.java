/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.store.*;
import com.bearsnake.komodo.engine.functions.test.*;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.util.HashSet;
import java.util.Set;

/**
 * Execute Repeated Instruction
 * (EXR) Fetches the instruction at the developed operand address and executes it repeatedly,
 * according to the repeat counter in R1.
 * See 6.27.2 for list of target instructions which are allowed.
 */
public class EXRFunction extends Function {

    public static final EXRFunction INSTANCE = new EXRFunction();

    public static final Set<Class<?>> ALLOWED_TARGET_INSTRUCTIONS = new HashSet<>();
    static {
        ALLOWED_TARGET_INSTRUCTIONS.add(SAFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SNAFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SMAFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SRFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SNZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SP1Function.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SN1Function.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SFSFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SFZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SASFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SAZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(SXFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(DSFunction.class);
        // TODO update this list (see 6.27.2)
        //ALLOWED_TARGET_INSTRUCTIONS.add(SSFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(RNGIFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(RNGBFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(AAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ANAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(AMAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ANMAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ADEFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DADEFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(SDEFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DSDEFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(AHFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ANHFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ATFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ANTFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(AXFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(ANXFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DANFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(MSIFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(FAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(FANFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(FMFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(FDFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DFAFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DFANFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DFMFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(DFDFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TEPFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TOPFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNOPFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TGZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TPZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TPFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TMZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TMZGFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNLZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TLZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TPZLFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNMZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNPZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNGZFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TSKPFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TLEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TGFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TWFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TNWFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MTEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MTNEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MTLEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MTGFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MTWFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MTNWFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MATLFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(MATGFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(DTEFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(DTGMFunction.class);
        ALLOWED_TARGET_INSTRUCTIONS.add(TGMFunction.class);
        // TODO update this list (see 6.27.2)
        //ALLOWED_TARGET_INSTRUCTIONS.add(TESFunction.class);
        //ALLOWED_TARGET_INSTRUCTIONS.add(TNESFunction.class);
    }

    private EXRFunction() {
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
        var operand = engine.getOperand(false, false, false, false, false);

        // If the repeat counter is zero, there's nothing to do.
        var r1Reg = engine.getExecOrUserRRegister(1);
        if (r1Reg.isZero()) {
            return true;
        }

        // Ensure target function is valid
        var targetFunc = Function.lookup(engine.getDesignatorRegister(), operand);
        if (targetFunc == null || !ALLOWED_TARGET_INSTRUCTIONS.contains(targetFunc.getClass())) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        }

        // Set the target instruction as the current instruction, set EXRF, and return false
        // so that cycle() can continue doing it's thing, which is to execute this target instruction
        // one ore more times.
        engine.getActivityStatePacket().setCurrentInstruction(operand);
        engine.getActivityStatePacket().getIndicatorKeyRegister().setExecuteRepeatedInstruction(true);
        return false;
    }
}
