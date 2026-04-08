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

public class TestJPFunction extends FunctionUnitTest {

    private long jpBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_02, a, x, h, i, u);
    }

    private long jpBM(long a, long u) {
        return jpBM(a, 0, 0, 0, u);
    }

    private long jpEM(long a, long x, long u) {
        return fjaxu(0_74, 0_02, a, x, u);
    }

    private long jpEM(long a, long u) {
        return jpEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJP_BM_1() throws MachineInterrupt {
        var code = new long[] {
            jpBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 1: A5 is positive -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_377777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_BM_2() throws MachineInterrupt {
        var code = new long[] {
            jpBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 2: A5 is positive zero -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_0L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_BM_3() throws MachineInterrupt {
        var code = new long[] {
            jpBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 3: A5 is negative -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        setHalted(null);
        run();
        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_EM_1() throws MachineInterrupt {
        var code = new long[] {
            jpEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 1: A5 is positive -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_377777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_EM_2() throws MachineInterrupt {
        var code = new long[] {
            jpEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 2: A5 is positive zero -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_0L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_EM_3() throws MachineInterrupt {
        var code = new long[] {
            jpEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        // Case 3: A5 is negative -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        setHalted(null);
        run();
        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            jpBM(5, 3, 0, 0, 0_1000),
            0, 0, 0, 0, 0
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(14, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserXRegister(3).setXM(0_5);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            jpEM(5, 3, 0_1000),
            0, 0, 0, 0, 0
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserXRegister(3).setXM(0_5);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJP_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            jpBM(5, 0, 0, 1, 0_1005),
            0, 0, 0, 0,
            fjaxhiu(0, 0, 0, 0, 0, 1, 0_1010),
            0, 0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 0_1015),
            0, 0, 0, 0, 0,
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(14, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1015, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
