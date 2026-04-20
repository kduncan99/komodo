/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.intControl;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.SignalInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestERFunction extends FunctionUnitTest {

    private long erBM(long x, long h, long i, long u) {
        return fjaxhiu(0_72, 0_11, 0_0, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testER_BM() throws MachineInterrupt {
        var code = new long[] {
            erBM(0, 0, 0, 0_42),
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
        assertEquals(0, interrupt.getShortStatusField());
        assertEquals(0_42, interrupt.getInterruptStatusWord0());
    }
}
