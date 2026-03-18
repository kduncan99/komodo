/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import static com.bearsnake.komodo.engine.Constants.*;

/**
 * Store a Quarter Word instruction
 * (SAQW) stores the lower 9 bits of A(a) into the quarter word at U as indicated by the byte offset
 * in bits 4 and 5 of X(x). The modifier portion of X(x) applies to developing U per normal.
 * Results are undefined if the i field of the instruction is set.
 *
 */
public class SAQWFunction extends Function {

    public static final SAQWFunction INSTANCE = new SAQWFunction();

    private SAQWFunction() {
        super("SAQW");
        var fc = new FunctionCode(0_07).setJField(0_05);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ic = engine.getCurrentInstruction();
        var xIndex = ic.getX();
        var xReg = engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserXRegisterIndex(xIndex));
        var jField = switch (xReg.getS1() & 0x03) {
            case 0 -> JFIELD_Q1;
            case 1 -> JFIELD_Q2;
            case 2 -> JFIELD_Q3;
            case 3 -> JFIELD_Q4;
            default -> JFIELD_U;// should never happen, but default to do-nothing
        };

        var aRegIndex = engine.getExecOrUserARegisterIndex(ic.getA());
        var valueToStore = engine.getGeneralRegisterSet().getRegister(aRegIndex).getW();

        engine.getDesignatorRegister().setQuarterWordModeEnabled(true);
        boolean result = engine.storePartialWordOperand(valueToStore, jField);
        engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
        return result;
    }
}
