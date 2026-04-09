/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJNSFunction extends FunctionUnitTest {

    private long jnsBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_72, 0_03, a, x, h, i, u);
    }

    private long jnsBM(long a, long u) {
        return jnsBM(a, 0, 0, 0, u);
    }

    private long jnsEM(long a, long x, long u) {
        return fjaxu(0_72, 0_03, a, x, u);
    }

    private long jnsEM(long a, long u) {
        return jnsEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJNS_Jump_BM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = jnsBM(5, 0_1000); // JNS if A5 is negative, jump to 0_1000

        var bank0 = new ArraySlice(code);
        loadBaseRegister(13, false, 0_1000, 0_2777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        // A5 = 0_400000_000000L (Negative)
        // Shift left circular by 1: 0_000000_000001L...
        // this happens twice, once with jump to the same instruction, the second time without a jump (goes to NI)
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter(), "Should jump");
        assertEquals(0_000000_000002L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJNS_Jump_EM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = jnsEM(5, 0_1000); // JNS if A5 is negative, jump to 0_1000

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_2777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        // A5 = 0_400000_000000L (Negative)
        // Shift left circular by 1: 0_000000_000001L...
        // this happens twice, once with jump to the same instruction, the second time without a jump (goes to NI)
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter(), "Should jump");
        assertEquals(0_000000_000002L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }
}
