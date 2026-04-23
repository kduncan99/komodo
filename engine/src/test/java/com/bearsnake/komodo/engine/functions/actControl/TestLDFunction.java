/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.actControl;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.DesignatorRegister;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.bearsnake.komodo.engine.Constants.GRS_X7;
import static org.junit.jupiter.api.Assertions.*;

public class TestLDFunction extends FunctionUnitTest {

    private long ldBM(long x, long h, long i, long u) {
        return fjaxhiu(0_73, 0_15, 0_14, x, h, i, u);
    }

    private long ldEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_15, 0_14, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testLD_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = ldBM(0, 0, 0, 0_2000);
        data[0] = 0_620156_610100L; // sets PP to 3

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);
        loadBaseRegister(12, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(13, false, 0_2000, 0_2777, 0, bank1);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short) 0)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        var dr = _engine.getDesignatorRegister();
        assertTrue(dr.isActivityLevelQueueMonitorEnabled());
        assertTrue(dr.isPerformanceMonitoringCounterEnabled());
        assertFalse(dr.isPerformanceMonitoringCounterInterruptControl());
        assertEquals(2, dr.getSoftwarePerformanceMonitor());
        assertFalse(dr.isFaultHandlingInProgress());
        assertTrue(dr.isExecutive24BitIndexingEnabled());
        assertTrue(dr.isQuantumTimerEnabled());
        assertFalse(dr.isDeferrableInterruptEnabled());
        assertEquals(3, dr.getProcessorPrivilege());
        assertTrue(dr.isBasicModeEnabled());
        assertFalse(dr.isExecRegisterSetSelected());

        assertTrue(dr.isCarry());
        assertTrue(dr.isOverflow());
        assertFalse(dr.isCharacteristicUnderflow());
        assertFalse(dr.isCharacteristicOverflow());
        assertTrue(dr.isDivideCheck());
        assertFalse(dr.isOperationTrapEnabled());
        assertTrue(dr.isArithmeticExceptionEnabled());
        assertFalse(dr.isQuarterWordModeEnabled());
    }

    @Test
    public void testLD_GRS_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = ldBM(0, 0, 0, GRS_X7);

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);
        loadBaseRegister(12, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(13, false, 0_2000, 0_2777, 0, bank1);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short) 0)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        _engine.getExecOrUserXRegister(7).setW(0_620156_610100L);

        run();

        var dr = _engine.getDesignatorRegister();
        assertTrue(dr.isActivityLevelQueueMonitorEnabled());
        assertTrue(dr.isPerformanceMonitoringCounterEnabled());
        assertFalse(dr.isPerformanceMonitoringCounterInterruptControl());
        assertEquals(2, dr.getSoftwarePerformanceMonitor());
        assertFalse(dr.isFaultHandlingInProgress());
        assertTrue(dr.isExecutive24BitIndexingEnabled());
        assertTrue(dr.isQuantumTimerEnabled());
        assertFalse(dr.isDeferrableInterruptEnabled());
        assertEquals(3, dr.getProcessorPrivilege());
        assertTrue(dr.isBasicModeEnabled());
        assertFalse(dr.isExecRegisterSetSelected());

        assertTrue(dr.isCarry());
        assertTrue(dr.isOverflow());
        assertFalse(dr.isCharacteristicUnderflow());
        assertFalse(dr.isCharacteristicOverflow());
        assertTrue(dr.isDivideCheck());
        assertFalse(dr.isOperationTrapEnabled());
        assertTrue(dr.isArithmeticExceptionEnabled());
        assertFalse(dr.isQuarterWordModeEnabled());
    }

    // No indirect tests - we require PP == 2, which prevents indirect addressing.
    // But we try it anyway to ensure we don't do indirect.
    @Test
    public void testLD_Indexed_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = ldBM(2, 1, 1, 0_2000);    // X2 will be set to 000001:000002
        code[012] = fjaxhiu(0, 0, 0, 2, 1, 0, 0_1020);
        data[02] = Word36.MASK_B12 | Word36.MASK_B15 | Word36.MASK_B16 | Word36.MASK_B32;

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);
        loadBaseRegister(12, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(13, false, 0_2000, 0_2777, 0, bank1);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short) 0)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);
        _engine.getExecOrUserXRegister(2).setXI(0_1).setXM(0_2);

        run();

        assertEquals(data[02], _engine.getDesignatorRegister().getCompositeValue());
        assertEquals(0_1, _engine.getExecOrUserXRegister(2).getXI());
        assertEquals(0_3, _engine.getExecOrUserXRegister(2).getXM());
    }

    @Test
    public void testLD_BadPP_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = ldBM(0, 0, 0, 0_2000);
        data[0] = 0_620156_610100L; // sets PP to 3

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);
        loadBaseRegister(12, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(13, false, 0_2000, 0_2777, 0, bank1);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short) 1)
               .setBasicModeEnabled(true)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        var i = assertThrows(InvalidInstructionInterrupt.class, this::run);
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(), i.getShortStatusField());
    }

    @Test
    public void testLD_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = ldEM(0, 0, 0, 5, 0_2000);
        data[0] = 0_620154_610100L; // sets PP to 3

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(5, false, 0_2000, 0_2777, 0, bank1);

        _engine.getDesignatorRegister()
               .clear()
               .setQuantumTimerEnabled(true)
               .setProcessorPrivilege((short) 0)
               .setBasicModeEnabled(false)
               .setQuarterWordModeEnabled(true);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        var expected = Word36.MASK_B12 | Word36.MASK_B15 | Word36.MASK_B32;
        assertEquals(data[0], _engine.getDesignatorRegister().getCompositeValue());
    }

    @Test
    public void testLD_Fuzz_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];
        code[0] = ldEM(0, 0, 0, 6, 0_2000);

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(6, false, 0_2000, 0_2777, 0, bank1);

        var rand = new Random();
        var dr = _engine.getDesignatorRegister();
        for (int i = 0; i < 1000; i++) {
            _engine.clear();

            var value = rand.nextLong() & ~DesignatorRegister.MASK_SetToZero & Word36.BIT_MASK;
            dr.clear()
              .setProcessorPrivilege((short) 0)
              .setBasicModeEnabled(false);

            // Use bits 14, 15, and 16 from the engine's designator register
            value &= ~(Word36.MASK_B14 | Word36.MASK_B15 | Word36.MASK_B16);
            value |= (dr.getCompositeValue() & (Word36.MASK_B14 | Word36.MASK_B15 | Word36.MASK_B16));

            _engine.getProgramAddressRegister().setProgramCounter(0_1000);
            data[0] = value;

            run();

            assertEquals(value, _engine.getDesignatorRegister().getCompositeValue());
        }
    }
}
