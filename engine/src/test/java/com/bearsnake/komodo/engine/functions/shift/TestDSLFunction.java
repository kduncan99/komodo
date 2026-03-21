package com.bearsnake.komodo.engine.functions.shift;
/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDSLFunction extends TestFunction {

    private long dslImm(long a, long x, long u) {
        return fjaxu(0_73, 0_03, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testDSL_BM() throws MachineInterrupt {
        var code = new long[] {
            dslImm(0, 0, 1),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_44)   // 044000
                                     .setUpperLimit(0_44777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(14).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_44000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0_000000_000001L);
        _engine.getExecOrUserARegister(1).setW(0_000000_000000L);
        run();
        // Shift right logical 1.
        // A0: 000...001
        // A1: 000...000
        // Combined: 000...001 000...000
        // Shift right 1: 000...000 100...000
        assertEquals(0L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testDSL_EM() throws MachineInterrupt {
        var code = new long[] {
            dslImm(3, 0, 9),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(3).setW(0_456765_432100L);
        _engine.getExecOrUserARegister(4).setW(0_000000_000000L);

        run();

        // 0_456765_432100L right shift logical 9.
        // 456 765 432 100 -> A0
        // 000 000 000 000 -> A1
        // result should be 000 456 765 432 and 100 000 000 000
        assertEquals(0_000456_765432L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0_100000_000000L, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testDSL_ShiftAll() throws MachineInterrupt {
        var code = new long[] {
            dslImm(5, 0, 72),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(6).getW());
    }
}
