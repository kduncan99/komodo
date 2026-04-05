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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJBFunction extends TestFunction {

    private long jbBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_11, a, x, h, i, u);
    }

    private long jbBM(long a, long u) {
        return jbBM(a, 0, 0, 0, u);
    }

    private long jbEM(long a, long x, long u) {
        return fjaxu(0_74, 0_11, a, x, u);
    }

    private long jbEM(long a, long u) {
        return jbEM(a, 0, u);
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
    public void testJB_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jbBM(5, 0_100)); // JB if A5 low bit is set, jump to 0_100

        // Case 1: A5 bit 0 is set -> Should jump
        _engine.getExecOrUserARegister(5).setW(01);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue());

        // Case 2: A5 bit 0 is clear -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(02);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000001L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJB_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jbEM(5, 0_100)); // JB if A5 low bit is set, jump to 0_100

        // Case 1: A5 bit 0 is set -> Should jump
        _engine.getExecOrUserARegister(5).setW(0_777777_777771L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 2: A5 bit 0 is clear -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(0_777777_777776L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_Indexed_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getExecOrUserARegister(5).setW(01);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jbBM(5, 3, 0, 0, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000110L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJB_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getExecOrUserARegister(5).setW(01);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jbBM(5, 0, 0, 1, 0_100)); // jump indirect via 0_100
        bank.set(0_100, fjaxu(0_74, 0_04, 0, 0, 0_200)); // second stage: J to 0_200
        bank.set(0_200, 0); // halt

        _engine.getProgramAddressRegister().setProgramCounter(0);
        run();
        assertEquals(0_200, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJB_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getExecOrUserARegister(5).setW(01);
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jbEM(5, 3, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_110, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
