/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTSKPFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tskpEM(long j, long x, long h, long i, long b, long d) {
        // Extended mode TSKIP has A field = 017
        return fjaxhibd(050, j, 017, x, h, i, b, d);
    }

    @Test
    public void testTSKP_EM() throws MachineInterrupt {
        var code = new long[] {
            tskpEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };
        var data = new long[]{ 0_123456L };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.construct(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // Should ALWAYS skip
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
