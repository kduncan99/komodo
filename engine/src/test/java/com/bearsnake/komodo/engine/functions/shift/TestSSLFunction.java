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

public class TestSSLFunction extends TestFunction {

    private long sslImm(long a, long x, long u) {
        return fjaxu(0_73, 0_02, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testSSL_BM() throws MachineInterrupt {
        var code = new long[] {
            sslImm(0, 0, 0_12),
            0,
        };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0_22)   // 022000
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(14).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0_777777_777777L);

        run();

        // 0_12 (octal) is 10 (decimal).
        // 0_777777_777777L >> 10 = 0_003777_777777L (67108863 decimal)
        // 00377777777 octal is 67108863 decimal.
        assertEquals(67108863L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testSSL_EM() throws MachineInterrupt {
        var code = new long[] {
            sslImm(3, 0, 9),
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

        _engine.getExecOrUserARegister(3).setW(0_123456_654321L);

        run();

        assertEquals(0_000123_456654L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testSSL_ShiftAll() throws MachineInterrupt {
        var code = new long[] {
            sslImm(5, 0, 36),
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

        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);

        run();

        assertEquals(0L, _engine.getExecOrUserARegister(5).getW());
    }
}
