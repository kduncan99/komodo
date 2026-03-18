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

import static com.bearsnake.komodo.engine.Constants.GRS_X3;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSLJFunction extends TestFunction {

    private long sljBM(
        long x,
        long h,
        long i,
        long u
    ) {
        return fjaxhiu(072, 001, 0, x, h, i, u);
    }

    private long sljBM(
        long u
    ) {
        return fjaxu(072, 001, 0, 0, u);
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
    public void testSLJ_Simple() throws MachineInterrupt {
        var code = new long[]{
            sljBM(01003),
            0,
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
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

        run();

        assertEquals(0_000000_001001L, code[3]);
        assertEquals(0_001004L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testSLJ_Indexed() throws MachineInterrupt {
        var code = new long[]{
            sljBM(3, 1, 0, 01000),
            0,
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
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

        _engine.getGeneralRegisterSet().getRegister(GRS_X3).setXI(0_000100).setXM(0_03);

        run();

        assertEquals(0_000000_001001L, code[3]);
        assertEquals(0_001004L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_000100, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXI());
        assertEquals(0_000103, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXM());
    }

    @Test
    public void testSLJ_Indirect() throws MachineInterrupt {
        var code = new long[]{
            sljBM(0, 0, 1, 01001),
            fjaxhiu(0, 0, 0, 0, 0, 0, 01003),
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
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

        run();

        assertEquals(0_000000_001001L, code[3]);
        assertEquals(0_001004L, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
