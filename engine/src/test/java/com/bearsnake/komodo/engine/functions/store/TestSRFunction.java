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

import static com.bearsnake.komodo.engine.Constants.GRS_ER0;
import static com.bearsnake.komodo.engine.Constants.GRS_R1;
import static com.bearsnake.komodo.engine.Constants.GRS_R2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSRFunction extends TestFunction {

    private long srBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(004, j, a, x, h, i, u);
    }

    private long srEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(004, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSR_BM() throws MachineInterrupt {
        var code = new long[] {
            srBM(Constants.JFIELD_W, 1, 0, 0, 0, 040000),
            0,
        };

        var data = new long[02000];
        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0_22).setUpperLimit(0_22777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0_40).setUpperLimit(0_40777).setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3).setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserRRegisterIndex(1)).setW(0_123456_765432L);

        run();

        assertEquals(0_123456_765432L, data[0]);
    }

    @Test
    public void testSR_EM() throws MachineInterrupt {
        var code = new long[] {
            srEM(Constants.JFIELD_W, 1, 0, 0, 0, 2, 01000),
            0,
        };

        var data = new long[02000];
        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserRRegisterIndex(1)).setW(0_000000_123456L);

        run();

        assertEquals(0_000000_123456L, data[01000]);
    }

    @Test
    public void testSR_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            srEM(Constants.JFIELD_W, 1, 0, 0, 0, 0, GRS_R2),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserRRegisterIndex(1)).setW(0_765432_123456L);

        run();

        assertEquals(0_765432_123456L, _engine.getGeneralRegisterSet().getRegister(GRS_R2).getW());
    }

    @Test
    public void testSR_ReferenceViolation_EM() {
        var code = new long[] {
            srEM(Constants.JFIELD_W, 1, 0, 0, 0, 2, 02000), // Out of range
            0,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(new long[02000]);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        var ex = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, ex._errorType);
    }

    @Test
    public void testSR_GRSViolation_EM() {
        var code = new long[] {
            srEM(Constants.JFIELD_W, 1, 0, 0, 0, 0, GRS_ER0),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(01777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3); // User
        _engine.getProgramAddressRegister().setProgramCounter(0);

        var ex = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertTrue(ex._errorType == ReferenceViolationInterrupt.ErrorType.GRSViolation ||
                   ex._errorType == ReferenceViolationInterrupt.ErrorType.WriteAccessViolation);
    }
}
