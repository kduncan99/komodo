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

public class TestJCFunction extends TestFunction {

    private long jcBM(long x, long h, long i, long u) {
        return fjaxhiu(0_74, 0_16, 0, x, h, i, u);
    }

    private long jcBM(long u) {
        return jcBM(0, 0, 0, u);
    }

    private long jcEM(long x, long u) {
        return fjaxu(0_74, 0_14, 0_04, x, u);
    }

    private long jcEM(long u) {
        return jcEM(0, u);
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
    public void testJC_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jcBM(0_100));

        // Case 1: Carry set -> Should jump
        _engine.getDesignatorRegister().setCarry(true);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue());

        // Case 2: Carry clear -> Should NOT jump
        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000001L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJC_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jcEM(0_100));

        // Case 1: Carry set -> Should jump
        _engine.getDesignatorRegister().setCarry(true);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 2: Carry clear -> Should NOT jump
        _engine.getDesignatorRegister().setCarry(false);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }
    @Test
    public void testJC_Indexed_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getExecOrUserXRegister(3).setXM(0_10);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jcBM(3, 0, 0, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getDesignatorRegister().setCarry(true);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_440000_000110L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    @Test
    public void testJC_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jcBM(0, 0, 1, 0_100)); // jump indirect via 0_100
        bank.set(0_100, fjaxu(0_74, 0_04, 0, 0, 0_200)); // second stage: J to 0_200
        bank.set(0_200, 0); // halt

        _engine.getDesignatorRegister().setCarry(true);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        run();
        assertEquals(0_200, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testJC_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        _engine.getExecOrUserXRegister(3).setXM(0_10);
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jcEM(3, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getDesignatorRegister().setCarry(true);
        _engine.cycle();
        assertEquals(0_110, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
