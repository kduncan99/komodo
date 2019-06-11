/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.*;
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
    //  Helpful structs
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class LoadBankIndirectInfo {
        private final BankDescriptor _bdSource;
        private BankDescriptor _bdTarget = null;
        private final Instruction _instruction;
        private final InstructionProcessor _instructionProcessor;
        private boolean _treatAsVoid = false;

        private LoadBankIndirectInfo(
            final InstructionProcessor ip,
            final BankDescriptor bdSource,
            final Instruction instruction
        ) {
            _bdSource = bdSource;
            _instruction = instruction;
            _instructionProcessor = ip;
        }
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
     * All the common functional stuff needed for LBE and LBU.
     */
    protected void loadBank(
        final InstructionProcessor ip,
        final int baseRegisterIndex
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long op = ip.getOperand(false, true, false, false);
        int bankLevel = (int) (op >> 33);
        int bankDescriptorIndex = (int) (op >> 18) & 077777;
        int offset = (int) (op & 0777777);
        Instruction instruction = getInstruction();
        boolean lbe = instruction == Instruction.LBE;
        boolean lbu = instruction == Instruction.LBU;

        if ((bankLevel == 0) & ((bankDescriptorIndex > 0) && (bankDescriptorIndex < 32))) {
            if (lbe) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       bankLevel,
                                                       bankDescriptorIndex);
            } else if (lbu) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                       bankLevel,
                                                       bankDescriptorIndex);
            }
        }

        boolean voidBank = (bankLevel == 0) && (bankDescriptorIndex == 0);
        BankDescriptor bdTarget = new BankDescriptor();
        if (!voidBank) {
            BankDescriptor bdSource = ip.findBankDescriptor(bankLevel, bankDescriptorIndex);
            switch (bdSource.getBankType()) {
                case BasicMode:
                    if (lbu) {
                        if ( ip.getDesignatorRegister().getProcessorPrivilege() < 2 ) {
                            bdTarget = bdSource;
                        } else {
                            if ((bdSource.getGeneraAccessPermissions()._enter) || (bdSource.getSpecialAccessPermissions()._enter)) {
                                bdTarget = bdSource;
                            } else {
                                voidBank = true;
                            }
                        }
                    }
                    break;

                case Indirect:
                    LoadBankIndirectInfo lbiInfo = new LoadBankIndirectInfo(ip, bdSource, instruction);
                    loadBankIndirect(lbiInfo);
                    bdTarget = lbiInfo._bdTarget;
                    if (lbiInfo._treatAsVoid) {
                        voidBank = true;
                    }
                    break;

                case QueueRepository:
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                           bankLevel,
                                                           bankDescriptorIndex);

                default:
                    bdTarget = bdSource;
            }
        }

        //  LBU instruction deals with B2-B15 (B0,B1 filtered out).  So we need to update the ABT.
        if (lbu) {
            ActiveBaseTableEntry abte = new ActiveBaseTableEntry(bankLevel, bankDescriptorIndex, offset);
            ip.loadActiveBaseTableEntry(baseRegisterIndex - 1, abte);
        }

        BaseRegister baseRegister = voidBank ? new BaseRegister() : new BaseRegister(bdTarget, offset);
        ip.setBaseRegister(baseRegisterIndex, baseRegister);

        if ((!baseRegister._voidFlag) && (bdTarget.getGeneralFault())) {
            throw new TerminalAddressingExceptionInterrupt(TerminalAddressingExceptionInterrupt.Reason.GBitSetInTargetBD,
                                                           bankLevel,
                                                           bankDescriptorIndex);
        }
    }

    /**
     * Handles the case where the source bank loaded by LBU or LBE is an indirect bank
     * @param lbiInfo info struct
     * @throws MachineInterrupt as appropriate
     */
    private void loadBankIndirect(
        final LoadBankIndirectInfo lbiInfo
    ) throws MachineInterrupt {
        int targetLBDI = lbiInfo._bdSource.getTargetLBDI();
        int targetLevel = targetLBDI >> 15;
        int targetBankDescriptorIndex = targetLBDI & 077777;

        if ((targetLevel == 0) && (targetBankDescriptorIndex < 32)) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   targetLevel,
                                                   targetBankDescriptorIndex);
        }

        if (lbiInfo._bdSource.getGeneralFault()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   targetLevel,
                                                   targetBankDescriptorIndex);
        }

        try {
            lbiInfo._bdTarget = lbiInfo._instructionProcessor.findBankDescriptor(targetLevel, targetBankDescriptorIndex);
            switch (lbiInfo._bdTarget.getBankType()) {
                case BasicMode:
                    if (lbiInfo._instruction == Instruction.LBU) {
                        if ( (lbiInfo._instructionProcessor.getDesignatorRegister()
                            .getProcessorPrivilege() > 1)
                            && (!lbiInfo._bdTarget.getGeneraAccessPermissions()._enter)
                            && (!lbiInfo._bdTarget.getSpecialAccessPermissions()._enter) ) {
                            lbiInfo._treatAsVoid = true;
                        }
                    }
                    break;

                case Indirect:
                    //  Not sure if this is possible, but it is certainly not allowed
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                           targetLevel,
                                                           targetBankDescriptorIndex);

                case QueueRepository:
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                           targetLevel,
                                                           targetBankDescriptorIndex);
            }
        } catch (AddressingExceptionInterrupt aex) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   aex.getBankLevel(),
                                                   aex.getBankDescriptorIndex());
        }
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
     * Sells a 2-word RCS stack frame and returns the content of said frame as a 2-word long array.
     * Caller must have invoked rcsPopCheck() before hand, in order to provide the framePointer.
     * @return 2-word RCS entry
     */
    protected long[] rcsPop(
        final InstructionProcessor ip,
        final int framePointer
    ) {
        BaseRegister rcsBReg = ip.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
        IndexRegister rcsXReg = ip.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        rcsXReg.setXM(framePointer);

        int offset = framePointer - rcsBReg._lowerLimitNormalized - 2;
        long[] result = new long[0];
        //  ignore the null-dereference warning in the next line
        result[0] = rcsBReg._storage.get(offset++);
        result[1] = rcsBReg._storage.get(offset);
        return result;
    }

    /**
     * Checks Return Control Stack to ensure it is set up properly, and that there is at least one RCS entry available.
     * Caller should invoke this at some point before invoking rcsPop().
     * @param ip instruction processor of interest
     * @return frame pointer
     * @throws MachineInterrupt if one of the checks fail
     */
    protected int rcsPopCheck(
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

        return framePointer;
    }

    /**
     * Buys a 2-word RCS stack frame and populates it appropriately
     * @param ip instruction processor of interest
     * @param bField value to be placed in the .B field of the stack frame.
     * @throws MachineInterrupt if anything goes awry
     */
    protected void rcsPush(
        final InstructionProcessor ip,
        final int bField
    ) throws MachineInterrupt {
        // Make sure the return control stack base register is valid
        BaseRegister rcsBReg = ip.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
        if (rcsBReg._voidFlag) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                InstructionProcessor.RCS_BASE_REGISTER,
                                                                0);
        }

        IndexRegister rcsXReg = ip.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);

        int framePointer = (int) rcsXReg.getXM() - 2;
        if (framePointer < rcsBReg._lowerLimitNormalized) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                InstructionProcessor.RCS_BASE_REGISTER,
                                                                framePointer);
        }

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
