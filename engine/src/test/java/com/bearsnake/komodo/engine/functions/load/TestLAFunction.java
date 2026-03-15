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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLAFunction extends TestFunction {

    private long laImm(long j, long a, long x, long u) {
        return fjaxu(010, j, a, x, u);
    }

    private long laBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(010, j, a, x, h, i, u);
    }

    private long laEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(010, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLAImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            laImm(Constants.JFIELD_U, 0, 0, 0123),
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

        run();

        assertEquals(0_123, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testLAImmediate_Large_BM() throws MachineInterrupt {
        var code = new long[0200000];
        code[0] = laImm(Constants.JFIELD_U, 0, 0, 0200000);

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

        run();

        assertEquals(0_200000, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testLAImmediate_EM() throws MachineInterrupt {
        var code = new long[] {
            laImm(Constants.JFIELD_U, 0, 0, 0123),
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

        assertEquals(0_123, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testLA_W_EM() throws MachineInterrupt {
        var code = new long[] {
            laEM(Constants.JFIELD_W, 2, 0, 0, 0, 1, 02),
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

        assertEquals(3L, _engine.getExecOrUserARegister(2).getW());
    }

    @Test
    public void testLA_from_GRS() throws MachineInterrupt {
        var code = new long[] {
            // use Q1 to verify reg->reg is always full-word
            laEM(Constants.JFIELD_Q1, 2, 0, 0, 0, 0, Constants.GRS_A3),
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(3).setW(0_400013_022100L);

        run();

        assertEquals(0_400013_022100L, _engine.getExecOrUserARegister(2).getW());
    }

    @Test
    public void testLA_LongIndexed_EM() throws MachineInterrupt {
        var code = new long[] {
            laEM(Constants.JFIELD_W, 4, 4, 1, 0, 1, 0),
            0,
            };

        var data = new long[0_1000_1000];
        data[0_1000_0000] = 0_445544_667766L;

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_10000_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(1).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)1)
               .setExecutive24BitIndexingEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(4).setXI12(04).setXM24(0_1000_0000);

        run();

        assertEquals(0_445544_667766L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(04, _engine.getExecOrUserXRegister(4).getXI12());
        assertEquals(0_1000_0004, _engine.getExecOrUserXRegister(4).getXM24());
    }

    @Test
    public void testLA_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            laEM(Constants.JFIELD_W, 5, 3, 1, 0, 1, 01),
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

        run();

        assertEquals(0_15L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_01L, _engine.getExecOrUserXRegister(3).getXI());
        assertEquals(0_04L, _engine.getExecOrUserXRegister(3).getXM());
    }

    @Test
    public void testLA_Tx_BM() throws MachineInterrupt {
        var code = new long[]{
            laBM(Constants.JFIELD_T1, 0, 0, 0, 0, 040000),
            laBM(Constants.JFIELD_T2, 1, 0, 0, 0, 040001),
            laBM(Constants.JFIELD_T3, 2, 0, 0, 0, 040002),
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

        assertEquals(0_2211L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_1100L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_777777_775301L, _engine.getExecOrUserARegister(2).getW());
    }

    @Test
    public void testLA_Qx_EM() throws MachineInterrupt {
        var code = new long[] {
            laEM(Constants.JFIELD_Q1, 12, 0, 0, 0, 1, 0),
            laEM(Constants.JFIELD_Q2, 13, 0, 0, 0, 1, 0),
            laEM(Constants.JFIELD_Q3, 14, 0, 0, 0, 1, 0),
            laEM(Constants.JFIELD_Q4, 15, 0, 0, 0, 1, 0),
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

        assertEquals(0_112L, _engine.getExecOrUserARegister(12).getW());
        assertEquals(0_233L, _engine.getExecOrUserARegister(13).getW());
        assertEquals(0_445L, _engine.getExecOrUserARegister(14).getW());
        assertEquals(0_566L, _engine.getExecOrUserARegister(15).getW());
    }

    @Test
    public void testLA_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            laBM(Constants.JFIELD_W, 5, 0, 0, 1, 022002),
            0,
            laBM(0, 0, 0, 0, 1, 022003),
            laBM(0, 0, 0, 0, 0, 040000),
            };

        var data = new long[] { 0_221111_111111L };

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
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXI(0_01).setXM(0_040000);

        run();

        assertEquals(0_221111_111111L, _engine.getExecOrUserARegister(5).getW());
    }
}
