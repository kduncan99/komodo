/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJPSFunction extends FunctionUnitTest {

    private long jpsBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_72, 0_02, a, x, h, i, u);
    }

    private long jpsBM(long a, long u) {
        return jpsBM(a, 0, 0, 0, u);
    }

    private long jpsEM(long a, long x, long u) {
        return fjaxu(0_72, 0_02, a, x, u);
    }

    private long jpsEM(long a, long u) {
        return jpsEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJPS_Jump_BM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = jpsBM(5, 0_1000); // JPS if A5 is positive, jump to 0_100

        loadBaseRegister((short) 13, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_000000_000001L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter(), "Should jump");
        assertEquals(0_000000_000001L, _engine.getExecOrUserARegister(5).getW(), "Should shift");
    }

    @Test
    public void testJPS_Jump_EM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = jpsEM(5, 0_1000); // JPS if A5 is positive, jump to 0_100

        loadBaseRegister((short) 0, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_000000_000001L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter(), "Should jump");
        assertEquals(0_000000_000001L, _engine.getExecOrUserARegister(5).getW(), "Should shift");
    }
}
