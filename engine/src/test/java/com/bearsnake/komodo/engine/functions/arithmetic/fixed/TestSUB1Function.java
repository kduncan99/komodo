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

public class TestSUB1Function extends FunctionUnitTest {

    private long sub1Imm(long x, long h, long i, long u) {
        return fjaxhiu(005, JFIELD_U, 016, x, h, i, u);
    }

    private long sub1BM(long j, long x, long h, long i, long u) {
        return fjaxhiu(005, j, 016, x, h, i, u);
    }

    private long sub1EM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(005, j, 016, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testSUB1_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1001);
        code[2] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1002);
        code[3] = 0;

        data[0] = 1;
        data[1] = 0L;
        data[2] = 0_777777_777777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0, data[0]);
        assertEquals(0_777777_777776L, data[1]);
        assertEquals(0_777777_777776L, data[2]);
    }

    @Test
    public void testSUB1_NoCarryOverflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = 0;

        data[0] = 0L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_777777_777776L, data[0]);
        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testSUB1_Carry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = 0;

        data[0] = 0_777777_777777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertTrue(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testSUB1_Overflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        data[0] = 0_400000_000000L;

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertTrue(_engine.getDesignatorRegister().isCarry());
        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testSUB1_JField_ThirdWord_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = sub1EM(Constants.JFIELD_H2, 0, 0, 0, 2, 0_1001);
        code[2] = sub1EM(Constants.JFIELD_H1, 0, 0, 0, 2, 0_1002);
        code[3] = sub1EM(Constants.JFIELD_XH2, 0, 0, 0, 2, 0_1003);
        code[4] = sub1EM(Constants.JFIELD_XH1, 0, 0, 0, 2, 0_1004);
        code[5] = sub1EM(Constants.JFIELD_T3, 0, 0, 0, 2, 0_1005);
        code[6] = sub1EM(Constants.JFIELD_T2, 0, 0, 0, 2, 0_1006);
        code[7] = sub1EM(Constants.JFIELD_T1, 0, 0, 0, 2, 0_1007);
        code[010] = sub1EM(Constants.JFIELD_S6, 0, 0, 0, 2, 0_1010);
        code[011] = sub1EM(Constants.JFIELD_S5, 0, 0, 0, 2, 0_1011);
        code[012] = sub1EM(Constants.JFIELD_S4, 0, 0, 0, 2, 0_1012);
        code[013] = sub1EM(Constants.JFIELD_S3, 0, 0, 0, 2, 0_1013);
        code[014] = sub1EM(Constants.JFIELD_S2, 0, 0, 0, 2, 0_1014);
        code[015] = sub1EM(Constants.JFIELD_S1, 0, 0, 0, 2, 0_1015);
        code[016] = sub1EM(Constants.JFIELD_U, 0, 0, 0, 0, 0_1016);
        code[017] = sub1EM(Constants.JFIELD_XU, 0, 0, 0, 0, 0_1017);
        code[020] = 0;

        data[0] = 0L;
        data[1] = 0L;
        data[2] = 0L;
        data[3] = 0L;
        data[4] = 0L;
        data[5] = 0L;
        data[6] = 0L;
        data[7] = 0L;
        data[010] = 0L;
        data[011] = 0L;
        data[012] = 0L;
        data[013] = 0L;
        data[014] = 0L;
        data[015] = 0L;
        data[016] = 0L;
        data[017] = 0L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_777777_777776L, data[0]);
        assertEquals(0_000000_777777L, data[1]);
        assertEquals(0_777777_000000L, data[2]);
        assertEquals(0_000000_777776L, data[3]);
        assertEquals(0_777776_000000L, data[4]);
        assertEquals(0_0000_0000_7776L, data[5]);
        assertEquals(0_0000_7776_0000L, data[6]);
        assertEquals(0_7776_0000_0000L, data[7]);
        assertEquals(0_000000_000077L, data[010]);
        assertEquals(0_000000_007700L, data[011]);
        assertEquals(0_000000_770000L, data[012]);
        assertEquals(0_000077_000000L, data[013]);
        assertEquals(0_007700_000000L, data[014]);
        assertEquals(0_770000_000000L, data[015]);
        assertEquals(0L, data[016]);
        assertEquals(0L, data[017]);
    }

    @Test
    public void testSUB1_JField_QuarterWord_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = sub1EM(Constants.JFIELD_H2, 0, 0, 0, 2, 0_1001);
        code[2] = sub1EM(Constants.JFIELD_H1, 0, 0, 0, 2, 0_1002);
        code[3] = sub1EM(Constants.JFIELD_XH2, 0, 0, 0, 2, 0_1003);
        code[4] = sub1EM(Constants.JFIELD_Q2, 0, 0, 0, 2, 0_1004);
        code[5] = sub1EM(Constants.JFIELD_Q4, 0, 0, 0, 2, 0_1005);
        code[6] = sub1EM(Constants.JFIELD_Q3, 0, 0, 0, 2, 0_1006);
        code[7] = sub1EM(Constants.JFIELD_Q1, 0, 0, 0, 2, 0_1007);
        code[010] = sub1EM(Constants.JFIELD_S6, 0, 0, 0, 2, 0_1010);
        code[011] = sub1EM(Constants.JFIELD_S5, 0, 0, 0, 2, 0_1011);
        code[012] = sub1EM(Constants.JFIELD_S4, 0, 0, 0, 2, 0_1012);
        code[013] = sub1EM(Constants.JFIELD_S3, 0, 0, 0, 2, 0_1013);
        code[014] = sub1EM(Constants.JFIELD_S2, 0, 0, 0, 2, 0_1014);
        code[015] = sub1EM(Constants.JFIELD_S1, 0, 0, 0, 2, 0_1015);
        code[016] = sub1EM(Constants.JFIELD_U, 0, 0, 0, 0, 0_1016);
        code[017] = sub1EM(Constants.JFIELD_XU, 0, 0, 0, 0, 0_1017);
        code[020] = 0;

        data[0] = 0L;
        data[1] = 0L;
        data[2] = 0L;
        data[3] = 0L;
        data[4] = 0L;
        data[5] = 0L;
        data[6] = 0L;
        data[7] = 0L;
        data[010] = 0L;
        data[011] = 0L;
        data[012] = 0L;
        data[013] = 0L;
        data[014] = 0L;
        data[015] = 0L;
        data[016] = 0L;
        data[017] = 0L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_777777_777776L, data[0]);
        assertEquals(0_000000_777777L, data[1]);
        assertEquals(0_777777_000000L, data[2]);
        assertEquals(0_000000_777776L, data[3]);
        assertEquals(0_000777_000000L, data[4]);
        assertEquals(0_000000_000777L, data[5]);
        assertEquals(0_000000_777000L, data[6]);
        assertEquals(0_777000_000000L, data[7]);
        assertEquals(0_000000_000077L, data[010]);
        assertEquals(0_000000_007700L, data[011]);
        assertEquals(0_000000_770000L, data[012]);
        assertEquals(0_000077_000000L, data[013]);
        assertEquals(0_007700_000000L, data[014]);
        assertEquals(0_770000_000000L, data[015]);
        assertEquals(0L, data[016]);
        assertEquals(0L, data[017]);
    }

    @Test
    public void testSUB1_GRS_EM() throws MachineInterrupt {
        var code = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 0, GRS_A0);
        code[1] = sub1EM(Constants.JFIELD_U, 0, 0, 0, 0, GRS_A1);
        code[2] = sub1EM(Constants.JFIELD_XU, 0, 0, 0, 0, GRS_A2);
        code[3] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0_177777_777777L);
        _engine.getExecOrUserARegister(1).setW(0_177777_777777L);
        _engine.getExecOrUserARegister(2).setW(0_177777_777777L);

        run();

        assertEquals(0_177777_777776L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_177777_777777L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_177777_777777L, _engine.getExecOrUserARegister(2).getW());
    }

    @Test
    public void testSUB1_GRS_BM() throws MachineInterrupt {
        var code = new long[0_1000];

        code[0] = sub1EM(Constants.JFIELD_W, 0, 0, 0, 0, GRS_A0);
        code[1] = sub1EM(Constants.JFIELD_T2, 0, 0, 0, 0, GRS_A1);
        code[2] = sub1EM(Constants.JFIELD_S4, 0, 0, 0, 0, GRS_A2);
        code[3] = 0;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0L);
        _engine.getExecOrUserARegister(1).setW(0L);
        _engine.getExecOrUserARegister(2).setW(0L);

        run();

        assertEquals(0_777777_777776L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_777777_777776L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(2).getW());
    }
}
