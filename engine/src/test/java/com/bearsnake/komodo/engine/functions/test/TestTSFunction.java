/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.TestAndSetInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test and Set instruction
 * (TS) If U:05 is set, take an interrupt. Otherwise, set U:05 and continue.
 * f=073, j=017, a=00
 */
public class TestTSFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tsEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_17, 0, x, h, i, b, d);
    }

    private long tsBM(long x, long u) {
        return fjaxu(0_73, 0_17, 0, x, u);
    }

    @Test
    public void testTS_SetsBit_EM() throws MachineInterrupt {
        var code = new long[] {
            tsEM(0, 0, 0, 2, 42),      // TS (U)
            0
        };

        var data = new long[100];
        data[42] = 0_000000_000000L;   // (U) bit 5 is clear


        loadBaseRegister((short) 0, false, 0, 0_777777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_777777, AbsoluteAddress.construct(0, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        run();

        // Check if bit 5 was set (0_010000_000000L)
        assertEquals(0_010000_000000L, data[42]);
    }

    @Test
    public void testTS_Interrupts_EM() {
        var code = new long[] {
            tsEM(0, 0, 0, 2, 42),      // TS (U)
            0,
        };

        var data = new long[100];
        data[42] = 0_010000_000000L;   // (U) bit 5 is already set


        loadBaseRegister((short) 0, false, 0, 0_777777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_777777, AbsoluteAddress.construct(0, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        assertThrows(TestAndSetInterrupt.class, this::run);
    }

    @Test
    public void testTS_SetsBit_BM() throws MachineInterrupt {
        var code = new long[0_20000];
        code[0] = tsBM(0, 0_100);

        loadBaseRegister((short) 12, false, 0, 0_177777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false); // Selects 12, 14, 13, 15

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        run();

        // Check if bit 5 was set
        assertEquals(0_010000_000000L, code[0_100]);
    }

    @Test
    public void testTS_Interrupts_BM() {
        var code = new long[0_20000];
        code[0] = tsBM(0, 0_100);
        code[0_100] = 0_010000_000000L;

        loadBaseRegister((short) 13, false, 0, 0_177777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false); // Selects 12, 14, 13, 15

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        assertThrows(TestAndSetInterrupt.class, this::run);
    }
}
