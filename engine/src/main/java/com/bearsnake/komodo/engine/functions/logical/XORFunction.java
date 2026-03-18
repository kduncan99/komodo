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
 * Logical XOR instruction
 * (XOR) computes the logical XOR of the content of A(a) and the developed U field.
 * The result is stored in A(a+1).
 */
public class XORFunction extends Function {

    public static final XORFunction INSTANCE = new XORFunction();

    private XORFunction() {
        super("XOR");
        var fc = new FunctionCode(0_41);
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
        var regA = engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserARegisterIndex(ci.getA()));
        var result = Word36.logicalXor(regA.getW(), operand);

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
