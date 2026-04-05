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

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Double-Precision Test Equal instruction
 * (DTE) skips if (U | U+1) = A(a) | A(a+1).
 * f=071, j=017 for both modes.
 */
public class TestDTEFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private long dteBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_71, 0_17, a, x, h, i, u);
    }

    private long dteEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 0_17, a, x, h, i, b, d);
    }

    @Test
    public void testDTE_BM() throws MachineInterrupt {
        var code = new long[] {
            dteBM(2, 0, 0, 0, 0_2000), // Equal, skip
            0,
            dteBM(2, 0, 0, 0, 0_2002), // Not equal, no skip
            0,
            0,
            };

        var data = new long[] {
            0_123456_654321L,
            0_777777_000000L,
            0_123456_654321L,
            0_777777_000001L,
        };

        var bank = new ArraySlice(new long[65536]);
        bank.load(code, 0, code.length, 0_1000);
        bank.load(data, 0, data.length, 0_2000);

        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(0_17777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_123456_654321L);
        _engine.getExecOrUserARegister(3).setW(0_777777_000000L);

        run();

        // 0_1000: dteBM(2, 0, 0, 0, 0_2000) -> skips 0_1001 to 0_1002
        // 0_1002: dteBM(2, 0, 0, 0, 0_2002) -> no skip, next is 0_1003
        // 0_1003: 0 -> halt
        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTE_EM() throws MachineInterrupt {
        var code = new long[] {
            dteEM(2, 0, 0, 0, 2, 0), // Equal, skip
            0,
            dteEM(2, 0, 0, 0, 2, 2), // Not equal, no skip
            0,
            0,
            };

        var data = new long[] {
            0_111111_222222L,
            0_333333_444444L,
            0_111111_222222L,
            0_333333_444445L,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_111111_222222L);
        _engine.getExecOrUserARegister(3).setW(0_333333_444444L);

        run();

        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTE_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            dteEM(2, 0, 0, 0, 0, GRS_A5), // Equal, skip
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(3).setW(0_000004_000004L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(6).setW(0_000004_000004L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTE_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            dteEM(2, 0, 0, 0, 2, 0), // Point to missing bank
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(null); // Explicitly ensure it's null

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserARegister(3).setW(0_000001_123000L);

        // This will now throw NullPointerException in Engine.getConsecutiveOperands
        assertThrows(NullPointerException.class, () -> run());

        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
