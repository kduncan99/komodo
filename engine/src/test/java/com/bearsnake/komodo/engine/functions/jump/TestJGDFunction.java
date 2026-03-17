/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJGDFunction extends TestFunction {

    private long jgd(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(070, j, a, x, h, i, u);
    }

    @BeforeEach
    public void setup() {
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine();
        _engine.clear();
    }

    @Test
    public void testJGD_Jump_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jgd(0, 010, 0, 0, 0, 0_1000), // GRS index 010 (X8)
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
        // BM PC: E=1, L=1 (level 0), BDI=0, offset=0
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup GRS value > 0
        _engine.getGeneralRegister(010).setW(010);

        // Execute JGD
        _engine.cycle(); // First cycle: RESOLVING_ADDRESS
        _engine.cycle(); // Second cycle: Execute

        // Should have jumped to 01000
        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: 010 -> 007
        assertEquals(007, _engine.getGeneralRegister(010).getW());
    }

    @Test
    public void testJGD_NoJump_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jgd(0, 010, 0, 0, 0, 0_1000), // GRS index 010
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

        // Setup GRS value = 0
        _engine.getGeneralRegister(010).setW(0);

        // Execute JGD
        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped, PC should be 1
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: 0 -> 0777777777777L (ones complement -0)
        assertEquals(0777777777777L, _engine.getGeneralRegister(010).getW());
    }

    @Test
    public void testJGD_Negative_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jgd(0, 010, 0, 0, 0, 0_1000), // GRS index 010
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

        // Setup GRS value < 0
        _engine.getGeneralRegister(010).setW(0777777777770L); // -7

        // Execute JGD
        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: -7 -> -8
        assertEquals(0777777777767L, _engine.getGeneralRegister(010).getW());
    }

    @Test
    public void testJGD_Jump_EM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jgd(0, 010, 0, 0, 0, 0_1000), // GRS index 010
            0,
            };

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        // Setup GRS value > 0
        _engine.getGeneralRegister(010).setW(01);

        // Execute JGD
        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should have jumped to 01000
        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented: 1 -> 0
        assertEquals(0, _engine.getGeneralRegister(010).getW());
    }

    @Test
    public void testJGD_GRSViolation() {
        var code = new long[] {
            jgd(2, 0, 0, 0, 0, 0_1000), // GRS index (2 << 4) | 0 = 32 = 040. Restricted.
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
               .setProcessorPrivilege((short)1) // not exec
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        assertThrows(ReferenceViolationInterrupt.class, () -> run());
    }

    @Test
    public void testJGD_Indexed_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[]{
            jgd(0, 010, 3, 0, 0, 0_1000), // GRS index 010, Indexed by X3
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
               .setProcessorPrivilege((short) 0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup GRS value > 0
        _engine.getGeneralRegister(010).setW(1);
        // Setup Index Register X3
        _engine.getExecOrUserXRegister(3).setXM(0_100);

        // Execute JGD
        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should have jumped to 01000 + 0100 = 01100
        assertEquals(0_1100, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented
        assertEquals(0, _engine.getGeneralRegister(010).getW());
    }

    @Test
    public void testJGD_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[]{
            jgd(0, 010, 3, 0, 0, 0_1000), // GRS index 010, Indexed by X3
            0,
        };

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);

        // Setup GRS value > 0
        _engine.getGeneralRegister(010).setW(1);
        // Setup Index Register X3
        _engine.getExecOrUserXRegister(3).setXM(0_200);

        // Execute JGD
        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should have jumped to 01000 + 0200 = 01200
        assertEquals(0_1200, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented
        assertEquals(0, _engine.getGeneralRegister(010).getW());
    }

    @Test
    public void testJGD_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[]{
            jgd(0, 010, 0, 0, 1, 0_1000), // GRS index 010, Indirect bit set, U=01000
            0,
        };

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);
        // Place the target jump address at memory location 01000
        bank0.set(0_1000, 0_1500);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 2) // Privilege > 1 required for BM indirect jump
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup GRS value > 0
        _engine.getGeneralRegister(010).setW(1);

        // Execute JGD
        _engine.cycle(); // RESOLVING_ADDRESS (first part)
        _engine.cycle(); // RESOLVING_ADDRESS (indirect resolution)
        _engine.cycle(); // Execute

        // Should have jumped to 01500 (the value stored at 01000)
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be decremented
        assertEquals(0, _engine.getGeneralRegister(010).getW());
    }
}
