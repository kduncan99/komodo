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
 * Test Less Than Zero instruction
 * (TLZ) skips if (U) < -0.
 * Extended Mode only, f=050, j=0, a=010.
 */
public class TestTLZFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tlzImm(long j, long u) {
        return fjaxu(050, j, 010, 0, u);
    }

    private long tlzEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 010, x, h, i, b, d);
    }

    @Test
    public void testTLZ_Immediate_EM() throws MachineInterrupt {
        var code = new long[] {
            tlzImm(Constants.JFIELD_XU, 0777776),
            0,
            tlzImm(Constants.JFIELD_U, 0377777),
            0,
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLZ_W_EM() throws MachineInterrupt {
        var code = new long[] {
            tlzEM(Constants.JFIELD_W, 3, 0, 0, 2, 0),
            0,
            tlzEM(Constants.JFIELD_W, 3, 0, 0, 2, 01),
            0,
            0
        };

        var data = new long[] {
            0_777777_777776L,
            0_000000_000001L
        };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLZ_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tlzEM(Constants.JFIELD_W, 3, 0, 0, 2, 0),
            0,
            tlzEM(Constants.JFIELD_W, 3, 0, 0, 2, 01),
            0,
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        assertThrows(ReferenceViolationInterrupt.class, this::run);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLZ_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            tlzEM(Constants.JFIELD_W, 0, 0, 0, 0, GRS_A5),
            0,
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(5).setW(0_400003_000003L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLZ_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tlzEM(Constants.JFIELD_W, 0, 0, 0, 0, 040),
            0,
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTLZ_XH1_EM() throws MachineInterrupt {
        var code = new long[] {
            tlzEM(Constants.JFIELD_XH1, 0, 0, 0, 2, 0),
            0,
            0
        };

        var data = new long[] { 0_711111_111111L };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0_0, 0_777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
