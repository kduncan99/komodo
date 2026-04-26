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

public class TestTNFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tnImm(long j, long a, long x, long u) {
        return fjaxu(061, j, a, x, u);
    }

    private long tnBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(061, j, a, x, h, i, u);
    }

    private long tnEM(long j, long x, long h, long i, long b, long d) {
        // Extended mode TN has A field = 014 (12 decimal), f=050
        return fjaxhibd(050, j, 014, x, h, i, b, d);
    }

    @Test
    public void testTN_NegativeZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tnEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), // Use JFIELD_W to fetch from B2+0
            0,
            0
        };

        var data = new long[]{ 0_777777_777777L }; // Negative zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.construct(2, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // Negative zero IS negative
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTN_PositiveZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tnEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] { 0L }; // Positive zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.construct(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // Positive zero is NOT negative
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTN_NegativeNonZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tnEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] { 0_777777_777776L }; // -1


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.construct(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTN_PositiveNonZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tnEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] { 1L }; // Positive non-zero


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.construct(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            tnImm(Constants.JFIELD_U, 0, 0, 0), // U=0
            0,
            0,
            };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNImmediate_NoSkip_BM() throws MachineInterrupt {
        var code = new long[] {
            tnImm(Constants.JFIELD_U, 0, 0, 1), // U=1
            0,
            0,
            };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNImmediate_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            tnImm(Constants.JFIELD_U, 0, 3, 0), // U = 0 + X3
            0,
            0
        };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // U = 0 + 0_777777_777776 (-1)
        _engine.getExecOrUserXRegister(3).setXM(0_777777_777776L);

        run();

        // In BM, immediate operand is truncated to 18 bits.
        // 0_777777_777776 & 0_777777 = 0_777776.
        // Bit 35 of 0_777776 is 0. So NO skip.
        assertEquals(0_022001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            tnBM(Constants.JFIELD_W, 0, 0, 0, 1, 022004), // Indirect via 022004
            0,
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005), // Address 022003
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005), // Address 022004 -> points to 022005
            0_777777_777777L, // Address 022005 -> value -0
            0
        };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
