/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.ArithmeticExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.JFIELD_W;
import static org.junit.jupiter.api.Assertions.*;

public class TestDIFunction extends FunctionUnitTest {

    private long diBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(034, j, a, x, h, i, u);
    }

    private long diEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(034, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testDI_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = diEM(JFIELD_W, 2, 0, 0, 0, 2, 0_1002);
        code[1] = 0;
        data[2] = 0_000001_635035L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setArithmeticExceptionEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000000_011416L);
        _engine.getExecOrUserARegister(3).setW(0_110621_672145L);

        run();

        assertEquals(0_005213_747442L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_000000_244613L, _engine.getExecOrUserARegister(3).getW());
        assertFalse(_engine.getDesignatorRegister().isDivideCheck());
    }

    @Test
    public void testDI_DivideByZero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = diEM(JFIELD_W, 2, 0, 0, 0, 2, 0_1002);
        code[1] = 0;
        data[2] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setArithmeticExceptionEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0_000000_011416L);
        _engine.getExecOrUserARegister(3).setW(0_110621_672145L);

        var i = assertThrows(ArithmeticExceptionInterrupt.class, this::run);
        assertEquals(ArithmeticExceptionInterrupt.Reason.DivideCheck.getCode(), i.getShortStatusField());
        assertTrue(_engine.getDesignatorRegister().isDivideCheck());
        assertEquals(0, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testDI_QuotientOverflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = diEM(JFIELD_W, 2, 0, 0, 0, 2, 0_1002);
        code[1] = 0;
        data[2] = 0_1L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setArithmeticExceptionEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0L);
        _engine.getExecOrUserARegister(3).setW(0_400000_000000L);

        var i = assertThrows(ArithmeticExceptionInterrupt.class, this::run);
        assertEquals(ArithmeticExceptionInterrupt.Reason.DivideCheck.getCode(), i.getShortStatusField());
        assertTrue(_engine.getDesignatorRegister().isDivideCheck());
        assertEquals(0, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0, _engine.getExecOrUserARegister(3).getW());
    }
}
