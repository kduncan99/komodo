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
 * Masked Test Less Than or Equal instruction
 * (MTLE) Checks the logical AND of U AND R2 to see if the result is not greater than
 * the logical AND of A(a) AND R2.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * f=071, j=002. Extended mode only.
 */
public class TestMTLEFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long mtleEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 2, a, x, h, i, b, d);
    }

    @Test
    public void testMTLE_W_EM() throws MachineInterrupt, com.bearsnake.komodo.engine.exceptions.EngineHaltedException {
        var code = new long[] {
            mtleEM(2, 3, 0, 0, 2, 0),   // U & R2 = 123, A2 & R2 = 123. Match (equal). PC: 1000 -> 1002.
            0,
            mtleEM(2, 3, 0, 0, 2, 01),  // U & R2 = 123000, A2 & R2 = 123. No match (greater). PC: 1002 -> 1003.
            0,
            mtleEM(2, 3, 0, 0, 2, 01),  // Re-run the greater case, now at 1003.
            0,
            };

        var data = new long[] {
            0_000001_000123L,
            0_000001_123000L,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        // Run MTLE at 1000. Equal -> Skip. PC: 1002.
        _engine.cycle();
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());

        // Run MTLE at 1002. Greater (123000 > 123) -> No skip. PC: 1003.
        _engine.cycle();
        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());

        // Now setup for less than.
        _engine.getExecOrUserARegister(2).setW(0_000001_123001L); // A2 = 123001. U = 123000.
        // Run MTLE at 1004 (Wait, the code array has 1003 as 0? No, 1002 is MTLE, 1003 is 0, 1004 is MTLE)
        // Correcting my mental map:
        // 1000: MTLE (equal) -> skips 1001. PC = 1002.
        // 1002: MTLE (greater) -> no skip. PC = 1003.
        // 1003: 0 (This will probably cause some instruction issue if I run cycle again? Oh wait, TestFunction.run() runs until 0)
        // I should just check the cycle at 1004.
        _engine.getProgramAddressRegister().setProgramCounter(0_1004);
        _engine.cycle(); // 123000 <= 123001 -> Skip. PC: 1004 -> 1006.
        assertEquals(0_1006, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTLE_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            mtleEM(2, 3, 0, 0, 2, 0),
            0,
            mtleEM(2, 3, 0, 0, 2, 01),
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
    public void testMTLE_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            mtleEM(2, 0, 0, 0, 0, GRS_A5),
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

        run(); // 3 <= 3 -> Skip. PC: 1000 -> 1002.

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTLE_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            mtleEM(2, 0, 0, 0, 0, 040),
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
    public void testMTLE_DualMasking() throws MachineInterrupt, com.bearsnake.komodo.engine.exceptions.EngineHaltedException {
        var code = new long[] {
            mtleEM(2, 0, 0, 0, 2, 0), // (U & R2) = (0_777777_000000 & 0_000000_777777) = 0.
                                      // (A2 & R2) = (0_000000_777777 & 0_000000_777777) = 0_000000_777777.
                                      // 0 <= 777777. Match! Skip. PC: 1000 -> 1002.
            0,
            mtleEM(2, 0, 0, 0, 2, 1), // (U & R2) = (0_123456_765432 & 0_000000_777777) = 0_765432.
                                      // (A2 & R2) = (0_000000_765432 & 0_000000_777777) = 0_765432.
                                      // Match! Skip. PC: 1002 -> 1004.
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

        // Run MTLE at 1000. 0 <= 777777 -> skip. PC: 1002.
        _engine.cycle();
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());

        // Now we are at 1002. A2=777777, U[1]=123456_765432.
        // Setup for equal.
        _engine.getExecOrUserARegister(2).setW(0_000000_765432L);

        // MTLE at 1002 (matches now): skip. PC: 1002 -> 1004.
        _engine.cycle();
        assertEquals(0_1004, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTLE_Masking_NoMatch() throws MachineInterrupt {
        var code = new long[] {
            mtleEM(2, 0, 0, 0, 2, 0), // (U & R2) = (0777777777777 & 1) = 1. (A2 & R2) = (000000000000 & 1) = 0.
                                      // 1 > 0. No match. No skip. PC: 1000 -> 1001.
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

        _engine.getExecOrUserARegister(2).setW(0_000000L);
        _engine.getExecOrUserRRegister(2).setW(1L);

        run();

        // No match -> stop at 1001.
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
