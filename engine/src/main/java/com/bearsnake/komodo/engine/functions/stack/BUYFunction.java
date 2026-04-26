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
 * Buy function
 * (BUY) Allocates a stack frame of some variable size, on a particular base register and index register.
 * The base register defines the storage containing the stack; the index register acts as the stack pointer.
 */
public class BUYFunction extends Function {

    public static final BUYFunction INSTANCE = new BUYFunction();

    private BUYFunction() {
        super("BUY");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_14).setAField(0_02));

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
        var operand = ci.getD();
        var newPtr = (oldPtr - operand - (xMod24 ? stackPtr.getSignedXI12() : stackPtr.getSignedXI())) & 0_777777;

        if (!bReg.isWithinLimits(newPtr)) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                ci.getB(),
                                                                (int)newPtr);
        }

        stackPtr.setXM(newPtr);
        return true;
    }
}
