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
 * Unit tests for TSS and TS functions.
 */
public class TestTSSFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tssEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_17, 0_01, x, h, i, b, d);
    }

    private long tssBM(long x, long u) {
        return fjaxu(0_73, 0_17, 0_01, x, u);
    }

    private long tsBM(long x, long u) {
        return fjaxu(0_73, 0_17, 0_00, x, u);
    }

    @Test
    public void testTSS_Skip_EM() throws MachineInterrupt {
        var code = new long[] {
            tssEM(0, 0, 0, 2, 42),      // TSS (U)
            0,                           // Skipped if bit 5 (S1 bit 1) is 0
            0,                           // Normal stop
        };

        var data = new long[50];
        data[42] = 0L;                  // Initial value, bit 5 is 0

        loadBaseRegister((short) 0, false, 0, 0_177777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_177777, AbsoluteAddress.encodeToLong(0, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // Check if bit 5 was set (0_010000_000000L)
        assertEquals(0_010000_000000L, data[42]);
        // Check if next instruction was skipped (PC = 2)
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTSS_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tssEM(0, 0, 0, 2, 42),      // TSS (U)
            0,                           // Executed if bit 5 is 1 (Normal stop)
            0,
        };

        var data = new long[50];
        data[42] = 0_010000_000000L;    // Initial value, bit 5 is 1

        loadBaseRegister((short) 0, false, 0, 0_177777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_177777, AbsoluteAddress.encodeToLong(0, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // Check if bit 5 is still set
        assertEquals(0_010000_000000L, data[42]);
        // Check if next instruction was NOT skipped (PC = 1)
        assertEquals(0_1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTSS_Skip_BM() throws MachineInterrupt {
        var code = new long[0_20000];
        code[0] = tssBM(0, 42);
        code[42] = 0L;                  // Initial value, bit 5 is 0

        loadBaseRegister((short) 14, false, 0, 0_177777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        run();

        // Check if bit 5 was set
        assertEquals(0_010000_000000L, code[42]);
        // Check if next instruction was skipped
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTSS_Success_EM() throws MachineInterrupt {
        var code = new long[] {
            tssEM(0, 0, 0, 2, 42),
            0,
            0
        };

        var data = new long[50];
        data[42] = 0L;                  // bit 5 is 0

        loadBaseRegister((short) 0, false, 0, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_1777, AbsoluteAddress.encodeToLong(0, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // Check if bit 5 was set
        assertEquals(0_010000_000000L, data[42]);
        // Check if next instruction was reached
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
