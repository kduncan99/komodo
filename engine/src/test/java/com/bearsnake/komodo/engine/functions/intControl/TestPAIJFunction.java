/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.intControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPAIJFunction extends FunctionUnitTest {

    private long paijBM(long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_14, 0_07, x, h, i, u);
    }

    private long paijEM(long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_14, 0_07, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testPAIJ_BM() throws MachineInterrupt {
        var code = new long[] {
            paijBM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertFalse(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    // No test for indirect - not possible with PP>1, and we have to be at 0 to even try.

    @Test
    public void testPAIJ_BM_InsufficientProcessorPrivilege() throws MachineInterrupt {
        var code = new long[] {
            paijBM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        assertThrows(InvalidInstructionInterrupt.class, this::run);
        assertEquals(0_1000L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testPAIJ_EM() throws MachineInterrupt {
        var code = new long[] {
            paijEM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertFalse(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testPAIJ_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            paijEM(2, 1, 0, 0_1000),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        _engine.getExecOrUserXRegister(2).setXI(0_01).setXM(0_05);

        run();

        assertFalse(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1005L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(1, _engine.getExecOrUserXRegister(2).getXI());
        assertEquals(0_06L, _engine.getExecOrUserXRegister(2).getXM());
    }

    @Test
    public void testPAIJ_EM_InsufficientProcessorPrivilege() throws MachineInterrupt {
        var code = new long[] {
            paijEM(0, 0, 0, 0_1005),
            0, 0, 0, 0, 0
        };
        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        assertThrows(InvalidInstructionInterrupt.class, this::run);
        assertTrue(_engine.getDesignatorRegister().isDeferrableInterruptEnabled());
        assertEquals(0_1000L, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
