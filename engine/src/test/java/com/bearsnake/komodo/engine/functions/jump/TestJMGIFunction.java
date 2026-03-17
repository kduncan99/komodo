/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJMGIFunction extends TestFunction {

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
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine();
        _engine.clear();
    }

    @Test
    public void testJMGI_Jump_BM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000
        // J-field 012 is encoded in the instruction. In BM, the jump base is ci.getU().
        // ci.getU() for J=012, U=01000 is 0101000? No, (012 << 12) | 01000 = 0121000?
        // Let's check: J is bits 29-26. U is bits 15-0.
        // Bit 29 is not in getU(). bits 28, 27, 26 are in getU()?
        // getU() is bits 15-0. J is bits 29-26. No overlap.
        // Wait, J is 012 = 1010 binary. bits are 29, 28, 27, 26.
        // My previous run showed Expected 520 (01010), Actual 522 (01012).
        // 522 - 512 = 10 (decimal) = 012 (octal).
        // If U was 01000 (512), and it jumped to 522, it added 012.
        // Where did 012 come from? It's the J-field!
        // getJumpOperand() in Engine.java:
        // operand = ci.getU(); ... return operand & 0777777;
        // InstructionWord.getU(): return (int)(_value & 0_000000_177777L);
        // bits 15-0.
        // InstructionWord.compose(): _value |= ((long)j & 0_17) << 26;
        // J bits are 29, 28, 27, 26. They are NOT in getU().
        // Wait, why did it jump to 522?
        // Ah! In testJMGI_AEqualsX_H0_BM, I had:
        // jmgi(010, 010, 0, 0, 0_1000)
        // a=010, x=010.
        // getJumpOperand():
        // operand = ci.getU() = 01000.
        // xReg = getExecOrUserXRegister(010). XM=10.
        // operand = addSimple(01000, 10) = 01010.
        // Wait, 01010 is 520 decimal.
        // The error said Actual 522.
        // 522 - 520 = 2.
        // Where does 2 come from?
        // J is 012. 012 & 03 is 2.
        // InstructionWord.getU() mask is 0177777.
        // 0177777 is bits 15-0.
        // Wait, bits 17 and 16 are H and I.
        // bit 15 is ...?
        // bits 17-0 are H, I, U.
        // bit 17: H
        // bit 16: I
        // bits 15-0: U.
        // My jmgi helper: ((012L & 017) << 26). bits 29-26.
        // There is NO WAY J bits are in U.
        // Let me re-read the failure: "Expected :520 Actual :522"
        // Actual 522 is 01012.
        // 01012 = 01000 + 012.
        // It seems it added the J-field (012) instead of the X-modifier (10)?
        // No, X-modifier was 10. 01000 + 10 = 01010.
        // If it jumped to 01012, it added 012.
        // Wait! ci.getX() was 010.
        // getExecOrUserXRegister(010).
        // Register 010 (X8) is GRS index 010.
        // In my test: _engine.getGeneralRegister(010).setXI(1).setXM(10);
        // XM is 10.
        // If it added 012, it must have read register 010 and got 012? No.
        // WAIT. GRS_X10 is 012.
        // If ci.getX() was 012, it would read X10.
        // But ci.getX() was 010 (X8).
        // I am confused. Let me just run a simple test and look at the debug output if possible.
        // Actually, the failure message: "--> JMGI      X8,01000,X8    X8=000001:000012"
        // LOOK! X8=000001:000012.
        // XM is 012!
        // Why is XM 012? I set it to 10.
        // 10 is decimal! 010 is 8.
        // I used `setXM(10)`. That is 10 decimal, which is 012 octal!
        // HA! Found it. `10` vs `010`.

        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000), // X8, U=01000
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=010 (8 decimal)
        _engine.getGeneralRegister(010).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should have jumped to 01000
        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_NoJump_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000),
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=0
        _engine.getGeneralRegister(010).setXI(1).setXM(0);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped, PC should be 1
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should STILL be incremented: XM 0 -> 1
        assertEquals(1, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_Negative_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000),
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=-1 (ones complement 0777776)
        _engine.getGeneralRegister(010).setXI(1).setXM(0777776);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped because -1 <= 0
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM -1 -> 0
        assertEquals(0, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_Indexed_BM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000, X9
        var code = new long[] {
            jmgi(010, 011, 0, 0, 0_1000),
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);
        // Setup X9 (011): XM=0500
        _engine.getGeneralRegister(011).setXM(0500);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should jump to 01000 + 0500 = 01500
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, *01000
        var code = new long[0_2000];
        code[0] = jmgi(010, 0, 0, 1, 0_1000);
        code[0_1000] = 0_1500; // indirect address
        code[1] = 0;

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS (Start)
        _engine.cycle(); // RESOLVING_ADDRESS (Indirect)
        _engine.cycle(); // Execute

        // Should jump to 01500
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_AEqualsX_H0_BM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000, X8 (h=0)
        var code = new long[] {
            jmgi(010, 010, 0, 0, 0_1000),
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Target: 01000 + 010 = 01010
        assertEquals(0_1010, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented EXACTLY ONCE: 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_AEqualsX_H1_BM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000, *X8 (h=1)
        var code = new long[] {
            jmgi(010, 010, 1, 0, 0_1000),
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS (this will increment X8)
        _engine.cycle(); // Execute (should NOT increment X8 again)

        // Target: 01000 + 010 = 01010
        assertEquals(0_1010, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented EXACTLY ONCE by address resolution: 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_NegativeZero_BM() throws MachineInterrupt, EngineHaltedException {
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000),
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup X8 (010): XI=1, XM=-0 (ones complement 0777777)
        _engine.getGeneralRegister(010).setXI(1).setXM(0777777);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped because -0 <= 0
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM -0 -> 1 (assuming ones-complement add: 0777777 + 1 = 1)
        // Wait, 0777777 + 1 in 18-bit ones complement:
        // 0777777 is -0. -0 + 1 = 1.
        assertEquals(1, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_Jump_EM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000),
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

        // Setup X8 (010): XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should have jumped to 01000
        assertEquals(0_1000, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_NoJump_EM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000
        var code = new long[] {
            jmgi(010, 0, 0, 0, 0_1000),
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

        // Setup X8 (010): XM=0
        _engine.getGeneralRegister(010).setXI(1).setXM(0);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should NOT have jumped, PC should be 1
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Register should be incremented: XM 0 -> 1
        assertEquals(1, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, 01000, X9
        var code = new long[] {
            jmgi(010, 011, 0, 0, 0_1000),
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

        // Setup X8 (010): XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);
        // Setup X9 (011): XM=0500
        _engine.getGeneralRegister(011).setXM(0500);

        _engine.cycle(); // RESOLVING_ADDRESS
        _engine.cycle(); // Execute

        // Should jump to 01000 + 0500 = 01500
        assertEquals(0_1500, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }

    @Test
    public void testJMGI_Indirect_EM() throws MachineInterrupt, EngineHaltedException {
        // JMGI X8, *01000
        var code = new long[0_2000];
        code[0] = jmgi(010, 0, 0, 1, 0_1000);
        code[0_1000] = 0_1500; // indirect address
        code[1] = 0;

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

        // Setup X8 (010): XI=1, XM=010
        _engine.getGeneralRegister(010).setXI(1).setXM(010);

        _engine.cycle(); // RESOLVING_ADDRESS (Indirect addressing does not happen in EM for JMGI via getJumpOperand?)
        // Wait, Engine.getJumpOperand says:
        // if (dr.isBasicModeEnabled() && (ci.getI() != 0) && (dr.getProcessorPrivilege() > 1)) { ... indirect ... }
        // So EM does NOT do indirect in getJumpOperand!
        // This is consistent with many Univac instructions where I bit is part of address resolution
        // but not necessarily "indirect" in the same way as BM.

        _engine.cycle(); // Execute

        // Target: 01000 + 0100000 (I-bit is 0200000 bit) = 0201000
        assertEquals(0201000, _engine.getProgramAddressRegister().getProgramCounter());
        // X8 should be incremented: 010 -> 011
        assertEquals(011, _engine.getGeneralRegister(010).getXM());
    }
}
