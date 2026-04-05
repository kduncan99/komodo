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

import static com.bearsnake.komodo.engine.Constants.GRS_EX0;
import static com.bearsnake.komodo.engine.Constants.GRS_X1;
import static com.bearsnake.komodo.engine.Constants.GRS_X2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSXFunction extends TestFunction {

    private long sxBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(006, j, a, x, h, i, u);
    }

    private long sxEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(006, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSX_BM() throws MachineInterrupt {
        var code = new long[] {
            sxBM(Constants.JFIELD_W, 1, 0, 0, 0, 040000), // Store X1
            sxBM(Constants.JFIELD_H2, 2, 0, 0, 0, 040001), // Store X2 H2
            0,
        };

        var data = new long[02000];
        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0_22).setUpperLimit(0_22777).setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0_40).setUpperLimit(0_40777).setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3).setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(1).setW(0_000000_123456L);
        _engine.getExecOrUserXRegister(2).setW(0_000000_654321L);

        run();

        assertEquals(0_000000_123456L, data[0]);
        assertEquals(0_000000_654321L, data[1]);
    }

    @Test
    public void testSX_EM() throws MachineInterrupt {
        var code = new long[] {
            sxEM(Constants.JFIELD_W, 1, 0, 0, 0, 2, 01000),
            0,
        };

        var data = new long[02000];
        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        _engine.getExecOrUserXRegister(1).setW(0_000000_111222L);

        run();

        assertEquals(0_000000_111222L, data[01000]);
    }

    @Test
    public void testSX_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            sxEM(Constants.JFIELD_W, 1, 0, 0, 0, 0, GRS_X2),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        _engine.getExecOrUserXRegister(1).setW(0_000000_333444L);

        run();

        assertEquals(0_000000_333444L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
    }

    @Test
    public void testSX_ReferenceViolation_EM() {
        var code = new long[] {
            sxEM(Constants.JFIELD_W, 1, 0, 0, 0, 2, 02000),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(new long[02000]);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(01777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(0)
               .setBankDescriptor(bd0)
               .setStorage(bank0);
        _engine.getBaseRegister(2)
               .setBankDescriptor(bd1)
               .setStorage(bank1);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        var ex = assertThrows(ReferenceViolationInterrupt.class, this::run);

        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, ex._errorType);
    }

    @Test
    public void testSX_GRSViolation_EM() {
        var code = new long[] {
            sxEM(Constants.JFIELD_W, 1, 0, 0, 0, 0, GRS_EX0),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        var ex = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertTrue(ex._errorType == ReferenceViolationInterrupt.ErrorType.GRSViolation ||
                   ex._errorType == ReferenceViolationInterrupt.ErrorType.WriteAccessViolation);
    }
}
