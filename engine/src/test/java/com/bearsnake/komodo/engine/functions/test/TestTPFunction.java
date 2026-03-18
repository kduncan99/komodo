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
 * Test Positive instruction
 * (TP) skips if (U) is positive (>= +0).
 * f=050, a=03 for extended mode, f=060 for basic mode.
 */
public class TestTPFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tpImm(long j, long a, long x, long u) {
        return fjaxu(060, j, a, x, u);
    }

    private long tpBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(060, j, a, x, h, i, u);
    }

    private long tpEM(long j, long x, long h, long i, long b, long d) {
        // Extended mode TP has A field = 03, f=050
        return fjaxhibd(050, j, 03, x, h, i, b, d);
    }

    @Test
    public void testTP_PositiveZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpEM(Constants.JFIELD_W, 0, 0, 0, 2, 0), // Use JFIELD_W to fetch from B2+0
            0,
            0,
            };

        var data = new long[]{ 0L }; // Positive zero

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
    public void testTP_NegativeZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_777777_777777L }; // Negative zero

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

        // Negative zero is NOT positive
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTP_PositiveNonZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 1L }; // Positive non-zero

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

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTP_NegativeNonZero_EM() throws MachineInterrupt {
        var code = new long[] {
            tpEM(Constants.JFIELD_W, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_777777_777776L }; // -1

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
    public void testTPImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            tpImm(Constants.JFIELD_U, 0, 0, 0), // U=0 (Positive Zero)
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000 base address
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPImmediate_NoSkip_BM() throws MachineInterrupt {
        // In basic mode immediate, U is 18 bits. If it's sign-extended to 36 bits, it could be negative.
        // However, TP usually checks the final operand.
        // For f=060, it's an immediate instruction in basic mode.
        // Actually, in basic mode, f=060 is "Test Positive Immediate".
        // Let's assume U is positive.
        var code = new long[] {
            tpImm(Constants.JFIELD_U, 0, 0, 0_777777), // U is 18 bits, all 1s.
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // If U is 18 bits 0_777777, it's positive as a 36-bit word (high 18 bits are 0)
        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPImmediate_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            tpImm(Constants.JFIELD_U, 0, 3, 0), // U = 0 + X3
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(3).setXM(1); // U = 0 + 1 = 1 (positive)

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTPIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            tpBM(Constants.JFIELD_W, 0, 0, 0, 1, 022004), // Indirect via 022004
            0,
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005), // Address 022003
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005), // Address 022004 -> points to 022005
            0, // Address 022005 -> value 0
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short)3);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
