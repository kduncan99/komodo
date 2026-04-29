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

public class TestAMAFunction extends FunctionUnitTest {

    private long amaImm(long a, long x, long u) {
        return fjaxu(016, JFIELD_U, a, x, u);
    }

    private long amaBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(016, j, a, x, h, i, u);
    }

    private long amaEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(016, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testAMA_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = amaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
        code[2] = amaEM(Constants.JFIELD_W, 7, 0, 0, 0, 2, 0_1007);
        code[3] = amaEM(Constants.JFIELD_W, 8, 0, 0, 0, 2, 0_1010);
        code[4] = 0;
        data[5] = 0;
        data[6] = 0_777777_777777L;
        data[7] = 0_777777_777777L;
        data[8] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(7).setW(0);
        _engine.getExecOrUserARegister(8).setW(0_777777_777777L);

        run();

        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0, _engine.getExecOrUserARegister(7).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(8).getW());
    }

    @Test
    public void testAMA_OneZeroAddend_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = amaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
        code[2] = 0;
        data[5] = 0_377777_777777L;
        data[6] = 0_777777_654321L;

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

        assertEquals(0_377777_777777L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_000000_123456L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testAMA_SameSigns_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = amaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
        code[2] = 0;
        data[5] = 0_100L;
        data[6] = 0_777777_777000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_300000_000000L);
        _engine.getExecOrUserARegister(6).setW(0_777777_000777L);

        run();

        assertEquals(0_300000_000100L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_001776L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testAMA_OppositeSigns_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = amaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
        code[5] = 0;
        data[5] = 0_5L;
        data[6] = 0_777777_777772L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_777777_777577L);
        _engine.getExecOrUserARegister(6).setW(0_1L);

        run();

        assertEquals(0_777777_777604L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_6L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testAMA_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaEM(Constants.JFIELD_W, 5, 4, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[10] = 0_100;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_100);
        _engine.getExecOrUserXRegister(4).setXM(5);

        run();

        assertEquals(0_200, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testAMA_Immediate_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaBM(Constants.JFIELD_U, 0, 0, 1, 1, 0177776); // U=0_777776
        code[1] = amaBM(Constants.JFIELD_XU, 1, 0, 1, 1, 0177776); // U=0_777776
        code[2] = 0;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(0).setW(0);
        _engine.getExecOrUserARegister(1).setW(0);

        run();

        assertEquals(0_000000_777776L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_1L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testAMA_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaBM(Constants.JFIELD_W, 10, 0, 0, 1, 0_1010);
        code[0_10] = fjaxhiu(0,0, 0, 0, 0, 1, 0_1020);
        code[0_20] = fjaxhiu(0, 0, 0, 0, 0, 0, 0_22002);
        data[2] = 0_777777_776543L;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(10).setW(0_10);

        run();

        assertEquals(0_000000_001244L, _engine.getExecOrUserARegister(10).getW());
    }

    @Test
    public void testAMA_PartialWord_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaBM(Constants.JFIELD_H2, 1, 0, 0, 0, 0_22002);
        code[1] = amaBM(Constants.JFIELD_S3, 2, 0, 0, 0, 0_22002);
        code[2] = amaBM(Constants.JFIELD_XH2, 3, 0, 0, 0, 0_22002);
        code[3] = amaBM(Constants.JFIELD_T1, 4, 0, 0, 0, 0_22002);
        data[2] = 0_112233_445566L;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(1).setW(0_100);
        _engine.getExecOrUserARegister(2).setW(0_100);
        _engine.getExecOrUserARegister(3).setW(0_100);
        _engine.getExecOrUserARegister(4).setW(0_100);

        run();

        assertEquals(0_000000_445666L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_000000_000133L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_000000_332311L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0_000000_001222L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testAMA_GRS_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaBM(Constants.JFIELD_W, 1, 0, 0, 0, GRS_R0);
        code[1] = amaBM(Constants.JFIELD_S3, 2, 0, 0, 0, GRS_X5);
        data[2] = 0_112233_445566L;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(1).setW(0_10);
        _engine.getExecOrUserARegister(2).setW(0_20);
        _engine.getExecOrUserRRegister(0).setW(0_300300_000030L);
        _engine.getExecOrUserXRegister(5).setW(0_300300_000040L);

        run();

        assertEquals(0_300300_000040L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_300300_000060L, _engine.getExecOrUserARegister(2).getW());
    }

    @Test
    public void testAMA_No_Overflow_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaBM(JFIELD_W, 5, 0, 0, 0, 0_22000);
        data[0] = 0_400000_000001L;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(5).setW(0_400000_000001L);

        run();

        assertEquals(0_0L, _engine.getExecOrUserARegister(5).getW());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testAMA_Overflow_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = amaBM(JFIELD_W, 5, 0, 0, 0, 0_22000);
        data[0] = 0_777777_777775L;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(5).setW(0_377777_777777L);

        run();

        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }
}
