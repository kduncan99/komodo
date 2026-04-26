/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJKFunction extends FunctionUnitTest {

    private long jkBM(long x, long h, long i, long u) {
        return fjaxhiu(074, 004, 001, x, h, i, u);
    }

    private long jkBM(long u) {
        return fjaxu(074, 004, 001, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJK_Simple_BM() throws MachineInterrupt {
        var code = new long[]{
            jkBM(01003),
            0,
            0,
            0,
            };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        run();

        assertEquals(0_001001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJK_ToDBank_BM() throws MachineInterrupt {
        var code = new long[]{
            jkBM(03000),
            0,
            };
        var data = new long[]{ 0, 0, 0, };


        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_3000, 0_3777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        run();

        assertEquals(0_001001L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJK_Indexed_Indirect_BM() throws MachineInterrupt {
        var code = new long[]{
            jkBM(3, 1, 1, 02),
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 01002),
            0,
            };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);
        _engine.getExecOrUserXRegister(3).setXI(0_10).setXM(0_01000);

        run();

        assertEquals(0_10, _engine.getExecOrUserXRegister(3).getXI());
        assertEquals(0_01010, _engine.getExecOrUserXRegister(3).getXM());
        assertEquals(0_001001L, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
