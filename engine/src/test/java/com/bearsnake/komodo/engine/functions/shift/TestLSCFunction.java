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

    private long lsc(long a, long x, long u) {
        return fjaxu(0_73, 0_06, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private void setupEM(long[] code, int bdi) {
        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(code.length - 1)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(bdi).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(bdi);
    }

    @Test
    public void testLSC_Basic() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = lsc(4, 0, 0_400); // Use address > 0200
        code[1] = 0; // NOP
        code[0_400] = 0_123400_000000L;
        setupEM(code, 0);

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
    public void testLSC_Typical() throws MachineInterrupt {
        var code = new long[0_4000];
        code[0] = lsc(10, 0, 0_2000);
        code[1] = 0; // NOP
        code[0_2000] = 0_000007_000000L;
        setupEM(code, 0);

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
    public void testLSC_AllZeros() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = lsc(2, 0, 0_400);
        code[1] = 0;
        code[0_400] = 0;
        setupEM(code, 0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(35L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testLSC_AllOnes() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = lsc(6, 0, 0_400);
        code[1] = 0;
        code[0_400] = 0_777777_777777L;
        setupEM(code, 0);

        run();

        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(35L, _engine.getExecOrUserARegister(7).getW());
    }

    @Test
    public void testLSC_NoShift() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = lsc(0, 0, 0_400);
        code[1] = 0;
        // 0_200000_000000L = 010 000 ... Bit 0=0, Bit 1=1. No shift.
        code[0_400] = 0_200000_000000L;
        setupEM(code, 0);

        run();

        assertEquals(0_200000_000000L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(1).getW());
    }
}
