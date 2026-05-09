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

public class TestFCLFunction extends FunctionUnitTest {

    private long fclBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 017, a, x, h, i, u);
    }

    private long fclEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 017, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testFCL_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = fclEM(6, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_2051_63456711L;
        data[6] = 0_456712_345677L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(6).setW(0_111111_222222L);

        run();

        assertEquals(0_251_634567114L, _engine.getExecOrUserARegister(6).getW());
    }
}
