/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJGDFunction extends FunctionUnitTest {

    private long jgd(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(070, j, a, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJGD_Jump_BM() throws MachineInterrupt {
        var code = new long[] {
            jgd(0, 8, 0, 0, 0, 0_1000), // GRS index 8 (X8)
            0,
            };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup GRS value > 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setW(010);

        // Execute JGD
        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: 010 -> 007
        assertEquals(0_777777_777777L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getW());
    }

    @Test
    public void testJGD_Negative_BM() throws MachineInterrupt {
        var code = new long[] {
            jgd(0, 8, 0, 0, 0, 0_1000), // GRS index 8
            0,
            };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup GRS value < 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setW(0777777777770L); // -7

        // Execute JGD
        run();

        // Should NOT have jumped
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: -7 -> -8
        assertEquals(0777777777767L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getW());
    }

    @Test
    public void testJGD_Jump_EM() throws MachineInterrupt {
        var code = new long[] {
            jgd(0, 8, 0, 0, 0, 0_1010), // GRS index 8
            0, 0, 0, 0, 0, 0, 0, 0,
            };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0).setBankLevel((short)0);

        // Setup GRS value > 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setW(01);

        // Execute JGD
        run();

        // Should have jumped to 01000
        assertEquals(0_1010, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: 1 -> 0
        assertEquals(0, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getW());
    }

    @Test
    public void testJGD_GRSViolation() {
        var code = new long[] {
            jgd(2, 0, 0, 0, 0, 0_1000), // GRS index (2 << 4) | 0 = 32 = 040. Restricted.
            0,
            };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)1) // not exec
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        assertThrows(ReferenceViolationInterrupt.class, () -> run());
    }

    @Test
    public void testJGD_Indexed_BM() throws MachineInterrupt {
        var code = new long[01000];
        code[0] = jgd(0, 8, 3, 0, 0, 0_1000); // GRS index 8, Indexed by X3

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup GRS value > 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setW(1);
        // Setup Index Register X3
        _engine.getExecOrUserXRegister(3).setXM(0_100);

        // Execute JGD
        run();

        // Should have jumped to 01000 + 0100 = 01100
        assertEquals(0_1100, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented
        assertEquals(0, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getW());
    }

    @Test
    public void testJGD_Indexed_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = jgd(0, 8, 3, 0, 0, 0_1000); // GRS index 8, Indexed by X3

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0).setBankLevel((short) 0);

        // Setup GRS value > 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setW(1);
        // Setup Index Register X3
        _engine.getExecOrUserXRegister(3).setXM(0_200);

        // Execute JGD
        run();

        // Should have jumped to 01000 + 0200 = 01200
        assertEquals(0_1200, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented
        assertEquals(0, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getW());
    }

    @Test
    public void testJGD_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_3000];
        code[0] = jgd(0, 8, 0, 0, 1, 0_3000); // GRS index 8, Indirect bit set, U=03000
        code[0_2000] = 0_1500;

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_3777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 2) // Privilege > 1 required for BM indirect jump
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup GRS value > 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setW(1);

        // Execute JGD
        run();

        // Should have jumped to 01500 (the value stored at 01000)
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented
        assertEquals(0, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getW());
    }
}
