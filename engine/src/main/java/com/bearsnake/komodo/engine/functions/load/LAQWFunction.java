/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load A Quarter Word instruction
 * (LAQW) loads a quarter word from U into register Aa.
 * Xx.Mod is used to develop U. Xx(bit 4:5) determine which quarter word should be selected:
 *      value 00: Q1
 *      value 01: Q2
 *      value 02: Q3
 *      value 03: Q4
 * The architecture leaves it undefined as to the result of setting F0.H (x-register incrementation).
 * We will increment Xx in that case, which will result in strangeness, so don't set F0.H.
 * It is also undefined as to what happens when F0.X is zero. We will use X0 for selecting the
 * quarter-word via bits 4:5, but we will NOT use X0.Mod for developing U.
 */
public class LAQWFunction extends Function {

    public static final LAQWFunction INSTANCE = new LAQWFunction();

    private LAQWFunction() {
        super("LAQW");
        var fc = new FunctionCode(0_07).setJField(0_04);
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
        var operand = engine.getOperand(false, false, false, false, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var xReg = engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserXRegisterIndex(ci.getX()));
        operand = switch (xReg.getS1() & 03) {
            case 0 -> Word36.getQ1(operand);
            case 1 -> Word36.getQ2(operand);
            case 2 -> Word36.getQ3(operand);
            case 3 -> Word36.getQ4(operand);
            default -> throw new IllegalStateException("Unexpected value: " + (xReg.getS1() & 03)); // this isn't really possible
        };

        engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserARegisterIndex(ci.getA())).setW(operand);
        return true;
    }
}
