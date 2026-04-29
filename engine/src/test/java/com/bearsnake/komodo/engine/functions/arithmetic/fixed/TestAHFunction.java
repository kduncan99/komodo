/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAHFunction extends FunctionUnitTest {

    private long ahEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(072, 004, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testAH_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = ahEM(4, 0, 0, 0, 2, 0_1004);
        code[1] = ahEM(6, 0, 0, 0, 2, 0_1006);
        code[2] = 0;
        data[4] = 0_001000_377000L;
        data[6] = 0_777775_777775L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_001000_000123L);
        _engine.getExecOrUserARegister(6).setW(0_777775_000005L);

        run();

        assertEquals(0_002000_377123L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_777773_000003L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testAH_NoCarryOrOverflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = ahEM(4, 0, 0, 0, 2, 0_1004);
        code[1] = 0;
        data[4] = 0_001000_377000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_001000_000123L);

        run();

        assertEquals(0_002000_377123L, _engine.getExecOrUserARegister(4).getW());
        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testAH_Carry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = ahEM(4, 0, 0, 0, 2, 0_1004);
        code[1] = 0;
        data[4] = 0_777776_377000L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_777775_000123L);

        run();

        assertEquals(0_777774_377123L, _engine.getExecOrUserARegister(4).getW());
        assertTrue(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testAH_Overflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = ahEM(4, 0, 0, 0, 2, 0_1004);
        code[1] = 0;
        data[4] = 0_777777_377777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(4).setW(0_777777_000001L);

        run();

        assertEquals(0_777777_400000L, _engine.getExecOrUserARegister(4).getW());
        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }
}
