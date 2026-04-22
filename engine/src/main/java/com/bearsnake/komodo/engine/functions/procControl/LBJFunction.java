/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.AddressingExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Bank and Jump function
 * (LBJ) Loads a bank (selected from B12:B15 for Basic Mode, B0 for Extended Mode)
 * and then jumps to the address in the U field.
 */
public class LBJFunction extends LXJFunction {

    public static final LBJFunction INSTANCE = new LBJFunction();

    public LBJFunction() {
        super("LBJ");
        setBasicModeFunctionCode(new FunctionCode(0_07).setJField(0_17));
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = (int)engine.getJumpOperand();
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var xa = ci.getA();
        if (xa == 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
        }
        var xaReg = engine.getExecOrUserARegister(xa);
        var xaValue = xaReg.getW();

        var baseRegisterNumber = (short) ((xaValue >> 33) & 03);
        return executeCommon(engine, operand, xaReg, baseRegisterNumber);
    }
}
