/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLXMFunction extends TestFunction {

    private long lxmImm(long j, long a, long x, long u) {
        return fjaxu(026, j, a, x, u);
    }

    private long lxmBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(026, j, a, x, h, i, u);
    }

    private long lxmEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(026, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLXMImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            lxmImm(Constants.JFIELD_U, 2, 0, 0123),
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(GRS_X2).setW(0_111111_111111L);

        run();

        assertEquals(0_111111_000123L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
    }

    @Test
    public void testLXMImmediate_EM() throws MachineInterrupt {
        var code = new long[] {
            lxmImm(Constants.JFIELD_U, 2, 0, 0123),
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(GRS_X2).setW(0_111111_111111L);

        run();

        assertEquals(0_111111_000123L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
    }

    @Test
    public void testLXM_BM() throws MachineInterrupt {
        var code = new long[] {
            lxmBM(Constants.JFIELD_W, 2, 0, 0, 0, 040002),
            0,
            };

        var data = new long[] {
            0_1L,
            0_2L,
            0_3L,
            0_4L,
            0_5L
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(040)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(13).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(GRS_X2).setW(0_111111_111111L);

        run();

        assertEquals(0_111111_000003L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
    }

    @Test
    public void testLXM_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            lxmEM(Constants.JFIELD_W, 5, 3, 1, 0, 1, 01),
            0,
            };

        var data = new long[] {
            0_11L,
            0_12L,
            0_13L,
            0_14L,
            0_15L
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(1).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXI(0_01).setXM(0_03);

        _engine.getGeneralRegisterSet().getRegister(GRS_X5).setW(0_111111_111111L);

        run();

        assertEquals(0_111111_000015L, _engine.getGeneralRegisterSet().getRegister(GRS_X5).getW());
        assertEquals(0_01L, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXI());
        assertEquals(0_04L, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXM());
    }

    @Test
    public void testLXM_Q3_EM() throws MachineInterrupt {
        var code = new long[] {
            lxmEM(Constants.JFIELD_Q3, 15, 0, 0, 0, 1, 0),
            0,
            };

        var data = new long[] {
            data(0_112, 0_233, 0_445, 0_566),
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(1).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(GRS_X15).setW(0_111111_111111L);

        run();

        assertEquals(0_111111_000445L, _engine.getGeneralRegisterSet().getRegister(GRS_X15).getW());
    }
}
