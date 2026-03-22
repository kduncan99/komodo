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

    private long ddei(long a, long x, long u) {
        return fjaxu(0_07, 0_07, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }


    @Test
    public void testDDEI_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 123,456,789,012,345,67
        // Word 0: 123456789
        // Word 1: 01234567 + Sign
        code[0] = ddei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(code.length - 1)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

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
        code[0] = ddei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | NEGATIVE_SIGN);

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(code.length - 1)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

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
        code[0] = ddei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDDEI_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DDEI A4, *0_400
        code[0] = fjaxu(0_07, 0_07, 4, 0, 0_200400); // I=1, U=400
        code[1] = 0;
        code[0_400] = 0_600; // Pointer
        code[0_600] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_601] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(14).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
                .setBasicModeEnabled(true)
                .setProcessorPrivilege((short)2);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_4);

        run();

        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDDEI_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DDEI A4, 0_300, X1
        code[0] = ddei(4, 1, 0_300);
        code[1] = 0;
        code[0_400] = ((1L << 32) | (2L << 28) | (3L << 24) | (4L << 20) | (5L << 16) | (6L << 12) | (7L << 8) | (8L << 4) | 9L);
        code[0_401] = ((0L << 32) | (1L << 28) | (2L << 24) | (3L << 20) | (4L << 16) | (5L << 12) | (6L << 8) | (7L << 4) | POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        _engine.getExecOrUserXRegister(1).setW(0_100);

        run();

        assertEquals(0_536705L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_213532645607L, _engine.getExecOrUserARegister(5).getW());
    }
}
