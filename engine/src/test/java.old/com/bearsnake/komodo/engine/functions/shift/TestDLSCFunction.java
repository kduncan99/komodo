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

public class TestDLSCFunction extends FunctionUnitTest {

    private long dlsc(long a, long x, long u) {
        return fjaxu(0_73, 0_07, a, x, u);
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
    public void testDLSC_Basic() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dlsc(4, 0, 0_400); // Address > 0200
        code[1] = 0; // NOP
        // 0_123400_000000_123400_000000L = 001 010 011 100 ... (72 bits)
        // Bit 0,1 are 0,0.
        // Shift 1: 010 100 111 000 ... (Bit 0=0, Bit 1=1 - STOP)
        // Count 1.
        code[0_400] = 0_123400_000000L;
        code[0_401] = 0_123400_000000L;
        setupEM(code, 0);

        run();

        // 72-bit shift of (0_123400_000000, 0_123400_000000) by 1
        // Word 0: (0_123400_000000 << 1) | (0_123400_000000 >> 35) = 0_247000_000000 | 0 = 0_247000_000000
        // Word 1: (0_123400_000000 << 1) | (0_123400_000000 >> 35) = 0_247000_000000 | 0 = 0_247000_000000
        assertEquals(0_247000_000000L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_247000_000000L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(1L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDLSC_Typical() throws MachineInterrupt {
        var code = new long[0_4000];
        code[0] = dlsc(10, 0, 0_2000);
        code[1] = 0; // NOP
        code[0_2000] = 0;
        code[0_2001] = 0_000007_000000L;
        setupEM(code, 0);

        run();

        assertEquals(0_340000_000000L, _engine.getExecOrUserARegister(10).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(11).getW());
        assertEquals(50L, _engine.getExecOrUserARegister(12).getW());
    }

    @Test
    public void testDLSC_AllZeros() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dlsc(2, 0, 0_400);
        code[1] = 0;
        code[0_400] = 0;
        code[0_401] = 0;
        setupEM(code, 0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(71L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDLSC_AllOnes() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dlsc(6, 0, 0_400);
        code[1] = 0;
        code[0_400] = 0_777777_777777L;
        code[0_401] = 0_777777_777777L;
        setupEM(code, 0);

        run();

        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(7).getW());
        assertEquals(71L, _engine.getExecOrUserARegister(8).getW());
    }

    @Test
    public void testDLSC_NoShift() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dlsc(0, 0, 0_400);
        code[1] = 0;
        // 0_200000_000000, 0 = 010 000 ... Bit 0=0, Bit 1=1. No shift.
        code[0_400] = 0_200000_000000L;
        code[0_401] = 0;
        setupEM(code, 0);

        run();

        assertEquals(0_200000_000000L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(2).getW());
    }
}
