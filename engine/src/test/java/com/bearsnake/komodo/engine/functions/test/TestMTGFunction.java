/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Masked Test Greater instruction
 * (MTG) Checks the logical AND of U AND R2 to see if the result is greater than
 * the logical AND of A(a) AND R2.
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 * f=071, j=003. Extended mode only.
 */
public class TestMTGFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long mtgEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 3, a, x, h, i, b, d);
    }

    @Test
    public void testMTG_W_EM() throws MachineInterrupt {
        var code = new long[] {
            mtgEM(2, 3, 0, 0, 2, 0),   // U & R2 = 123, A2 & R2 = 123. No skip (equal). PC: 1000 -> 1001.
            mtgEM(2, 3, 0, 0, 2, 1),   // U & R2 = 123000, A2 & R2 = 123. Skip (greater). PC: 1001 -> 1003.
            0,
            mtgEM(2, 3, 0, 0, 2, 0),   // Run at 1003. U & R2 = 123, A2 & R2 = 123000. No skip (less). PC: 1003 -> 1004.
            0,
            };

        var data = new long[] {
            0_000001_000123L,
            0_000001_123000L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_1004, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTG_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            mtgEM(2, 3, 0, 0, 2, 0),
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

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
    public void testMTG_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            mtgEM(2, 0, 0, 0, 0, GRS_A5), // U=4, A2=3. 4 > 3 -> Skip. PC: 1000 -> 1002.
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000003L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(5).setW(0_000000_000004L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTG_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            mtgEM(2, 0, 0, 0, 0, 040),
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

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
    public void testMTG_DualMasking() throws MachineInterrupt {
        var code = new long[] {
            mtgEM(2, 0, 0, 0, 2, 0), // (U & R2) = (0_777777_000000 & 0_000000_777777) = 0.
                                     // (A2 & R2) = (0_000000_777777 & 0_000000_777777) = 0_000000_777777.
                                     // 0 > 777777. No match. No skip. PC: 1000 -> 1001.
            mtgEM(2, 0, 0, 0, 2, 1), // (U & R2) = (0_123456_777777 & 0_000000_777777) = 0_777777.
                                     // (A2 & R2) = (0_000000_765432 & 0_000000_777777) = 0_765432.
                                     // 777777 > 765432. Match! Skip. PC: 1001 -> 1003.
            0,
            0,
        };

        var data = new long[] {
            0_777777_000000L,
            0_123456_777777L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_777777L);
        _engine.getExecOrUserRRegister(2).setW(0_000000_777777L);

        // Run MTG at 1000. 0 > 777777 -> no skip. PC: 1001.
        _engine.cycle();
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());

        // Now we are at 1001. A2=777777, U[1]=123456_777777.
        // Setup for match (A2 < U).
        _engine.getExecOrUserARegister(2).setW(0_000000_765432L);

        // MTG at 1001 (matches now): skip. PC: 1001 -> 1003.
        _engine.cycle();
        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTG_Masking_NoMatch() throws MachineInterrupt {
        var code = new long[] {
            mtgEM(2, 0, 0, 0, 2, 0), // (U & R2) = (0777777777777 & 1) = 1. (A2 & R2) = (000000000001 & 1) = 1.
                                     // 1 > 1. No match. No skip. PC: 1000 -> 1001.
            0,
            0,
        };

        var data = new long[] { 0_777777_777777L };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(1L);
        _engine.getExecOrUserRRegister(2).setW(1L);

        run();

        // No match -> stop at 1001.
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
