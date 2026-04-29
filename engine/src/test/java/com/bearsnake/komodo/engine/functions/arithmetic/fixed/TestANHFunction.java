/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestANHFunction extends FunctionUnitTest {

    private long anhEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(072, 005, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testANH_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anhEM(4, 0, 0, 0, 2, 0_1004);
        code[1] = anhEM(6, 0, 0, 0, 2, 0_1006);
        code[2] = anhEM(8, 0, 0, 0, 2, 0_1010);
        code[3] = 0;
        data[4] = 0_001000_377000L;
        data[6] = 0_777775_777775L;
        data[8] = 0_000004_000000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_001000_000123L);
        _engine.getExecOrUserARegister(6).setW(0_777775_000005L);
        _engine.getExecOrUserARegister(8).setW(0_000004_777777L);

        run();

        assertEquals(0_000000_401122L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_000000_000007L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_000000_777777L, _engine.getExecOrUserARegister(8).getW());
    }
}
