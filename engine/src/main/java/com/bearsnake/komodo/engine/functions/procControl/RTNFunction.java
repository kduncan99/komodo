/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Return function
 * (RTN) uses the RCS to determine how to re-establish a previously saved environment,
 * loading the appropriate bank and then jumping to the address in the U field.
 */
public class RTNFunction extends Function {

    public static final RTNFunction INSTANCE = new RTNFunction();

    private RTNFunction() {
        super("RTN");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_03));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var oldAddress = engine.getProgramAddressRegister().toCompositeValue();
        engine.bankManipulation(this, (short) 0, (short) 0, (short) 0, 0, null, 0L, null);
        engine.createJumpHistoryEntry(oldAddress);
        return true;
    }
}
