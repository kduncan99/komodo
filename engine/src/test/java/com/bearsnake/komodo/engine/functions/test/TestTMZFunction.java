/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Minus Zero instruction
 * (TMZ) skips if (U) = -0.
 * Extended Mode only, f=050, j=0, a=04.
 */
public class TestTMZFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tmzEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 04, x, h, i, b, d);
    }

    @Test
    public void testTMZ_NegativeZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tmzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), // Use JFIELD_W to fetch from B2+0
            0,
            0,
        };

        var data = new long[]{ 0_777777_777777L }; // Negative zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0_1)   // 01000 base address
            .setUpperLimit(0_1777)
            .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0)
            .setUpperLimit(0777)
            .setBaseAddress(new AbsoluteAddress(0, 2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister()
            .setBasicModeEnabled(false)
            .setProcessorPrivilege((short)3)
            .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTMZ_PositiveZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tmzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0L }; // Positive zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0_1)
            .setUpperLimit(0_1777)
            .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0)
            .setUpperLimit(0777)
            .setBaseAddress(new AbsoluteAddress(0, 2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTMZ_PositiveNonZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tmzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 1L }; // Non-zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0_1)
            .setUpperLimit(0_1777)
            .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0)
            .setUpperLimit(0777)
            .setBaseAddress(new AbsoluteAddress(0, 2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTMZ_NegativeNonZero_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            tmzEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0_777777_777776L }; // Negative non-zero

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0_1)
            .setUpperLimit(0_1777)
            .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
            .setLowerLimit(0)
            .setUpperLimit(0777)
            .setBaseAddress(new AbsoluteAddress(0, 2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
