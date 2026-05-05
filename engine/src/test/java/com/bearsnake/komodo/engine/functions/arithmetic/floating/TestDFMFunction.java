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

public class TestDFMFunction extends FunctionUnitTest {

    private long dfmBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 012, a, x, h, i, u);
    }

    private long dfmEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 012, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testDFM_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = dfmEM(5, 5, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0;
        data[6] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);

        run();

        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDFM_One_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        var oneMSW = 0_2001_40000000L;
        var oneLSW = 0_0L;
        var lotsMSW = 0_2004_43217654L;
        var lotsLSW = 0_765432_654321L;

        code[0] = dfmEM(5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = lotsMSW;
        data[6] = lotsLSW;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(oneMSW);
        _engine.getExecOrUserARegister(6).setW(oneLSW);

        run();

        assertEquals(lotsMSW, _engine.getExecOrUserARegister(5).getW());
        assertEquals(lotsLSW, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDFM_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = dfmEM(6, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_1754_66666666L;
        data[6] = 0_666666_666666L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(6).setW(0_2032_44444444L);
        _engine.getExecOrUserARegister(7).setW(0_444444_444444L);

        run();

        assertEquals(0_2005_76543207L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_654320_765430L, _engine.getExecOrUserARegister(7).getW());
    }

    // TODO need underflow, overflow, with/without operation trap
}
