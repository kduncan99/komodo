/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestHLTJFunction extends FunctionUnitTest {

    private long hltjBM(long x, long h, long i, long u) {
        return fjaxhiu(074, 015, 005, x, h, i, u);
    }

    private long hltjEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(074, 015, 005, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testHLTJ_EM() throws MachineInterrupt {
        var code = new long[]{
            hltjBM(2, 0, 0, 01000)
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        run();

        assertEquals(0_001000L, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(HaltCode.HLTJ_INSTRUCTION, _engine.getHaltCode());
    }

    @Test
    public void testHLTJ_BadPP_EM() throws MachineInterrupt {
        var code = new long[]{
            hltjEM(2, 0, 0, 0, 01000)
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        assertThrows(InvalidInstructionInterrupt.class, () -> run());
    }
}
