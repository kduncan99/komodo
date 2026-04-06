package com.bearsnake.komodo.engine.functions.shift;
/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSSCFunction extends FunctionUnitTest {

    private long sscImm(long a, long x, long u) {
        return fjaxu(0_73, 0_00, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSSC_BM() throws MachineInterrupt {
        var code = new long[] {
            sscImm(0, 0, 0_12),
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

        _engine.getExecOrUserARegister(0).setW(0_400000_000000L);
        run();
        // bit 35 moved to 25. 2^25 = 33554432.
        assertEquals(33554432L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testSSC_EM() throws MachineInterrupt {
        var code = new long[] {
            sscImm(3, 0, 9),
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

        _engine.getExecOrUserARegister(3).setW(0_000000_000007L); // last 3 bits

        run();

        // right shift circular 9.
        // bits 0,1,2 move to 27,28,29.
        // 2^27 + 2^28 + 2^29 = 134217728 + 268435456 + 536870912 = 939524096.
        assertEquals(939524096L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testSSC_ShiftAll() throws MachineInterrupt {
        var code = new long[] {
            sscImm(5, 0, 36),
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

        _engine.getExecOrUserARegister(5).setW(0_123456_765432L);

        run();

        assertEquals(0_123456_765432L, _engine.getExecOrUserARegister(5).getW());
    }
}
