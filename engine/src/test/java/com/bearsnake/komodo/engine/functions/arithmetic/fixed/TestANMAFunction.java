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

public class TestANMAFunction extends FunctionUnitTest {

    private long anmaImm(long a, long x, long u) {
        return fjaxu(017, JFIELD_U, a, x, u);
    }

    private long anmaBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(017, j, a, x, h, i, u);
    }

    private long anmaEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(017, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testANMA_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = anmaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
        code[2] = anmaEM(Constants.JFIELD_W, 7, 0, 0, 0, 2, 0_1007);
        code[3] = anmaEM(Constants.JFIELD_W, 8, 0, 0, 0, 2, 0_1010);
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
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0, _engine.getExecOrUserARegister(7).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(8).getW());
    }

    @Test
    public void testANMA_OneZeroAddend_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = anmaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
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

        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_654321L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testANMA_SameSigns_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = anmaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
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

        assertEquals(0_277777_777700L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_000000L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testANMA_OppositeSigns_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0_1005);
        code[1] = anmaEM(Constants.JFIELD_W, 6, 0, 0, 0, 2, 0_1006);
        code[2] = anmaEM(Constants.JFIELD_W, 7, 0, 0, 0, 2, 0_1007);
        code[3] = anmaEM(Constants.JFIELD_W, 8, 0, 0, 0, 2, 0_1010);
        code[5] = 0;
        data[5] = 0_100L;
        data[6] = 0_100L;
        data[7] = 0_777777_777757L;
        data[8] = 0_000000_100000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_1L);
        _engine.getExecOrUserARegister(6).setW(0_777777_777776L);
        _engine.getExecOrUserARegister(7).setW(0_10L);
        _engine.getExecOrUserARegister(8).setW(0_200000L);

        run();

        assertEquals(0_777777_777700L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_777676L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_777777_777767L, _engine.getExecOrUserARegister(7).getW());
        assertEquals(0_000000_100000L, _engine.getExecOrUserARegister(8).getW());
    }

    @Test
    public void testANMA_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaEM(Constants.JFIELD_W, 5, 4, 0, 0, 2, 0_1005);
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

        assertEquals(0_0, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testANMA_Immediate_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaBM(JFIELD_U, 0, 0, 1, 1, 0177776); // U=0_777776
        code[1] = anmaBM(Constants.JFIELD_XU, 1, 0, 1, 1, 0177776); // U=0_777776
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

        assertEquals(0_777777_000001L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_777777_777776L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testANMA_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaBM(Constants.JFIELD_W, 10, 0, 0, 1, 0_1010);
        code[0_10] = fjaxhiu(0,0, 0, 0, 0, 1, 0_1020);
        code[0_20] = fjaxhiu(0, 0, 0, 0, 0, 0, 0_22002);
        data[2] = 0_1234;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(10).setW(0_1);

        run();

        assertEquals(0_777777_776544L, _engine.getExecOrUserARegister(10).getW());
    }

    @Test
    public void testANMA_PartialWord_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = anmaBM(Constants.JFIELD_S4, 1, 0, 0, 0, 0_22002);
        code[1] = anmaBM(Constants.JFIELD_XH2, 2, 0, 0, 0, 0_22002);
        data[2] = 0_112233_445566L;

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(1).setW(0);
        _engine.getExecOrUserARegister(2).setW(0);

        run();

        assertEquals(0_777777_777733L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_777777_445566L, _engine.getExecOrUserARegister(2).getW());
    }
}
