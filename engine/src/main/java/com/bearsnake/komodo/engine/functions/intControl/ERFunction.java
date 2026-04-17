/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.intControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.SignalInterrupt;

/**
 * Executive Request instruction
 * (ER) Posts a signal interrupt ISW0 == U and SSF == 0
 */
public class ERFunction extends Function {

    public static final ERFunction INSTANCE = new ERFunction();

    private ERFunction() {
        super("ER");
        setBasicModeFunctionCode(new FunctionCode(0_72).setJField(0_11));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getImmediateOperand();
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }
        engine.postInterrupt(new SignalInterrupt(SignalInterrupt.SignalType.ExecutiveRequest, operand));
        return true;
    }
}
