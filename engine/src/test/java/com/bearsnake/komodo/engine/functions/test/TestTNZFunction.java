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

public class TestTNZFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private long tnzBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(051, j, a, x, h, i, u);
    }

    private long tnzEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 011, x, h, i, b, d);
    }

    @Test
    public void testTNZ_NonZero_BM() throws MachineInterrupt {
        var code = new long[] { tnzBM(Constants.JFIELD_U, 0, 0, 0, 0, 0_123456L), 0, 0 };
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0).setUpperLimit(0777777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();
        assertEquals(0_00002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNZ_PositiveZero_BM() throws MachineInterrupt {
        var code = new long[] { tnzBM(Constants.JFIELD_U, 0, 0, 0, 0, 0L), 0, 0 };
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0).setUpperLimit(0777777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();
        assertEquals(0_00001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNZ_NegativeZero_BM() throws MachineInterrupt {
        var code = new long[] { tnzBM(Constants.JFIELD_W, 0, 0, 0, 0, 0), 0, 0 };
        var data = new long[] { 0_777777_777777L };
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0).setUpperLimit(0777777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        var bank12 = new ArraySlice(data);
        var bd12 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0).setUpperLimit(0777777).setBaseAddress(new AbsoluteAddress(0, 12, 0));
        _engine.getBaseRegister(14).setBankDescriptor(bd12).setStorage(bank12).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();
        assertEquals(0_00001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNZ_NonZero_EM() throws MachineInterrupt {
        var code = new long[] { tnzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), 0, 0 };
        var data = new long[]{ 0_123456L };
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

    @Test
    public void testTNZ_Zero_EM() throws MachineInterrupt {
        var code = new long[] { tnzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), 0, 0 };
        var data = new long[]{ 0L };
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0_1).setUpperLimit(0_1777).setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        var bank2 = new ArraySlice(data);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
