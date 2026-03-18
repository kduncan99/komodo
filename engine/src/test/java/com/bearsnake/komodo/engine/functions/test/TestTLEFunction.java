/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test Less Than or Equal instruction
 * (TLE) skips if (U) <= A(a).
 * f=054 for both modes.
 */
public class TestTLEFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tleImm(long j, long a, long x, long u) {
        return fjaxu(0_54, j, a, x, u);
    }

    private long tleBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_54, j, a, x, h, i, u);
    }

    private long tleEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_54, j, a, x, h, i, b, d);
    }

    @Test
    public void testTLE_Immediate_BM() throws MachineInterrupt {
        var code = new long[] {
            tleImm(Constants.JFIELD_U, 2, 0, 0404040), // (U)=404040 <= A(2)=404040 -> SKIP
            0,
            tleImm(Constants.JFIELD_U, 2, 0, 0404037), // (U)=404037 <= A(2)=404040 -> SKIP
            0,
            tleImm(Constants.JFIELD_U, 2, 0, 0404041), // (U)=404041 <= A(2)=404040 -> NO SKIP
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_1005, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLE_Immediate_EM() throws MachineInterrupt {
        var code = new long[] {
            tleImm(Constants.JFIELD_U, 2, 0, 0404040), // (U)=404040 <= A(2)=404040 -> SKIP
            0,
            tleImm(Constants.JFIELD_U, 2, 0, 0404037), // (U)=404037 <= A(2)=404040 -> SKIP
            0,
            tleImm(Constants.JFIELD_U, 2, 0, 0404041), // (U)=404041 <= A(2)=404040 -> NO SKIP
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_1005, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLE_W_EM() throws MachineInterrupt {
        var code = new long[] {
            tleEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),    // (U)=A(2)=404040 <= A(2)=404040 -> SKIP
            0,
            tleEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),   // (U)=A(3)=404037 <= A(2)=404040 -> SKIP
            0,
            tleEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 02),   // (U)=A(4)=404041 <= A(2)=404040 -> NO SKIP
            0,
            0,
            };

        var data = new long[] {
            0_404040L,
            0_404037L,
            0_404041L,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_404040L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_1005, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLE_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tleEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_404040L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        // B2 is NOT set, should throw ReferenceViolationInterrupt on fetching operand
        assertThrows(ReferenceViolationInterrupt.class, this::run);
    }

    @Test
    public void testTLE_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            tleEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, GRS_A5), // (U)=A(5)=404040 <= A(2)=404040 -> SKIP
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);
        _engine.getExecOrUserARegister(5).setW(0_404040L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLE_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tleEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, 040),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);

        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLE_Q3_EM() throws MachineInterrupt {
        // (U) <= A(2)
        var code = new long[] {
            tleEM(Constants.JFIELD_Q3, 2, 3, 0, 0, 2, 0), // (U)=Q3 of A(3)
            0,
            0,
            };

        var data = new long[] {
            data(0, 0, 0_404L, 0), // Q3 = 404
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000000404L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLEIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            tleBM(Constants.JFIELD_U, 2, 0, 0, 1, 0_1002), // (U)=A(2)=404040 <= A(2)=404040 -> SKIP
            0,
            0,
            0404040, // indirect address points here
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
