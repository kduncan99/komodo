/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestANTFunction extends FunctionUnitTest {

    private long antEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(072, 007, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testANT_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var danta = new long[0_1000];

        code[0] = antEM(4, 0, 0, 0, 2, 0_1004);
        code[2] = 0;
        danta[4] = 0_0003_3000_7777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, danta);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_5000_2000_7777L);

        run();

        assertEquals(0_4775_6777_0000L, _engine.getExecOrUserARegister(4).getW());
    }
}
