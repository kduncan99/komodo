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
 * Masked Test Not Equal instruction
 * (MTNE) Checks the logical AND of U AND R2 to see if the result is NOT equal to
 * the logical AND of A(a) AND R2.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * f=071, j=001. Extended mode only.
 */
public class TestMTNEFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long mtneEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 1, a, x, h, i, b, d);
    }

    @Test
    public void testMTNE_W_EM() throws MachineInterrupt {
        var code = new long[] {
            mtneEM(2, 3, 0, 0, 2, 0), // (U & R2) = (000001000123 & 777777777777) = 000001000123. Match A2. No skip.
            mtneEM(2, 3, 0, 0, 2, 01), // (U & R2) = (000001123000 & 777777777777) = 000001123000. Not Match A2. Skip.
            0,
            0,
            };

        var data = new long[] {
            0_000001_000123L,
            0_000001_123000L,
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
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        // 1st MTNE: no skip (match). PC from 1000 to 1001.
        // 2nd MTNE: skip (no match). PC from 1001 to 1003.
        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNE_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            mtneEM(2, 3, 0, 0, 2, 0),
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        assertThrows(ReferenceViolationInterrupt.class, () -> run());

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNE_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            mtneEM(2, 0, 0, 0, 0, GRS_A5), // (U & R2) = (3_000003 & 777777777777) = 3_000003. Match A2. No skip.
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
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        run();

        // No skip. PC from 1000 to 1001. Stop at 0.
        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNE_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            mtneEM(2, 0, 0, 0, 0, 040),
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
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNE_DualMasking() throws MachineInterrupt, com.bearsnake.komodo.engine.exceptions.EngineHaltedException {
        var code = new long[] {
            mtneEM(2, 0, 0, 0, 2, 0), // (U & R2) = (0_777777_000000 & 0_000000_777777) = 0.
                                      // (A2 & R2) = (0_000000_777777 & 0_000000_777777) = 0_000000_777777.
                                      // Not equal! Skip. PC: 1000 -> 1002.
            0,
            mtneEM(2, 0, 0, 0, 2, 01), // (U & R2) = (0_123456_765432 & 0_000000_777777) = 0_765432.
                                       // (A2 & R2) = (0_000000_765432 & 0_000000_777777) = 0_765432.
                                       // Equal! No skip. PC: 1002 -> 1003.
            0,
            0,
        };

        var data = new long[] {
            0_777777_000000L,
            0_123456_765432L,
        };
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
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_777777L);
        _engine.getExecOrUserRRegister(2).setW(0_000000_777777L);

        // Run 1st MTNE. Skip. PC: 1000 -> 1002.
        _engine.cycle();
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());

        // Setup for 2nd MTNE specifically.
        _engine.getExecOrUserARegister(2).setW(0_000000_765432L);

        // Run 2nd MTNE at 1002. No skip. PC: 1002 -> 1003.
        _engine.cycle();
        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNE_Masking_Skip() throws MachineInterrupt {
        var code = new long[] {
            mtneEM(2, 0, 0, 0, 2, 0), // (U & R2) = (0777777777777 & 1) = 1. (A2 & R2) = (0777777 & 1) = 0.
                                      // Not equal. Skip. PC: 1000 -> 1002.
            0,
            0,
        };

        var data = new long[] { 0_777777_777777L };
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
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_777776L);
        _engine.getExecOrUserRRegister(2).setW(1L);

        run();

        // Not match -> skip to 1002.
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
