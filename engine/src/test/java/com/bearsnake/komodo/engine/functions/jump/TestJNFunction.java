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

public class TestJNFunction extends FunctionUnitTest {

    private long jnBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_03, a, x, h, i, u);
    }

    private long jnBM(long a, long u) {
        return jnBM(a, 0, 0, 0, u);
    }

    private long jnEM(long a, long x, long u) {
        return fjaxu(0_74, 0_03, a, x, u);
    }

    private long jnEM(long a, long u) {
        return jnEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJN_BM_1() throws MachineInterrupt {
        var code = new long[] {
            jnBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 1: A5 is negative -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_BM_2() throws MachineInterrupt {
        var code = new long[] {
            jnBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 2: A5 is negative zero -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_BM_3() throws MachineInterrupt {
        var code = new long[] {
            jnBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 3: A5 is positive -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(0_377777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_EM_1() throws MachineInterrupt {
        var code = new long[] {
            jnEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_477777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_EM_2() throws MachineInterrupt {
        var code = new long[] {
            jnEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_EM_3() throws MachineInterrupt {
        var code = new long[] {
            jnEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 3: A5 is negative -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(0_0L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            jnBM(5, 3, 0, 0, 0_1000),
            0, 0, 0, 0, 0
        };

        loadBaseRegister((short) 14, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserXRegister(3).setXM(0_5);
        _engine.getExecOrUserARegister(5).setW(0_444444_444444L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            jnEM(5, 3, 0_1000),
            0, 0, 0, 0, 0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserXRegister(3).setXM(0_5);
        _engine.getExecOrUserARegister(5).setW(0_400000_000001L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            jnBM(5, 0, 0, 1, 0_1005),
            0, 0, 0, 0,
            fjaxhiu(0, 0, 0, 0, 0, 1, 0_1010),
            0, 0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 0_1015),
            0, 0, 0, 0, 0,
            };

        loadBaseRegister((short) 14, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(5).setW(0_444444_444444L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1015, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
