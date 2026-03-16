/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.ArraySlice;
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

public class TestSMAFunction extends TestFunction {

    private long smaBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(003, j, a, x, h, i, u);
    }

    private long smaEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(003, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSMA_BM() throws MachineInterrupt {
        var code = new long[] {
            smaBM(Constants.JFIELD_W, 4, 0, 0, 0, 040000),
            smaBM(Constants.JFIELD_H1, 5, 0, 0, 0, 040001),
            smaBM(Constants.JFIELD_H2, 6, 0, 0, 0, 040001),
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

        _engine.getExecOrUserARegister(4).setW(0_777777_777776L); // -1 (magnitude 1)
        _engine.getExecOrUserARegister(5).setW(0_400000_000000L); // Negative zero? bit 35 is set.
        _engine.getExecOrUserARegister(6).setW(0_000000_000005L); // +5 (magnitude 5)

        run();

        assertEquals(0_000000_000001L, data[0]);
        assertEquals(68719214597L, data[1]);
    }

    @Test
    public void testSMA_BM_Fix() throws MachineInterrupt {
        var code = new long[] {
            smaBM(Constants.JFIELD_W, 4, 0, 0, 0, 040000),
            smaBM(Constants.JFIELD_H1, 5, 0, 0, 0, 040001),
            smaBM(Constants.JFIELD_H2, 6, 0, 0, 0, 040001),
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

        _engine.getExecOrUserARegister(4).setW(0_777777_777776L); // -1 (magnitude 1)
        _engine.getExecOrUserARegister(5).setW(0_000000_000000L); // +0 (magnitude 0)
        _engine.getExecOrUserARegister(6).setW(0_000000_000005L); // +5 (magnitude 5)

        run();

        assertEquals(0_000000_000001L, data[0]);
        assertEquals(0_000000_000005L, data[1]);
    }

    @Test
    public void testSMA_EM() throws MachineInterrupt {
        var code = new long[] {
            smaEM(Constants.JFIELD_W, 4, 0, 0, 0, 2, 01000),
            smaEM(Constants.JFIELD_H1, 5, 0, 0, 0, 2, 01001),
            smaEM(Constants.JFIELD_H2, 6, 0, 0, 0, 2, 01001),
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
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegister(GRS_A4).setW(0_777777_777770L); // -7 (magnitude 7)
        _engine.getExecOrUserARegister(5).setW(0_111111_111111L);
        _engine.getExecOrUserARegister(6).setW(0_777777_777772L); // -5 (magnitude 5)

        run();

        assertEquals(0_000000_000007L, data[01000]);
        assertEquals(0_111111_000005L, data[01001]);
    }

    @Test
    public void testSMA_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            smaEM(Constants.JFIELD_W, 4, 0, 0, 0, 0, 020),
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

        _engine.getExecOrUserARegister(4).setW(0_777777_777776L); // -1

        run();

        assertEquals(0_000000_000001L, _engine.getGeneralRegister(GRS_A4).getW());
    }

    @Test
    public void testSMA_ReferenceViolation_EM() {
        var code = new long[] {
            smaEM(Constants.JFIELD_W, 4, 0, 0, 0, 2, 02000),
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
}
