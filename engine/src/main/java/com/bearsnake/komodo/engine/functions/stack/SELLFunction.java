/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.stack;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Sell function
 * (SELL) Releases a stack frame of some variable size, on a particular base register and index register.
 * The base register defines the storage containing the stack; the index register acts as the stack pointer.
 */
public class SELLFunction extends Function {

    public static final SELLFunction INSTANCE = new SELLFunction();

    private SELLFunction() {
        super("SELL");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_14).setAField(0_03));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var bReg = engine.getBaseRegister(ci.getB());
        var stackPtr = engine.getExecOrUserXRegister(ci.getX());

        var dr = engine.getDesignatorRegister();
        var xMod24 = !dr.isBasicModeEnabled() && (dr.getProcessorPrivilege() < 2) && dr.isExecutive24BitIndexingEnabled();
        var oldPtr = xMod24 ? stackPtr.getXM24() : stackPtr.getXM();

        if (bReg.isVoid()) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                ci.getB(),
                                                                (int)oldPtr);
        }
        try {
            bReg.checkAccessLimits(oldPtr, false);
        } catch (ReferenceViolationInterrupt e) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                ci.getB(),
                                                                (int)oldPtr);
        }

        var operand = ci.getD();
        var newPtr = (oldPtr + operand + (xMod24 ? stackPtr.getSignedXI12() : stackPtr.getSignedXI())) & 0_777777;
        stackPtr.setXM(newPtr);
        return true;
    }
}
