/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.actControl;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.DesignatorRegister;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.bearsnake.komodo.engine.Constants.GRS_R5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSDFunction extends FunctionUnitTest {

    private long sdBM(long x, long h, long i, long u) {
        return fjaxhiu(0_73, 0_15, 0_15, x, h, i, u);
    }

    private long sdEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_15, 0_15, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testSD_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = sdBM(0, 0, 0, 0_2000);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short)1)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        var expected = Word36.MASK_B12 | Word36.MASK_B15 | Word36.MASK_B16 | Word36.MASK_B32;
        assertEquals(expected, data[0]);
    }

    @Test
    public void testSD_GRS_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = sdBM(0, 0, 0, GRS_R5);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short)1)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        var expected = Word36.MASK_B12 | Word36.MASK_B15 | Word36.MASK_B16 | Word36.MASK_B32;
        assertEquals(expected, _engine.getGeneralRegister(GRS_R5, false).getW());
    }

    // No indirect tests - we require PP < 2, which prevents indirect addressing.
    // But we try it anyway to ensure we don't do indirect.
    @Test
    public void testSD_Indexed_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = sdBM(2, 1, 1, 0_2000);    // X2 will be set to 000001:000002
        code[012] = fjaxhiu(0, 0, 0, 2, 1, 0, 0_1020);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short)1)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        _engine.getExecOrUserXRegister(2).setXI(0_1).setXM(0_2);

        run();

        var expected = Word36.MASK_B12 | Word36.MASK_B15 | Word36.MASK_B16 | Word36.MASK_B32;
        assertEquals(expected, data[02]);
        assertEquals(0_1, _engine.getExecOrUserXRegister(2).getXI());
        assertEquals(0_3, _engine.getExecOrUserXRegister(2).getXM());
    }

    @Test
    public void testSD_BadPP_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = sdBM(0, 0, 0, 0_2000);

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short)2)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        var i = assertThrows(InvalidInstructionInterrupt.class, this::run);
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(), i.getShortStatusField());
        assertEquals(0, data[0]);
    }

    @Test
    public void testSD_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = sdEM(0, 0, 0, 6, 0_2000);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 6, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short)1)
               .setBasicModeEnabled(false)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        var expected = Word36.MASK_B12 | Word36.MASK_B15 | Word36.MASK_B32;
        assertEquals(expected, data[0]);
    }

    @Test
    public void testSD_Fuzz_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = sdEM(0, 0, 0, 6, 0_2000);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 6, false, 0_2000, 0_2777, 0, data);

        var rand = new Random();
        var dr = _engine.getDesignatorRegister();
        for (int i = 0; i < 1000; i++) {
            _engine.clear();

            var value = rand.nextLong() & ~DesignatorRegister.MASK_SetToZero & Word36.BIT_MASK;
            dr.clear()
              .set(value)
              .setProcessorPrivilege((short) 1)
              .setBasicModeEnabled(false);
            // Use bits 14, 15, and 16 from the engine's designator register
            value &= ~(Word36.MASK_B14 | Word36.MASK_B15 | Word36.MASK_B16);
            value |= (dr.getCompositeValue() & (Word36.MASK_B14 | Word36.MASK_B15 | Word36.MASK_B16));

            _engine.getProgramAddressRegister().setProgramCounter(0_1000);
            data[0] = 0;

            run();

            assertEquals(value, data[0]);
        }
    }
}
