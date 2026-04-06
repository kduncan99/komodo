/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.TestAndSetInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for TSS and TS functions.
 */
public class TestTSSFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long tssEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_17, 0_01, x, h, i, b, d);
    }

    private long tsEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_17, 0_00, x, h, i, b, d);
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

        // Check if bit 5 was set (0_010000_000000L)
        assertEquals(0_010000_000000L, bank1.get(42));
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

        // Check if bit 5 is still set
        assertEquals(0_010000_000000L, bank1.get(42));
        // Check if next instruction was NOT skipped (PC = 1)
        assertEquals(0_1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTSS_Skip_BM() throws MachineInterrupt {
        var code = new long[] {
            tssBM(0, 42),               // TSS (U)
            0,                           // Skipped if bit 5 is 0
            0,                           // Normal stop
        };

        var data = new long[100];
        data[42] = 0L;                  // Initial value, bit 5 is 0

        var bank0 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        // In BM, the engine searches registers 12, 14, 13, 15 (or 13, 15, 12, 14)
        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false); // Selects 12, 14, 13, 15

        // Put code at the beginning of the bank
        bank0.set(0, code[0]);
        bank0.set(1, code[1]);
        bank0.set(2, code[2]);

        run();

        // Check if bit 5 was set
        assertEquals(0_010000_000000L, bank0.get(42));
        // Check if next instruction was skipped
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTSS_Success_EM() throws MachineInterrupt {
        var code = new long[] {
            tsEM(0, 0, 0, 2, 42),       // TS (U)
            0,                           // Normal stop
        };

        var data = new long[50];
        data[42] = 0L;                  // bit 5 is 0

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

        // Check if bit 5 was set
        assertEquals(0_010000_000000L, bank1.get(42));
        // Check if next instruction was reached
        assertEquals(0_1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTSS_Interrupt_EM() {
        var code = new long[] {
            tsEM(0, 0, 0, 2, 42),       // TS (U)
            0,
        };

        var data = new long[50];
        data[42] = 0_010000_000000L;    // bit 5 is 1

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

        assertThrows(TestAndSetInterrupt.class, this::run);
    }
}
