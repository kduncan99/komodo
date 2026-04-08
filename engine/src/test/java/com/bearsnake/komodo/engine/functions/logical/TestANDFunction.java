/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.logical;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.JFIELD_S2;
import static com.bearsnake.komodo.engine.Constants.JFIELD_W;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestANDFunction extends FunctionUnitTest {

    private long andImm(long a, long u) {
        return fjaxu(0_42, 016, a, 0, u);
    }

    private long andBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_42, j, a, x, h, i, u);
    }

    private long andEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_42, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine =  new Engine(this, this);
        _engine.clear();
    }

    @Test
    public void testAND_BM_Immediate() throws MachineInterrupt {
        var code = new long[]{
            andImm(0, 0_123456),    // AND,U     A0,0123456
            0,
            };

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        _engine.getExecOrUserARegister(0).setW(0_123456_654377L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_000000_000056L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testAND_EM_Immediate() throws MachineInterrupt {
        var code = new long[]{
            andImm(0, 0_123456),    // AND,U     A0,0123456
            0,
            };

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0, 0_1777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(0).setW(0_123456_654367L);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        run();

        assertEquals(0_000000_000046L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testAND_A15_EM() throws MachineInterrupt {
        var code = new long[] {
            andEM(JFIELD_W, 15, 0, 0, 0, 2, 0_03000),
            0,
            };
        var data = new long[0_4000];
        data[0_03000] = 0_000777_777000L;

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0, 0_1777, null, bank0);
        loadBaseRegister(2, false, 0, 0_3777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(15).setW(0_123456_654321L);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        run();

        assertEquals(0_000456_654000L, _engine.getExecOrUserARegister(16).getW());
    }

    @Test
    public void testAND_Indexed_EM() throws MachineInterrupt {
        var code = new long[0700];
        code[0] = andEM(JFIELD_W, 2, 1, 1, 0, 0, 0_500); // AND A2, [0_500 + X1] = [0_600]
        code[1] = 0; // stop
        code[0600] = 0_777000_000777L;

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0, 0_1777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserXRegister(1).setXM(0_100).setXI(1);
        _engine.getExecOrUserARegister(2).setW(0_123456_123456L);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        run();

        assertEquals(0_123000_000456L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0_101, _engine.getExecOrUserXRegister(1).getXM());
    }

    @Test
    public void testAND_Indirect_BM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = andBM(JFIELD_W, 4, 0, 0, 1, 0_1010);
        code[010] = 0_000000_001020L;// indirect to 0_1020
        code[020] = 0_000000_000017L;

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_2777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(4).setW(0_000000_000033L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_000000_000013L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testAND_PartialWord_BM() throws MachineInterrupt {
        var code = new long[]{
            andBM(JFIELD_S2, 6, 0, 0, 0, 0_500 + 0_2000),
            0,
            };
        var data = new long[0_4000];
        data[0_500] = 0_012300_777777L;

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);
        loadBaseRegister(13, false, 0_2000, 0_5777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(6).setW(0_343434_343434L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_000000_000020L, _engine.getExecOrUserARegister(7).getW());
    }
}
