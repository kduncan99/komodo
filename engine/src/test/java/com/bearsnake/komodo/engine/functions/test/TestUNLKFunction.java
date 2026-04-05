/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for UNLK function.
 */
public class TestUNLKFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
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

        var data = new long[50];
        // Bit 5 is the S1 bit 1 (mask 0_010000_000000L)
        // Set bit 5 and some other bits to ensure only bit 5 is cleared
        data[42] = 0_010000_123456L;

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // Check if bit 5 was cleared (S1 is bits 30-35, but in 1-based notation it's S1-S6)
        // High bit of S1 is bit 35 (0_400000_000000L)
        // Wait, UNLK clears S1 entirely in implementation: operand & 0_007777_777777
        // Let's re-verify bit positions.
        // Word36: S1 is bits 30-35.
        // 0_010000_000000L is indeed the "Test and Set" bit (bit 5 in S1, where S1 is 6 bits).
        // Actually, in this architecture, bit 5 is often referred to as the "Test and Set" bit.
        // Let's look at S1 mask: 077 000 000 000. S1 is the most significant 6 bits.
        // 0_010000_000000L:
        // Octal digits:
        // 0 (1 bit)
        // 01 (6 bits - S1) -> 01
        // 00 (6 bits - S2) -> 00
        // ...
        // So 0_010000_000000L is indeed S1 with its lowest bit set (which is bit 30 or bit 5 depending on numbering).
        // If UNLK does `operand & 0_007777_777777`, it clears ALL of S1.
        
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

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // UNLK clears S1 (top 6 bits)
        // Expected: 0_007777_777777L
        assertEquals(0_007777_777777L, bank1.get(42));
    }
}
