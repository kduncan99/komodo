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
 * Unit tests for TCS function.
 */
public class TestTCSFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long tcsEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_17, 0_02, x, h, i, b, d);
    }

    private long tcsBM(long x, long u) {
        return fjaxu(0_73, 0_17, 0_02, x, u);
    }

    @Test
    public void testTCS_Skip_EM() throws MachineInterrupt {
        var code = new long[] {
            tcsEM(0, 0, 0, 2, 42),      // TCS (U)
            0,                           // Skipped if bit 5 (S1 bit 1) is 1
            0,                           // Normal stop
        };

        var data = new long[50];
        // Bit 5 is set, and some other bits set to ensure only S1 is cleared
        data[42] = 0_010000_123456L;

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // Check if S1 was cleared (top 6 bits)
        assertEquals(0_000000_123456L, bank1.get(42));
        // Check if next instruction was skipped (PC = 2)
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTCS_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tcsEM(0, 0, 0, 2, 42),      // TCS (U)
            0,                           // Executed if bit 5 is 0 (Normal stop)
            0,
        };

        var data = new long[50];
        data[42] = 0_000000_123456L;    // Bit 5 is 0

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        run();

        // Check if nothing changed
        assertEquals(0_000000_123456L, bank1.get(42));
        // Check if next instruction was NOT skipped (PC = 1)
        assertEquals(0_1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTCS_Skip_BM() throws MachineInterrupt {
        var code = new long[] {
            tcsBM(0, 42),               // TCS (U)
            0,                           // Skipped if bit 5 is 1
            0,                           // Normal stop
        };

        var data = new long[100];
        data[42] = 0_010000_654321L;    // Bit 5 is 1

        var bank0 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        bank0.set(0, code[0]);
        bank0.set(1, code[1]);
        bank0.set(2, code[2]);

        run();

        // Check if S1 was cleared
        assertEquals(0_000000_654321L, bank0.get(42));
        // Check if next instruction was skipped
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
