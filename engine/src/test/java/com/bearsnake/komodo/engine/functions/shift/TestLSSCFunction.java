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

public class TestLSSCFunction extends TestFunction {

    private long lsscImm(long a, long x, long u) {
        return fjaxu(0_73, 0_10, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLSSC_BM() throws MachineInterrupt {
        var code = new long[] {
            lsscImm(0, 0, 0_12),
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

        // 000000 000001 octal -> bit 0 is set.
        _engine.getExecOrUserARegister(0).setW(0_000000_000001L);
        run();
        // Shift left circular 0_12 (10 decimal).
        // bit 0 moves to bit 10. 2^10 = 1024.
        assertEquals(1024L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testLSSC_EM() throws MachineInterrupt {
        var code = new long[] {
            lsscImm(3, 0, 9),
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

        // 0_123456_765432L
        _engine.getExecOrUserARegister(3).setW(0_123456_765432L);

        run();

        // 0_123456_765432L left shift circular 9.
        // Original: 001 010 011 100 101 110 111 110 101 100 011 010
        // Left 9:   100 101 110 111 110 101 100 011 010 001 010 011 (binary)
        //           4   5   6   7   6   5   4   3   2   1   2   3 (octal)
        assertEquals(0_456765_432123L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testLSSC_Circular() throws MachineInterrupt {
        var code = new long[] {
            lsscImm(5, 0, 36),
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

        _engine.getExecOrUserARegister(5).setW(0_123456_765432L);

        run();

        assertEquals(0_123456_765432L, _engine.getExecOrUserARegister(5).getW());
    }
}
