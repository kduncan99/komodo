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
 * Double-Precision Test Equal instruction
 * (DTE) skips if (U | U+1) = A(a) | A(a+1).
 * f=071, j=017 for both modes.
 */
public class TestDTEFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long dteBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_71, 0_17, a, x, h, i, u);
    }

    private long dteEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_71, 0_17, a, x, h, i, b, d);
    }

    @Test
    public void testDTE_BM() throws MachineInterrupt {
        var code = new long[] {
            dteBM(2, 0, 0, 0, 0_20000), // Equal, skip
            0,
            dteBM(2, 0, 0, 0, 0_20002), // Not equal, no skip
            0,
            0
        };

        var data = new long[] {
            0_123456_654321L,
            0_777777_000000L,
            0_123456_654321L,
            0_777777_000001L,
        };

        loadBaseRegister((short) 12, false, 0_1000, 0_17777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 15, false, 0_20000, 0_207777, AbsoluteAddress.construct(0, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_123456_654321L);
        _engine.getExecOrUserARegister(3).setW(0_777777_000000L);

        run();

        // 0_1000: dteBM(2, 0, 0, 0, 0_2000) -> skips 0_1001 to 0_1002
        // 0_1002: dteBM(2, 0, 0, 0, 0_2002) -> no skip, next is 0_1003
        // 0_1003: 0 -> halt
        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTE_EM() throws MachineInterrupt {
        var code = new long[] {
            dteEM(2, 0, 0, 0, 2, 0), // Equal, skip
            0,
            dteEM(2, 0, 0, 0, 2, 2), // Not equal, no skip
            0,
            0,
            };

        var data = new long[] {
            0_111111_222222L,
            0_333333_444444L,
            0_111111_222222L,
            0_333333_444445L,
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_0777, AbsoluteAddress.construct(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_111111_222222L);
        _engine.getExecOrUserARegister(3).setW(0_333333_444444L);

        run();

        assertEquals(0_1003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTE_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            dteEM(2, 0, 0, 0, 0, GRS_A5), // Equal, skip
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(3).setW(0_000004_000004L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(6).setW(0_000004_000004L);

        run();

        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDTE_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            dteEM(2, 0, 0, 0, 2, 0), // Point to missing bank
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000001_000123L);
        _engine.getExecOrUserARegister(3).setW(0_000001_123000L);

        // This will now throw NullPointerException in Engine.getConsecutiveOperands
        assertThrows(ReferenceViolationInterrupt.class, () -> run());

        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
