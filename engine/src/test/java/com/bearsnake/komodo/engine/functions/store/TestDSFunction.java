/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDSFunction extends FunctionUnitTest {

    private long dsBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(071, 012, a, x, h, i, u);
    }

    private long dsEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(071, 012, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testDS_BM() throws MachineInterrupt {
        var code = new long[] {
            dsBM(0, 0, 0, 0, 040000),
            0,
        };

        var data = new long[02000];


        loadBaseRegister((short) 14, false, 0_22000, 0_22777, 0, code);
        loadBaseRegister((short) 15, false, 0_40000, 0_40777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(0).setW(0_123456_765432L);
        _engine.getExecOrUserARegister(1).setW(0_111111_222222L);

        run();

        assertEquals(0_123456_765432L, data[0]);
        assertEquals(0_111111_222222L, data[1]);
    }

    @Test
    public void testDS_EM_A15() throws MachineInterrupt {
        var code = new long[] {
            dsEM(15, 0, 0, 0, 2, 01000),
            0,
        };

        var data = new long[02000];


        loadBaseRegister((short) 0, false, 0_01000, 0_01777, 0, code);
        loadBaseRegister((short) 2, false, 0_00000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(15).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(0).setW(0_000000_000000L); // A15+1 wrap to A0

        run();

        assertEquals(0_777777_777777L, data[01000]);
        assertEquals(0_000000_000000L, data[01001]);
    }

    @Test
    public void testDS_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            dsEM(4, 1, 0, 0, 2, 01000), // DS A4, 01000,X1
            0,
        };

        var data = new long[02000];


        loadBaseRegister((short) 0, false, 0_01000, 0_01777, 0, code);
        loadBaseRegister((short) 2, false, 0_00000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_123456_654321L);
        _engine.getExecOrUserARegister(5).setW(0_654321_123456L);
        _engine.getExecOrUserXRegister(1).setW(010L);

        run();

        assertEquals(0_123456_654321L, data[01010]);
        assertEquals(0_654321_123456L, data[01011]);
    }

    @Test
    public void testDS_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            dsBM(4, 0, 0, 1, 040000), // DS A4, *040000
            0,
        };

        var data = new long[02000];
        data[0] = 0_000000_040005L; // Pointer at 040000 pointing to 040005


        loadBaseRegister((short) 14, false, 0_22000, 0_22777, 0, code);
        loadBaseRegister((short) 15, false, 0_40000, 0_40777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_777666_555444L);
        _engine.getExecOrUserARegister(5).setW(0_111222_333444L);

        run();

        assertEquals(0_777666_555444L, data[5]);
        assertEquals(0_111222_333444L, data[6]);
    }
}
