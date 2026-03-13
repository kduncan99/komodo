/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDLNFunction extends TestFunction {

    private Engine _engine;

    private long dlnBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(071, 014, a, x, h, i, u);
    }

    private long dlnEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(071, 014, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testDLN_BM() throws MachineInterrupt {
        var code = new long[] {
            dlnBM(0, 0, 0, 0, 030000),
            dlnBM(2, 2, 0, 0, 030000),
            0,
            };

        var data = new long[] {
            0_000001_000001L,
            0_400002_000002L,
            0_400003_000003L,
            0_000004_000004L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_22)   // 022000
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_30)
                                      .setUpperLimit(0_30777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(14).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(2).setXI(0).setXM(0_02);

        try {
            for (;;) _engine.cycle();
        } catch (InvalidInstructionInterrupt e) {
        }

        assertEquals(0_777776_777776L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_377775_777775L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_377774_777774L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_777773_777773L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testDLN_EM() throws MachineInterrupt {
        var code = new long[] {
            dlnEM(10, 0, 0, 0, 5, 01000),
            dlnEM(12, 2, 0, 0, 5, 01000),
            0,
            };

        var data = new long[] {
            0_777777_777777L,
            0_000002_000002L,
            0_000003_000003L,
            0_000004_000004L,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)       // 01000
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_01)
                                      .setUpperLimit(0_01777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(5).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(2).setXI(0).setXM(0_02);

        try {
            for (;;) _engine.cycle();
        } catch (InvalidInstructionInterrupt e) {
        }

        assertEquals(0_000000_000000L, _engine.getExecOrUserARegister(10).getW());
        assertEquals(0_777775_777775L, _engine.getExecOrUserARegister(11).getW());
        assertEquals(0_777774_777774L, _engine.getExecOrUserARegister(12).getW());
        assertEquals(0_777773_777773L, _engine.getExecOrUserARegister(13).getW());
    }

    @Test
    public void testDLN_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            dlnBM(0, 0, 0, 0, 030000),
            dlnBM(2, 2, 0, 0, 030000),
            dlnBM(4, 3, 0, 1, 030000), // indirect to data,X3
            0,
            };

        var data = new long[] {
            0_000001_000001L,
            0_000002_000002L,
            0_400003_000003L,
            0_000004_000004L,
            0_500005_000005L,
            0_600006_000006L,
            fjaxhiu(0, 0, 0, 4, 1, 0, 030000),
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)   // 022000
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_30)
                                      .setUpperLimit(0_30777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

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

        try {
            for (;;) _engine.cycle();
        } catch (InvalidInstructionInterrupt e) {
        }

        assertEquals(0_000002_000002L, _engine.getExecOrUserXRegister(2).getW());
        assertEquals(0_000000_000006L, _engine.getExecOrUserXRegister(3).getW());
        assertEquals(0_000004_000010L, _engine.getExecOrUserXRegister(4).getW());

        assertEquals(0_777776_777776L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_777775_777775L, _engine.getExecOrUserARegister(1).getW());
        assertEquals(0_377774_777774L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0_777773_777773L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0_277772_777772L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_177771_777771L, _engine.getExecOrUserARegister(5).getW());
    }
}
