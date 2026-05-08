/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLCFFunction extends FunctionUnitTest {

    private long lcfBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 005, a, x, h, i, u);
    }

    private long lcfEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 005, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testLCF_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = lcfEM(0, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_000056_217704L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(0).setW(0_0256L);
        _engine.getExecOrUserARegister(1).setW(0_222333_333222L);

        run();

        assertEquals(0_0256L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_253_562177040L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testLCF_Alternate_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = lcfEM(0, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_056217_704077L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(0).setW(0_0256L);
        _engine.getExecOrUserARegister(1).setW(0_222333_333222L);

        run();

        assertEquals(0_0256L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_264_562177040L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testLCF_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = lcfEM(0, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_700655_443322L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(0).setW(0_0200L);
        _engine.getExecOrUserARegister(1).setW(0_222333_333222L);

        run();

        assertEquals(0_0200L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_571_006554433L, _engine.getExecOrUserARegister(1).getW());
    }
}
