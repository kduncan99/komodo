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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCDUFunction extends FunctionUnitTest {

    private long cduBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(076, 007, a, x, h, i, u);
    }

    private long cduEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(076, 007, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testCDU_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = cduEM(2, 0, 0, 0, 2, 0_1001);
        code[1] = cduEM(4, 0, 0, 0, 2, 0_1002);
        code[2] = cduEM(6, 0, 0, 0, 2, 0_1001);
        code[3] = cduEM(8, 0, 0, 0, 2, 0_1002);
        code[4] = 0;
        data[1] = 0;
        data[2] = 0_777777_777777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(2).setW(0L);
        _engine.getExecOrUserARegister(3).setW(0_444444_444444L);
        _engine.getExecOrUserARegister(4).setW(0L);
        _engine.getExecOrUserARegister(5).setW(0_444444_444444L);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(7).setW(0_444444_444444L);
        _engine.getExecOrUserARegister(8).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(9).setW(0_444444_444444L);


        run();

        assertEquals(0, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(7).getW());
        assertEquals(0, _engine.getExecOrUserARegister(9).getW());
    }

    @Test
    public void testCDU_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = cduEM(4, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_210_427365000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_175_532475022L);
        _engine.getExecOrUserARegister(5).setW(0_765_123456753L);

        run();

        assertEquals(0_777777_777764L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testCDU_Canonical_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = cduEM(4, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_245_427365000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_257_532475022L);
        _engine.getExecOrUserARegister(5).setW(0_765_123456753L);

        run();

        assertEquals(0_000000_000012L, _engine.getExecOrUserARegister(5).getW());
    }
}
