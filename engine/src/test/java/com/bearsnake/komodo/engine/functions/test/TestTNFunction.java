/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.baselib.ArraySlice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTNFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private long tnBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(061, j, a, x, h, i, u);
    }

    private long tnEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 014, x, h, i, b, d);
    }

    @Test
    public void testTN_Positive_BM() throws MachineInterrupt {
        var code = new long[] { tnBM(Constants.JFIELD_U, 0, 0, 0, 0, 0_123456L), 0, 0 };
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0).setUpperLimit(0777777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0x00000C);

        run();
        // Do not skip if positive
        assertEquals(0_00001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTN_Negative_EM() throws MachineInterrupt {
        var code = new long[] { tnEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), 0, 0 };
        var data = new long[]{ 0_400000_000000L };
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0_1).setUpperLimit(0_1777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        var bank2 = new ArraySlice(data);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
