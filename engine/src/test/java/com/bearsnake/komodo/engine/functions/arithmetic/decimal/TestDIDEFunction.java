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
 * Double Integer to Decimal instruction
 * (DIDE) Converts the double-precision one's complement binary operand
 * to three-word decimal operand.
 */
public class TestDIDEFunction extends TestDecimalFunction {

    private long dide(long a, long x, long u) {
        return fjaxu(0_07, 0_11, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }


    @Test
    public void testDIDE_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dide(4, 0, 0_400);
        code[1] = 0;
        // Value: 12,345,678,901,234,567
        // Binary (72-bit 1's complement):
        // MSW: 0_536705L
        // LSW: 0_213532645607L
        code[0_400] = 0_536705L;
        code[0_401] = 0_213532645607L;

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

        // 17 digits: 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7
        // digits 1 2 3 4 5 6 7 8 9 (9 digits) -> Word 1
        // digits 0 1 2 3 4 5 6 7 (8 digits) + sign -> Word 2
        // digits (none) -> Word 0
        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        // value1: digits 1 2 3 4 5 6 7 8 9
        assertEquals(0x123456789L, _engine.getExecOrUserARegister(5).getW());
        // value2: digits 0 1 2 3 4 5 6 7 + POSITIVE_SIGN
        assertEquals(0x012345670L | POSITIVE_SIGN, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDIDE_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dide(4, 0, 0_400);
        code[1] = 0;
        // Negative 12,345,678,901,234,567
        // MSW: 0_777777241072L
        // LSW: 0_564245132170L
        code[0_400] = 0_777777241072L;
        code[0_401] = 0_564245132170L;

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

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0x123456789L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0x012345670L | NEGATIVE_SIGN, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDIDE_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dide(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = 0;
        code[0_401] = 0;

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

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(POSITIVE_SIGN, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDIDE_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dide(4, 0, 0_400);
        code[1] = 0;
        // Value: 12,345,678,901,234,567
        code[0_400] = 0_536705L;
        code[0_401] = 0_213532645607L;

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

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0x123456789L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0x012345670L | POSITIVE_SIGN, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDIDE_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DIDE A4, *0_400
        code[0] = fjaxu(0_07, 0_11, 4, 0, 0_200400); // I=1, U=400
        code[1] = 0;
        code[0_400] = 0_600;
        code[0_600] = 0_536705L;
        code[0_601] = 0_213532645607L;

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

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0x123456789L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0x012345670L | POSITIVE_SIGN, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDIDE_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DIDE A4, 0_300, X1
        code[0] = dide(4, 1, 0_300);
        code[1] = 0;
        code[0_400] = 0_536705L;
        code[0_401] = 0_213532645607L;

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

        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0x123456789L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0x012345670L | POSITIVE_SIGN, _engine.getExecOrUserARegister(6).getW());
    }
}
