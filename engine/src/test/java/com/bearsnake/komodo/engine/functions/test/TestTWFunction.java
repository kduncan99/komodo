/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Within Range instruction
 * (TW) skips if A(a) < (U) <= A(a+1).
 * f=056 for both modes.
 */
public class TestTWFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long twImm(long j, long a, long x, long u) {
        return fjaxu(0_56, j, a, x, u);
    }

    private long twBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_56, j, a, x, h, i, u);
    }

    private long twEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_56, j, a, x, h, i, b, d);
    }

    @Test
    public void testTW_BM_Immediate_Skip() throws MachineInterrupt {
        var code = new long[] {
            twImm(Constants.JFIELD_U, 2, 0, 010),
            0,
            0
        };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_BM_Immediate_NoSkip_Below() throws MachineInterrupt {
        var code = new long[] {
            twImm(Constants.JFIELD_U, 2, 0, 005),
            0,
            0
        };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_EM_Memory_Skip_Boundary() throws MachineInterrupt {
        var code = new long[] {
            twEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] {
            0_000000_000015L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_EM_Memory_NoSkip_Above() throws MachineInterrupt {
        var code = new long[] {
            twEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] {
            0_000000_000016L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_BM_Immediate_NoSkip_Boundary_Below() throws MachineInterrupt {
        var code = new long[] {
            twImm(Constants.JFIELD_U, 2, 0, 005),
            0,
            0
        };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_EM_Immediate_Skip_Between() throws MachineInterrupt {
        var code = new long[] {
            twImm(Constants.JFIELD_U, 2, 0, 010),
            0,
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_EM_Memory_NoSkip_Below() throws MachineInterrupt {
        var code = new long[] {
            twEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] {
            0_000000_000004L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTW_EM_Memory_Skip_Boundary_High() throws MachineInterrupt {
        var code = new long[] {
            twEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[] {
            0_000000_000015L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000005L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000015L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
