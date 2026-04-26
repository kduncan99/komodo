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
 * Test Greater instruction
 * (TG) skips if (U) > A(a).
 * f=055 for both modes.
 */
public class TestTGFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tgImm(long j, long a, long x, long u) {
        return fjaxu(0_55, j, a, x, u);
    }

    private long tgBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_55, j, a, x, h, i, u);
    }

    private long tgEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_55, j, a, x, h, i, b, d);
    }

    @Test
    public void testTG_Immediate_BM() throws MachineInterrupt {
        var code = new long[] {
            tgImm(Constants.JFIELD_U, 2, 0, 0404041),
            0,
            tgImm(Constants.JFIELD_XU, 2, 0, 0404040),
            0,
            0,
            };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_404040L);

        run();

        assertEquals(0_01003, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTG_Immediate_EM() throws MachineInterrupt {
        var code = new long[] {
            tgImm(Constants.JFIELD_U, 2, 0, 0404042),
            0,
            tgImm(Constants.JFIELD_XU, 2, 0, 0404041),
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);

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
    public void testTG_W_EM() throws MachineInterrupt {
        var code = new long[] {
            tgEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            tgEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),
            0,
            0,
            };

        var data = new long[] {
            0_000000_000000L,
            0_777777_777776L,
            };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0_22000, 0_22777, AbsoluteAddress.construct(0, 0), data);

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
    public void testTG_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tgEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 0),
            0,
            tgEM(Constants.JFIELD_W, 2, 3, 0, 0, 2, 01),
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
        _engine.getExecOrUserXRegister(3).setXM(0_22000);

        assertThrows(ReferenceViolationInterrupt.class, this::run);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTG_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            tgEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, GRS_A5),
            0,
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_400000_000000L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTG_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            tgEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, 040),
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
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, this::run);
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTG_Q3_EM() throws MachineInterrupt {
        var code = new long[] {
            tgEM(Constants.JFIELD_Q3, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_111111_111111L };


        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.construct(2, 0), data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_101L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTGIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            tgBM(Constants.JFIELD_W, 2, 0, 0, 1, 022004),
            0,
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005),
            fjaxhiu(0, 0, 0, 0, 0, 1, 022003),
            077004,
            0,
            0,
            };

        loadBaseRegister((short) 12, false, 0_22000, 0_22777, AbsoluteAddress.construct(0, 0), code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_77003L);

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
