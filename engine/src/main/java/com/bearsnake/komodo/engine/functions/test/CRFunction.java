/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Conditional Replace instruction
 * (CR) Compares (U) with A(a), if equal, A(a+1) is stored in U and the next instruction is skipped.
 * Otherwise, the next instruction is executed.
 * This instruction is executed under storage lock for U.
 */
public class CRFunction extends Function {

    public static final CRFunction INSTANCE = new CRFunction();

    private CRFunction() {
        super("CR");
        setBasicModeFunctionCode(new FunctionCode(075).setJField(015).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(075).setJField(015));

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, false, false, false, true);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var aValue0 = engine.getExecOrUserARegister(ci.getA()).getW();
        var aValue1 = engine.getExecOrUserARegister(ci.getA() + 1).getW();

        if (Word36.compare(operand, aValue0) == 0) {
            engine.storeToCachedAddress(aValue1);
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
