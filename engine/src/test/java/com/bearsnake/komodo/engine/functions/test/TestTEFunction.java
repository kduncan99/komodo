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
 * Test Equal instruction
 * (TE) skips if (U) = A(a).
 * f=052 for both modes.
 */
public class TestTEFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private long teImm(long j, long a, long x, long u) {
        return fjaxu(0_52, j, a, x, u);
    }

    private long teBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_52, j, a, x, h, i, u);
    }

    private long teEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_52, j, a, x, h, i, b, d);
    }

    @Test
    public void testTE_Immediate_BM() throws MachineInterrupt {
        var code = new long[] {
            teImm(Constants.JFIELD_U, 2, 0, 0404040),
            0,
            teImm(Constants.JFIELD_XU, 2, 0, 0404040),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTE_Immediate_EM() throws MachineInterrupt {
        var code = new long[] {
            teImm(Constants.JFIELD_U, 2, 0, 0404040),
            0,
            teImm(Constants.JFIELD_XU, 2, 0, 0404040),
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

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTE_W_EM() throws MachineInterrupt {
        var code = new long[] {
            teEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            teEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),
            0,
            0,
            };

        var data = new long[] {
            0_000001_000123L,
            0_000001_123000L,
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
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTE_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            teEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            teEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),
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
    public void testTE_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            teEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, GRS_A5),
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

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTE_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            teEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, 040),
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
    public void testTE_Q3_EM() throws MachineInterrupt {
        var code = new long[] {
            teEM(Constants.JFIELD_Q3, 2, 0, 0, 0, 2, 0),
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

        _engine.getExecOrUserARegister(2).setW(0_111L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTEIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            teBM(Constants.JFIELD_W, 2, 0, 0, 1, 022004),
            0,
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005),
            fjaxhiu(0, 0, 0, 0, 0, 1, 022003),
            077003,
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000 base address
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_77003L);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
