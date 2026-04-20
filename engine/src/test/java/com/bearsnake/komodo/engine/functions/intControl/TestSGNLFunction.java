/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.intControl;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.SignalInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSGNLFunction extends FunctionUnitTest {

    private long sgnlBM(long x, long h, long i, long u) {
        return fjaxhiu(0_73, 0_15, 0_17, x, h, i, u);
    }

    private long sgnlEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_15, 0_17, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testSGNL_BM() throws MachineInterrupt {
        var code = new long[] {
            sgnlBM(0, 0, 0, 14458),
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, 0, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        var interrupt = assertThrows(SignalInterrupt.class, this::run);
        assertEquals(1, interrupt.getShortStatusField());
        assertEquals(14458, interrupt.getInterruptStatusWord0());
    }

    @Test
    public void testSGNL_EM() throws MachineInterrupt {
        var code = new long[] {
            sgnlEM(0, 0, 0, 4, 0_2773), // ignore B field
            0, 0, 0, 0, 0
        };
        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getDesignatorRegister().setDeferrableInterruptEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        var interrupt = assertThrows(SignalInterrupt.class, this::run);
        assertEquals(1, interrupt.getShortStatusField());
        assertEquals(0_2773, interrupt.getInterruptStatusWord0());
    }
}
