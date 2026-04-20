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

public class TestBUYFunction extends FunctionUnitTest {

    private long buy(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_14, 0_02, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
    }

    @Test
    public void testBUY() throws MachineInterrupt {
        var code = new long[]{
            buy(5, 0, 0, 5, 10),
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
        var origStackPtr = _engine.getExecOrUserXRegister(5).getXM();

        run();

        assertEquals(origStackPtr - 18, _engine.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testBUY_NonZeroLowerLimit() throws MachineInterrupt {
        var code = new long[]{
            buy(5, 0, 0, 5, 10),
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
        var origStackPtr = _engine.getExecOrUserXRegister(5).getXM();

        run();

        assertEquals(origStackPtr - 18, _engine.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testBUY_NegativeFrameSize() throws MachineInterrupt {
        var code = new long[]{
            buy(5, 0, 0, 5, 0),
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

        createStack(5, 5, 0_1000, -1, 1024);
        _engine.getExecOrUserXRegister(5).setXM(0_1000); // frame size is -1
        var origStackPtr = _engine.getExecOrUserXRegister(5).getXM();

        run();

        assertEquals(origStackPtr + 1, _engine.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testBUY_Overflow() throws MachineInterrupt {
        var code = new long[]{
            buy(5, 0, 0, 5, 10),
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

        var interrupt = assertThrows(RCSGenericStackUnderflowOverflowInterrupt.class, this::run);
        assertEquals(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow, interrupt.getReason());
    }
}
