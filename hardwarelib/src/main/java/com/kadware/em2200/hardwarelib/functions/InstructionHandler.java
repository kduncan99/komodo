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
        private final int _offset;
        private final BankDescriptor _sourceBankDescriptor;
        private final int _sourceBankDescriptorIndex;
        private final int _sourceBankLevel;

        private LoadBankJumpInfo(
            final InstructionProcessor ip,
            final Instruction instruction,
            final int brIndex,
            final int sourceBankLevel,
            final int sourceBankDescriptorIndex,
            final BankDescriptor sourceBankDescriptor,
            final int offset
        ) {
            _instruction = instruction;
            _instructionProcessor = ip;
            _brIndex = brIndex;
            _offset = offset;
            _sourceBankDescriptor = sourceBankDescriptor;
            _sourceBankDescriptorIndex = sourceBankDescriptorIndex;
            _sourceBankLevel = sourceBankLevel;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Internal worker methods
    //  ----------------------------------------------------------------------------------------------------------------------------


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
     * @param iw reference to the invoking instruction word (F0)
     * @param bankLevel level of the bank of interest (0:7)
     * @param bankDescriptorIndex BDI of the bank of interest (0:077777)
     * @param throwFatal Set reason to FatalAddressingException if we throw an AddressingExceptionInterrupt for a bad
     *                   specified leval/BDI, otherwise the reason will be InvalidSourceLevelBDI.
     * @return BankDescriptor object for all possibilities other than level,bdi == 0,0 - null otherwise
     */
    protected BankDescriptor getBankDescriptor(
        final InstructionProcessor ip,
        final InstructionWord iw,
        final int bankLevel,
        final int bankDescriptorIndex,
        final boolean followIndirectOrGate,
        final boolean throwFatal
    ) throws AddressingExceptionInterrupt {
        if (bankLevel == 0) {
            if (bankDescriptorIndex == 0) {
                return null;
            } else if (bankDescriptorIndex < 32) {
                AddressingExceptionInterrupt.Reason reason =
                    throwFatal ? AddressingExceptionInterrupt.Reason.FatalAddressingException
                               : AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI;
                throw new AddressingExceptionInterrupt(reason, bankLevel, bankDescriptorIndex);
            }
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

        //  Create a BankDescriptor object, maybe follow gates and indirects...
        BankDescriptor bd = new BankDescriptor(bdStorage, bdTableOffset);
        if (followIndirectOrGate) {
            BankDescriptor.BankType bankType = bd.getBankType();
            if ((bankType == BankDescriptor.BankType.Gate) || (bankType == BankDescriptor.BankType.Indirect)) {
                int targetLBDI = bd.getTargetLBDI();
                return getBankDescriptor(ip,
                                         iw,
                                         targetLBDI >> 15,
                                         targetLBDI & 077777,
                                         false,
                                         true);
            }
        }

        return bd;
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
        boolean lbu = instruction == Instruction.LBU;

        BankDescriptor bdSource = getBankDescriptor(ip,
                                                    iw,
                                                    bankLevel,bankDescriptorIndex,
                                                    true,
                                                    false);
        boolean voidBank = bdSource == null;
        BankDescriptor bdTarget = new BankDescriptor();
        if (!voidBank) {
            switch (bdSource.getBankType()) {
                case BasicMode:
                    if (lbu) {
                        if (ip.getDesignatorRegister().getProcessorPrivilege() < 2) {
                            bdTarget = bdSource;
                        } else {
                            if ((bdSource.getGeneraAccessPermissions()._enter) || (bdSource.getSpecialAccessPermissions()._enter)) {
                                bdTarget = bdSource;
                            } else {
                                voidBank = true;
                            }
                        }
                    } else {
                        bdTarget = bdSource;
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
     * Common code for LBJ, LIJ, and LDJ...
     * This is only called from basic mode, but *might* transition to extended mode.
     * If we stay in basic mode, we load a bank for one of B12:B15; for extended mode, for B0.
     */
    protected void loadBankJump(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt {
        int regIndex = (int) iw.getA();
        if (regIndex == 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidLinkageRegister);
        }

        boolean ldj = getInstruction() == Instruction.LDJ;
        boolean lij = getInstruction() == Instruction.LIJ;

        //  Split out the various fields
        IndexRegister linkageRegister = ip.getExecOrUserXRegister((int) iw.getA());
        long lrValue = linkageRegister.getW();
        boolean execFlag = (lrValue & 0_400000_000000L) != 0;
        int brIndex = (int) ((lrValue & 0_300000_000000L) >> 33) + 12;// TODO Depends on whether LBJ, LIJ, or LDJ
        boolean levelSpec = (lrValue & 0_040000_000000L) != 0;
        int interfaceSpec = (int) ((lrValue & 0_030000_000000L) >> 30);
        int sourceBankDescriptorIndex = (int) ((lrValue & 0_007777_000000L) >> 18);

        //  Find the source BD (which is the bank to be based).
        int sourceLevel = VirtualAddress.translateBasicToExtendedLevel(execFlag, levelSpec);
        BaseRegister bdtBaseRegister = ip.getBaseRegister(sourceLevel + 16);
        int bdtOffset = 8 * sourceBankDescriptorIndex;
        bdtBaseRegister.checkAccessLimits(bdtOffset,
                                          false,
                                          true,
                                          false,
                                          ip.getIndicatorKeyRegister().getAccessInfo());

        BankDescriptor bdSource = getBankDescriptor(ip,
                                                    iw,
                                                    sourceLevel,
                                                    sourceBankDescriptorIndex,
                                                    false,
                                                    false);

        AccessPermissions bdSourcePerms = bdSource.getEffectiveAccesPermissions(ip.getIndicatorKeyRegister().getAccessInfo());
        BankDescriptor.BankType sourceBankType = bdSource.getBankType();
        boolean basic = sourceBankType == BankDescriptor.BankType.BasicMode;
        boolean extended = sourceBankType == BankDescriptor.BankType.ExtendedMode;
        boolean noEnter = extended && !bdSourcePerms._enter;
        boolean gate = sourceBankType == BankDescriptor.BankType.Gate;

        LoadBankJumpInfo lbjInfo = new LoadBankJumpInfo(ip,
                                                        getInstruction(),
                                                        brIndex,
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
     * LxJ Case 1:Implements normal LxJ algorithm
     * @param lbjInfo
     * @throws MachineInterrupt
     */
    private void loadBankJumpCase1(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        InstructionProcessor ip = lbjInfo._instructionProcessor;

        //  Step 1 get prior L,BDI
        ActiveBaseTableEntry abte = ip.getActiveBaseTableEntries()[lbjInfo._brIndex - 1];
        int priorBDI = abte.getBDI();
        int priorLevel = abte.getLevel();
        int priorSubset = abte.getSubsetOffset();

        //  Step 2 Translate X(a) basic mode Virtual Address to L,BDI, then determine final target bank...
        //  Gate processing may occur.
        //TODO

        //  Step 3 Translate prior L,BDI to E,LS,BDI, write that and PAR.PC + 1 to X(a)
        long basicVaddr = new VirtualAddress(lbjInfo._sourceBankLevel,
                                             lbjInfo._sourceBankDescriptorIndex,
                                             lbjInfo._offset).translateToBasicMode();
        long newXValue = basicVaddr;
        newXValue |= (long)(lbjInfo._brIndex & 03) << 33;
        int newParPC = lbjInfo._instructionProcessor.getProgramAddressRegister().getProgramCounter() + 1;
        newXValue = (newXValue & (0_777777_000000L)) | newParPC;

        //  Step 4 DB16 and access key -> User X(0)
        long x0Value = ip.getDesignatorRegister().getBasicModeEnabled() ? 0_400000_000000L : 0;
        x0Value |= ip.getIndicatorKeyRegister().getAccessKey();
        ip.getGeneralRegister(0).setW(x0Value);

        //  Step 5 If a gate was processed... fun stuff
        //TODO

        //  Step 6 ABT is updated, and PAR.PC is set to U(18:35), and ABT(n).Offset is set to 0
        //TODO

        //  Step 7 BankRegister is created for B(whatever)
        //TODO

        //  Step 8 DB31 is set appropriately
        //TODO

        //  Step 9 If target BD.G is set, or selection of base register error occurs,
        //  throw terminal addressing exception
        //TODO
    }

    /**
     * LxJ Case 2:Implements LxJ/CALL
     * @param lbjInfo
     * @throws MachineInterrupt
     */
    private void loadBankJumpCase2(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        //  Step 1 check for RCS overflow
        //TODO

        //  Step 2 Fetch prior L,BDI from ABT
        //TODO

        //  Step 3 Translate Xa basic mode Virtual Address to Source L,BDI including gate processing
        //TODO

        //  Step 4 Mixed mode transfer is to occur, B0 is to be loaded, while X(a) is to be marked void
        //TODO

        //  Step 5 Create RCS frame
        //TODO

        //  Step 6 DB16 and access key stored in user X0.
        //TODO

        //  Step 7 If a gate was processed, then do some fun stuff
        //TODO

        //  Step 8 PAR.L,BDI is updated, PAR.PC is set to U(18:35)
        //TODO

        //  Step 9 Appropriate values are loaded into B0
        //TODO

        //  Step 10 If final based bank has g bit set, throw terminal addressing interrupt
        //TODO
    }

    /**
     * LxJ Case 3:Implements LxJ/GOTO
     * @param lbjInfo
     * @throws MachineInterrupt
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
     * @param lbjInfo
     * @throws MachineInterrupt
     */
    private void loadBankJumpCase4(
        final LoadBankJumpInfo lbjInfo
    ) throws MachineInterrupt {
        //  Step 1 check for RCS overflow
        //TODO

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

        //  Step 7 DB31 is set
        //TODO

        //  Step 8 if BD.G or RCS.Trap, throw terminal addressing exception
        //TODO
    }

    /**
     * LxJ Case 5:Implements LxJ/RETURN to extended mode
     * @param lbjInfo
     * @throws MachineInterrupt
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
