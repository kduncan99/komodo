/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFMFunction extends FunctionUnitTest {

    private long fmBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 002, a, x, h, i, u);
    }

    private long fmEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 002, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testFM_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = fmEM(5, 5, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0);

        run();

        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testFM_One_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        var one = 0_201_400_000_000L;
        var lots = 0_204_440_400_437L;

        code[0] = fmEM(5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = lots;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(one);

        run();

        assertEquals(lots, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testFM_Signs_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        var negativeFive = 0_203_500000000L ^ Word36.BIT_MASK;
        var positiveFour = 0_203_400000000L;

        code[0] = fmEM(0, 0, 0, 0, 2, 0_1000);
        code[1] = fmEM(1, 0, 0, 0, 2, 0_1001);
        code[2] = fmEM(2, 0, 0, 0, 2, 0_1000);
        code[3] = fmEM(3, 0, 0, 0, 2, 0_1001);
        code[4] = 0;
        data[0] = positiveFour;
        data[1] = negativeFive;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(0).setW(positiveFour);
        _engine.getExecOrUserARegister(1).setW(positiveFour);
        _engine.getExecOrUserARegister(2).setW(negativeFive);
        _engine.getExecOrUserARegister(3).setW(negativeFive);

        run();

        var negativeTwenty = 0_206_240000000L ^ Word36.BIT_MASK;
        var positiveTwentyFive = 0_205_620000000L;
        var positiveSixteen = 0_205_400000000L;

        assertEquals(positiveSixteen, _engine.getExecOrUserARegister(0).getW());
        assertEquals(negativeTwenty, _engine.getExecOrUserARegister(1).getW());
        assertEquals(negativeTwenty, _engine.getExecOrUserARegister(2).getW());
        assertEquals(positiveTwentyFive, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testFM_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = fmEM(5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_172_650454045L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_211_654321001L);

        run();

        assertEquals(0_203_543210122L, _engine.getExecOrUserARegister(5).getW());
    }

    // TODO need underflow, overflow, with/without operation trap
}
