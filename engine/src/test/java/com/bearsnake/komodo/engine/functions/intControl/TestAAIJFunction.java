/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.intControl;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAAIJFunction extends FunctionUnitTest {

    private long aaijBM(long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_07, 0_0, x, h, i, u);
    }

    private long aaijEM(long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_14, 0_06, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testAAIJ_BM() throws MachineInterrupt {
        var code = new long[] {
            aaijBM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertTrue(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testAAIJ_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            aaijBM(0, 0, 1, 0_1002),
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 0_1005),
            0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertTrue(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testAAIJ_EM() throws MachineInterrupt {
        var code = new long[] {
            aaijEM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertTrue(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testAAIJ_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            aaijEM(2, 1, 0, 0_1000),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        _engine.getExecOrUserXRegister(2).setXI(0_01).setXM(0_05);

        run();

        assertTrue(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(1, _engine.getExecOrUserXRegister(2).getXI());
        assertEquals(0_06L, _engine.getExecOrUserXRegister(2).getXM());
    }

    @Test
    public void testAAIJ_EM_InsufficientProcessorPrivilege() throws MachineInterrupt {
        var code = new long[] {
            aaijEM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        assertThrows(InvalidInstructionInterrupt.class, this::run);

        assertFalse(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1000L, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
