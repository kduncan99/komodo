/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDJZFunction extends FunctionUnitTest {

    private long djzBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_71, 0_16, a, x, h, i, u);
    }

    private long djzBM(long a, long u) {
        return djzBM(a, 0, 0, 0, u);
    }

    private long djzEM(long a, long x, long u) {
        return fjaxu(0_71, 0_16, a, x, u);
    }

    private long djzEM(long a, long u) {
        return djzEM(a, 0, u);
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
    public void testDJZ_GRS_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, djzBM(5, 0_100)); // DJZ using A5,A6. Jump to 0_100

        // Case 1: A5,A6 are positive zero -> Should jump
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 2: A5,A6 are negative zero -> Should jump
        _engine.getExecOrUserARegister(5).setW(Word36.NEGATIVE_ZERO);
        _engine.getExecOrUserARegister(6).setW(Word36.NEGATIVE_ZERO);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 3: A5 is non-zero -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(1);
        _engine.getExecOrUserARegister(6).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 4: A6 is non-zero -> Should NOT jump
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(1);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());

        // Case 5: A0, A1 are zero -> Should jump
        bank.set(1, djzBM(0, 0_200));
        _engine.getExecOrUserARegister(0).setW(0);
        _engine.getExecOrUserARegister(1).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(1);
        _engine.cycle();
        assertEquals(0_200, _engine.getProgramAddressRegister().getProgramCounter());
    }


    @Test
    public void testDJZ_Indexed_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, djzBM(5, 3, 0, 0, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_110, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDJZ_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, djzBM(5, 0, 0, 1, 0_100)); // jump indirect via 0_100
        bank.set(0_100, fjaxu(0_74, 0_04, 0, 0, 0_200)); // second stage: J to 0_200
        bank.set(0_200, 0); // halt

        _engine.getProgramAddressRegister().setProgramCounter(0);
        run();
        assertEquals(0_200, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDJZ_GRS_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, djzEM(5, 0_100)); // DJZ if A5,A6 are zero, jump to 0_100

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter());

        // Case: A0, A1 are zero -> Should jump
        bank.set(1, djzEM(0, 0_200));
        _engine.getExecOrUserARegister(0).setW(0);
        _engine.getExecOrUserARegister(1).setW(0);
        _engine.getProgramAddressRegister().setProgramCounter(1);
        _engine.cycle();
        assertEquals(0_200, _engine.getProgramAddressRegister().getProgramCounter());

        // Non-zero
        _engine.getExecOrUserARegister(5).setW(1);
        bank.set(2, djzEM(5, 0_300));
        _engine.getProgramAddressRegister().setProgramCounter(2);
        _engine.cycle();
        assertEquals(3, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testDJZ_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        _engine.getExecOrUserXRegister(3).setXM(0_10);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, djzEM(5, 3, 0_100)); // jump to 0_100 + X3.m (0_10) = 0_110

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();
        assertEquals(0_110, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
