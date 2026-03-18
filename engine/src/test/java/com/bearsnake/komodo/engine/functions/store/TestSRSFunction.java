/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt.ErrorType.GRSViolation;
import static org.junit.jupiter.api.Assertions.*;

public class TestSRSFunction extends TestFunction {

    private long srsBM(long a, long x, long h, long i, long u) {
        return fjaxhibd(072, 016, a, x, h, i, 0, u);
    }

    private long srsEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(072, 016, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine();
        _engine.clear();
    }

    @Test
    public void testSRS_Simple_BM() throws MachineInterrupt {
        var code = new long[] {
            srsBM(1, 0, 0, 0, 0_1000), // store SRS starting at offset 01000 relative to B0
            0,
            };

        var bank0 = new ArraySlice(new long[0_2000]);
        bank0.load(code, 0, code.length, 0);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777) // 1024 words
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        // BM PC: E=1, L=1 (level 0), BDI=0, offset=0
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);

        // Setup GRS values
        _engine.getGeneralRegisterSet().getRegister(0100).setW(0111);
        _engine.getGeneralRegisterSet().getRegister(0101).setW(0222);

        // Setup SRS parameters in A1
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ1(0);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ2(0);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ3(2);     // range 1 count
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ4(0100);  // range 1 first GRS index

        run();

        assertEquals(0111, bank0.get(0_1000));
        assertEquals(0222, bank0.get(0_1001));
    }

    @Test
    public void testSRS_Simple_EM() throws MachineInterrupt {
        var code = new long[] {
            srsEM(1, 0, 0, 0, 2, 0),// store SRS starting at bank 2, offset 0
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(new long[10]); // buffer for storage

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

        // Setup GRS values (using user registers 0100-0117)
        _engine.getGeneralRegisterSet().getRegister(0100).setW(0100);
        _engine.getGeneralRegisterSet().getRegister(0101).setW(0101);
        _engine.getGeneralRegisterSet().getRegister(0110).setW(0200);
        _engine.getGeneralRegisterSet().getRegister(0111).setW(0201);
        _engine.getGeneralRegisterSet().getRegister(0112).setW(0202);

        // Setup SRS parameters in A1
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ1(3);     // range 2 count
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ2(0110);  // range 2 first GRS index
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ3(2);     // range 1 count
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ4(0100);  // range 1 first GRS index

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

        _engine.getGeneralRegisterSet().getRegister(127).setW(0777);
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X0).setW(01000);

        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ1(0);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ2(0);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ3(2);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ4(127);

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
               .setProcessorPrivilege((short)3) // User mode
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ1(0);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ2(0);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ3(1);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setQ4(040); // Attempt to read protected register 040

        ReferenceViolationInterrupt mi = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(GRSViolation, mi._errorType);
    }
}
