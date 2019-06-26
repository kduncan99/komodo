/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;

/**
 * Base class for all the instruction handlers
 */
@SuppressWarnings("Duplicates")
public abstract class InstructionHandler extends FunctionHandler {

    public enum Instruction {
        AA,     AAIJ,   ACEL,   ACK,    ADD1,   ADE,    AH,     AMA,
        ANA,    AND,    ANH,    ANMA,   ANT,    ANU,    ANX,    AT,
        AU,     AX,     BAO,    BBN,    BDE,    BIC,    BICL,   BIM,
        BIML,   BIMT,   BN,     BT,     BUY,    CALL,   CDU,    CJHE,
        CR,     DA,     DABT,   DADE,   DAN,    DCB,    DCEL,   DDEI,
        DEB,    DEC,    DEC2,   DEI,    DEPOSITQB,      DEQ,    DEQW,
        DF,     DFA,    DFAN,   DFD,    DFM,    DFU,    DI,     DIDE,
        DJZ,    DL,     DLCF,   DLM,    DLN,    DLSC,   DS,     DSA,
        DSC,    DSDE,   DSF,    DSL,    DTE,    DTGM,   EDDE,   ENQ,
        DNQF,   ENZ,    ER,     EX,     EXR,    FA,     FAN,    FCL,
        FD,     FEL,    FM,     GOTO,   HALT,   HJ,     HKJ,    HLTJ,
        IAR,    IDE,    INC,    INC2,   INV,    IPC,    J,      JB,
        JC,     JDF,    JFO,    JFU,    JGD,    JK,     JMGI,   JN,
        JNB,    JNC,    JNDF,   JNFO,   JNFU,   JNO,    JNS,    JNZ,
        JO,     JP,     JPS,    JZ,     KCHG,   LA,     LAE,    LAQW,
        LATP,   LBE,    LBED,   LBJ,    LBN,    LBRX,   LBU,    LBUD,
        LCC,    LCF,    LD,     LDJ,    LDSC,   LDSL,   LIJ,    LINC,
        LMA,    LMC,    LMJ,    LNA,    LNMA,   LOCL,   KPD,    LPM,
        LR,     LRD,    LRS,    LS,     LSA,    LSBL,   LSBO,   LSC,
        LSSC,   LSSL,   LUD,    LUF,    LX,     LXI,    LXLM,   LXM,
        LXSI,   MASG,   MASL,   MATG,   MATL,   MCDU,   MF,     MI,
        MLU,    MSE,    MSG,    MSI,    MSLE,   MSNE,   MSNW,   MSW,
        MTE,    MTG,    MTLE,   MTNE,   MTNW,   MTW,    NOP,    OR,
        PAIJ,   PRBA,   PRBC,   RDC,    RMD,    RTN,    SA,     SAQW,
        SAS,    SAZ,    SBED,   SBU,    SBUD,   SCC,    SD,     SDE,
        SDMF,   SDMN,   SDMS,   SE,     SELL,   SEND,   SFS,    SFZ,
        SG,     SGNL,   SINC,   SJH,    SKQT,   SLE,    SLJ,    SMA,
        SMD,    SNA,    SNE,    SNW,    SNZ,    SN1,    SPD,    SPID,
        SPM,    SP1,    SR,     SRS,    SS,     SSA,    SSC,    SSAIL,
        SSIP,   SSL,    SUB1,   SUD,    SW,     SX,     SYSC,   SZ,
        TCS,    TE,     TEP,    TES,    TG,     TGM,    TGZ,    TLE,
        TLEM,   TLZ,    TMZ,    TMZG,   TN,     TNE,    TNES,   TNGZ,
        TNLZ,   TNMZ,   TNOP,   TNPZ,   TNW,    TNZ,    TOP,    TP,
        TPZ,    TPZL,   TRA,    TRARS,  TS,     TSKP,   TSS,    TVA,
        TW,     TZ,     UNLK,   UR,     WITHDRAWQB,     XOR,    ZEROP,
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Internal worker methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Buys a 2-word RCS stack frame and populates it appropriately.
     * rcsPushCheck() must be invoked first.
     * @param ip instruction processor of interest
     * @param bField value to be placed in the .B field of the stack frame.
     * @param framePointer where the frame will be stored (retrieved from rcsPushCheck())
     */
    private void rcsPush(
        final InstructionProcessor ip,
        final int bField,
        final int framePointer
    ) {
        BaseRegister rcsBReg = ip.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
        IndexRegister rcsXReg = ip.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        rcsXReg.setXM(framePointer);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        long reentry = par.getH1() << 18;
        reentry |= (par.getH2() + 1) & 0777777;

        long state = (bField & 03) << 24;
        state |= ip.getDesignatorRegister().getW() & 0_000077_000000;
        state |= ip.getIndicatorKeyRegister().getAccessKey();

        int offset = framePointer - rcsBReg._lowerLimitNormalized;
        //  ignore the null-dereference warning in the next line
        rcsBReg._storage.set(offset++, reentry);
        rcsBReg._storage.set(offset, state);
    }

    /**
     * Buys a 2-word RCS stack frame and populates it with the given data
     * rcsPushCheck() must be invoked first.
     * @param ip instruction processor of interest
     * @param data data to be placed in the frame
     * @param framePointer where the frame will be stored (retrieved from rcsPushCheck())
     */
    private void rcsPush(
        final InstructionProcessor ip,
        final long[] data,
        final int framePointer
    ) {
        BaseRegister rcsBReg = ip.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
        IndexRegister rcsXReg = ip.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        rcsXReg.setXM(framePointer);
        int offset = framePointer - rcsBReg._lowerLimitNormalized;

        //  ignore the null-dereference warning in the next line
        rcsBReg._storage.set(offset++, data[0]);
        rcsBReg._storage.set(offset, data[1]);
    }

    /**
     * Checks whether we can buy a 2-word RCS stack frame
     * @param ip instruction processor of interest
     * @return framePointer pointer to the frame which will be the target of the push
     * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCStack has no more space
     */
    private int rcsPushCheck(
        final InstructionProcessor ip
    ) throws RCSGenericStackUnderflowOverflowInterrupt {
        // Make sure the return control stack base register is valid
        BaseRegister rcsBReg = ip.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
        if (rcsBReg._voidFlag) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(
                RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                InstructionProcessor.RCS_BASE_REGISTER,
                0);
        }

        IndexRegister rcsXReg = ip.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);

        int framePointer = (int) rcsXReg.getXM() - 2;
        if (framePointer < rcsBReg._lowerLimitNormalized) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(
                RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                InstructionProcessor.RCS_BASE_REGISTER,
                framePointer);
        }

        return framePointer;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Worker methods for the subclasses
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Certain instructions (ADD1, INC1, etc) choose to do either 1's or 2's complement arithemtic based upon the
     * j-field (and apparently, the quarter-word-mode).  Such instructions call here to make that determination.
     * @param instructionWord instruction word of interest
     * @param designatorRegister designator register of interest
     * @return true if we are to do two's complement
     */
    protected boolean chooseTwosComplementBasedOnJField(
        final InstructionWord instructionWord,
        final DesignatorRegister designatorRegister
    ) {
        switch ((int)instructionWord.getJ()) {
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

            default:    //  includes .W and .XH2
                return false;
        }
    }

    /**
     * Retrieves a BankDescriptor object representing the BD entry in a particular BDT.
     * @param ip reference to InstructionProcessor
     * @param bankLevel level of the bank of interest (0:7)
     * @param bankDescriptorIndex BDI of the bank of interest (0:077777)
     * @param throwFatal Set reason to FatalAddressingException if we throw an AddressingExceptionInterrupt for a bad
     *                   specified leval/BDI, otherwise the reason will be InvalidSourceLevelBDI.
     * @return BankDescriptor object representing the bank descriptor in memory
     */
    protected BankDescriptor getBankDescriptor(
        final InstructionProcessor ip,
        final int bankLevel,
        final int bankDescriptorIndex,
        final boolean throwFatal
    ) throws AddressingExceptionInterrupt {
        if ((bankLevel == 0) && (bankDescriptorIndex < 32)) {
            AddressingExceptionInterrupt.Reason reason =
                throwFatal ? AddressingExceptionInterrupt.Reason.FatalAddressingException
                           : AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI;
            throw new AddressingExceptionInterrupt(reason, bankLevel, bankDescriptorIndex);
        }

        int bdRegIndex = bankLevel + 16;
        ArraySlice bdStorage = ip.getBaseRegister(bdRegIndex)._storage;
        int bdTableOffset = 8 * bankDescriptorIndex;
        if (bdTableOffset + 8 > bdStorage.getSize()) {
            AddressingExceptionInterrupt.Reason reason =
                throwFatal ? AddressingExceptionInterrupt.Reason.FatalAddressingException
                           : AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI;
            throw new AddressingExceptionInterrupt(reason, bankLevel, bankDescriptorIndex);
        }

        //  Create a BankDescriptor object
        return new BankDescriptor(bdStorage, bdTableOffset);
    }

    /**
     * Sells a 2-word RCS stack frame and returns the content of said frame as a 2-word long array
     * after verifying the stack is usable and contains at least one entry.
     * @return 2-word RCS entry
     * @throws MachineInterrupt if anything goes wrong
     */
    protected long[] rcsPop(
        final InstructionProcessor ip
    ) throws MachineInterrupt {
        BaseRegister rcsBReg = ip.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
        if (rcsBReg._voidFlag) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                InstructionProcessor.RCS_BASE_REGISTER,
                                                                0);
        }

        IndexRegister rcsXReg = ip.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        int framePointer = (int) rcsXReg.getXM() + 2;
        if (framePointer > rcsBReg._upperLimitNormalized) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                InstructionProcessor.RCS_BASE_REGISTER,
                                                                framePointer);
        }
        rcsXReg.setXM(framePointer);

        int offset = framePointer - rcsBReg._lowerLimitNormalized - 2;
        long[] result = new long[2];
        //  ignore the null-dereference warning in the next line
        result[0] = rcsBReg._storage.get(offset++);
        result[1] = rcsBReg._storage.get(offset);
        return result;
    }

    /**
     * Buys a 2-word RCS stack frame and populates it appropriately
     * @param ip instruction processor of interest
     * @param bField value to be placed in the .B field of the stack frame.
     * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCStack has no more space
     */
    protected void rcsPush(
        final InstructionProcessor ip,
        final int bField
    ) throws RCSGenericStackUnderflowOverflowInterrupt {
        int framePointer = rcsPushCheck(ip);
        rcsPush(ip, bField, framePointer);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Override-able methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Retrieve the Instruction enumeration for this instruction
     */
    public abstract Instruction getInstruction();

    /**
     * Retrieves the standard quantum timer charge for an instruction.
     * More complex instructions may override this, and special cases exist for indirect addressing and
     * for iterative instructions.
     * @return quantum charge for this instruction
     */
    public int getQuantumTimerCharge(
    ) {
        return 20;
    }
}
