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

public class TestJZFunction extends FunctionUnitTest {

    private long jzBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_00, a, x, h, i, u);
    }

    private long jzBM(long a, long u) {
        return jzBM(a, 0, 0, 0, u);
    }

    private long jzEM(long a, long x, long u) {
        return fjaxu(0_74, 0_00, a, x, u);
    }

    private long jzEM(long a, long u) {
        return jzEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJZ_BM_1() throws MachineInterrupt {
        var code = new long[] {
            jzBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_0L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJZ_BM_2() throws MachineInterrupt {
        var code = new long[] {
            jzBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJZ_BM_3() throws MachineInterrupt {
        var code = new long[] {
            jzBM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_01L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        setHalted(null);
        run();
        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJZ_EM_1() throws MachineInterrupt {
        var code = new long[] {
            jzEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_0L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        run();
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJZ_EM_2() throws MachineInterrupt {
        var code = new long[] {
            jzEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

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
    public void testJZ_EM_3() throws MachineInterrupt {
        var code = new long[] {
            jzEM(5, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(5).setW(0_777777_777776L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        setHalted(null);
        run();
        assertEquals(0_1001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJZ_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            jzBM(5, 3, 0, 0, 0_1000),
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
    public void testJZ_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            jzEM(5, 3, 0_1000),
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
    public void testJZ_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            jzBM(5, 0, 0, 1, 0_1005),
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
        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_1015, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
