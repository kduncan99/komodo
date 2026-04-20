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
 * Decimal to Integer instruction
 * (DEI) Converts the one-word BCD operand to one's complement binary.
 */
public class TestDEIFunction extends TestDecimalFunction {

    private long deiBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_07, 0_06, a, x, h, i, u);
    }

    private long deiEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_07, 0_06, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testDEI_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 12,345,678
        code[0] = deiEM(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(12345678L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // -12,345,678
        code[0] = deiEM(4, 0, 0, 0, 2, 0_400);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = decWord(1, 2, 3, 4, 5, 6, 7, 8, NEGATIVE_SIGN);

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank0);
        loadBaseRegister(2, false, 0, 0_777, AbsoluteAddress.construct(1, 0), bank2);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // 1's complement of 12345678
        assertEquals(~12345678L & 0_777777_777777L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = deiEM(4, 0, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_NegativeZero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = deiEM(4, 0, 0, 0, 2, 0_400);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, NEGATIVE_SIGN);

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank0);
        loadBaseRegister(2, false, 0, 0_777, AbsoluteAddress.construct(1, 0), bank2);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // -0 is +0 in this implementation (see DEI execute method)
        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 12,345,678
        code[0] = deiBM(4, 0, 0, 0, 0_1400);
        code[1] = 0;
        code[0_400] = decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister().setBasicModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(12345678L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DEI A4, *0_400
        code[0] = deiBM(4, 0, 0, 1, 0_1400);
        code[1] = 0;
        code[0_400] = fjaxhiu(0, 0, 0, 0, 0, 0, 0_1600);
        // decWord(8, 7, 6, 5, 4, 3, 2, 1, POSITIVE_SIGN) -> 87654321
        code[0_600] = decWord(8, 7, 6, 5, 4, 3, 2, 1, POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank);

        _engine.getDesignatorRegister()
                .setBasicModeEnabled(true)
                .setProcessorPrivilege((short)2);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_4);

        run();

        assertEquals(87654321L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DEI A4, 0_300, X1
        code[0] = deiEM(4, 1, 1, 0, 2, 0_300);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = decWord(1, 1, 1, 1, 1, 1, 1, 1, POSITIVE_SIGN);

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, AbsoluteAddress.construct(0, 0), bank0);
        loadBaseRegister(2, false, 0, 0_777, AbsoluteAddress.construct(1, 0), bank2);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        _engine.getExecOrUserXRegister(1).setXI(1).setXM(0_100);

        run();

        assertEquals(11111111L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_101, _engine.getExecOrUserXRegister(1).getXM());
    }
}
