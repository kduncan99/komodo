/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.system;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestIARFunction extends FunctionUnitTest {

    private long iar() {
        return fjaxhibd(073, 017, 006, 0, 0, 0, 0, 0);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testIAR() throws MachineInterrupt {
        var code = new long[] {
            iar()
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

        assertEquals(HaltCode.INITIATE_AUTO_RECOVERY, _engine.getHaltCode());
    }

    @Test
    public void testIAR_BadPP() throws MachineInterrupt {
        var code = new long[] {
            iar()
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

        var i = assertThrows(InvalidInstructionInterrupt.class, this::run);
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(), i.getShortStatusField());
    }
}
