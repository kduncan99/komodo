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

public class TestTZFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tzImm(long j, long a, long x, long u) {
        return fjaxu(050, j, a, x, u);
    }

    private long tzBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(050, j, a, x, h, i, u);
    }

    private long tzEM(long j, long x, long h, long i, long b, long d) {
        // Extended mode TZ has A field = 06
        return fjaxhibd(050, j, 06, x, h, i, b, d);
    }

    @Test
    public void testTZ_PositiveZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), // Use JFIELD_W to fetch from B2+0
            0,
            0
        };

        var data = new long[]{ 0L }; // Positive zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTZ_NegativeZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[]{ 0_777777_777777L }; // Negative zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTZ_NonZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[]{ 1L }; // Non-zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTZImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            tzImm(Constants.JFIELD_U, 0, 0, 0), // U=0
            0,
            0
        };

        loadBaseRegister((short) 12, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTZImmediate_NoSkip_BM() throws MachineInterrupt {
        var code = new long[] {
            tzImm(Constants.JFIELD_U, 0, 0, 1), // U=1
            0,
            0
        };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTZImmediate_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            tzImm(Constants.JFIELD_U, 0, 3, 0), // U = 0 + X3
            0,
            0
        };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXM(1); // U = 0 + 1 = 1 (non-zero)

        run();

        assertEquals(0_022001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTZIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            tzBM(Constants.JFIELD_W, 0, 0, 0, 1, 022004), // Indirect via 022004
            0,
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005), // Address 022003
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005), // Address 022004 -> points to 022005
            0, // Address 022005 -> value 0
            0
        };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
