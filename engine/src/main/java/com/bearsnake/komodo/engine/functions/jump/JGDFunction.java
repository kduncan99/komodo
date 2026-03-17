/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Jump Greater and Decrement instruction
 * (JGD) If the GRS register indicated by the concatenation of the j-field and the a-field
 * is greater than zero, we jump to the address indicated by resolving U.
 * Regardless of whether we jump, the GRS register is decremented.
 */
public class JGDFunction extends Function {

    public static final JGDFunction INSTANCE = new JGDFunction();

    private JGDFunction() {
        super("J");
        setBasicModeFunctionCode(new FunctionCode(0_70));
        setExtendedModeFunctionCode(new FunctionCode(0_70));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var grsx = ((ci.getJ() << 4) | ci.getA()) & 0177;
        var reg = engine.getGeneralRegister(grsx);
        var dr = engine.getDesignatorRegister();
        var pPriv = dr.getProcessorPrivilege();
        if (!Engine.isGRSAccessAllowed(grsx, pPriv, true)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
        }

        if (reg.isPositive() && !reg.isZero()) {
            doJump(engine, operand);
        }

        reg.decrement();
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
