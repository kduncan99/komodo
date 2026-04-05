/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJPSFunction extends TestFunction {

    private long jpsBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_72, 0_02, a, x, h, i, u);
    }

    private long jpsBM(long a, long u) {
        return jpsBM(a, 0, 0, 0, u);
    }

    private long jpsEM(long a, long x, long u) {
        return fjaxu(0_72, 0_02, a, x, u);
    }

    private long jpsEM(long a, long u) {
        return jpsEM(a, 0, u);
    }

    @BeforeEach
    public void setup() {
        com.bearsnake.komodo.engine.functions.FunctionTable.clear();
        _engine = new Engine();
        _engine.clear();
    }

    private void setupBM() {
        var bd = new BankDescriptor().setBankType(BankType.BasicMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        var bank = new ArraySlice(new long[0_2000]);
        _engine.getBaseRegister(12).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().fromComposite(0_440000_000000L);
    }

    private void setupEM() {
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0)
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));
        var bank = new ArraySlice(new long[0_2000]);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testJPS_Jump_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jpsBM(5, 0_100)); // JPS if A5 is positive, jump to 0_100

        // A5 = 0_000000_000001L (Positive)
        // Shift left circular by 1: 0_000000_000002L
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_000000_000001L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue(), "Should jump");
        assertEquals(0_000000_000002L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJPS_NoJump_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jpsBM(5, 0_100)); // JPS if A5 is positive, jump to 0_100

        // A5 = 0_400000_000000L (Negative)
        // Shift left circular by 1: 0_000000_000001L
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_440000_000001L, _engine.getProgramAddressRegister().getCompositeValue(), "Should not jump");
        assertEquals(0_000000_000001L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJPS_PositiveZero_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jpsBM(5, 0_100)); // JPS if A5 is positive, jump to 0_100

        // A5 = 0 (Positive Zero)
        // Shift left circular by 1: 0
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue(), "Should jump");
        assertEquals(0, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJPS_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jpsEM(5, 0_100)); // JPS if A5 is positive, jump to 0_100

        // A5 = 0_200000_000000L (Positive)
        // Shift left circular by 1: 0_400000_000000L (Negative after shift)
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_200000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter(), "Should jump");
        assertEquals(0_400000_000000L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }
}
