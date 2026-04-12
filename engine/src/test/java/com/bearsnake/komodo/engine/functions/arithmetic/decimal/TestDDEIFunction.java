/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Double Decimal to Integer instruction
 * (DDEI) Converts the two-word BCD operand to one's complement double-word binary.
 */
public class TestDDEIFunction extends TestDecimalFunction {

    private long ddeiBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_07, 0_07, a, x, h, i, u);
    }

    private long ddeiEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_07, 0_07, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testDDEI_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 123,456,789,012,345,67
        // Word 0: 123456789
        // Word 1: 01234567 + Sign
        code[0] = ddeiEM(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // Value = 123,456,789,012,345,67
        // Binary (72-bit 1's complement):
        // 12345678901234567 = 0x2BDC546291F447
        // MSW: 0_536705L
        // LSW: 0_213532645607L
        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDDEI_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ddeiEM(4, 0, 0, 0, 2, 0_400);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        data[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | NEGATIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank);
        loadBaseRegister(2, false, 0, 0_777, new AbsoluteAddress(1, 0), new ArraySlice(data));

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // MSW: 0_777777241072L
        // LSW: 0_564245132170L
        assertEquals(0_777777241072L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_564245132170L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDDEI_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 123,456,789,012,345,67
        code[0] = ddeiBM(4, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDDEI_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DDEI A4, *0_400
        code[0] = ddeiBM(4, 0, 0, 1, 01400);
        code[1] = 0;
        code[0_400] = fjaxhiu(0, 0, 0, 0, 0, 0, 0_1600); // Pointer
        code[0_600] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_601] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank);

        _engine.getDesignatorRegister()
                .setBasicModeEnabled(true)
                .setProcessorPrivilege((short)2);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_4);

        run();

        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDDEI_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DDEI A4, 0_300, X1
        code[0] = ddeiEM(4, 1, 1, 0, 2, 0_300);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        data[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank);
        loadBaseRegister(2, false, 0, 0_777, new AbsoluteAddress(1, 0), new ArraySlice(data));

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);
        _engine.getExecOrUserXRegister(1).setW(0_100);

        run();

        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }
}
