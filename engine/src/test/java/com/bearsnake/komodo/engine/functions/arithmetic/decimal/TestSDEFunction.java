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
 * Subtract Decimal instruction
 * (SDE) Subtracts decimal (U) from decimal A(a) and stores the result in A(a).
 */
public class TestSDEFunction extends TestDecimalFunction {

    private long sde(long a, long x, long u) {
        return fjaxu(0_07, 0_02, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSDE_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 13,456,789 - 1,111,111 = 12,345,678
        code[0] = sde(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(0, 1, 1, 1, 1, 1, 1, 1, POSITIVE_SIGN);
        _engine.getExecOrUserARegister(4).setW(decWord(1, 3, 4, 5, 6, 7, 8, 9, POSITIVE_SIGN));

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

        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 100 - (-50) = 150
        code[0] = sde(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 5, 0, NEGATIVE_SIGN);
        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 0, 1, 0, 0, POSITIVE_SIGN));

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

        assertEquals(decWord(0, 0, 0, 0, 0, 1, 5, 0, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_Zero_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = sde(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN);
        _engine.getExecOrUserARegister(4).setW(decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN));

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

        assertEquals(decWord(1, 2, 3, 4, 5, 6, 7, 8, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_Positive_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // 30 - 20 = 10
        code[0] = sde(4, 0, 0_400);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 0, 0, 2, 0, POSITIVE_SIGN);
        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 0, 0, 3, 0, POSITIVE_SIGN));

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

        assertEquals(decWord(0, 0, 0, 0, 0, 0, 1, 0, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        // SDE A4, *0_400
        code[0] = fjaxu(0_07, 0_02, 4, 0, 0_200400); // I=1, U=400
        code[1] = 0;
        code[0_400] = 0_600; 
        code[0_600] = decWord(0, 0, 0, 0, 0, 0, 0, 5, POSITIVE_SIGN);
        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 0, 0, 1, 0, POSITIVE_SIGN));

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
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_4); // bank 14 is BDI 4

        run();

        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 5, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // SDE A4, 0_300, X1
        code[0] = sde(4, 1, 0_300);
        code[1] = 0;
        code[0_400] = decWord(0, 0, 0, 0, 1, 1, 1, 1, POSITIVE_SIGN);
        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 3, 3, 3, 3, POSITIVE_SIGN));

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

        assertEquals(decWord(0, 0, 0, 0, 2, 2, 2, 2, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testSDE_GRS_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        // SDE A4, 0_100 (U=R0)
        code[0] = sde(4, 0, 0_100);
        code[1] = 0;

        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 3, 3, 3, 3, POSITIVE_SIGN));
        _engine.getExecOrUserRRegister(0).setW(decWord(0, 0, 0, 0, 1, 1, 1, 1, POSITIVE_SIGN));

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

        assertEquals(decWord(0, 0, 0, 0, 2, 2, 2, 2, POSITIVE_SIGN), _engine.getExecOrUserARegister(4).getW());
    }
}
