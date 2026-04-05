/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Double-Precision Test Greater Magnitude instruction
 * (DTGM) skips if magnitude of (U | U+1) > magnitude of A(a) | A(a+1).
 * f=033, j=014 for Extended Mode.
 */
public class TestDTGMFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private long dtgmEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_33, 0_14, a, x, h, i, b, d);
    }

    @Test
    public void testDTGM_PositiveGreater() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 2, 0), // (2000, 2001) = 10, A2/3 = 5. Skip.
            0,
            0,
            };

        var data = new long[] {
            0_000000_000012L, // 10
            0_000000_000000L,
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
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000000L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTGM_NegativeGreater() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 2, 0), // (2000, 2001) = -10 (mag 10), A2/3 = 5. Skip.
            0,
            0,
            };

        var data = new long[] {
            ~0_000000_000012L & 0_777777_777777L, // -10 magnitude (bitwise NOT of 10)
            0_777777_777777L,
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
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000000L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTGM_Equal_NoSkip() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 2, 0), // (2000, 2001) = 5, A2/3 = 5. No Skip.
            0,
            };

        var data = new long[] {
            0_000000_000005L,
            0_000000_000000L,
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
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000000L);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTGM_Zeros() throws MachineInterrupt {
        // negative zero magnitude is +0. positive zero magnitude is +0.
        // We need to compare double-precision positive and negative zero as negative zero is less than positive zero.
        // BUT for DTGM, we take magnitude first.
        // Magnitude of -0 is +0. Magnitude of +0 is +0.
        // +0 is NOT greater than +0. So no skip.

        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 2, 0),
            0,
            };

        var data = new long[] {
            0_777777_777777L, // negative zero
            0_777777_777777L,
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
        _engine.getExecOrUserARegister(2).setW(0); // positive zero
        _engine.getExecOrUserARegister(3).setW(0);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTGM_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 0, GRS_A5), // (A5, A6) = 10, A2/3 = 5. Skip.
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

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000000L);
        _engine.getExecOrUserARegister(5).setW(0_000000_000012L);
        _engine.getExecOrUserARegister(6).setW(0_000000_000000L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTGM_Indexed() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 3, 0, 0, 2, 0), // Base D=0, X3=2. Effective U=2. (2002, 2003) = 10, A2/3 = 5. Skip.
            0,
            0,
            };

        var data = new long[] {
            0, 0, // 2000, 2001
            0_000000_000012L, // 2002
            0_000000_000000L, // 2003
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
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000000L);
        _engine.getExecOrUserXRegister(3).setXM(2);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTGM_StorageViolation() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 2, 0), // Point to missing bank
            0,
            };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(null);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        assertThrows(NullPointerException.class, () -> run());
    }

    @Test
    public void testDTGM_GRSViolation() throws MachineInterrupt {
        var code = new long[] {
            dtgmEM(2, 0, 0, 0, 0, 0177), // Starts at 0177, needs 2 words. Violation.
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

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);
    }
}
