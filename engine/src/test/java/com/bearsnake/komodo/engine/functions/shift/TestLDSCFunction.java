/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.shift;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLDSCFunction extends FunctionUnitTest {

    private long ldscImm(long a, long x, long u) {
        return fjaxu(0_73, 0_11, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testLDSC_BM() throws MachineInterrupt {
        var code = new long[] {
            ldscImm(0, 0, 1),
            0,
        };

        loadBaseRegister((short) 14, false, 0_44000, 0_44777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_44000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0_400000_000000L); // Bit 35 set
        _engine.getExecOrUserARegister(1).setW(0_000000_000000L);

        run();

        // Left Shift Circular 1.
        // Combined (72 bits): 100...000 000...000
        // Left 1: 000...000 000...001
        assertEquals(0L, _engine.getExecOrUserARegister(0).getW());
        assertEquals(1L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testLDSC_EM() throws MachineInterrupt {
        var code = new long[] {
            ldscImm(2, 0, 1),
            0,
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000000L);
        _engine.getExecOrUserARegister(3).setW(0_000000_000001L); // Bit 0 set

        run();

        // Left Shift Circular 1.
        // Combined: 000...000 000...001
        // Left 1: 000...000 000...010
        assertEquals(0L, _engine.getExecOrUserARegister(2).getW());
        assertEquals(2L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testLDSC_Shift36_EM() throws MachineInterrupt {
        var code = new long[] {
            ldscImm(4, 0, 36),
            0,
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_123456_765432L);
        _engine.getExecOrUserARegister(5).setW(0_000000_777777L);

        run();

        // 36-bit circular shift swaps words.
        assertEquals(0_000000_777777L, _engine.getExecOrUserARegister(4).getW());
        assertEquals(0_123456_765432L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testLDSC_Shift72_EM() throws MachineInterrupt {
        var code = new long[] {
            ldscImm(6, 0, 72),
            0,
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(6).setW(0_123456_765432L);
        _engine.getExecOrUserARegister(7).setW(0_000000_777777L);

        run();

        // 72-bit circular shift is identity.
        assertEquals(0_123456_765432L, _engine.getExecOrUserARegister(6).getW());
        assertEquals(0_000000_777777L, _engine.getExecOrUserARegister(7).getW());
    }
}
