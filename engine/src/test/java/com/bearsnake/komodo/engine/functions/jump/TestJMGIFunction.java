/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJMGIFunction extends FunctionUnitTest {

    /**
     * Constructs a JMGI instruction word.
     * F=074, J=012 (subfunction code).
     * In Basic Mode, the U field (16 bits) effectively includes the J-field (4 bits)
     * as its upper 4 bits if we use ci.getU() as the base address.
     */
    private long jmgi(long a, long x, long h, long i, long u) {
        return ((074L & 077) << 30) | ((012L & 017) << 26) | ((a & 017) << 22) | ((x & 017) << 18)
               | ((h & 01) << 17) | ((i & 01) << 16) | (u & 0177777);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testJMGI_Jump_BM() throws MachineInterrupt {
        var code = new long[0_2000];
        code[0] = jmgi(8, 0, 0, 0, 0_2000);

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=010 (8 decimal)
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);

        // If Xa.M > 0, Jump to U. Always increment Xa.
        run();

        // Should have jumped to 02000
        assertEquals(0_2000, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }

    @Test
    public void testJMGI_NoJump_BM() throws MachineInterrupt {
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000),
            0,
            };

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=0
        _engine.getGeneralRegisterSet().getRegister(010).setXI(1).setXM(0);

        run();

        // Should NOT have jumped, PC should be 1
        assertEquals(01001, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should STILL be incremented: XM 0 -> 1
        assertEquals(1, _engine.getGeneralRegisterSet().getRegister(010).getXM());
    }

    @Test
    public void testJMGI_Negative_BM() throws MachineInterrupt {
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1010),
            0,
            };

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=-1 (ones complement 0777776)
        _engine.getGeneralRegisterSet().getRegister(010).setXI(1).setXM(0777776);

        run();

        // Should NOT have jumped because -1 <= 0
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM -1 -> 0
        assertEquals(0, _engine.getGeneralRegisterSet().getRegister(010).getXM());
    }

    @Test
    public void testJMGI_Indexed_BM() throws MachineInterrupt {
        // JMGI X8, 01000, X9
        var code = new long[02000];
        code[0] = jmgi(010, 011, 0, 0, 0_1000);

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);
        // Setup X9 (011): XM=0500
        _engine.getGeneralRegisterSet().getRegister(011).setXM(0500);

        run();

        // Should jump to 01000 + 0500 = 01500
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }

    @Test
    public void testJMGI_Indirect_BM() throws MachineInterrupt {
        // JMGI X8, *01000
        var code = new long[0_2000];
        code[0] = jmgi(010, 0, 0, 1, 0_2000);
        code[0_1000] = 0_1500; // indirect address

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);

        run();

        // Should jump to 01500
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }

    @Test
    public void testJMGI_AEqualsX_H0_BM() throws MachineInterrupt {
        // JMGI X8, 01000, X8 (h=0)
        var code = new long[02000];
        code[0] = jmgi(010, 010, 0, 0, 0_1000);

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Target: 01000 + 010 = 01010
        assertEquals(0_1010, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented EXACTLY ONCE: 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }

    @Test
    public void testJMGI_AEqualsX_H1_BM() throws MachineInterrupt {
        // JMGI X8, 01000, *X8 (h=1)
        var code = new long[02500];
        code[0] = jmgi(010, 010, 1, 0, 0_1000);
        code[0_2000] = 0_1500;

        loadBaseRegister((short) 12, false, 0_1000, 0_3477, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS (this will increment X8)
        _engine.cycle(); // Execute (should NOT increment X8 again)

        // Target: 01000 + 010 = 01010
        assertEquals(0_1010, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented EXACTLY ONCE by address resolution: 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }

    @Test
    public void testJMGI_NegativeZero_BM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = jmgi(010, 0, 0, 0, 0_1500);

        loadBaseRegister((short) 12, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_001000L);

        // Setup X8 (010): XI=1, XM=-0 (ones complement 0777777)
        _engine.getGeneralRegisterSet().getRegister(010).setXI(1).setXM(0777777);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped because -0 <= 0
        assertEquals(01001, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM -0 -> 1 (assuming ones-complement add: 0777777 + 1 = 1)
        // Wait, 0777777 + 1 in 18-bit ones complement:
        // 0777777 is -0. -0 + 1 = 1.
        assertEquals(1, _engine.getGeneralRegisterSet().getRegister(010).getXM());
    }

    @Test
    public void testJMGI_Jump_EM() throws MachineInterrupt {
        // JMGI X8, 01000
        var code = new long[02000];
        code[0] = jmgi(010, 0, 0, 0, 0_2000);

        loadBaseRegister((short) 0, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(01000).setBankDescriptorIndex(0).setBankLevel((short)0);

        // Setup X8 (010): XM=010
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should have jumped to 02000
        assertEquals(0_2000, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }

    @Test
    public void testJMGI_NoJump_EM() throws MachineInterrupt {
        // JMGI X8, 01000
        var code = new long[02000];
        code[0] = jmgi(8, 0, 0, 0, 0_1000);

        loadBaseRegister((short) 0, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(01000).setBankDescriptorIndex(0).setBankLevel((short)0);

        // Setup X8 (010): XM=0
        _engine.getGeneralRegisterSet().getRegister(010).setXI(1).setXM(0);

        run();

        // Should NOT have jumped, PC should be 1
        assertEquals(01001, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM 0 -> 1
        assertEquals(1, _engine.getGeneralRegisterSet().getRegister(010).getXM());
    }

    @Test
    public void testJMGI_Indexed_EM() throws MachineInterrupt {
        // JMGI X8, 01000, X9
        var code = new long[02000];
        code[0] = jmgi(010, 011, 0, 0, 0_1000);

        loadBaseRegister((short) 0, false, 0_1000, 0_2777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0).setBankLevel((short)0);

        // Setup X8 (010): XM=010
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).setXI(1).setXM(010);
        // Setup X9 (011): XM=0500
        _engine.getGeneralRegisterSet().getRegister(011).setXM(0500);

        run();

        // Should jump to 01000 + 0500 = 01500
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X8).getXM());
    }
}
