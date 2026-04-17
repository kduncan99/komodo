/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
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
        var ci = engine.getCurrentInstruction();
        var xa = ci.getA();
        if (xa == 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
        }
        var xaReg = engine.getExecOrUserARegister(xa);
        var xaValue = xaReg.getW();

        var e = xaValue >> 35;
        var bdr = (xaValue >> 33) & 03;
        var ls = (xaValue >> 32) & 01;
        var is = (xaValue >> 30) & 03;
        var bdi = (xaValue >> 18) & 0_077777;

        // docs say xa.e and xa.bdi are ignored when is==2 (for RTN)...
        // TODO should we also assume xa.ls is ignored?
        var level = switch ((int) ((e << 1) | ls)) {
            case 0b00 -> 4;
            case 0b01 -> 6;
            case 0b10 -> 2;
            case 0b11 -> 0;
            default -> throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
        };

        return true;//TODO
    }
}
