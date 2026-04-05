/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestLXLMFunction extends TestFunction {

    private long lxlmBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(075, 013, a, x, h, i, u);
    }

    private long lxlmEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(075, 013, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLXLM_BM_InvalidPP() throws MachineInterrupt {
        var code = new long[] {
            lxlmBM(2, 0, 0, 0, 040002),
            0,
            };

        var data = new long[] {
            0_1L,
            0_2L,
            0_3L,
            0_4L,
            0_5L
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(040)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(13).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(GRS_X2).setW(0_111111_111111L);

        assertThrows(InvalidInstructionInterrupt.class, () -> run());

        assertEquals(0_111111_111111L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
    }

    @Test
    public void testLXLM_BM() throws MachineInterrupt {
        var code = new long[] {
            lxlmBM(2, 0, 0, 0, 040004),
            0,
            };

        var data = new long[] {
            0_1L,
            0_2L,
            0_3L,
            0_4L,
            0_232334_454567L
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(040)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(13).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(GRS_X2).setW(0_111111_111111L);

        run();

        System.out.printf("%012o <=> %012o\n", 0_111134_454567L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
        assertEquals(0_111134_454567L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
    }

    @Test
    public void testLXLM_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            lxlmEM(5, 3, 1, 0, 1, 01),
            0,
            };

        var data = new long[] {
            0_11L,
            0_12L,
            0_13L,
            0_14L,
            0_15L
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(1).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXI(0_01).setXM(0_03);

        _engine.getGeneralRegisterSet().getRegister(GRS_X5).setW(0_111111_111111L);

        run();

        assertEquals(0_111100_000015L, _engine.getGeneralRegisterSet().getRegister(GRS_X5).getW());
        assertEquals(0_01L, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXI());
        assertEquals(0_04L, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXM());
    }
}
