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

/**
 * Test Positive Zero or Less Than Zero instruction
 * (TPZL) skips if (U) = +0 OR (U) < -0.
 * Extended Mode only, f=050, j=0, a=012.
 */
public class TestTPZLFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tpzlEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 012, x, h, i, b, d);
    }

    @Test
    public void testTPZL_PositiveZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzlEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[]{ 0L }; // Positive zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZL skips on +0
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPZL_NegativeZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzlEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[]{ 0_777777_777777L }; // Negative zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZL does NOT skip on -0
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPZL_PositiveNonZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzlEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[]{ 1L }; // Positive non-zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZL does NOT skip on +NZ
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPZL_NegativeNonZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzlEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[]{ 0_777777_777776L }; // -1


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZL skips on -NZ (less than -0)
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
