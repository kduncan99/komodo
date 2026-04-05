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
 * Masked Alphanumeric Test Greater instruction
 * (MATG) Checks the logical AND of U AND R2 to see if the result is
 * alphanumerically greater than the logical AND of A(a) AND R2.
 * Alphanumeric comparisons treat bit 0 as a data bit rather than a sign bit.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * f=071, j=007. Extended mode only.
 */
public class TestMATGFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long matgEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 0_07, a, x, h, i, b, d);
    }

    @Test
    public void testMATG_W_EM() throws MachineInterrupt {
        var code = new long[] {
            matgEM(2, 3, 0, 0, 2, 0),    // Operand > A(2) - true, skip
            0,                           // Skipped
            matgEM(2, 3, 0, 0, 2, 01),   // Operand > A(2) - false, don't skip
            0,                           // Executed (stops run)
            0,
            };

        var data = new long[] {
            0_400000_000000L,            // Operand 1 (Bit 0 set)
            0_000000_000001L,            // Operand 2 (Small)
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        // 01000: MATG (Skip) -> 01002
        // 01002: MATG (No skip) -> 01003
        // 01003: 0 (Stop)
        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMATG_Bit0_AsData() throws MachineInterrupt {
        // Here we test that 0400000000000 (bit 0 set) is greater than 0377777777777
        var code = new long[] {
            matgEM(2, 3, 0, 0, 2, 0),    // Operand(0400...) > A(2)(0377...) - true, skip
            0,
            0,
        };

        var data = new long[] {
            0_400000_000000L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_377777_777777L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        // 01000: MATG (Skip) -> 01002
        // 01002: 0 (Stop)
        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
