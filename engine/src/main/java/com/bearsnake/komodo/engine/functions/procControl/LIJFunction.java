/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load I-Bank and Jump function
 * (LIJ) Loads a bank (selected from B12:B13) and then jumps to the address in the U field.
 */
public class LIJFunction extends LXJFunction {

    public static final LIJFunction INSTANCE = new LIJFunction();

    public LIJFunction() {
        super("LIJ");
        setBasicModeFunctionCode(new FunctionCode(0_07).setJField(0_13));
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = (int)engine.getJumpOperand();
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var dr = engine.getDesignatorRegister();
        var ci = engine.getCurrentInstruction();
        var xa = ci.getA();
        if (xa == 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
        }
        var xaReg = engine.getExecOrUserARegister(xa);

        var baseRegisterNumber = (short) (dr.getBasicModeBaseRegisterSelection() ? 13 : 11);
        return executeCommon(engine, operand, xaReg, baseRegisterNumber);
    }
}
