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

public class TestDSCFunction extends TestFunction {

    private long dscImm(long a, long x, long u) {
        return fjaxu(0_73, 0_01, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testDSC_BM() throws MachineInterrupt {
        var code = new long[] {
            dscImm(0, 0, 1),
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

        // A0 = 1, A1 = 0.
        // Combined 72-bit: ...001 000... (bit 36 is 1, others 0)
        // Shift right 1 circular: bit 36 moves to 35.
        // A0 = 0, A1 = 0_400000_000000L
        _engine.getExecOrUserARegister(0).setW(1L);
        _engine.getExecOrUserARegister(1).setW(0L);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testDSC_EM() throws MachineInterrupt {
        var code = new long[] {
            dscImm(2, 0, 1),
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

        // A2 = 0, A3 = 1.
        // Combined 72-bit: ...000 001. (bit 0 is 1)
        // Shift right 1 circular: bit 0 moves to 71.
        // A2 = 0_400000_000000L, A3 = 0.
        _engine.getExecOrUserARegister(2).setW(0L);
        _engine.getExecOrUserARegister(3).setW(1L);

        run();

        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(0L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testDSC_Shift36() throws MachineInterrupt {
        var code = new long[] {
            dscImm(4, 0, 36),
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

        _engine.getExecOrUserARegister(4).setW(0_123456_765432L);
        _engine.getExecOrUserARegister(5).setW(0_000000_111111L);

        run();

        // Shifting by 36 bits in a 72-bit circular shift swaps the words.
        assertEquals(0_000000_111111L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_123456_765432L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testDSC_Shift72() throws MachineInterrupt {
        var code = new long[] {
            dscImm(6, 0, 72),
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

        _engine.getExecOrUserARegister(6).setW(0_123456_765432L);
        _engine.getExecOrUserARegister(7).setW(0_000000_111111L);

        run();

        assertEquals(0_123456_765432L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_000000_111111L, _engine.getExecOrUserARegister(7).getW());
    }
}
