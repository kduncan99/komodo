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

    private static class LoadBankJumpInfo {
        private final int _brIndex; //  Index of BR involved in this request 0:31
        private final Instruction _instruction;
        private final InstructionProcessor _instructionProcessor;
        private final int _jumpAddress;
        private final int _offset;
        private final BankDescriptor _sourceBankDescriptor;
        private final int _sourceBankDescriptorIndex;
        private final int _sourceBankLevel;

        private LoadBankJumpInfo(
            final InstructionProcessor ip,
            final Instruction instruction,
            final int brIndex,
            final int jumpAddress,
            final int sourceBankLevel,
            final int sourceBankDescriptorIndex,
            final BankDescriptor sourceBankDescriptor,
            final int offset
        ) {
            _instruction = instruction;
            _instructionProcessor = ip;
            _brIndex = brIndex;
            _jumpAddress = jumpAddress;
            _offset = offset;
            _sourceBankDescriptor = sourceBankDescriptor;
            _sourceBankDescriptorIndex = sourceBankDescriptorIndex;
            _sourceBankLevel = sourceBankLevel;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Internal worker methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * LxJ Case 1:Implements normal LxJ algorithm.
     * Source bank is either Basic Mode, or Extended Mode with no Enter Access.  IS is 0 or 1.
     * The basic-to-extended case is special, and designed to allow basic mode programs acces to
     * extended mode data - the assumption is that the jump is *not* to the newly-based bank.
     */
    private void loadBankJumpCase1(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        InstructionProcessor ip = lbjInfo._instructionProcessor;

        //  Step 1 Get prior L,BDI
        ActiveBaseTableEntry abte = ip.getActiveBaseTableEntries()[lbjInfo._brIndex - 1];
        int priorBDI = abte.getBDI();
        int priorLevel = abte.getLevel();
        int priorSubset = abte.getSubsetOffset();

        //  Step 2 Determine final target bank - for this case, it is always the source bank
        //  so there is nothing to be done.

        //  Step 3 Translate prior L,BDI to E,LS,BDI, write that and PAR.PC + 1 to X(a)
        VirtualAddress vaPrior = new VirtualAddress(priorLevel, priorBDI, priorSubset);
        long newXValue = vaPrior.translateToBasicMode();
        newXValue |= (long)(lbjInfo._brIndex & 03) << 33;
        int newParPC = ip.getProgramAddressRegister().getProgramCounter() + 1;
        newXValue = (newXValue & (0_777777_000000L)) | newParPC;

        //  Step 4 DB16 and access key -> User X(0)
        long x0Value = ip.getDesignatorRegister().getBasicModeEnabled() ? 0_400000_000000L : 0;
        x0Value |= ip.getIndicatorKeyRegister().getAccessKey();
        ip.getGeneralRegister(0).setW(x0Value);

        //  Step 5 If a gate was processed... but it wasn't.  Nothing to do.

        //  Step 6 ABT is updated, and PAR.PC is set to U(18:35), and ABT(n).Offset is set to 0
        ip.getActiveBaseTableEntries()[lbjInfo._brIndex - 1] = new ActiveBaseTableEntry(lbjInfo._sourceBankLevel,
                                                                                        lbjInfo._sourceBankDescriptorIndex,
                                                                                        0);
        ip.setProgramCounter(lbjInfo._jumpAddress, false);

        //  Step 7 BankRegister is created for B(whatever)
        BaseRegister br = new BaseRegister(lbjInfo._sourceBankDescriptor);
        if (br._voidFlag) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                   lbjInfo._sourceBankLevel,
                                                   lbjInfo._sourceBankDescriptorIndex);
        }
        ip.setBaseRegister(lbjInfo._brIndex, br);

        //  Step 8 DB31 is set appropriately
        try {
            int basicBRIndex = ip.getBasicModeBankRegisterIndex();
            boolean brFlag = (basicBRIndex == 15) || (basicBRIndex == 13);
            ip.getDesignatorRegister().setBasicModeBaseRegisterSelection(brFlag);
        } catch (UnresolvedAddressException ex) {
            //  can't happen... we hope
            //  Indirect addressing was already completely satisfied, and F0 updated.
            assert(false);
        }

        //  Step 9 If target BD.G is set, or selection of base register error occurs,
        //  throw terminal addressing exception
        if (lbjInfo._sourceBankDescriptor.getGeneralFault()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   lbjInfo._sourceBankLevel,
                                                   lbjInfo._sourceBankDescriptorIndex);
        }
    }

    /**
     * LxJ Case 2:Implements LxJ/CALL - Source bank is extended mode or gate bank
     */
    private void loadBankJumpCase2(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        //  Step 1 check for RCS overflow - deferred until we actually grab a frame

        //  Step 2 Fetch prior L,BDI from ABT
        InstructionProcessor ip = lbjInfo._instructionProcessor;
        ActiveBaseTableEntry abte = ip.getActiveBaseTableEntries()[lbjInfo._brIndex - 1];
        int priorBDI = abte.getBDI();
        int priorLevel = abte.getLevel();
        int priorSubset = abte.getSubsetOffset();

        //  Step 3 Determine final target bank.
        int targetBankLevel = lbjInfo._sourceBankLevel;
        int targetBankDescriptorIndex = lbjInfo._sourceBankDescriptorIndex;
        BankDescriptor bdTarget = lbjInfo._sourceBankDescriptor;
        boolean gate = false;
        if (bdTarget.getBankType() == BankDescriptor.BankType.Gate) {
            int targetLBDI = bdTarget.getTargetLBDI();
            targetBankLevel = targetLBDI >> 15;
            targetBankDescriptorIndex = targetLBDI & 077777;
            //TODO gate processing
        }

        //  Step 4 Mixed mode transfer is to occur.  Set the base register we came from to void.
        ip.setBaseRegister(lbjInfo._brIndex, new BaseRegister());
        ip.getActiveBaseTableEntries()[lbjInfo._brIndex - 1] = new ActiveBaseTableEntry(0);

        //  Step 5 Create an RCS frame
        long[] frame = new long[2];
        int rtnAddr = ip.getProgramAddressRegister().getProgramCounter() + 1;
        frame[0] = ((long) priorLevel << 33) | ((long) priorBDI << 18) | (rtnAddr & 0777777);
        frame[1] = (lbjInfo._brIndex << 24)
            | (ip.getDesignatorRegister().getW() & 0_000077_000000L)
            | (ip.getIndicatorKeyRegister().getAccessKey());
        rcsPush(ip, frame);

        //  Step 6 DB16 and access key stored in user X0.
        long x0Value = ip.getDesignatorRegister().getBasicModeEnabled() ? 0_400000_000000L : 0;
        x0Value |= ip.getIndicatorKeyRegister().getAccessKey();
        ip.getGeneralRegister(0).setW(x0Value);

        //  Step 7 If a gate was processed, then do some fun stuff
        if (gate) {
            //TODO
            /*
            If a Gate was processed and Gate.DBI = 0, then the hard-held DB12–15 := Gate.DB12-15 and
            DB17 := Gate.DB17 and/or if Gate.AKI = 0,
                Indicator/Key_Register.Access_Key := Gate.Access_Key.DB16 := 1, indicating a transfer to
            Extended_Mode.
                If a Gate was processed and LP0I = 0, then if either DB17 = 0, User
            R0 := Gate.Latent_Parameter_0 Value or DB17 = 1, Executive R0 := Gate Latent Parameter 0
            Value; and/or if LP1I = 0, then if either DB17 = 0, User R1 := Gate.Latent_Parameter_1 Value or
            DB17 = 1, Executive R1 := Gate.Latent_Parameter_1 Value. Note: writing a Latent Parameter
            into Executive R0/R1 does not cause a GRS violation regardless of the level of processor
            privilege in effect.
             */
        }

        //  Step 8 PAR.L,BDI is updated, PAR.PC is set to U(18:35)
        //TODO
        ip.setProgramCounter(lbjInfo._jumpAddress, false);

        //  Step 9 Appropriate values are loaded into B0
        BaseRegister br = new BaseRegister(bdTarget);
        if (br._voidFlag) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                   lbjInfo._sourceBankLevel,
                                                   lbjInfo._sourceBankDescriptorIndex);
        }
        ip.setBaseRegister(0, br);

        //  Step 10 If final based bank has g bit set, throw terminal addressing interrupt
        if (bdTarget.getGeneralFault()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   targetBankLevel,
                                                   targetBankDescriptorIndex);
        }
    }

    /**
     * LxJ Case 3:Implements LxJ/GOTO
     */
    private void loadBankJumpCase3(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        //  Step 1 Translate Xa basic mode Virtual Address to Source L,BDI including gate processing
        //TODO

        //  Step 2 Mixed mode transfer is to occur, B0 is to be loaded, while X(a) is to be marked void
        //TODO

        //  Step 3 DB16 and access key stored in user X0.
        //TODO

        //  Step 4 If a gate was processed, then do some fun stuff
        //TODO

        //  Step 5 PAR.L,BDI is updated, PAR.PC is set to U(18:35)
        //TODO

        //  Step 6 Appropriate values are loaded into B0
        //TODO

        //  Step 7 If final based bank has g bit set, throw terminal addressing interrupt
        //TODO
    }

    /**
     * LxJ Case 4:Implements LxJ/RETURN to basic mode
     */
    private void loadBankJumpCase4(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        //  Step 1 check for RCS overflow
        InstructionProcessor ip = lbjInfo._instructionProcessor;
        int framePointer = rcsPopCheck(ip);

        //  Step 2 determine final target bank
        //TODO

        //  Step 3 RTN to basic mode - load one of B12:B15 - which one is determined by RCS.B + 12
        //TODO

        //  Step 4 IKR:AccessKey is set to the RCS Access Key, and DB12-17 are loaded from RCS.DB12-17
        //TODO

        //  Step 5 ABT(n) set to RCS:L,BDI
        //      PAR.PC set to RCS.offset
        //      ABT(n).offset set to 0
        //TODO

        //  Step 6 BankRegister is created for B(n) that we are transferring to
        //TODO

        //  Step 7 DB31 is set appropriately
        //TODO

        //  Step 8 if BD.G or RCS.Trap, throw terminal addressing exception
        //TODO
    }

    /**
     * LxJ Case 5:Implements LxJ/RETURN to extended mode
     */
    private void loadBankJumpCase5(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        //  Step 1 check for RCS overflow
        //TODO

        //  Step 2 determine final target bank
        //TODO

        //  Step 3 return to extended mode, B0 will be loaded, and the BR for the bank we came from is set void
        //TODO

        //  Step 4 IKR:AccessKey is set to the RCS Access Key, and DB12-17 are loaded from RCS.DB12-17
        //TODO

        //  Step 5 PAR.L,BDI set to RCS.L,BDI
        //      PAR.PC set to RCS.offset
        //TODO

        //  Step 6 BankRegister is created for B(0)
        //TODO

        //  Step 7 if BD.G or RCS.Trap, throw terminal addressing exception
        //TODO
    }

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
     * All the common functional stuff needed for LBE and LBU.
     */
    protected void loadBank(
        final InstructionProcessor ip,
        final InstructionWord iw,
        final int baseRegisterIndex
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long op = ip.getOperand(false, true, false, false);
        int bankLevel = (int) (op >> 33);
        int bankDescriptorIndex = (int) (op >> 18) & 077777;
        int offset = (int) (op & 0777777);
        Instruction instruction = getInstruction();

        //  L,BDI of 0,0 produces a void bank
        BaseRegister br = new BaseRegister();
        boolean generalFault = false;
        boolean voidFlag = false;
        if ( (bankLevel == 0) && (bankDescriptorIndex == 0) ) {
            ip.setBaseRegister(baseRegisterIndex, new BaseRegister());
            voidFlag = true;
        }

        BaseRegister baseRegister = new BaseRegister();
        if (!voidFlag) {
            BankDescriptor bdSource = getBankDescriptor(ip, bankLevel, bankDescriptorIndex, false);
            BankDescriptor bdTarget = bdSource;
            switch (bdSource.getBankType()) {
                case BasicMode:
                    if (instruction == Instruction.LBU) {
                        if (ip.getDesignatorRegister().getProcessorPrivilege() < 2) {
                            break;
                        } else {
                            if ((bdSource.getGeneraAccessPermissions()._enter) || (bdSource.getSpecialAccessPermissions()._enter)) {
                                break;
                            } else {
                                voidFlag = true;
                            }
                        }
                    } else {
                        baseRegister = new BaseRegister(bdSource, offset);
                    }
                    break;

                //TODO need cases for Gate and Indirect

                case QueueRepository:
                    throw new AddressingExceptionInterrupt(
                        AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                        bankLevel,
                        bankDescriptorIndex);

                default:
                    break;
            }

            if (!voidFlag) {
                baseRegister = new BaseRegister(bdSource, offset);
            }
        }

        //  LBU instruction deals with B2-B15 (B0,B1 filtered out).  So we need to update the ABT.
        if (instruction == Instruction.LBU) {
            if (voidFlag) {
                ip.loadActiveBaseTableEntry(baseRegisterIndex - 1, new ActiveBaseTableEntry(0));
            } else {
                ActiveBaseTableEntry abte = new ActiveBaseTableEntry(bankLevel, bankDescriptorIndex, offset);
                ip.loadActiveBaseTableEntry(baseRegisterIndex - 1, abte);
            }
        }

        ip.setBaseRegister(baseRegisterIndex, baseRegister);
        if ( (!baseRegister._voidFlag) && (generalFault) ) {
            throw new TerminalAddressingExceptionInterrupt(
                TerminalAddressingExceptionInterrupt.Reason.GBitSetInTargetBD,
                bankLevel,
                bankDescriptorIndex);
        }
    }

    /**
     * Common code for LBJ, LIJ, and LDJ...
     * This is only called from basic mode, but *might* transition to extended mode.
     * If we stay in basic mode, we load a bank for one of B12:B15; for extended mode, for B0.
     */
    protected void loadBankJump(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int regIndex = (int) iw.getA();
        if (regIndex == 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
        }

        int jumpAddress = ip.getJumpOperand(false);
        Instruction instruction = getInstruction();

        //  Split out the various fields
        IndexRegister linkageRegister = ip.getExecOrUserXRegister((int) iw.getA());
        long lrValue = linkageRegister.getW();
        boolean execFlag = (lrValue & 0_400000_000000L) != 0;
        boolean levelSpec = (lrValue & 0_040000_000000L) != 0;
        int interfaceSpec = (int) ((lrValue & 0_030000_000000L) >> 30);
        int sourceBankDescriptorIndex = (int) ((lrValue & 0_007777_000000L) >> 18);

        int brIndex = 0;
        switch (instruction) {
            case LBJ:
                brIndex = (int) ((lrValue & 0_300000_000000L) >> 33) + 12;
                break;

            case LDJ:
                brIndex = ip.getDesignatorRegister().getBasicModeBaseRegisterSelection() ? 15 : 14;
                break;

            case LIJ:
                brIndex = ip.getDesignatorRegister().getBasicModeBaseRegisterSelection() ? 13 : 12;
                break;

        }

        //  Find the source BD (which is the bank to be based).
        int sourceLevel = VirtualAddress.translateBasicToExtendedLevel(execFlag, levelSpec);
        BaseRegister bdtBaseRegister = ip.getBaseRegister(sourceLevel + 16);
        int bdtOffset = 8 * sourceBankDescriptorIndex;
        bdtBaseRegister.checkAccessLimits(bdtOffset,
                                          false,
                                          true,
                                          false,
                                          ip.getIndicatorKeyRegister().getAccessInfo());

        BankDescriptor bdSource = getBankDescriptor(ip, sourceLevel, sourceBankDescriptorIndex, false);
        AccessPermissions bdSourcePerms = bdSource.getEffectiveAccesPermissions(ip.getIndicatorKeyRegister().getAccessInfo());
        BankDescriptor.BankType sourceBankType = bdSource.getBankType();
        boolean basic = sourceBankType == BankDescriptor.BankType.BasicMode;
        boolean extended = sourceBankType == BankDescriptor.BankType.ExtendedMode;
        boolean noEnter = extended && !bdSourcePerms._enter;
        boolean gate = sourceBankType == BankDescriptor.BankType.Gate;

        LoadBankJumpInfo lbjInfo = new LoadBankJumpInfo(ip,
                                                        instruction,
                                                        brIndex,
                                                        jumpAddress,
                                                        sourceLevel,
                                                        sourceBankDescriptorIndex,
                                                        bdSource,
                                                        (int) linkageRegister.getH2());

        switch (interfaceSpec) {
            case 0:
                if (basic || (extended && noEnter)) {
                    loadBankJumpCase1(lbjInfo);  //  Normal LBJ
                } else if (extended || gate) {
                    loadBankJumpCase2(lbjInfo);  //  LBJ/CALL
                } else {
                    //  Bad bank type
                }
                break;

            case 1:
                if (basic || (extended && noEnter)) {
                    loadBankJumpCase1(lbjInfo);  //  Normal LBJ
                } else if (extended || gate) {
                    loadBankJumpCase3(lbjInfo);  //  LBJ/GOTO
                } else {
                    //  Bad bank type
                }
                break;

            case 2:
                //  get DB16 from RCS
                //  If DB16 is set, case4() else case5()
                break;

            case 3:
                //  Illegal ISpec
                break;
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
     * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCStack has no more space
     */
    protected void rcsPush(
        final InstructionProcessor ip,
        final int bField
    ) throws RCSGenericStackUnderflowOverflowInterrupt {
        int framePointer = rcsPushCheck(ip);
        rcsPush(ip, bField, framePointer);
    }

    /**
     * Buys a 2-word RCS stack frame and populates it from the given data
     * @param ip instruction processor of interest
     * @param data what we populate the frame with
     * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCStack has no more space
     */
    protected void rcsPush(
        final InstructionProcessor ip,
        final long data[]
    ) throws RCSGenericStackUnderflowOverflowInterrupt {
        int framePointer = rcsPushCheck(ip);
        rcsPush(ip, data, framePointer);
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
