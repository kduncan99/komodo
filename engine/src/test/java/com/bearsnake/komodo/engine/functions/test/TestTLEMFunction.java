/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTLEMFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    private long tlemBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(047, j, a, x, h, i, u);
    }

    private long tlemEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(047, j, a, x, h, i, b, d);
    }

    @Test
    public void testTLEM_BasicMode() throws MachineInterrupt {
        // TLEM, J=W, A=1 (X1), X=0, H=0, I=0, U=01000
        // X1: XI=0, XM=02000
        // U=01000 <= XM=02000 -> Skip
        var code = new long[1024];
        code[0] = tlemBM(Constants.JFIELD_W, 1, 0, 0, 0, 01000);

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);

        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X1).setXI(0).setXM(02000);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short) 3);
        _engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        // Skip NI: PC should be 2 (instruction at 0, next instruction at 1 skipped)
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 02000 + 0 = 02000
        assertEquals(02000, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X1).getXM());
    }

    @Test
    public void testTLEM_ExtendedMode_NoSkip_EM() throws MachineInterrupt {
        // TLEM, J=W, A=1, X=0, H=0, I=0, B=2, D=0
        // Bank 2 Offset 0: 03000
        // X1: XI=0, XM=02000
        // U=03000 > XM=02000 -> No skip
        var code = new long[] {
            tlemEM(Constants.JFIELD_W, 1, 0, 0, 0, 2, 0),
            0,
        };
        var data = new long[]{
            03000,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_0777, new AbsoluteAddress(0, 0), bank2);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(0).setXM(02000);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // No skip: PC should be 1
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 02000 + 0 = 02000
        assertEquals(02000, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Indexing() throws MachineInterrupt {
        // TLEM, J=W, A=1, X=2, H=0, I=0, B=2, D=0
        // X2: XM=5
        // Bank 2 Offset 5: 01500
        // X1: XI=10, XM=02000
        // U=01500 <= XM=02000 -> Skip
        var code = new long[]{
            tlemEM(Constants.JFIELD_W, 1, 2, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{
            0, 0, 0, 0, 0, 01500, 0, 0,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_0777, new AbsoluteAddress(0, 0), bank2);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(10).setXM(02000);
        _engine.getGeneralRegisterSet().getRegister(2).setXM(5);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // Skip NI: PC should be 2
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 02000 + 10 (decimal 8) = 02012 (octal)
        assertEquals(02012, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Indirect_BM() throws MachineInterrupt {
        var code = new long[]{
            tlemBM(Constants.JFIELD_W, 1, 0, 0, 1, 0_20000),
            0,
            0,
            };

        var indirectData = new long[] {
            fjaxhiu(0, 0, 0, 1, 0, 0, 0),
            0, 0, 0, 0, 0, 0, 0,
        };

        var targetData = new long[] {
            0, 0, 0, 0, 0, 0_3000, 0, 0,
        };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(indirectData);
        var bank3 = new ArraySlice(targetData);

        loadBaseRegister(12, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(13, false, 0_20000, 0_20777, new AbsoluteAddress(3, 0), bank2);
        loadBaseRegister(14, false, 0_30000, 0_30777, new AbsoluteAddress(5, 0), bank3);

        _engine.getExecOrUserXRegister(1).setXI(0).setXM(05);
        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // Skip NI: PC should be 2
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(05, _engine.getExecOrUserXRegister(1).getXM());
    }

    @Test
    public void testTLEM_Immediate_EM() throws MachineInterrupt {
        // TLEM, J=XU (Immediate), A=1, U=03000
        // X1: XI=5, XM=03000
        // U=03000 <= XM=03000 -> Skip
        var code = new long[]{
            fjaxhiu(047, Constants.JFIELD_XU, 1, 0, 0, 0, 03000),
            0,
            0,
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(5).setXM(03000);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // Skip NI: PC should be 2
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 03000 + 5 = 03005
        assertEquals(03005, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Canonical_Case1_EM() throws MachineInterrupt {
        var code = new long[] {
            tlemEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0_000135_471234L };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_0777, new AbsoluteAddress(0, 0), bank2);

        _engine.getGeneralRegisterSet().getRegister(5).setW(0_000002_061234L);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        // No skip: PC should be 1
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_000002_061236L, _engine.getGeneralRegisterSet().getRegister(5).getW());
    }

    @Test
    public void testTNGM_Canonical_Case2_EM() throws MachineInterrupt {
        var code = new long[] {
            tlemEM(Constants.JFIELD_S5, 5, 0, 0, 0, 2, 0),
            0,
            0,
        };

        var data = new long[]{ 0_000135_471234L };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0_1000, 0_1777, new AbsoluteAddress(0, 0), bank0);
        loadBaseRegister(2, false, 0_0, 0_0777, new AbsoluteAddress(0, 0), bank2);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);
        _engine.getGeneralRegisterSet().getRegister(5).setW(0_000002_061236L);

        run();

        // Skip NI: PC should be 2
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X5 after: 000002 061240
        assertEquals(0_000002_061240L, _engine.getGeneralRegisterSet().getRegister(5).getW());
    }
}
