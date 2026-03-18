/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.logical;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Masked Load Upper instruction
 * (MLU) computes (the content of R2 AND the content of A(a))
 * OR ((the logical negation of the content of R2) AND the developed U field).
 * This can be conceptualized as choosing bits from A(a) vs U based on the corresponding bit in R2.
 * The result is stored in A(a+1).
 */
public class MLUFunction extends Function {

    public static final MLUFunction INSTANCE = new MLUFunction();

    private MLUFunction() {
        super("MLU");
        var fc = new FunctionCode(0_43);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(true, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var regAValue = engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserARegisterIndex(ci.getA())).getW();
        var regR2Value = engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserRRegisterIndex(2)).getW();
        var result = (regR2Value & operand) | (Word36.logicalNot(regR2Value) & regAValue);

        var dr = engine.getDesignatorRegister();
        var pPriv = dr.getProcessorPrivilege();
        var grsx = engine.getExecOrUserARegisterIndex((ci.getA() + 1) & 017);
        if (!GeneralRegisterSet.isAccessAllowed(grsx, pPriv, true)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
        }

        engine.getGeneralRegisterSet().getRegister(grsx).setW(result);

        return true;
    }
}
