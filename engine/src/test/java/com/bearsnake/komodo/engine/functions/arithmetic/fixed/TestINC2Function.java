/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestINC2Function extends FunctionUnitTest {

    private long inc2Imm(long x, long h, long i, long u) {
        return fjaxhiu(005, JFIELD_U, 012, x, h, i, u);
    }

    private long inc2XImm(long x, long h, long i, long u) {
        return fjaxhiu(005, JFIELD_XU, 012, x, h, i, u);
    }

    private long inc2BM(long j, long x, long h, long i, long u) {
        return fjaxhiu(005, j, 012, x, h, i, u);
    }

    private long inc2EM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(005, j, 012, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testINC2_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = inc2EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = inc2EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1001);
        code[2] = inc2EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1002);
        code[3] = 0_777777_777777L; // should be skipped (-1 and 1 are not zero)
        code[4] = inc2EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1003);
        code[5] = inc2EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1004);
        code[6] = 0_777777_777777L; // should be skipped
        code[7] = 0; // should NOT be skipped

        data[0] = 0;
        data[1] = 0_777777_777777L;
        data[2] = 0_777777_777776L;
        data[3] = 0_777777_777775L;
        data[4] = 0_777777_777774L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(2, data[0]);
        assertEquals(2, data[1]);
        assertEquals(1, data[2]);
        assertEquals(0, data[3]);
        assertEquals(0_777777_777776L, data[4]);
    }

    @Test
    public void testINC2_JField_ThirdWord_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = inc2EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = inc2EM(Constants.JFIELD_H2, 0, 0, 0, 2, 0_1001);
        code[2] = inc2EM(Constants.JFIELD_H1, 0, 0, 0, 2, 0_1002);
        code[3] = inc2EM(Constants.JFIELD_XH2, 0, 0, 0, 2, 0_1003);
        code[4] = inc2EM(Constants.JFIELD_XH1, 0, 0, 0, 2, 0_1004);
        code[5] = inc2EM(Constants.JFIELD_T3, 0, 0, 0, 2, 0_1005);
        code[6] = inc2EM(Constants.JFIELD_T2, 0, 0, 0, 2, 0_1006);
        code[7] = inc2EM(Constants.JFIELD_T1, 0, 0, 0, 2, 0_1007);
        code[010] = inc2EM(Constants.JFIELD_S6, 0, 0, 0, 2, 0_1010);
        code[011] = inc2EM(Constants.JFIELD_S5, 0, 0, 0, 2, 0_1011);
        code[012] = inc2EM(Constants.JFIELD_S4, 0, 0, 0, 2, 0_1012);
        code[013] = inc2EM(Constants.JFIELD_S3, 0, 0, 0, 2, 0_1013);
        code[014] = inc2EM(Constants.JFIELD_S2, 0, 0, 0, 2, 0_1014);
        code[015] = inc2EM(Constants.JFIELD_S1, 0, 0, 0, 2, 0_1015);
        code[016] = inc2Imm(0, 1, 1, 0_177777);
        code[017] = inc2XImm(0, 1, 1, 0_177777);
        code[020] = 0;

        data[0] = 0_777777_777777L;
        data[1] = 0_777777_777777L;
        data[2] = 0_777777_777777L;
        data[3] = 0_777777_777777L;
        data[4] = 0_777777_777777L;
        data[5] = 0_777777_777777L;
        data[6] = 0_777777_777777L;
        data[7] = 0_777777_777777L;
        data[010] = 0_777777_777777L;
        data[011] = 0_777777_777777L;
        data[012] = 0_777777_777777L;
        data[013] = 0_777777_777777L;
        data[014] = 0_777777_777777L;
        data[015] = 0_777777_777777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(02, data[0]);
        assertEquals(0_777777_000001L, data[1]);
        assertEquals(0_000001_777777L, data[2]);
        assertEquals(0_777777_000002L, data[3]);
        assertEquals(0_000002_777777L, data[4]);
        assertEquals(0_7777_7777_0002L, data[5]);
        assertEquals(0_7777_0002_7777L, data[6]);
        assertEquals(0_0002_7777_7777L, data[7]);
        assertEquals(0_777777_777701L, data[010]);
        assertEquals(0_777777_770177L, data[011]);
        assertEquals(0_777777_017777L, data[012]);
        assertEquals(0_777701_777777L, data[013]);
        assertEquals(0_770177_777777L, data[014]);
        assertEquals(0_017777_777777L, data[015]);
    }
}
