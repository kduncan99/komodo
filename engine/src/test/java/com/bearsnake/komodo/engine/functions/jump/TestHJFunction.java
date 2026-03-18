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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHJFunction extends TestFunction {

    private long hjBM(
        long a,
        long x,
        long h,
        long i,
        long u
    ) {
        return fjaxhiu(074, 005, a, x, h, i, u);
    }

    private long hjBM(
        long a,
        long u
    ) {
        return fjaxu(074, 005, a, 0, u);
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
    public void testHJ_Simple_BM() throws MachineInterrupt {
        var code = new long[]{
            hjBM(0, 01003),
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

        run();

        assertEquals(0_001003L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testHJ_ToDBank_BM() throws MachineInterrupt {
        var code = new long[]{ hjBM(0, 03000), };
        var data = new long[]{ 0, 0, 0, };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)   // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_3)
                                      .setUpperLimit(0_3777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12)
               .setBankDescriptor(bd0)
               .setStorage(bank0)
               .setSubsetting(0);
        _engine.getBaseRegister(13)
               .setBankDescriptor(bd1)
               .setStorage(bank1)
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

        assertEquals(0_003000L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testHJ_ToOppositeIBank_BM() throws MachineInterrupt {
        var code = new long[]{ hjBM(0, 04000), };
        var other = new long[]{ 0, 0, 0, };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(other);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)   // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_4)   // 04000
                                      .setUpperLimit(0_4777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12)
               .setBankDescriptor(bd0)
               .setStorage(bank0)
               .setSubsetting(0);
        _engine.getBaseRegister(14)
               .setBankDescriptor(bd1)
               .setStorage(bank1)
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

        assertEquals(0_004000L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testHJ_ToOppositeDBank_BM() throws MachineInterrupt {
        var code = new long[]{ hjBM(0, 05000), };
        var other = new long[]{ 0, 0, 0, };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(other);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)   // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_5)   // 05000
                                      .setUpperLimit(0_5777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12)
               .setBankDescriptor(bd0)
               .setStorage(bank0)
               .setSubsetting(0);
        _engine.getBaseRegister(15)
               .setBankDescriptor(bd1)
               .setStorage(bank1)
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

        assertEquals(0_005000L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testHJ_Indexed_Indirect_BM() throws MachineInterrupt {
        var code = new long[]{
            hjBM(0, 3, 1, 1, 01),
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
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).setXI(0_10).setXM(0_01000);

        run();

        assertEquals(0_10, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).getXI());
        assertEquals(0_01010, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).getXM());
        assertEquals(0_001002L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testHJ_Indirect_BM() throws MachineInterrupt {
        var code = new long[]{
            hjBM(0, 0, 0, 1, 01001),
            fjaxhiu(0, 0, 0, 0, 0, 1, 01002),
            fjaxhiu(0, 0, 0, 0, 0, 1, 01003),
            fjaxhiu(0, 0, 0, 0, 0, 1, 01004),
            fjaxhiu(0, 0, 0, 0, 0, 1, 01005),
            fjaxhiu(0, 0, 0, 0, 0, 1, 01006),
            fjaxhiu(0, 0, 0, 0, 0, 0, 01007),
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

        run();

        assertEquals(0_001007L, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
