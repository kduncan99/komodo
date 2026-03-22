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

    private long dei(long a, long x, long u) {
        return fjaxu(0_07, 0_06, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }


    @Test
    public void testDEI_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 12,345,678
        code[0] = dei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN);

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

        assertEquals(12345678L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // -12,345,678
        code[0] = dei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(1, 2, 3, 4, 5, 6, 7, 8, NEGATIVE_SIGN);

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

        // 1's complement of 12345678
        assertEquals(~12345678L & 0_777777_777777L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN);

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
    }

    @Test
    public void testDEI_NegativeZero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, NEGATIVE_SIGN);

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

        // -0 is +0 in this implementation (see DEI execute method)
        assertEquals(0L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 12,345,678
        code[0] = dei(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN);

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

        assertEquals(12345678L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DEI A4, *0_400
        // instruction: f=007, j=006, a=4, x=0, h=0, i=1, u=0_400
        code[0] = fjaxu(0_07, 0_06, 4, 0, 0_200400); // I=1, U=400
        code[1] = 0;
        code[0_400] = 0_600; // Pointer to 0_600 (interpreted as XHIU)
        // decWord(8, 7, 6, 5, 4, 3, 2, 1, POSITIVE_SIGN) -> 87654321
        code[0_600] = decWord(8, 7, 6, 5, 4, 3, 2, 1, POSITIVE_SIGN);

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                .setLowerLimit(0)
                .setUpperLimit(code.length - 1)
                .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        bd.setInactive(false);
        _engine.getBaseRegister(14).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
                .setBasicModeEnabled(true)
                .setProcessorPrivilege((short)2); // Required for indirect addressing in BM
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_4); // bank 14 is BDI 4

        run();

        assertEquals(87654321L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDEI_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // DEI A4, 0_300, X1
        code[0] = dei(4, 1, 0_300);
        code[1] = 0;
        code[0_400] = decWord(1, 1, 1, 1, 1, 1, 1, 1, POSITIVE_SIGN);

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

        assertEquals(11111111L, _engine.getExecOrUserARegister(4).getW());
    }
}
