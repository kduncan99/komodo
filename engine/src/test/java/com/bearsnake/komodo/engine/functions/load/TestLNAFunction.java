/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLNAFunction extends TestFunction {

    private long lnaImm(long j, long a, long x, long u) {
        return fjaxu(011, j, a, x, u);
    }

    private long lnaBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(011, j, a, x, h, i, u);
    }

    private long lnaEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(011, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLNAImmediate_EM() throws MachineInterrupt {
        var code = new long[] {
            lnaImm(Constants.JFIELD_U, 0, 0, 0123),
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

        run();

        assertEquals(0_777777_777654L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(0)).getW());
    }

    @Test
    public void testLNA_EM() throws MachineInterrupt {
        var code = new long[] {
            lnaEM(Constants.JFIELD_W, 2, 0, 0, 0, 1, 02),
            0,
        };

        var data = new long[] {
            0_1L,
            0_2L,
            0_777777_777774L,
            0_4L,
            0_5L
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

        run();

        assertEquals(0_03L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(2)).getW());
    }

    @Test
    public void testLA_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            lnaEM(Constants.JFIELD_W, 5, 3, 1, 0, 1, 01),
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
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).setXI(0_01).setXM(0_03);

        run();

        assertEquals(0_777777_777762L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(5)).getW());
        assertEquals(0_01L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).getXI());
        assertEquals(0_04L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).getXM());
    }

    @Test
    public void testLNA_Tx_BM() throws MachineInterrupt {
        var code = new long[]{
            lnaBM(Constants.JFIELD_T1, 0, 0, 0, 0, 040000),
            lnaBM(Constants.JFIELD_T2, 1, 0, 0, 0, 040001),
            lnaBM(Constants.JFIELD_T3, 2, 0, 0, 0, 040002),
            0,
            };

        var data = new long[]{
            0_221111_111111L,
            0_113311_007766L,
            0_111144_675301L,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_40)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);

        run();

        assertEquals(0_777777_775566L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(0)).getW());
        assertEquals(0_777777_776677L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).getW());
        assertEquals(0_000000_002476L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(2)).getW());
    }

    @Test
    public void testLNA_Qx_EM() throws MachineInterrupt {
        var code = new long[] {
            lnaEM(Constants.JFIELD_Q1, 12, 0, 0, 0, 1, 0),
            lnaEM(Constants.JFIELD_Q2, 13, 0, 0, 0, 1, 0),
            lnaEM(Constants.JFIELD_Q3, 14, 0, 0, 0, 1, 0),
            lnaEM(Constants.JFIELD_Q4, 15, 0, 0, 0, 1, 0),
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

        run();

        assertEquals(0_777777_777665L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(12)).getW());
        assertEquals(0_777777_777544L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(13)).getW());
        assertEquals(0_777777_777332L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(14)).getW());
        assertEquals(0_777777_777211L, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(15)).getW());
    }
}
