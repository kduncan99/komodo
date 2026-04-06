/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDLFunction extends FunctionUnitTest {

    private long dlBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(071, 013, a, x, h, i, u);
    }

    private long dlEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(071, 013, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testDL_BM() throws MachineInterrupt {
        var code = new long[] {
            dlBM(0, 0, 0, 0, 030000),
            dlBM(2, 2, 0, 0, 030000),
            0,
            };

        var data = new long[] {
            0_000001_000001,
            0_000002_000002,
            0_000003_000003,
            0_000004_000004,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_22)   // 022000
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_30)
                                      .setUpperLimit(0_30777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(14).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(2).setXI(0).setXM(0_02);

        run();

        assertEquals(0_000001_000001L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_000002_000002L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_000003_000003L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_000004_000004L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testDL_EM() throws MachineInterrupt {
        var code = new long[] {
            dlEM(10, 0, 0, 0, 5, 01000),
            dlEM(12, 2, 0, 0, 5, 01000),
            0,
            };

        var data = new long[] {
            0_000001_000001,
            0_000002_000002,
            0_000003_000003,
            0_000004_000004,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)       // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(5).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(2).setXI(0).setXM(0_02);

        run();

        assertEquals(0_000001_000001L, _engine.getExecOrUserARegister(10).getW());
        assertEquals(0_000002_000002L, _engine.getExecOrUserARegister(11).getW());
        assertEquals(0_000003_000003L, _engine.getExecOrUserARegister(12).getW());
        assertEquals(0_000004_000004L, _engine.getExecOrUserARegister(13).getW());
    }

    @Test
    public void testDL_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            dlBM(0, 0, 0, 0, 030000),
            dlBM(2, 2, 0, 0, 030000),
            dlBM(4, 3, 0, 1, 030000), // indirect to data,X3
            0,
            };

        var data = new long[] {
            0_000001_000001,
            0_000002_000002,
            0_000003_000003,
            0_000004_000004,
            0_000005_000005,
            0_000006_000006,
            fjaxhiu(0, 0, 0, 4, 1, 0, 030000),
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)   // 022000
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_30)
                                      .setUpperLimit(0_30777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(14).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(2).setXI(0_02).setXM(0_02);
        _engine.getExecOrUserXRegister(3).setXI(0_00).setXM(0_06);
        _engine.getExecOrUserXRegister(4).setXI(0_04).setXM(0_04);

        run();

        assertEquals(0_000002_000002L, _engine.getExecOrUserXRegister(2).getW());
        assertEquals(0_000000_000006L, _engine.getExecOrUserXRegister(3).getW());
        assertEquals(0_000004_000010L, _engine.getExecOrUserXRegister(4).getW());

        assertEquals(0_000001_000001L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_000002_000002L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_000003_000003L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_000004_000004L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0_000005_000005L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_000006_000006L, _engine.getExecOrUserARegister(5).getW());
    }
}
