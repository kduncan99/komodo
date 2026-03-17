/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.logical;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
public class TestMLUFunction extends TestFunction {

    private long mluBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_43, 0, a, x, h, i, u);
    }

    private long mluEM(long a, long x, long u) {
        return fjaxu(0_43, 0, a, x, u);
    }

    @BeforeEach
    public void setup() throws MachineInterrupt {
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false)
               .setBasicModeBaseRegisterSelection(false);
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
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
    }

    @Test
    public void testMLU_BM_Immediate() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        // MLU A0, immediate 0_000000_123456 (using j=016)
        bank.set(0, fjaxhiu(0_43, 016, 0, 0, 0, 0, 0_123456));

        // R2 = 0_777777_000000 (Choose high from operand, low from A0)
        _engine.getExecOrUserRRegister(2).setW(0_777777_000000L);
        // A0 = 0_111111_222222
        _engine.getExecOrUserARegister(0).setW(0_111111_222222L);
        // operand = 0_000000_123456 (immediate)

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute

        // Expected result:
        // (R2 & operand) = (0_777777_000000 & 0_000000_123456) = 0
        // (~R2 & A0) = (0_000000_777777 & 0_111111_222222) = 0_000000_222222
        // result = 0_000000_222222
        assertEquals(0_000000_222222L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testMLU_EM_Immediate() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        // MLU A0, immediate 0_123456_765432 (using j=0) - EM immediate is full word if J=0?
        // Wait, J=0 in EM for MLU might not be immediate. 
        // MLU supports immediate mode if setIsImmediateMode(true) is called in constructor.
        // For AND/OR/XOR J=016 was used for JFIELD_U.
        bank.set(0, fjaxu(0_43, 016, 0, 0, 0_123456));

        // R2 = 0_000000_777777 (Choose low from operand, high from A0)
        _engine.getExecOrUserRRegister(2).setW(0_000000_777777L);
        // A0 = 0_111111_222222
        _engine.getExecOrUserARegister(0).setW(0_111111_222222L);
        // operand = 0_000000_123456 (immediate)

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute

        // Expected result:
        // (R2 & operand) = (0_000000_777777 & 0_000000_123456) = 0_000000_123456
        // (~R2 & A0) = (0_777777_000000 & 0_111111_222222) = 0_111111_000000
        // result = 0_111111_123456
        assertEquals(0_111111_123456L, _engine.getExecOrUserARegister(1).getW());
    }

    @Test
    public void testMLU_A15() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        // MLU A15, immediate 0_123456 (using j=016)
        bank.set(0, fjaxu(0_43, 016, 017, 0, 0_123456));

        // R2 = 0_777777_777777 (Choose all from operand)
        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(017).setW(0_111111_111111L);

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute

        // Result in A(15+1) & 017 = A0
        assertEquals(0_000000_123456L, _engine.getExecOrUserARegister(0).getW());
    }

    @Test
    public void testMLU_Indexed_EM() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();
        _engine.getExecOrUserXRegister(1).setW(0_000000_000100L);
        // MLU A2, [0_500 + X1] = [0_600]
        bank.set(0, mluEM(2, 1, 0_500));
        bank.set(0_600, 0_707070_707070L);

        // R2 = 0_555555_555555
        _engine.getExecOrUserRRegister(2).setW(0_555555_555555L);
        // A2 = 0_222222_222222
        _engine.getExecOrUserARegister(2).setW(0_222222_222222L);

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute

        // result = (R2 & operand) | (~R2 & A2)
        // operand = 0_707070_707070
        // R2 & operand = 0_555555_555555 & 0_707070_707070 = 0_505050_505050
        // ~R2 & A2 = 0_222222_222222 & 0_222222_222222 = 0_222222_222222
        // result = 0_505050_505050 | 0_222222_222222 = 0_727272_727272
        assertEquals(0_727272_727272L, _engine.getExecOrUserARegister(3).getW());
    }

    @Test
    public void testMLU_Indirect_BM() throws MachineInterrupt, EngineHaltedException {
        setupBM();
        var bank = _engine.getBaseRegister(12).getStorage();
        // MLU A4, *0_1010
        bank.set(0, fjaxhiu(0_43, 0, 4, 0, 0, 1, 0_1010));
        bank.set(0_1010, 0_000000_001020L); // indirect to 0_1020
        bank.set(0_1020, 0_777777_777777L);

        _engine.getExecOrUserRRegister(2).setW(0_777777_777777L); // mask = all operand
        _engine.getExecOrUserARegister(4).setW(0L);

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        while (_engine.getInstructionPoint() != Engine.InstructionPoint.BETWEEN_INSTRUCTIONS) {
            _engine.cycle();
        }

        // result = (0_777777_777777 & 0_777777_777777) | (0 & 0) = 0_777777_777777
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(5).getW());
    }

    @Test
    public void testMLU_PartialWord() throws MachineInterrupt, EngineHaltedException {
        setupEM();
        var bank = _engine.getBaseRegister(0).getStorage();

        // MLU A6, (S2) of [0_500] (J=12 is S2)
        bank.set(0, fjaxu(0_43, 014, 6, 0, 0_500));
        // S2 is bits 24-29 (upper part of low half).
        // 0_00XX00_000000
        bank.set(0_500, 0_001200_000000L); // 12 in S2

        // R2 = 0_000000_777777 (Select operand for low bits, A6 for high)
        _engine.getExecOrUserRRegister(2).setW(0_000000_777777L);
        // A6 = 0_123456_777777
        _engine.getExecOrUserARegister(6).setW(0_123456_777777L);

        _engine.getProgramAddressRegister().setProgramCounter(0);
        _engine.cycle(); // fetch
        _engine.cycle(); // execute

        // operand = S2 of [0_500] = 0_000000_000012
        // R2 & operand = 0_000000_777777 & 0_000000_000012 = 0_000000_000012
        // ~R2 & A6 = 0_777777_000000 & 0_123456_777777 = 0_123456_000000
        // result = 0_123456_000012
        assertEquals(0_123456_000012L, _engine.getExecOrUserARegister(7).getW());
    }
}
