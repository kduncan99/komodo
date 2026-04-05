/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Masked Test Within Range instruction
 * (MTW) Checks the following:
 *      (A(a) AND R2) < ((U) AND R2) <= (A(a+1) AND R2)).
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TestMTWFunction extends TestFunction {
    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long mtwEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 4, a, x, h, i, b, d);
    }

    @Test
    public void testMTW_W_EM() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 123 <= 200 -> Skip
            0,
            mtwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 123 <= 200 -> Skip
            0,
            0,
            0,
            };

        var data = new long[] {
            123L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01004, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTW_NoMatch() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 150 < 123 <= 200 -> No Skip
            0,
            };

        var data = new long[] {
            123L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(150L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTW_UpperBoundary() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 200 <= 200 -> Skip
            0,
            0,
            };

        var data = new long[] {
            200L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTW_LowerBoundary() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 100 <= 200 -> No Skip
            0,
            };

        var data = new long[] {
            100L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTW_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 0, 0, 0, 0, GRS_A5), // (A2) < (A5) <= (A3) ? 10 < 15 <= 20 -> Skip
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

        _engine.getExecOrUserARegister(2).setW(10L);
        _engine.getExecOrUserARegister(3).setW(20L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(5).setW(15L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTW_Masking() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 0, 0, 0, 2, 0), // (A2&R2) < (U&R2) <= (A3&R2) ? (100&7) < (123&7) <= (200&7)
                                     // 4 < 3 <= 0 -> No Skip
            0,
            };

        var data = new long[] {
            123L,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0777)
                                      .setBaseAddress(new AbsoluteAddress(2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(7L);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTW_ReferenceViolation() throws MachineInterrupt {
        var code = new long[] {
            mtwEM(2, 3, 0, 0, 2, 0),
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        assertThrows(ReferenceViolationInterrupt.class, () -> run());
    }
}
