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
 * Test Greater Magnitude instruction
 * (TGM) skips if the magnitude of (U) is > A(a).
 * f=033, j=013 extended mode only.
 */
public class TestTGMFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tgmEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_33, 0_13, a, x, h, i, b, d);
    }

    @Test
    public void testTGM_W_EM() throws MachineInterrupt {
        var code = new long[] {
            tgmEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            tgmEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),
            0,
            0,
            };

        var data = new long[] {
            0_400000_000000L,
            0_777777_777777L,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
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
        _engine.getExecOrUserARegister(2).setW(0L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTGM_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tgmEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            tgmEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        assertThrows(ReferenceViolationInterrupt.class, () -> run());

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTGM_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            tgmEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, GRS_A5),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_400000_000000L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTGM_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tgmEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, 040),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTGM_Q3_EM() throws MachineInterrupt {
        var code = new long[] {
            tgmEM(Constants.JFIELD_Q3, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_111111_111111L };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
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
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_101L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
