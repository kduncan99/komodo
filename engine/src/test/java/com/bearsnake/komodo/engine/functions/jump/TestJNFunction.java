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

public class TestJNFunction extends TestFunction {

    private long jnBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_03, a, x, h, i, u);
    }

    private long jnBM(long a, long u) {
        return jnBM(a, 0, 0, 0, u);
    }

    private long jnEM(long a, long x, long u) {
        return fjaxu(0_74, 0_03, a, x, u);
    }

    private long jnEM(long a, long u) {
        return jnEM(a, 0, u);
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
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
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
                                     .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bank = new ArraySlice(new long[0_2000]);
        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testJN_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnBM(5, 0_100)); // JN if A5 is negative, jump to 0_100

        // Case 1: X5 is negative -> Should jump
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue());

        // Case 2: X5 is negative zero -> Should jump
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(Word36.NEGATIVE_ZERO);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue());

        // Case 3: X5 is positive -> Should NOT jump
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_377777_777777L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000001L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJN_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jnEM(5, 0_100)); // JN if A5 is negative, jump to 0_100

        // Case 1: X5 is negative -> Should jump
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 2: X5 is positive
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_Indexed_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).setXM(0_10);
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnBM(5, 3, 0, 0, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000110L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJN_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnBM(5, 0, 0, 1, 0_100)); // jump indirect via 0_100
        bank.set(0_100, fjaxu(0_74, 0_04, 0, 0, 0_200)); // second stage: J to 0_200
        bank.set(0_200, 0); // halt

        _engine.getProgramAddressRegister().setProgramCounter(0);
        run();
        assertEquals(0_200, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(3)).setXM(0_10);
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jnEM(5, 3, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_110, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJN_GRS_X() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnBM(3, 0_100)); // JN if GRS[3] (X3) is negative, jump to 0_100

        // Case 1: X3 is negative -> Should jump
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A3).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJN_GRS_A() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnBM(14, 0_100)); // JN if GRS[014] (A0) is negative, jump to 0_100

        // Case 1: A0 is negative -> Should jump
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A14).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue());
    }

}
