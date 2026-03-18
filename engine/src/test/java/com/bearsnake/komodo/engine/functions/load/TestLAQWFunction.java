/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLAQWFunction extends TestFunction {

    private long laqwBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(07, 04, a, x, h, i, u);
    }

    private long laqwEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(07, 04, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLAQW_BM() throws MachineInterrupt {
        var code = new long[]{
            laqwBM( 0, 4, 0, 0, 040000),
            laqwBM( 1, 5, 0, 0, 040000),
            laqwBM( 2, 6, 0, 0, 040000),
            laqwBM( 3, 7, 0, 0, 040000),
            0,
            };

        var data = new long[]{
            0_040777_777777L,
            0_777030_777777L,
            0_777777_020777L,
            0_777777_777010L,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_40)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);

        _engine.getExecOrUserARegister(0).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(1).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(2).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(3).setW(0_333333_333333L);
        _engine.getExecOrUserXRegister(4).setXI(0_000000).setXM(0_000000);
        _engine.getExecOrUserXRegister(5).setXI(0_010000).setXM(0_000001);
        _engine.getExecOrUserXRegister(6).setXI(0_020000).setXM(0_000002);
        _engine.getExecOrUserXRegister(7).setXI(0_030000).setXM(0_000003);

        run();

        assertEquals(0_040L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_030L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_020L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_010L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testLAQW_EM() throws MachineInterrupt {
        var code = new long[]{
            laqwEM( 0, 4, 0, 0, 3, 0),
            laqwEM( 1, 5, 0, 0, 4, 0),
            laqwEM( 2, 6, 0, 0, 5, 0),
            laqwEM( 3, 7, 0, 0, 6, 0),
            0,
            };

        var data0 = new long[]{ 0_040777_777777L };
        var data1 = new long[]{ 0, 0_777030_777777L };
        var data2 = new long[]{ 0, 0, 0_777777_020777L };
        var data3 = new long[]{ 0, 0, 0, 0_777777_777010L };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data0);
        var bank2 = new ArraySlice(data1);
        var bank3 = new ArraySlice(data2);
        var bank4 = new ArraySlice(data3);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 2, 0));
        var bd3 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 3, 0));
        var bd4 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 4, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(3).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getBaseRegister(4).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getBaseRegister(5).setBankDescriptor(bd3).setStorage(bank3).setSubsetting(0);
        _engine.getBaseRegister(6).setBankDescriptor(bd4).setStorage(bank4).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);

        _engine.getExecOrUserARegister(0).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(1).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(2).setW(0_333333_333333L);
        _engine.getExecOrUserARegister(3).setW(0_333333_333333L);
        _engine.getExecOrUserXRegister(4).setXI(0_000000).setXM(0_000000);
        _engine.getExecOrUserXRegister(5).setXI(0_010000).setXM(0_000001);
        _engine.getExecOrUserXRegister(6).setXI(0_020000).setXM(0_000002);
        _engine.getExecOrUserXRegister(7).setXI(0_030000).setXM(0_000003);

        run();

        assertEquals(0_040L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_030L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_020L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_010L, _engine.getExecOrUserARegister(3).getW());
    }
}
