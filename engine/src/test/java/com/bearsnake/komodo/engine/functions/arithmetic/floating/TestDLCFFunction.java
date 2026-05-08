/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDLCFFunction extends FunctionUnitTest {

    private long dlcfBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 015, a, x, h, i, u);
    }

    private long dlcfEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 015, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testDLCF_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = dlcfEM(8, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_000543212345L;
        data[6] = 0_671122334455L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(8).setW(0_02101L);
        _engine.getExecOrUserARegister(9).setW(0_100007_777777L);
        _engine.getExecOrUserARegister(10).setW(0_543234_456765L);

        run();

        assertEquals(0_02101L, _engine.getExecOrUserARegister(8).getW());
        assertEquals(0_2104_54321234L, _engine.getExecOrUserARegister(9).getW());
        assertEquals(0_567112_233445L, _engine.getExecOrUserARegister(10).getW());
    }
}
