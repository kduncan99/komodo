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

public class TestDSAFunction extends TestFunction {

    private long dsaImm(long a, long x, long u) {
        return fjaxu(0_73, 0_05, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testDSA_Positive() throws MachineInterrupt {
        var code = new long[] {
            dsaImm(0, 0, 1),
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

        // A0 = 0_000000_000001, A1 = 0
        // Combined 72-bit: 0x000000001 000000000
        // Shift right 1: 0x000000000 1000000000 (octal) -> 0x0, 0x4000000000 (no, that's wrong)
        // Let's use simple values.
        // A0 = 1, A1 = 0. 72-bit: ...001 000...
        // Shift right 1: A0 = 0, A1 = 0_400000_000000L
        _engine.getExecOrUserARegister(0).setW(1L);
        _engine.getExecOrUserARegister(1).setW(0L);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testDSA_Negative() throws MachineInterrupt {
        var code = new long[] {
            dsaImm(2, 0, 1),
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

        // A2 = 0_400000_000000L, A3 = 0
        // Shift right 1 algebraic: A2 = 0_600000_000000L, A3 = 0
        _engine.getExecOrUserARegister(2).setW(0_400000_000000L);
        _engine.getExecOrUserARegister(3).setW(0L);

        run();

        assertEquals(0_600000_000000L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testDSA_ShiftAll() throws MachineInterrupt {
        var code = new long[] {
            dsaImm(4, 0, 72),
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

        // Negative number
        _engine.getExecOrUserARegister(4).setW(0_400000_000000L);
        _engine.getExecOrUserARegister(5).setW(0L);

        run();

        // All bits become sign bit (1)
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(5).getW());
    }
}
