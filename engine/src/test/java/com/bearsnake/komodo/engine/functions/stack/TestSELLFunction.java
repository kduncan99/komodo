/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.stack;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSELLFunction extends FunctionUnitTest {

    private long sell(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_14, 0_03, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testSELL() throws MachineInterrupt {
        var code = new long[]{
            sell(5, 0, 0, 5, 10),
            0
        };

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        createStack(5, 5, 0, 8, 1024);
        _engine.getExecOrUserXRegister(5).setXM(0);
        var origStackPtr = _engine.getExecOrUserXRegister(5).getXM();

        run();

        assertEquals(origStackPtr + 18, _engine.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testSELL_NonZeroLowerLimit() throws MachineInterrupt {
        var code = new long[]{
            sell(5, 0, 0, 5, 10),
            0
        };

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        createStack(5, 5, 0_1000, 8, 1024);
        _engine.getExecOrUserXRegister(5).setXM(0_1000);
        var origStackPtr = _engine.getExecOrUserXRegister(5).getXM();

        run();

        assertEquals(origStackPtr + 18, _engine.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testSELL_Underflow() throws MachineInterrupt {
        var code = new long[]{
            sell(5, 0, 0, 5, 10),
            0
        };

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        createStack(5, 5, 0_1000, 8, 1024);

        var interrupt = assertThrows(RCSGenericStackUnderflowOverflowInterrupt.class, this::run);
        assertEquals(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow, interrupt.getReason());
    }
}
