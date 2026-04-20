package com.bearsnake.komodo.engine.functions.shift;
/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLSCFunction extends FunctionUnitTest {

    private long lscBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_73, 0_06, a, x, h, i, u);
    }

    private long lscEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_73, 0_06, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testLSC_Simple_EM() throws MachineInterrupt {
        var code = new long[] {
            lscEM(4, 0, 0, 0, 2, 0_0),
            0,
        };

        var data = new long[] {
            0_123400_000000L,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(2, false, 0_0, 0_1777, 0, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // 0_123400_000000L = 001 010 011 100 ...
        // Bits 0,1 are 0,0.
        // Shift 1: 010 100 111 000 ... (0,1 are 0,1 - STOP)
        // Shift count should be 1.
        // Result in A4: 0_247000_000000L
        // Result in A5: 1
        assertEquals(0_247000_000000L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(1L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testLSC_Typical_EM() throws MachineInterrupt {
        var code = new long[] {
            lscEM(10, 0, 0, 0, 2, 0_040),
            0,
            };

        var data = new long[] {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0_000007_000000L,
            };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(2, false, 0_0, 0_1777, 0, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        // 0_000007_000000L = 000 000 000 000 000 000 000 111 000 000 000 000
        // Need to shift left until bit 0 != bit 1.
        // Bit 0,1 are 0,0.
        // It takes 14 shifts to move the first '1' to bit 1. (bit 15 moves to bit 1)
        // Then bit 0=0, bit 1=1.
        // Count should be 14 (decimal).
        // Result = 0_340000_000000L
        assertEquals(0_340000_000000L, _engine.getExecOrUserARegister(10).getW());
        assertEquals(14L, _engine.getExecOrUserARegister(11).getW());
    }

    @Test
    public void testLSC_AllZeros_EM() throws MachineInterrupt {
        var code = new long[] {
            lscEM(4, 0, 0, 0, 2, 0_0),
            0,
            };

        var data = new long[] {
            0_0L,
            };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        loadBaseRegister(2, false, 0_0, 0_1777, 0, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(35L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testLSC_AllOnes_BM() throws MachineInterrupt {
        var code = new long[] {
            lscBM(4, 0, 0, 0, 0_0200),
            0,
            };

        var data = new long[02000];
        data[0200] = 0_777777_777777L;

        var bank13 = new ArraySlice(code);
        var bank12 = new ArraySlice(data);

        loadBaseRegister(13, false, 0_4000, 0_4777, 0, bank13);
        loadBaseRegister(12, false, 0_0, 0_1777, 0, bank12);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_4000).setBankDescriptorIndex(0_000005).setBankLevel((short)0_7);

        run();

        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(35L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testLSC_GRS_BM() throws MachineInterrupt {
        var code = new long[] {
            lscBM(4, 0, 0, 0, 0_01),
            0,
            };

        var data = new long[] { 1, 1, 1, 1, 1 };

        var bank13 = new ArraySlice(code);
        var bank12 = new ArraySlice(data);

        loadBaseRegister(13, false, 0_4000, 0_4777, 0, bank13);
        loadBaseRegister(12, false, 0_0, 0_1777, 0, bank12);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_4000).setBankDescriptorIndex(0_000005).setBankLevel((short)0_7);
        _engine.getGeneralRegister(01, true).setW(0_040000_000000L);

        run();

        // make sure we used X1 (GRS) instead of data bank
        assertEquals(0_200000_000000L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(2, _engine.getExecOrUserARegister(5).getW());
    }
}
