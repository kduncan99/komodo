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
 * Signal Condition instruction
 * (SGNL) Posts a signal interrupt ISW0 == U and SSF == 1
 */
public class SGNLFunction extends Function {

    public static final SGNLFunction INSTANCE = new SGNLFunction();

    private SGNLFunction() {
        super("SGNL");
        setBasicModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_17));
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_17));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getImmediateOperand();
        if (!engine.getDesignatorRegister().isBasicModeEnabled()) {
            operand &= 0_7777;
        }
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }
        engine.postInterrupt(new SignalInterrupt(SignalInterrupt.SignalType.Signal, operand));
        return true;
    }
}
