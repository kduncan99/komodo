/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Subtract Decimal instruction
 * (SDE) Subtracts decimal (U) from decimal A(a) and stores the result in A(a).
 */
public class TestSDEFunction extends TestDecimalFunction {

    private long sdeBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_07, 0_02, a, x, h, i, u);
    }

    private long sde(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_07, 0_02, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testSDE_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = sde(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = decWord(0, 1, 1, 1, 1, 1, 1, 1, POSITIVE_SIGN);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getExecOrUserARegister(4).setW(decWord(1, 3, 4, 5, 6, 7, 8, 9, POSITIVE_SIGN));
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_Negative_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = sdeBM(4, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 5, 0, NEGATIVE_SIGN);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 0, 1, 0, 0, POSITIVE_SIGN));
        _engine.getDesignatorRegister().setBasicModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(decWord(0, 0, 0, 0, 0, 1, 5, 0, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }
}
