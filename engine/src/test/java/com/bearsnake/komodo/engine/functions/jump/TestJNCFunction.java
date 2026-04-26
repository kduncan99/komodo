/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJNCFunction extends FunctionUnitTest {

    private long jncBM(long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_17, 0, x, h, i, u);
    }

    private long jncBM(long u) {
        return jncBM(0, 0, 0, u);
    }

    private long jncEM(long x, long u) {
        return fjaxu(0_74, 0_14, 0_05, x, u);
    }

    private long jncEM(long u) {
        return jncEM(0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJNC_BM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jncBM(0_1005);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJNC_BM_NoJump() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jncBM(0_1005);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getDesignatorRegister().setCarry(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJNC_EM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jncEM(0_1005);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJNC_EM_NoJump() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jncEM(0_1005);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getDesignatorRegister().setCarry(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJNC_Indexed_BM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jncBM(3, 0, 0, 0_1005); // jump to 0_1005 + X3.m (0_10) = 0_1015

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1015L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJNC_Indirect_BM() throws MachineInterrupt {
        var code = new long[03000];
        code[0] = jncBM(0, 0, 1, 0_2000); // jump indirect via 0_1000
        code[0_1000] = fjaxu(0, 0, 0, 0, 0_1200); // second stage: J to 0_1200

        loadBaseRegister((short) 12, false, 0_1000, 0_3777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1200, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJNC_Indexed_EM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jncEM(3, 0_1100); // jump to 0_1100 + X3.m (0_10) = 0_1100

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1110, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
