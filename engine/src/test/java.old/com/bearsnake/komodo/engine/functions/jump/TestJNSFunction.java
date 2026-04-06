/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJNSFunction extends FunctionUnitTest {

    private long jnsBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_72, 0_03, a, x, h, i, u);
    }

    private long jnsBM(long a, long u) {
        return jnsBM(a, 0, 0, 0, u);
    }

    private long jnsEM(long a, long x, long u) {
        return fjaxu(0_72, 0_03, a, x, u);
    }

    private long jnsEM(long a, long u) {
        return jnsEM(a, 0, u);
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
    public void testJNS_Jump_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnsBM(5, 0_100)); // JNS if A5 is negative, jump to 0_100

        // A5 = 0_400000_000000L (Negative)
        // Shift left circular by 1: 0_000000_000001L
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_400000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue(), "Should jump");
        assertEquals(0_000000_000001L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJNS_NoJump_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnsBM(5, 0_100)); // JNS if A5 is negative, jump to 0_100

        // A5 = 0_000000_000001L (Positive)
        // Shift left circular by 1: 0_000000_000002L
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_000000_000001L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_440000_000001L, _engine.getProgramAddressRegister().getCompositeValue(), "Should not jump");
        assertEquals(0_000000_000002L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJNS_NegativeZero_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, jnsBM(5, 0_100)); // JNS if A5 is negative, jump to 0_100

        // A5 = Word36.NEGATIVE_ZERO (0_777777_777777L)
        // Shift left circular by 1: 0_777777_777777L
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(Word36.NEGATIVE_ZERO);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_440000_000100L, _engine.getProgramAddressRegister().getCompositeValue(), "Should jump");
        assertEquals(Word36.NEGATIVE_ZERO, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }

    @Test
    public void testJNS_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, jnsEM(5, 0_100)); // JNS if A5 is negative, jump to 0_100

        // A5 = 0_600000_000000L (Negative)
        // Shift left circular by 1: 0_400000_000001L
        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).setW(0_600000_000000L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle();

        assertEquals(0_100, _engine.getProgramAddressRegister().getProgramCounter(), "Should jump");
        assertEquals(0_400000_000001L, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_A5).getW(), "Should shift");
    }
}
