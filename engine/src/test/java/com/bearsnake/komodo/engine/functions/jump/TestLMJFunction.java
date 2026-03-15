/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLMJFunction extends TestFunction {

    private long lmjBM(
        long a,
        long x,
        long h,
        long i,
        long u
    ) {
        return fjaxhiu(074, 013, a, x, h, i, u);
    }

    private long lmjEM(
        long a,
        long x,
        long h,
        long i,
        long u
    ) {
        return fjaxhiu(074, 013, a, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister()
               .clear();
        _engine.getProgramAddressRegister()
               .setProgramCounter(0)
               .setBankDescriptorIndex(0)
               .setBankLevel((short) 0);
    }

    @Test
    public void testLMJ_Simple_BM() throws MachineInterrupt {
        var code = new long[]{
            lmjBM(11, 0, 0, 0, 01003),
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)   // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12)
               .setBankDescriptor(bd0)
               .setStorage(bank0)
               .setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getGeneralRegister(GRS_X11).setW(0_111111_111111L);

        run();

        assertEquals(0_001003L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_111111_001001L, _engine.getGeneralRegister(GRS_X11).getW());
    }

    @Test
    public void testLMJ_Indexed_Indirect_BM() throws MachineInterrupt {
        var code = new long[]{
            lmjBM(10, 3, 1, 1, 01),
            fjaxhiu(0, 0, 0, 0, 0, 0, 01002),
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)   // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12)
               .setBankDescriptor(bd0)
               .setStorage(bank0)
               .setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getGeneralRegister(GRS_X3).setXI(0_10).setXM(0_01000);

        run();

        assertEquals(0_10, _engine.getGeneralRegister(GRS_X3).getXI());
        assertEquals(0_01010, _engine.getGeneralRegister(GRS_X3).getXM());
        assertEquals(0_01001, _engine.getGeneralRegister(GRS_X10).getXM());
        assertEquals(0_001002L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testLMJ_Simple_EM() throws MachineInterrupt {
        var code = new long[]{
            lmjEM(5, 0, 0, 0, 01003),
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0)
               .setBankDescriptor(bd0)
               .setStorage(bank0)
               .setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        _engine.getGeneralRegister(GRS_X5).setW(0_111111_111111L);

        run();

        assertEquals(0_001003L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_111111_001001L, _engine.getGeneralRegister(GRS_X5).getW());
    }
}
