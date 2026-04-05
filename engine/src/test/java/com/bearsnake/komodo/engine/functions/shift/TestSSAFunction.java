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

public class TestSSAFunction extends TestFunction {

    private long ssaImm(long a, long x, long u) {
        return fjaxu(0_73, 0_04, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSSA_BM() throws MachineInterrupt {
        var code = new long[] {
            ssaImm(0, 0, 0_12),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_44)   // 044000
                                     .setUpperLimit(0_44777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(14).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_44000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // Positive number
        _engine.getExecOrUserARegister(0).setW(0_000777_777777L);

        run();

        // 0_12 octal is 10 decimal.
        // 0_000777_777777L >> 10 (algebraic) = 0_000000_177777L (131071 decimal)
        assertEquals(131071L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testSSA_Negative() throws MachineInterrupt {
        var code = new long[] {
            ssaImm(3, 0, 9),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // Negative number: 0_400000_000000L (sign bit only)
        _engine.getExecOrUserARegister(3).setW(0_400000_000000L);

        run();

        // right shift algebraic 9.
        // preserve sign bit and fill with sign bit.
        // 0_400000_000000L >> 9 (algebraic) = 68652367872 decimal
        assertEquals(68652367872L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testSSA_Positive_1() throws MachineInterrupt {
        var code = new long[] {
            ssaImm(0, 0, 1),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // Positive number 1 >> 1 = 0
        _engine.getExecOrUserARegister(0).setW(1L);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testSSA_Negative_1() throws MachineInterrupt {
        var code = new long[] {
            ssaImm(1, 0, 1),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // Negative number 0_400000_000000 >> 1 = 0_600000_000000
        _engine.getExecOrUserARegister(1).setW(0_400000_000000L);

        run();

        assertEquals(0_600000_000000L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testSSA_ShiftAll() throws MachineInterrupt {
        var code = new long[] {
            ssaImm(5, 0, 36),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // Negative number
        _engine.getExecOrUserARegister(5).setW(0_400000_000000L);

        run();

        // Shift 36 or more: all bits become sign bit.
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(5).getW());
    }
}
