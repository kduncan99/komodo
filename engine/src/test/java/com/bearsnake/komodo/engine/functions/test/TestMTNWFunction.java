/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Masked Test Not Within Range instruction
 * (MTNW) Checks the following:
 *     ! ( (A(a) AND R2) < ((U) AND R2) <= (A(a+1) AND R2)) )
 * If the test succeeds, skip the next instruction by incrementing the program counter.
 */
public class TestMTNWFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long mtnwEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 5, a, x, h, i, b, d);
    }

    @Test
    public void testMTNW_W_EM() throws MachineInterrupt {
        var code = new long[] {
            mtnwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 50 <= 200 -> Not In Range -> Skip
            0,
            mtnwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 250 <= 200 -> Not In Range -> Skip
            0,
            0,
            0,
            };

        var data = new long[] {
            50L,
            250L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01004, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNW_NoMatch() throws MachineInterrupt {
        var code = new long[] {
            mtnwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 150 <= 200 -> In Range -> No Skip
            0,
            };

        var data = new long[] {
            150L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNW_UpperBoundary() throws MachineInterrupt {
        var code = new long[] {
            mtnwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 200 <= 200 -> In Range -> No Skip
            0,
            };

        var data = new long[] {
            200L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNW_LowerBoundary() throws MachineInterrupt {
        var code = new long[] {
            mtnwEM(2, 3, 0, 0, 2, 0), // (A2) < (U) <= (A3) ? 100 < 100 <= 200 -> Not In Range -> Skip
            0,
            0,
            };

        var data = new long[] {
            100L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNW_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            mtnwEM(2, 0, 0, 0, 0, GRS_A5), // (A2) < (A5) <= (A3) ? 10 < 5 <= 20 -> Skip
            0,
            0,
            };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(10L);
        _engine.getExecOrUserARegister(3).setW(20L);
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(5).setW(5L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMTNW_Masking() throws MachineInterrupt {
        var code = new long[] {
            mtnwEM(2, 0, 0, 0, 2, 0), // (A2&R2) < (U&R2) <= (A3&R2) ? (100&7) < (123&7) <= (200&7)
                                     // 4 < 3 <= 0 -> Not In Range -> Skip
            0,
            0,
            };

        var data = new long[] {
            123L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(100L);
        _engine.getExecOrUserARegister(3).setW(200L);
        _engine.getExecOrUserRRegister(2).setW(7L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
