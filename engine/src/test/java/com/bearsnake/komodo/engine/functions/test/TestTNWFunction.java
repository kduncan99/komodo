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
 * Test Not Within Range instruction
 * (TNW) skips if (U) <= A(a) or (U) > A(a+1).
 * f=057 for both modes.
 */
public class TestTNWFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tnwImm(long j, long a, long x, long u) {
        return fjaxu(0_57, j, a, x, u);
    }

    private long tnwBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_57, j, a, x, h, i, u);
    }

    private long tnwEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_57, j, a, x, h, i, b, d);
    }

    @Test
    public void testTNW_BM_Immediate_Skip_Below() throws MachineInterrupt {
        var code = new long[] {
            tnwImm(Constants.JFIELD_U, 2, 0, 004),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_BM_Immediate_NoSkip_Between() throws MachineInterrupt {
        var code = new long[] {
            tnwImm(Constants.JFIELD_U, 2, 0, 010),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_EM_Memory_Skip_Boundary_Low() throws MachineInterrupt {
        var code = new long[] {
            tnwEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[] {
            0_000000_000005L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0x22000));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_EM_Memory_Skip_Above() throws MachineInterrupt {
        var code = new long[] {
            tnwEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[] {
            0_000000_000016L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0x22000));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_BM_Immediate_Skip_Boundary_High() throws MachineInterrupt {
        var code = new long[] {
            tnwImm(Constants.JFIELD_U, 2, 0, 016),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_EM_Immediate_NoSkip_Between() throws MachineInterrupt {
        var code = new long[] {
            tnwImm(Constants.JFIELD_U, 2, 0, 010),
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
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_EM_Memory_NoSkip_Boundary_High() throws MachineInterrupt {
        var code = new long[] {
            tnwEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[] {
            0_000000_000015L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0x22000));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNW_EM_Memory_NoSkip_Between() throws MachineInterrupt {
        var code = new long[] {
            tnwEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[] {
            0_000000_000006L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0x22000));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        
        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
