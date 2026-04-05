/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
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
public class TestTSFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
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
            0,                         // Normal stop
        };

        var data = new long[100];
        data[42] = 0_000000_000000L;   // (U) bit 5 is clear

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        run();

        // Check if bit 5 was set (0_010000_000000L)
        assertEquals(0_010000_000000L, bank2.get(42));
    }

    @Test
    public void testTS_Interrupts_EM() {
        var code = new long[] {
            tsEM(0, 0, 0, 2, 42),      // TS (U)
            0,
        };

        var data = new long[100];
        data[42] = 0_010000_000000L;   // (U) bit 5 is already set

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        assertThrows(TestAndSetInterrupt.class, this::run);
    }

    @Test
    public void testTS_SetsBit_BM() throws MachineInterrupt {
        var code = new long[] {
            tsBM(0, 0_100),            // TS (U) where U=0_100
            0,                         // Normal stop
        };

        var bank0 = new ArraySlice(new long[1000]);
        bank0.set(0, code[0]);
        bank0.set(1, code[1]);
        bank0.set(0_100, 0L);          // Initial value clear

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false); // Selects 12, 14, 13, 15

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        run();

        // Check if bit 5 was set
        assertEquals(0_010000_000000L, bank0.get(0_100));
    }

    @Test
    public void testTS_Interrupts_BM() {
        var code = new long[] {
            tsBM(0, 0_100),            // TS (U) where U=0_100
            0,
        };

        var bank0 = new ArraySlice(new long[1000]);
        bank0.set(0, code[0]);
        bank0.set(1, code[1]);
        bank0.set(0_100, 0_010000_000000L); // Initial value set

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false); // Selects 12, 14, 13, 15

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        assertThrows(TestAndSetInterrupt.class, this::run);
    }
}
