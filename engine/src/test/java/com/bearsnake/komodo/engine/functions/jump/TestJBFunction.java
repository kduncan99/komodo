/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJBFunction extends FunctionUnitTest {

    private long jbBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_11, a, x, h, i, u);
    }

    private long jbBM(long a, long u) {
        return jbBM(a, 0, 0, 0, u);
    }

    private long jbEM(long a, long x, long u) {
        return fjaxu(0_74, 0_11, a, x, u);
    }

    private long jbEM(long a, long u) {
        return jbEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJB_BM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jbBM(5, 0_1005);   // JB  A5,01005

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        // Case 1: A5 bit 0 is set -> Should jump
        _engine.getExecOrUserARegister(5).setW(01);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_BM_NoJump() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jbBM(5, 0_1005);   // JB  A5,01005

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        // Case 2: A5 bit 0 is clear -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(02);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_EM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jbEM(5, 0_1005);   // JB  A5,01005

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        // Case 1: A5 bit 0 is set -> Should jump
        _engine.getExecOrUserARegister(5).setW(01);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_EM_NoJump() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jbEM(5, 0_1005);   // JB  A5,01005

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        // Case 2: A5 bit 0 is clear -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(02);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_Indexed_BM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jbBM(5, 3, 0, 0, 0_1005); // jump to 0_1005 + X3.m (0_10) = 0_1015

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getExecOrUserARegister(5).setW(01);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1015L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_Indirect_BM() throws MachineInterrupt {
        var code = new long[03000];
        code[0] = jbBM(5, 0, 0, 1, 0_2000); // jump indirect via 0_1000
        code[0_1000] = fjaxu(0, 0, 0, 0, 0_1200); // second stage: J to 0_1200

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_3777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getExecOrUserARegister(5).setW(01);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1200, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_Indexed_EM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jbEM(5, 3, 0_1100); // jump to 0_1100 + X3.m (0_10) = 0_1100

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getExecOrUserARegister(5).setW(01);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1110, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
