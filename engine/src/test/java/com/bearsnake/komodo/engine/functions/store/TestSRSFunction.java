/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt.ErrorType.GRSViolation;
import static org.junit.jupiter.api.Assertions.*;

public class TestSRSFunction extends FunctionUnitTest {

    private long srsBM(long a, long x, long h, long i, long u) {
        return fjaxhibd(072, 016, a, x, h, i, 0, u);
    }

    private long srsEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(072, 016, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine(this, this);
        _engine.clear();
    }

    @Test
    public void testSRS_Simple_BM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = srsBM(1, 0, 0, 0, 0_1005); // store SRS starting at offset 05

        var bank0 = new ArraySlice(code);
        loadBaseRegister(12, false, 0_1000, 0_1777, null, bank0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        // BM PC: E=1, L=1 (level 0), BDI=0, offset=0
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        // Setup GRS values
        _engine.getGeneralRegisterSet().getRegister(0100).setW(0111);
        _engine.getGeneralRegisterSet().getRegister(0101).setW(0222);

        // Setup SRS parameters in A1
        _engine.getExecOrUserARegister(1).setQ1(0);
        _engine.getExecOrUserARegister(1).setQ2(0);
        _engine.getExecOrUserARegister(1).setQ3(2);     // range 1 count
        _engine.getExecOrUserARegister(1).setQ4(0100);  // range 1 first GRS index

        run();

        assertEquals(0111, bank0.get(05));
        assertEquals(0222, bank0.get(06));
    }

    @Test
    public void testSRS_Simple_EM() throws MachineInterrupt {
        var code = new long[02000];
        code[0] = srsEM(1, 0, 0, 0, 2, 0);

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(new long[10]); // buffer for storage

        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);
        loadBaseRegister(2, false, 0_0, 0_1777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        // Setup GRS values (using user registers 0100-0117)
        _engine.getGeneralRegisterSet().getRegister(0100).setW(0100);
        _engine.getGeneralRegisterSet().getRegister(0101).setW(0101);
        _engine.getGeneralRegisterSet().getRegister(0110).setW(0200);
        _engine.getGeneralRegisterSet().getRegister(0111).setW(0201);
        _engine.getGeneralRegisterSet().getRegister(0112).setW(0202);

        // Setup SRS parameters in A1
        _engine.getExecOrUserARegister(1).setQ1(3);     // range 2 count
        _engine.getExecOrUserARegister(1).setQ2(0110);  // range 2 first GRS index
        _engine.getExecOrUserARegister(1).setQ3(2);     // range 1 count
        _engine.getExecOrUserARegister(1).setQ4(0100);  // range 1 first GRS index

        run();

        assertEquals(0100, bank2.get(0));
        assertEquals(0101, bank2.get(1));
        assertEquals(0200, bank2.get(2));
        assertEquals(0201, bank2.get(3));
        assertEquals(0202, bank2.get(4));
    }

    @Test
    public void testSRS_GRSWrap_EM() throws MachineInterrupt {
        var code = new long[] {
            srsEM(1, 0, 0, 0, 2, 0),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(new long[10]);

        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);
        loadBaseRegister(2, false, 0_0, 0_1777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        _engine.getGeneralRegisterSet().getRegister(127).setW(0777);
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X0).setW(01000);

        _engine.getExecOrUserARegister(1).setQ1(0);
        _engine.getExecOrUserARegister(1).setQ2(0);
        _engine.getExecOrUserARegister(1).setQ3(2);
        _engine.getExecOrUserARegister(1).setQ4(127);

        run();

        assertEquals(0777, bank2.get(0));
        assertEquals(01000, bank2.get(1));
    }

    @Test
    public void testSRS_GRSReadViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            srsEM(1, 0, 0, 0, 2, 0),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(new long[10]);

        loadBaseRegister(0, false, 0_1000, 0_1777, null, bank0);
        loadBaseRegister(2, false, 0_0, 0_1777, null, bank2);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3) // User mode
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000);

        _engine.getExecOrUserARegister(1).setQ1(0);
        _engine.getExecOrUserARegister(1).setQ2(0);
        _engine.getExecOrUserARegister(1).setQ3(1);
        _engine.getExecOrUserARegister(1).setQ4(040); // Attempt to read protected register 040

        ReferenceViolationInterrupt mi = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(GRSViolation, mi._errorType);
    }
}
