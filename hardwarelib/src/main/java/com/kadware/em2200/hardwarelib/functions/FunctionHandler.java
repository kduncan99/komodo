/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Base class for all the instruction handlers
 */
public abstract class FunctionHandler {

    /**
     * Certain instructions (ADD1, INC1, etc) choose to do either 1's or 2's complement arithemtic based upon the
     * j-field (and apparently, the quarter-word-mode).  Such instructions call here to make that determination.
     * <p>
     * @param instructionWord
     * @param designatorRegister
     * <p>
     * @return
     */
    public boolean chooseTwosComplementBasedOnJField(
        final InstructionWord instructionWord,
        final DesignatorRegister designatorRegister
    ) {
        switch ((int)instructionWord.getJ()) {
            case InstructionWord.W:
            case InstructionWord.XH2:
                return false;

            case InstructionWord.H1:
            case InstructionWord.H2:
            case InstructionWord.S1:
            case InstructionWord.S2:
            case InstructionWord.S3:
            case InstructionWord.S4:
            case InstructionWord.S5:
            case InstructionWord.S6:
                return true;

            case InstructionWord.Q1:    //  also T1
            case InstructionWord.Q2:    //  also XH1
            case InstructionWord.Q3:    //  also T2
            case InstructionWord.Q4:    //  also T3
                return (designatorRegister.getQuarterWordModeEnabled());

            default:
                return false;
        }
    }

    /**
     * Handles a function (i.e., an instruction)
     * <p>
     * @param ip reference to the owning InstructionProcessor
     * @param instructionWord reference to F0 (_currentInstruction)
     * <p>
     * @throws MachineInterrupt for any conditions which need to raise an interrupt
     * @throws UnresolvedAddressException for basic mode indirect addressing mid-resolution situation
     */
    public abstract void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt,
             UnresolvedAddressException;

    /**
     * Retrieves the standard quantum timer charge for an instruction.
     * More complex instructions may override this, and special cases exist for indirect addressing and
     * for iterative instructions.
     * @return
     */
    public int getQuantumTimerCharge(
    ) {
        return 20;
    }
}
