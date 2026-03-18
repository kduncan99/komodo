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
 * Test Odd Parity instruction
 * (TOP) Checks the parity of A(a) AND U, and jumps if the parity is odd.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TOPFunction extends Function {

    public static final TOPFunction INSTANCE = new TOPFunction();

    private TOPFunction() {
        super("TOP");
        var fc = new FunctionCode(0_45);
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
        var operand = engine.getOperand(false, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var value = Word36.logicalAnd(engine.getExecOrUserARegister(ci.getA()).getW(), operand);
        if ((Long.bitCount(value) & 01) != 0) {
            // increment once here. In any case, the normal cycle() operation will increment as well.
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
