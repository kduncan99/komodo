/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Go To function
 * (GOTO) Loads a bank to B0 then jumps to the address in the U field.
 */
public class GOTOFunction extends Function {

    public static final GOTOFunction INSTANCE = new GOTOFunction();

    private GOTOFunction() {
        super("GOTO");
        setExtendedModeFunctionCode(new FunctionCode(0_07).setJField(0_17).setAField(0_00));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, false, false, false);
        var oldAddress = engine.getProgramAddressRegister().getCompositeValue();
        engine.bankManipulation(this, (short) 0, (short) 0, (short) 0, 0, null, operand, null);
        engine.createJumpHistoryEntry(oldAddress);
        return true;
    }
}
