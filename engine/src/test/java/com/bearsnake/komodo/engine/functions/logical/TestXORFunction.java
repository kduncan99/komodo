/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.logical;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestXORFunction extends TestFunction {

    private long xorBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_41, 0, a, x, h, i, u);
    }

    private long xorEM(long a, long x, long u) {
        return fjaxu(0_41, 0, a, x, u);
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
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
    }

    @Test
    public void testXOR_BM_Immediate() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        // XOR A0, immediate 0_123456
        bank.set(0, fjaxhiu(0_41, 016, 0, 0, 0, 0, 0_123456));
        
        _engine.getExecOrUserARegister(0).setW(0_123456_654321L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute
        
        assertEquals(0_123456_777777L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testXOR_EM_Immediate() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        // XOR A0, immediate 0_123456
        bank.set(0, fjaxu(0_41, 016, 0, 0, 0_123456));
        
        _engine.getExecOrUserARegister(0).setW(0_123456_654321L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute
        
        assertEquals(0_123456_777777L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testXOR_A15() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, fjaxu(0_41, 016, 017, 0, 0_123456)); // XOR A15, immediate 0_123456
        
        _engine.getExecOrUserARegister(017).setW(0_123456_654321L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute
        
        assertEquals(0_123456_777777L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testXOR_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        _engine.getExecOrUserXRegister(1).setXM(0_100);
        bank.set(0, xorEM(2, 1, 0_500)); // XOR A2, [0_500 + X1] = [0_600]
        bank.set(0_600, 0_777777_777777L);
        
        _engine.getExecOrUserARegister(2).setW(0_123456_123456L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute
        
        assertEquals(0_654321_654321L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testXOR_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        bank.set(0, fjaxhiu(0_41, 0, 4, 0, 0, 1, 0_1010)); // XOR A4, *0_1010
        bank.set(0_1010, 0_000000_001020L); // indirect to 0_1020
        bank.set(0_1020, 0_000000_000017L);
        
        _engine.getExecOrUserARegister(4).setW(0_000000_000020L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        
        _engine.cycle(); // fetch
        while (_engine.getInstructionPoint() != Engine.InstructionPoint.BETWEEN_INSTRUCTIONS) {
            _engine.cycle();
        }
        
        assertEquals(0_000000_000037L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testXOR_PartialWord() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        bank.set(0, fjaxu(0_41, 014, 6, 0, 0_500)); // XOR A6, (S2) of [0_500] (J=12 is S2)
        bank.set(0_500, 0_001200_000000L); // 12 in S2
        
        _engine.getExecOrUserARegister(6).setW(0_000000_000123L);
        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute
        
        assertEquals(0_000000_000131L, _engine.getExecOrUserARegister(7).getW());
    }
}
