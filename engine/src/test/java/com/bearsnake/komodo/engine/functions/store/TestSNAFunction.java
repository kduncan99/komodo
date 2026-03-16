/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A4;
import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static com.bearsnake.komodo.engine.Constants.GRS_ER0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSNAFunction extends TestFunction {

    private long snaBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(002, j, a, x, h, i, u);
    }

    private long snaEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(002, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSNA_BM() throws MachineInterrupt {
        var code = new long[] {
            snaBM(Constants.JFIELD_W, 4, 0, 0, 0, 040000),
            snaBM(Constants.JFIELD_H1, 5, 0, 0, 0, 040001),
            snaBM(Constants.JFIELD_H2, 6, 0, 0, 0, 040001),
            0,
        };

        var data = new long[02000];

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
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_123456_654321L);
        _engine.getExecOrUserARegister(5).setW(0_111111_111111L);
        _engine.getExecOrUserARegister(6).setW(0_222222_222222L);

        run();

        assertEquals(0_654321_123456L, data[0]);
        assertEquals(0_666666_555555L, data[1]);
    }

    @Test
    public void testSNA_EM() throws MachineInterrupt {
        var code = new long[] {
            snaEM(Constants.JFIELD_W, 4, 0, 0, 0, 2, 01000),
            snaEM(Constants.JFIELD_H1, 5, 0, 0, 0, 2, 01001),
            snaEM(Constants.JFIELD_H2, 6, 0, 0, 0, 2, 01001),
            snaEM(Constants.JFIELD_T1, 7, 0, 0, 0, 2, 01002),
            snaEM(Constants.JFIELD_T2, 8, 0, 0, 0, 2, 01003),
            snaEM(Constants.JFIELD_Q1, 9, 0, 0, 0, 2, 01004),
            snaEM(Constants.JFIELD_Q2, 10, 0, 0, 0, 2, 01005),
            snaEM(Constants.JFIELD_S1, 11, 0, 0, 0, 2, 01006),
            snaEM(Constants.JFIELD_S2, 12, 0, 0, 0, 2, 01007),
            0,
            };

        var data = new long[02000];

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_00)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setQuarterWordModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegister(GRS_A4).setW(0_030000_322334L);
        _engine.getExecOrUserARegister(5).setW(0_111111_111111L);
        _engine.getExecOrUserARegister(6).setW(0_222222_222222L);
        _engine.getExecOrUserARegister(7).setW(0_111111_111111L);
        _engine.getExecOrUserARegister(8).setW(0_222222_222222L);
        _engine.getExecOrUserARegister(9).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(10).setW(0_444444_444444L);
        _engine.getExecOrUserARegister(11).setW(0_555555_555555L);
        _engine.getExecOrUserARegister(12).setW(0_666666_666666L);

        run();

        assertEquals(0_747777_455443L, data[01000]);
        assertEquals(0_666666_555555L, data[01001]);
        assertEquals(0_666000_000000L, data[01002]); // Q1 (j=7 in q-word mode) of ~A7
        assertEquals(0_000000_555000L, data[01003]); // Q3 (j=6 in q-word mode) of ~A8
        assertEquals(0_444000_000000L, data[01004]); // Q1 of ~A9
        assertEquals(0_000333_000000L, data[01005]); // Q2 of ~A10
        assertEquals(0_220000_000000L, data[01006]); // S1 of ~A11
        assertEquals(0_001100_000000L, data[01007]); // S2 of ~A12
    }

    @Test
    public void testSNA_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            snaEM(Constants.JFIELD_W, 4, 0, 0, 0, 0, 020),
            snaEM(Constants.JFIELD_H1, 5, 0, 0, 0, 0, 021),
            0,
        };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_00)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_000000).setBankLevel((short)0_0);

        _engine.getGeneralRegister(GRS_A4).setW(0_000000_000000L);
        _engine.getGeneralRegister(GRS_A5).setW(0_000000_000000L);
        _engine.getExecOrUserARegister(4).setW(0_123456_765432L);
        _engine.getExecOrUserARegister(5).setW(0_111111_222222L);

        run();

        assertEquals(0_654321_012345L, _engine.getGeneralRegister(GRS_A4).getW());
        assertEquals(0_666666_555555L, _engine.getGeneralRegister(GRS_A5).getW());
    }

    @Test
    public void testSNA_X_XU_EM() throws MachineInterrupt {
        var code = new long[] {
            snaEM(Constants.JFIELD_U, 4, 0, 0, 0, 2, 01000),
            snaEM(Constants.JFIELD_XU, 4, 0, 0, 0, 2, 01001),
            0,
        };

        var data = new long[02000];
        data[01000] = 0_123456_765432L;
        data[01001] = 0_123456_765432L;

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_00)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_777777_777777L);

        run();

        assertEquals(0_123456_765432L, data[01000]); // Should NOT have changed
        assertEquals(0_123456_765432L, data[01001]); // Should NOT have changed
    }

    @Test
    public void testSNA_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            snaBM(Constants.JFIELD_W, 4, 1, 0, 0, 040000),
            0,
        };

        var data = new long[02000];

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
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_123456_654321L);
        _engine.getExecOrUserXRegister(1).setXM(010);

        run();

        assertEquals(0_654321_123456L, data[010]);
    }

    @Test
    public void testSNA_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            snaBM(Constants.JFIELD_W, 4, 0, 0, 1, 040000),
            0,
        };

        var data = new long[02000];
        data[0] = 0_000000_040005L;

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
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_777666_555444L);

        run();

        assertEquals(0_000111_222333L, data[5]);
    }

    @Test
    public void testSNA_MultiIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            snaBM(Constants.JFIELD_W, 4, 0, 0, 1, 040000),
            0,
        };

        var data = new long[02000];
        data[0] = (1L << 16) | 040001L;
        data[1] = 040005L;

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
        bd1.getGeneralAccessPermissions().setCanRead(true).setCanWrite(true);

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_112233_445566L);

        run();

        assertEquals(0_665544_332211L, data[5]);
    }

    @Test
    public void testSNA_ReferenceViolation_EM() {
        var code = new long[] {
            snaEM(Constants.JFIELD_W, 4, 0, 0, 0, 2, 02000),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(new long[02000]);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_00)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_00)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_000000).setBankLevel((short)0_0);

        var ex = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, ex._errorType);
    }

    @Test
    public void testSNA_GRSViolation_EM() {
        var code = new long[] {
            snaEM(Constants.JFIELD_W, 4, 0, 0, 0, 0, GRS_ER0),
            0,
        };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_00)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_000000).setBankLevel((short)0_0);

        var ex = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertTrue(ex._errorType == ReferenceViolationInterrupt.ErrorType.GRSViolation ||
                   ex._errorType == ReferenceViolationInterrupt.ErrorType.WriteAccessViolation);
    }
}
