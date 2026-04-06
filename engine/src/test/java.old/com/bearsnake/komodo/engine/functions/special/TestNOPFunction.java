/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNOPFunction extends FunctionUnitTest {

    private long nopEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(073, 014, 0, x, h, i, b, d);
    }

    private long nopBM(long x, long h, long i, long u) {
        return fjaxhiu(073, 014, 0, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testNOP_EM() throws MachineInterrupt {
        var code = new long[] {
            nopEM(0, 0, 0, 0, 0),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        run();
    }

    @Test
    public void testNOP_BM() throws MachineInterrupt {
        var code = new long[] {
            nopBM(0, 0, 0, 0),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22) // 022000
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();
    }

    @Test
    public void testNOP_Indirect() throws MachineInterrupt {
        var code = new long[] {
            nopBM(1, 1, 1, 040000),
            0,
        };
        var data = new long[] {
            fjaxhiu(0, 0, 0, 2, 1, 1, 040002),// here first
            fjaxhiu(0, 0, 0, 3, 1, 1, 040001),// not touched
            fjaxhiu(0, 0, 0, 4, 1, 0, 040000),// then here
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_10)
                                      .setUpperLimit(0_10777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        var bank1 = new ArraySlice(data);
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_40)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(13).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_10000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(1).setXI(0_01).setXM(0_0);
        _engine.getExecOrUserXRegister(2).setXI(0_02).setXM(0_0);
        _engine.getExecOrUserXRegister(3).setXI(0_03).setXM(0_0);
        _engine.getExecOrUserXRegister(4).setXI(0_04).setXM(0_0);

        run();

        assertEquals(010001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(01, _engine.getExecOrUserXRegister(1).getXM());
        assertEquals(02, _engine.getExecOrUserXRegister(2).getXM());
        assertEquals(00, _engine.getExecOrUserXRegister(3).getXM());
        assertEquals(04, _engine.getExecOrUserXRegister(4).getXM());
    }
}
