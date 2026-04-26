/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSAQWFunction extends FunctionUnitTest {

    private long saqwBM(long a, long x, long h, long i, long u) {
        return ((0_07L & 077) << 30) | (0_05L << 26) | ((a & 017) << 22) | ((x & 017) << 18)
               | ((h & 01) << 17) | ((i & 01) << 16) | (u & 0177777);
    }

    private long saqwEM(long a, long x, long h, long i, long b, long d) {
        return ((0_07L & 077) << 30) | (0_05L << 26) | ((a & 017) << 22) | ((x & 017) << 18)
               | ((h & 01) << 17) | ((i & 01) << 16) | ((b & 017) << 12) | (d & 07777);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testSAQW_BasicMode() throws MachineInterrupt {
        var code = new long[] {
            saqwBM(1, 1, 0, 0, 0_1000), // SAQW A1, 01000, X1
            saqwBM(2, 2, 0, 0, 0_1000), // SAQW A2, 01000, X1
            saqwBM(3, 3, 0, 0, 0_1000), // SAQW A3, 01000, X1
            saqwBM(4, 4, 0, 0, 0_1000), // SAQW A4, 01000, X1
            0, 0, 0, 0,
        };

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_1000L);

        _engine.getExecOrUserARegister(1).setW(0777);
        _engine.getExecOrUserARegister(2).setW(0333);
        _engine.getExecOrUserARegister(3).setW(0222);
        _engine.getExecOrUserARegister(4).setW(0111);

        _engine.getExecOrUserXRegister(1).setXI(0_000000).setXM(0_000007);
        _engine.getExecOrUserXRegister(2).setXI(0_010000).setXM(0_000007);
        _engine.getExecOrUserXRegister(3).setXI(0_020000).setXM(0_000007);
        _engine.getExecOrUserXRegister(4).setXI(0_030000).setXM(0_000007);

        run();

        long expected = 0_777333_222111L;
        for (var v = 0; v < code.length; v ++) {
            System.out.printf("%04o : %012o\n", v, code[v]);
        }
        assertEquals(expected, code[0_7]);
    }

    @Test
    public void testSAQW_ExtendedMode() throws MachineInterrupt {
        var code = new long[] {
            saqwEM(4, 2, 0, 0, 2, 0), // SAQW A4, 2, 0, X2 (Bank 2, Offset 0)
            0,
        };
        var data = new long[02000];

        loadBaseRegister((short) 0, false, 0_0, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_0, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(4).setW(0_123L); // 0o123
        _engine.getExecOrUserXRegister(2).setS1(2); // Q3

        run();

        // Q3 in 36-bit word is bits 9-17 (0-indexed from right: 0-8, 9-17, 18-26, 27-35)
        // 0123 octal is 1010011 binary.
        // Q3 is bits 9-17.
        // 0123 << 9 = 0000000123000 octal.
        long expected = 0_000000_123_000L;
        assertEquals(expected, data[0]);
    }

    @Test
    public void testSAQW_Indirect() throws MachineInterrupt {
        var code = new long[0_2000];
        code[0] = saqwBM(1, 2, 0, 1, 0_1000); // SAQW A1, *01000, X2
        code[0_1000] =  0_1005L;

        loadBaseRegister((short) 12, false, 0_0, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0_2) // User privilege 2 allows indirect
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_000000_000000L);

        _engine.getExecOrUserARegister(1).setW(0_777L);
        _engine.getExecOrUserXRegister(2).setS1(3); // Q4

        run();

        // The indirect word at 01000 is 01005 (octal).
        // This sets X=0 for the second stage of the instruction.
        // SAQW uses the X-register to select the quarter.
        // X0 has S1=0 by default, so it selects Q1 (bits 27-35).
        // A1=0777.
        // Q1 is 0777 << 27 = 0777_000_000_000.
        long expected = 0_777_000_000_000L;
        assertEquals(expected, code[0_1005]);
    }
}
