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

    private long ide(long a, long x, long u) {
        return fjaxu(0_07, 0_10, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }


    @Test
    public void testIDE_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ide(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = 12345678L;

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(code.length - 1)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        // Result in A4, A5.
        // 12345678 is 8 digits.
        // MSW (A4) should be 0 (top 9 digits)
        // LSW (A5) should be 12345678 + POSITIVE_SIGN
        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ide(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = ~12345678L & 0_777777777777L;

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(code.length - 1)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 1, 2, 3, 4, 5, 6, 7, 8, NEGATIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ide(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = 0;

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(code.length - 1)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(POSITIVE_SIGN, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = ide(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = 12345678L;

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // IDE A4, *0_400
        code[0] = fjaxu(0_07, 0_10, 4, 0, 0_200400); // I=1, U=400
        code[1] = 0;
        code[0_400] = 0_600;
        code[0_600] = 87654321L;

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(14).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
                .setBasicModeEnabled(true)
                .setProcessorPrivilege((short)2);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_4);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 8, 7, 6, 5, 4, 3, 2, 1, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testIDE_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // IDE A4, 0_300, X1
        code[0] = ide(4, 1, 0_300);
        code[1] = 0;
        code[0_400] = 11111111L;

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        _engine.getExecOrUserXRegister(1).setW(0_100);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 1, 1, 1, 1, 1, 1, 1, 1, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
    }

    // Helper for 10 digits test to check word split
    private long decWord(long c0, long c1, long c2, long c3, long c4, long c5, long c6, long c7, long c8, long c9) {
        // c0 is digit 1 (highest), c8 is digit 9. c9 is sign.
        // Wait, TestDecimalFunction.decWord is for 9 cells total.
        // IDE result: value0 (top 9), value1 (bottom 8 + sign).
        // Total 17 cells + sign.
        // This helper is for value1.
        return ((c1 & 017) << 32) | ((c2 & 017) << 28) | ((c3 & 017) << 24)
                | ((c4 & 017) << 20) | ((c5 & 017) << 16) | ((c6 & 017) << 12)
                | ((c7 & 017) << 8) | ((c8 & 017) << 4) | (c9 & 017);
    }
}
