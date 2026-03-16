/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import static com.bearsnake.komodo.engine.Constants.GRS_X3;
import static com.bearsnake.komodo.engine.Constants.GRS_X5;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * For testing SZ, SNZ, SP1, SN1, SFS, FSZ, SAS, SAZ
 */
public abstract class TestConstantFunction extends TestFunction {

    private final int _aField;
    private final long _constant;

    private static final long DATA_INIT_VALUE = 0_765432_123456L;
    private final long[] _data = {
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
        DATA_INIT_VALUE,
    };

    protected TestConstantFunction(
        final int aField,
        final long constant
    ) {
        _aField = aField;
        _constant = constant;
    }

    protected long sBM(long j, long x, long h, long i, long u) {
        return fjaxhiu(005, j, _aField, x, h, i, u);
    }

    protected long sEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(005, j, _aField, x, h, i, b, d);
    }

    protected void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    public void test_Simple_BM() throws MachineInterrupt {
        var code = new long[] {
            sBM(Constants.JFIELD_W, 0, 0, 0,  022003),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_022)
                                      .setUpperLimit(0_022777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(_constant, _data[03]);
    }

    public void test_Simple_EM() throws MachineInterrupt {
        var code = new long[] {
            sEM(Constants.JFIELD_W, 0, 0, 0, 2, 3),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(_constant, _data[03]);
    }

    public void test_H_BM() throws MachineInterrupt {
        var code = new long[] {
            sBM(Constants.JFIELD_H1, 0, 0, 0,  022001),
            sBM(Constants.JFIELD_H2, 0, 0, 0,  022002),
            sBM(Constants.JFIELD_XH1, 0, 0, 0,  022003),
            sBM(Constants.JFIELD_XH2, 0, 0, 0,  022004),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_022)
                                      .setUpperLimit(0_022777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(Word36.setH1(DATA_INIT_VALUE, _constant), _data[01]);
        assertEquals(Word36.setH2(DATA_INIT_VALUE, _constant), _data[02]);
        assertEquals(Word36.setH1(DATA_INIT_VALUE, _constant), _data[03]);
        assertEquals(Word36.setH2(DATA_INIT_VALUE, _constant), _data[04]);
    }

    public void test_T_EM() throws MachineInterrupt {
        var code = new long[] {
            sEM(Constants.JFIELD_T1, 0, 0, 0, 2, 1),
            sEM(Constants.JFIELD_T2, 0, 0, 0, 2, 2),
            sEM(Constants.JFIELD_T3, 0, 0, 0, 2, 3),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(Word36.setT1(DATA_INIT_VALUE, _constant), _data[01]);
        assertEquals(Word36.setT2(DATA_INIT_VALUE, _constant), _data[02]);
        assertEquals(Word36.setT3(DATA_INIT_VALUE, _constant), _data[03]);
    }

    public void test_S_Indirect_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            sBM(Constants.JFIELD_S1, 0, 0, 1,  022000),
            sBM(Constants.JFIELD_S2, 0, 0, 1,  022000),
            sBM(Constants.JFIELD_S3, 0, 0, 1,  022000),
            sBM(Constants.JFIELD_S4, 0, 0, 1,  022000),
            sBM(Constants.JFIELD_S5, 0, 0, 1,  022000),
            sBM(Constants.JFIELD_S6, 0, 0, 1,  022000),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_022)
                                      .setUpperLimit(0_022777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _data[0] = fjaxhiu(0, 0, 0, 0, 0, 1, 022001);
        _data[1] = fjaxhiu(0, 0, 0, 5, 1, 0, 022000);
        _engine.getGeneralRegister(GRS_X5).setXI(1).setXM(5);

        run();

        assertEquals(Word36.setS1(DATA_INIT_VALUE, _constant), _data[5]);
        assertEquals(Word36.setS2(DATA_INIT_VALUE, _constant), _data[6]);
        assertEquals(Word36.setS3(DATA_INIT_VALUE, _constant), _data[7]);
        assertEquals(Word36.setS4(DATA_INIT_VALUE, _constant), _data[8]);
        assertEquals(Word36.setS5(DATA_INIT_VALUE, _constant), _data[9]);
        assertEquals(Word36.setS6(DATA_INIT_VALUE, _constant), _data[10]);
    }

    public void test_U_EM() throws MachineInterrupt {
        var code = new long[] {
            sEM(Constants.JFIELD_U, 0, 0, 0, 2, 3),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        long original = _data[03];

        run();

        assertEquals(original, _data[03]);
    }

    public void test_XU_BM() throws MachineInterrupt {
        var code = new long[] {
            sBM(Constants.JFIELD_U, 0, 0, 0, 022003),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_022)
                                      .setUpperLimit(0_022777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(13).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        long original = _data[03];

        run();

        assertEquals(original, _data[03]);
    }

    public void test_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            sEM(Constants.JFIELD_Q2, 0, 0, 0, 0, GRS_X3),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(_data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(_constant, _engine.getGeneralRegister(GRS_X3).getW());
    }
}
