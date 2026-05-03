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

public class TestINCFunction extends FunctionUnitTest {

    private long incImm(long x, long h, long i, long u) {
        return fjaxhiu(005, JFIELD_U, 010, x, h, i, u);
    }

    private long incXImm(long x, long h, long i, long u) {
        return fjaxhiu(005, JFIELD_XU, 010, x, h, i, u);
    }

    private long incBM(long j, long x, long h, long i, long u) {
        return fjaxhiu(005, j, 010, x, h, i, u);
    }

    private long incEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(005, j, 010, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testINC_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1001);
        code[2] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1002);
        code[3] = 0;

        data[0] = 0;
        data[1] = 0_777777_777777L;
        data[2] = 0_777777_777776L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(1, data[0]);
        assertEquals(1, data[1]);
        assertEquals(0, data[2]);
    }

    @Test
    public void testINC_NoCarryOverflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = 0;

        data[0] = 0_333222_111000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_333222_111001L, data[0]);
        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testINC_Carry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
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
    public void testINC_Overflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        data[0] = 0_377777_777777L;

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testINC_JField_ThirdWord_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = incEM(Constants.JFIELD_H2, 0, 0, 0, 2, 0_1001);
        code[2] = incEM(Constants.JFIELD_H1, 0, 0, 0, 2, 0_1002);
        code[3] = incEM(Constants.JFIELD_XH2, 0, 0, 0, 2, 0_1003);
        code[4] = incEM(Constants.JFIELD_XH1, 0, 0, 0, 2, 0_1004);
        code[5] = incEM(Constants.JFIELD_T3, 0, 0, 0, 2, 0_1005);
        code[6] = incEM(Constants.JFIELD_T2, 0, 0, 0, 2, 0_1006);
        code[7] = incEM(Constants.JFIELD_T1, 0, 0, 0, 2, 0_1007);
        code[010] = incEM(Constants.JFIELD_S6, 0, 0, 0, 2, 0_1010);
        code[011] = incEM(Constants.JFIELD_S5, 0, 0, 0, 2, 0_1011);
        code[012] = incEM(Constants.JFIELD_S4, 0, 0, 0, 2, 0_1012);
        code[013] = incEM(Constants.JFIELD_S3, 0, 0, 0, 2, 0_1013);
        code[014] = incEM(Constants.JFIELD_S2, 0, 0, 0, 2, 0_1014);
        code[015] = incEM(Constants.JFIELD_S1, 0, 0, 0, 2, 0_1015);
        code[016] = incImm(0, 1, 1, 0_177777);
        code[017] = incXImm(0, 1, 1, 0_177777);
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

        assertEquals(01, data[0]);
        assertEquals(0_777777_000000L, data[1]);
        assertEquals(0_000000_777777L, data[2]);
        assertEquals(0_777777_000001L, data[3]);
        assertEquals(0_000001_777777L, data[4]);
        assertEquals(0_7777_7777_0001L, data[5]);
        assertEquals(0_7777_0001_7777L, data[6]);
        assertEquals(0_0001_7777_7777L, data[7]);
        assertEquals(0_777777_777700L, data[010]);
        assertEquals(0_777777_770077L, data[011]);
        assertEquals(0_777777_007777L, data[012]);
        assertEquals(0_777700_777777L, data[013]);
        assertEquals(0_770077_777777L, data[014]);
        assertEquals(0_007777_777777L, data[015]);
    }

    @Test
    public void testINC_JField_QuarterWord_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 2, 0_1000);
        code[1] = incEM(Constants.JFIELD_H2, 0, 0, 0, 2, 0_1001);
        code[2] = incEM(Constants.JFIELD_H1, 0, 0, 0, 2, 0_1002);
        code[3] = incEM(Constants.JFIELD_XH2, 0, 0, 0, 2, 0_1003);
        code[4] = incEM(Constants.JFIELD_Q2, 0, 0, 0, 2, 0_1004);
        code[5] = incEM(Constants.JFIELD_Q4, 0, 0, 0, 2, 0_1005);
        code[6] = incEM(Constants.JFIELD_Q3, 0, 0, 0, 2, 0_1006);
        code[7] = incEM(Constants.JFIELD_Q1, 0, 0, 0, 2, 0_1007);
        code[010] = incEM(Constants.JFIELD_S6, 0, 0, 0, 2, 0_1010);
        code[011] = incEM(Constants.JFIELD_S5, 0, 0, 0, 2, 0_1011);
        code[012] = incEM(Constants.JFIELD_S4, 0, 0, 0, 2, 0_1012);
        code[013] = incEM(Constants.JFIELD_S3, 0, 0, 0, 2, 0_1013);
        code[014] = incEM(Constants.JFIELD_S2, 0, 0, 0, 2, 0_1014);
        code[015] = incEM(Constants.JFIELD_S1, 0, 0, 0, 2, 0_1015);
        code[022] = 0;

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
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(01, data[0]);
        assertEquals(0_777777_000000L, data[1]);
        assertEquals(0_000000_777777L, data[2]);
        assertEquals(0_777777_000001L, data[3]);
        assertEquals(0_777000_777777L, data[4]);
        assertEquals(0_777777_777000L, data[5]);
        assertEquals(0_777777_000777L, data[6]);
        assertEquals(0_000777_777777L, data[7]);
        assertEquals(0_777777_777700L, data[010]);
        assertEquals(0_777777_770077L, data[011]);
        assertEquals(0_777777_007777L, data[012]);
        assertEquals(0_777700_777777L, data[013]);
        assertEquals(0_770077_777777L, data[014]);
        assertEquals(0_007777_777777L, data[015]);
    }

    @Test
    public void testINC_GRS_EM() throws MachineInterrupt {
        var code = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 0, GRS_A0);
        code[1] = 0_777777_777777L; // this is skipped
        code[2] = incEM(Constants.JFIELD_U, 0, 0, 0, 0, GRS_A1);
        code[3] = 0_777777_777777L; // this is skipped
        code[4] = incEM(Constants.JFIELD_XU, 0, 0, 0, 0, GRS_A2);
        code[5] = 0_777777_777777L; // this is skipped
        code[6] = 0;

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

        assertEquals(0_200000_000000L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_177777_777777L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_177777_777777L, _engine.getExecOrUserARegister(2).getW());
    }

    @Test
    public void testINC_GRS_BM() throws MachineInterrupt {
        var code = new long[0_1000];

        code[0] = incEM(Constants.JFIELD_W, 0, 0, 0, 0, GRS_A0);
        code[1] = incEM(Constants.JFIELD_T2, 0, 0, 0, 0, GRS_A1);
        code[2] = incEM(Constants.JFIELD_S4, 0, 0, 0, 0, GRS_A2);
        code[3] = 0;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0_777777_777776L);
        _engine.getExecOrUserARegister(1).setW(0_777777_777776L);
        _engine.getExecOrUserARegister(2).setW(0_777777_777776L);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(2).getW());
    }
}
