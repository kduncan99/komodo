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

public class TestDFUFunction extends FunctionUnitTest {

    private long dfuBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 014, a, x, h, i, u);
    }

    private long dfuEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 014, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testDFU_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = dfuEM(6, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_2163_63456742L;
        data[6] = 0_641147_534415L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_02163L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_000063_456742L, _engine.getExecOrUserARegister(7).getW());
        assertEquals(0_641147_534415L, _engine.getExecOrUserARegister(8).getW());
    }
}
