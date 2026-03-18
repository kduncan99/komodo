/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSAQWFunction extends TestFunction {

    private void cycle() throws MachineInterrupt {
        try {
            _engine.cycle();
        } catch (EngineHaltedException e) {
            // ignore
        }
    }

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
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine();
        _engine.clear();
        _engine.getDesignatorRegister().setBasicModeEnabled(true);
    }

    @Test
    public void testSAQW_BasicMode() throws MachineInterrupt {
        var code = new long[] {
            saqwBM(1, 1, 0, 0, 0_1000), // SAQW A1, 01000, X1
            saqwBM(1, 1, 0, 0, 0_1000), // SAQW A1, 01000, X1
            saqwBM(1, 1, 0, 0, 0_1000), // SAQW A1, 01000, X1
            saqwBM(1, 1, 0, 0, 0_1000), // SAQW A1, 01000, X1
            0,
        };

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // A1 contains 0x1FF (all 9 bits set)
        _engine.getExecOrUserARegister(1).setW(0x1FFL);
        
        // Q1: bits 4,5 of X1 = 0
        _engine.getExecOrUserXRegister(1).setS1(0);
        cycle();

        // Q2: bits 4,5 of X1 = 1
        _engine.getExecOrUserXRegister(1).setS1(1);
        cycle();

        // Q3: bits 4,5 of X1 = 2
        _engine.getExecOrUserXRegister(1).setS1(2);
        cycle();

        // Q4: bits 4,5 of X1 = 3
        _engine.getExecOrUserXRegister(1).setS1(3);
        cycle();

        // In 36-bit word: Q1 Q2 Q3 Q4
        // Each 9 bits. 0x1FF is all 1s.
        // Q1 is bits 27-35: 0x1FF << 27 = 0x1FF_00000000
        // Q2 is bits 18-26: 0x1FF << 18 = 0x000_7FC0000 -> wait
        // 0x1FF << 27 = 0377000000000L
        // 0x1FF << 18 = 0000777000000L
        // 0x1FF << 9  = 0000000777000L
        // 0x1FF       = 0000000000777L
        // Sum         = 0777777777777L
        long expected = 0_777777_777777L;
        assertEquals(expected, bank0.get(0_1000));
    }

    @Test
    public void testSAQW_ExtendedMode() throws MachineInterrupt {
        var code = new long[] {
            saqwEM(4, 2, 0, 0, 2, 0), // SAQW A4, 2, 0, X2 (Bank 2, Offset 0)
            0,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(new long[10]);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
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
        assertEquals(expected, bank2.get(0));
    }

    @Test
    public void testSAQW_Indirect() throws MachineInterrupt {
        var code = new long[] {
            saqwBM(1, 2, 0, 1, 0_1000), // SAQW A1, *01000, X2
            0,
        };

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);
        
        // Pointer at 01000 pointing to 01005
        bank0.set(0_1000, 0_1005L);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
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
        assertEquals(expected, bank0.get(0_1005));
    }
}
