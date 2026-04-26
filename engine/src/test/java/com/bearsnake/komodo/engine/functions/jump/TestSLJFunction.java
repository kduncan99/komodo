/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_X3;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSLJFunction extends FunctionUnitTest {

    private long sljBM(long x, long h, long i, long u) {
        return fjaxhiu(072, 001, 0, x, h, i, u);
    }

    private long sljBM(long u) {
        return fjaxu(072, 001, 0, 0, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testSLJ_Simple() throws MachineInterrupt {
        var code = new long[]{
            sljBM(01003),
            0,
            0,
            0,
            0
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

        assertEquals(0_000000_001001L, code[3]);
        assertEquals(0_001004L, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testSLJ_Indexed() throws MachineInterrupt {
        var code = new long[]{
            sljBM(3, 1, 0, 01000),
            0,
            0,
            0,
            0
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

        _engine.getGeneralRegisterSet().getRegister(GRS_X3).setXI(0_000100).setXM(0_03);

        run();

        assertEquals(0_000000_001001L, code[3]);
        assertEquals(0_001004L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_000100, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXI());
        assertEquals(0_000103, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getXM());
    }

    @Test
    public void testSLJ_Indirect() throws MachineInterrupt {
        var code = new long[]{
            sljBM(0, 0, 1, 01001),
            fjaxhiu(0, 0, 0, 0, 0, 0, 01003),
            0,
            0,
            0
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

        assertEquals(0_000000_001001L, code[3]);
        assertEquals(0_001004L, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
