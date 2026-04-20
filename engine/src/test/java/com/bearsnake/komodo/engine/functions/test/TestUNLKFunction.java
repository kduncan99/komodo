/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for UNLK function.
 */
public class TestUNLKFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long unlkEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_14, 0_04, x, h, i, b, d);
    }

    @Test
    public void testUNLK_EM() throws MachineInterrupt {
        var code = new long[] {
            unlkEM(0, 0, 0, 2, 42),      // UNLK (U)
            0,                           // Normal stop
        };

        var data = new long[01000];
        // Bit 5 is the S1 bit 1 (mask 0_010000_000000L)
        // Set bit 5 and some other bits to ensure only bit 5 is cleared
        data[42] = 0_010000_123456L;

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_0777, AbsoluteAddress.construct(1, 0), bank1);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_000000_123456L, bank1.get(42));
    }

    @Test
    public void testUNLK_AllOnes_EM() throws MachineInterrupt {
        var code = new long[] {
            unlkEM(0, 0, 0, 2, 42),      // UNLK (U)
            0,                           // Normal stop
        };

        var data = new long[50];
        data[42] = 0_777777_777777L;    // All 36 bits set

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_0777, AbsoluteAddress.construct(1, 0), bank1);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        // UNLK clears S1 (top 6 bits)
        // Expected: 0_007777_777777L
        assertEquals(0_007777_777777L, bank1.get(42));
    }
}
