/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.functions.FunctionTable;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Positive Zero instruction
 * (TPZ) skips if (U) = +0.
 * Extended Mode only, f=050, j=0, a=02.
 */
public class TestTPZFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tpzEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 02, x, h, i, b, d);
    }

    @Test
    public void testTPZ_PositiveZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0L }; // Positive zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0, 0777, new AbsoluteAddress(2, 0), bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZ skips on +0
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPZ_NegativeZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0_777777_777777L }; // Negative zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_777, new AbsoluteAddress(2, 0), bank2);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZ does NOT skip on -0
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPZ_PositiveNonZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 1L }; // Positive non-zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0, 0777, new AbsoluteAddress(2, 0), bank2);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZ does NOT skip on +NZ
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPZ_NegativeNonZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tpzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0_777777_777776L }; // -1

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_777, new AbsoluteAddress(2, 0), bank2);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // TPZ does NOT skip on -NZ
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
