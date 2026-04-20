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
 * Integer to Decimal instruction
 * (IDE) Converts the single-precision one's complement binary operand
 * to two-word decimal operand.
 */
public class TestIDEFunction extends TestDecimalFunction {

    private long ideBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_07, 0_10, a, x, h, i, u);
    }

    private long ideEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_07, 0_10, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testIDE_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ideEM(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = 12345678L;

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // Result in A4, A5.
        // 12345678 is 8 digits.
        // MSW (A4) should be 0 (top 9 digits)
        // LSW (A5) should be 12345678 + POSITIVE_SIGN
        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ideEM(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = ~12345678L & 0_777777777777L;

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, NEGATIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ideEM(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = 0;

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(POSITIVE_SIGN, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ideBM(4, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = 12345678L;

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }
}
