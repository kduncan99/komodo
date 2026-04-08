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

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MLU computes (the content of R2 AND the content of A(a))
 * OR ((the logical negation of the content of R2) AND the developed U field).
 * The result is stored in A(a+1).
 * 
 * NOTE: The implementation of MLUFunction.java:
 * var result = (regR2Value & operand) | (Word36.logicalNot(regR2Value) & regAValue);
 * 
 * This can be seen as:
 * result = (R2 & operand) | (~R2 & A(a))
 * Where A(a) is the source register value and R2 is the mask.
 * Bits in R2 = 1 => choose from operand (U)
 * Bits in R2 = 0 => choose from A(a)
 */
public class TestMLUFunction extends FunctionUnitTest {

    private long mluImm(long a, long u) {
        return fjaxu(0_43, 016, a, 0, u);
    }

    private long mluBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_43, j, a, x, h, i, u);
    }

    private long mluEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_43, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine(this, this);
        _engine.clear();
    }

    @Test
    public void testMLU_BM_Immediate() throws MachineInterrupt {
        var code = new long[]{
            mluImm(0, 0_123456),    // MLU,U     A0,0123456
            0,
            };
        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);

        _engine.getExecOrUserARegister(0).setW(0_123456_654321L);
        _engine.getExecOrUserRRegister(2).setW(0_525252_252525L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        // Rbit -> Ubit, !Rbit -> Abit
        // U    000 000 000 000 000 000 | 001 010 011 100 101 110
        // A    001 010 011 100 101 110 | 110 101 100 011 010 001
        // R    101 010 101 010 101 010 | 010 101 010 101 010 101
        // res  000 000 010 100 000 100 | 100 000 110 110 000 100
        assertEquals(0_002404_406604L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testMLU_EM_Immediate() throws MachineInterrupt {
        var code = new long[]{
            mluImm(0, 0_123456),    // MLU,U     A0,0123456
            0,
            };

        var bank = new ArraySlice(code);
        loadBaseRegister(0, false, 0, 0_1777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getExecOrUserARegister(0).setW(0_123456_654321L);
        _engine.getExecOrUserRRegister(2).setW(0_525252_252525L);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        run();

        // Rbit -> Ubit, !Rbit -> Abit
        // U    000 000 000 000 000 000 | 001 010 011 100 101 110
        // A    001 010 011 100 101 110 | 110 101 100 011 010 001
        // R    101 010 101 010 101 010 | 010 101 010 101 010 101
        // res  000 000 010 100 000 100 | 100 000 110 110 000 100
        assertEquals(0_002404_406604L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testMLU_A15_EM() throws MachineInterrupt {
        var code = new long[] {
            mluEM(JFIELD_W, 15, 0, 0, 0, 2, 0_03000),
            0,
            };
        var data = new long[0_4000];
        data[0_03000] = 0_323232_323232L;

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(0, false, 0, 0_1777, null, bank0);
        loadBaseRegister(2, false, 0, 0_3777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(15).setW(0_123456_654321L);
        _engine.getExecOrUserRRegister(2).setW(0_707070_070707L);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        run();

        assertEquals(0_323436_624222L, _engine.getExecOrUserARegister(16).getW());
    }

    @Test
    public void testMLU_Indexed_EM() throws MachineInterrupt {
        var code = new long[0700];
        code[0] = mluEM(JFIELD_W, 2, 1, 1, 0, 0, 0_500); // MLU A2, [0_500 + X1] = [0_600]
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
        _engine.getExecOrUserRRegister(2).setW(0_777777_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);

        run();

        // Rbit -> Ubit, !Rbit -> Abit
        assertEquals(0_777000_123456L, _engine.getExecOrUserARegister(3).getW());
        assertEquals(0_101, _engine.getExecOrUserXRegister(1).getXM());
    }

    @Test
    public void testMLU_Indirect_BM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = mluBM(JFIELD_W, 4, 0, 0, 1, 0_1010);
        code[010] = 0_000000_001020L;// indirect to 0_1020
        code[020] = 0_000000_000017L;

        var bank = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_2777, null, bank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(4).setW(0_776655_000033L);
        _engine.getExecOrUserRRegister(2).setW(0_707070_707070L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0070605_000013L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testMLU_PartialWord_BM() throws MachineInterrupt {
        var code = new long[]{
            mluBM(JFIELD_S4, 6, 0, 0, 0, 0_500 + 0_2000),
            0,
            };
        var data = new long[0_4000];
        data[0_500] = 0_112233_556677L;

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);
        loadBaseRegister(13, false, 0_2000, 0_5777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);
        _engine.getExecOrUserRRegister(2).setW(0_707070_707070L);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        run();

        assertEquals(0_070707_070757L, _engine.getExecOrUserARegister(7).getW());
    }
}
