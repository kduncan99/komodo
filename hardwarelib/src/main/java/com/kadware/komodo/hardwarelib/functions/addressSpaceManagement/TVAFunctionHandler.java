/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.addressSpaceManagement;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.GeneralRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.ReferenceViolationInterrupt;
import com.kadware.komodo.hardwarelib.misc.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the TVA instruction f=075 j=010
 */
@SuppressWarnings("Duplicates")
public class TVAFunctionHandler extends InstructionHandler {

    private class ScratchPad {
        final InstructionProcessor _instructionProcessor;
        final InstructionWord _instructionWord;
        final int _processorPrivilege;

        int _step = 1;
        boolean _done = false;

        GeneralRegister _xReg0;                         //  Reference to X(a)
        GeneralRegister _xReg1;                         //  Reference to X(a+1)

        VirtualAddress _virtualAddress;                 //  VAddr to be checked
        int _checkLevel;                                //  Level of bank to be checked
        int _checkBankDescriptorIndex;                  //  BDI to be checked
        int _checkOffset;                               //  offset to be checked
                                                        //      The above three items start out derived from _virtualAddress,
                                                        //      but may be changed if we find an indirect or gate bank.

        boolean _gatedBankFlag = false;                 //  true if we encounter a GATE bank
        boolean _recursionFlag = false;                 //  true if recursion is caused by an indirect source bank
        BankDescriptor _targetBankDescriptor = null;    //  BD for the final bank referred to by the original virtual address

        //  options
        boolean _addressTranslationRequested;           //  tr
        boolean _alternateAccessKeyEnable;              //  aake
        AccessInfo _effectiveAccessKey;                 //  aak if aake, else key from indicator/key register
        boolean _enterAccessRequired;                   //  e
        boolean _queueBankRepositoryBDCheckRequested;   //  tqbr
        boolean _queueBDCheckRequested;                 //  tq
        boolean _readAcccessRequired;                   //  r
        boolean _writeAccessRequired;                   //  w

        //  flags and such
        boolean _accessDenied = false;                  //  ad
        boolean _gateViolation = false;                 //  gv
        boolean _generalFault = false;                  //  g
        boolean _invalidRealAddress = false;            //  ia
        boolean _limitsViolation = false;               //  lim
        boolean _qbrFound = false;                      //  qbrf
        boolean _skipNextInstruction = true;
        AbsoluteAddress _realAddress = null;

        ScratchPad(
            final InstructionProcessor ip,
            final InstructionWord iw
        ) {
            _instructionProcessor = ip;
            _instructionWord = iw;
            _processorPrivilege = _instructionProcessor.getDesignatorRegister().getProcessorPrivilege();
        }
    }

    private interface IStepHandler {
        void invoke(
            final ScratchPad scratchPad
        ) throws MachineInterrupt,
                 UnresolvedAddressException;
    }

    /**
     * Sets up certain fields in the ScratchPad object for subsequent processing
     */
    private static class Step1Handler implements IStepHandler {

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            //  Get the virtual address to be checked from X(a) and the various flags and alternate key from X(a+1).
            //  If a==15, then X(a) is A(3), and X(a+1) is A(4).
            int aField = (int) scratchPad._instructionWord.getA();
            scratchPad._virtualAddress = new VirtualAddress(scratchPad._instructionProcessor.getExecOrUserXRegister(aField).getW());
            scratchPad._checkLevel = scratchPad._virtualAddress.getLevel();
            scratchPad._checkBankDescriptorIndex = scratchPad._virtualAddress.getBankDescriptorIndex();
            scratchPad._checkOffset = scratchPad._virtualAddress.getOffset();

            long options = (aField < 15) ?
                           scratchPad._instructionProcessor.getExecOrUserXRegister(aField + 1).getW() :
                           scratchPad._instructionProcessor.getExecOrUserARegister(4).getW();

            //  Get the options
            scratchPad._queueBDCheckRequested = (options & 0_020000_000000L) != 0;
            scratchPad._alternateAccessKeyEnable = (options & 0_004000_000000L) != 0;
            scratchPad._queueBankRepositoryBDCheckRequested = (options & 0_002000_000000L) != 0;
            scratchPad._addressTranslationRequested = (options & 0_001000_000000L) != 0;
            scratchPad._enterAccessRequired = (options & 0_000400_000000L) != 0;
            scratchPad._readAcccessRequired = (options & 0_000200_000000L) != 0;
            scratchPad._writeAccessRequired = (options & 0_000100_000000L) != 0;

            if (scratchPad._alternateAccessKeyEnable) {
                scratchPad._effectiveAccessKey = new AccessInfo(options & 0_777777);
            } else {
                scratchPad._effectiveAccessKey = scratchPad._instructionProcessor.getIndicatorKeyRegister().getAccessInfo();
            }

            scratchPad._step = 2;
        }
    }

    /**
     * Step 2 - Decide whether to do step 5 (callOrQBankVAddrTranslate) or step 3 (lbuVAddrTranslateOrCheckQBRAccess)
     */
    private static class Step2Handler implements IStepHandler {

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            scratchPad._step = scratchPad._enterAccessRequired ? 5 : 3;
        }
    }

    /**
     * Step 3 - Interprets the virtual address as if we were doing an LBU, or checks QBRAccess
     */
    private static class Step3Handler implements IStepHandler {

        private void doDefault(
            final ScratchPad scratchPad,
            final BankDescriptor bankDescriptor
        ) {
            if (bankDescriptor.getGeneralFault()) {
                scratchPad._skipNextInstruction = false;
                scratchPad._generalFault = true;
            }
            scratchPad._step = 4;
        }

        private void doIndirect(
            final ScratchPad scratchPad,
            final BankDescriptor bankDescriptor
        ) throws MachineInterrupt {
            if (bankDescriptor.getGeneralFault()) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GBitSetIndirect,
                                                       scratchPad._checkLevel,
                                                       scratchPad._checkBankDescriptorIndex);
            }

            int targetLBDI = bankDescriptor.getTargetLBDI();
            scratchPad._checkLevel = targetLBDI >> 15;
            scratchPad._checkBankDescriptorIndex = targetLBDI & 077777;
            scratchPad._recursionFlag = true;
        }

        private void doQueue(
            final ScratchPad scratchPad
        ) {
            scratchPad._step = 4;
        }

        private void doQueueBankRepository(
            final ScratchPad scratchPad
        ) {
            if (!scratchPad._queueBankRepositoryBDCheckRequested) {
                scratchPad._skipNextInstruction = false;
                scratchPad._accessDenied = true;
                scratchPad._invalidRealAddress = true;
                scratchPad._qbrFound = true;
                scratchPad._step = 9;
            } else {
                //  manual access check, then step 8
                scratchPad._qbrFound = true;
                boolean accessAllowed = checkAccess(false,
                                                    scratchPad._readAcccessRequired,
                                                    scratchPad._writeAccessRequired,
                                                    scratchPad._effectiveAccessKey,
                                                    scratchPad._targetBankDescriptor.getAccessLock(),
                                                    scratchPad._targetBankDescriptor.getGeneraAccessPermissions(),
                                                    scratchPad._targetBankDescriptor.getSpecialAccessPermissions());
                if (!accessAllowed) {
                    scratchPad._skipNextInstruction = false;
                    scratchPad._accessDenied = true;
                }
                scratchPad._step = 8;
            }
        }

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) throws MachineInterrupt {
            //  Go find the bank descriptor for the given level/BDI.
            //  If we don't get one, we should move on to step 9.
            BankDescriptor bd = findBankDescriptor(scratchPad);
            if (bd == null) {
                scratchPad._skipNextInstruction = false;
                scratchPad._invalidRealAddress = true;
                scratchPad._step = 9;
                return;
            }

            switch (bd.getBankType()) {
                case Gate:
                    scratchPad._gatedBankFlag = true;
                    break;

                case Indirect:
                    doIndirect(scratchPad, bd);
                    break;

                case QueueRepository:
                    doQueueBankRepository(scratchPad);
                    break;

                case Queue:
                    doQueue(scratchPad);
                    break;

                default:
                    doDefault(scratchPad, bd);
                    break;
            }
        }
    }

    /**
     * Step 4 - Access check for LBU situation.
     * Compare target BD access control depending on the GAP or SAP
     * as determined by IP or AAKE access key comparison to the BD lock.
     */
    private static class Step4Handler implements IStepHandler {

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            boolean accessAllowed = checkAccess(false, scratchPad._readAcccessRequired,
                                                scratchPad._writeAccessRequired,
                                                scratchPad._effectiveAccessKey,
                                                scratchPad._targetBankDescriptor.getAccessLock(),
                                                scratchPad._targetBankDescriptor.getGeneraAccessPermissions(),
                                                scratchPad._targetBankDescriptor.getSpecialAccessPermissions());
            if (!accessAllowed) {
                scratchPad._skipNextInstruction = false;
                scratchPad._accessDenied = true;
            }

            scratchPad._step =  7;
        }
    }

    /**
     * Step 5 - CALL or Queue bank vaddr translate.
     * Interpret the Bank Descriptor found for the vaddr as if it were for a CALL instruction.
     */
    private static class Step5Handler implements IStepHandler {

        private void doGate(
            final ScratchPad scratchPad,
            final BankDescriptor bankDescriptor
        ) throws MachineInterrupt {
            scratchPad._gatedBankFlag = true;
            if (bankDescriptor.getGeneralFault()) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GBitSetIndirect,
                                                       scratchPad._checkLevel,
                                                       scratchPad._checkBankDescriptorIndex);
            }

            if ((scratchPad._checkOffset < bankDescriptor.getLowerLimitNormalized())
                || (scratchPad._checkOffset > bankDescriptor.getUpperLimitNormalized())) {
                scratchPad._skipNextInstruction = false;
                scratchPad._invalidRealAddress = true;
                scratchPad._step = 9;
                return;
            }

            boolean enterAccess = checkAccess(true,
                                              false,
                                              false,
                                              scratchPad._effectiveAccessKey,
                                              bankDescriptor.getAccessLock(),
                                              bankDescriptor.getGeneraAccessPermissions(),
                                              bankDescriptor.getSpecialAccessPermissions());
            if (!enterAccess) {
                scratchPad._skipNextInstruction = false;
                scratchPad._gateViolation = true;
                scratchPad._invalidRealAddress = true;
                scratchPad._step = 9;
                return;
            }

            int targetLBDI = bankDescriptor.getTargetLBDI();
            scratchPad._checkLevel = targetLBDI >> 15;
            scratchPad._checkBankDescriptorIndex = targetLBDI & 077777;
            scratchPad._recursionFlag = true;
        }

        private void doIndirect(
            final ScratchPad scratchPad,
            final BankDescriptor bankDescriptor
        ) throws MachineInterrupt {
            if (bankDescriptor.getGeneralFault()) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GBitSetIndirect,
                                                       scratchPad._checkLevel,
                                                       scratchPad._checkBankDescriptorIndex);
            }

            int targetLBDI = bankDescriptor.getTargetLBDI();
            scratchPad._checkLevel = targetLBDI >> 15;
            scratchPad._checkBankDescriptorIndex = targetLBDI & 077777;
            scratchPad._recursionFlag = true;
        }

        private void doQueue(
            final ScratchPad scratchPad,
            final BankDescriptor bankDescriptor
        ) {
            if (!scratchPad._queueBDCheckRequested) {
                scratchPad._skipNextInstruction = false;
                scratchPad._accessDenied = true;
                scratchPad._invalidRealAddress = true;
                scratchPad._step = 9;
            } else {
                boolean accessAllowed = checkAccess(false,
                                                    scratchPad._readAcccessRequired,
                                                    scratchPad._writeAccessRequired,
                                                    scratchPad._effectiveAccessKey,
                                                    bankDescriptor.getAccessLock(),
                                                    bankDescriptor.getGeneraAccessPermissions(),
                                                    bankDescriptor.getSpecialAccessPermissions());
                if (!accessAllowed) {
                    scratchPad._skipNextInstruction = false;
                    scratchPad._accessDenied = true;
                }
            }
        }

        private void doQueueBankRepository(
            final ScratchPad scratchPad
        ) {
            scratchPad._skipNextInstruction = false;
            scratchPad._accessDenied = true;
            scratchPad._invalidRealAddress = true;
            scratchPad._qbrFound = true;
            scratchPad._step = 9;
        }

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) throws MachineInterrupt {
            //  Go find the bank descriptor for the given level/BDI.
            //  If we don't get one, we should move on to step 9.
            BankDescriptor bd = findBankDescriptor(scratchPad);
            if (bd == null) {
                scratchPad._skipNextInstruction = false;
                scratchPad._invalidRealAddress = true;
                scratchPad._step = 9;
                return;
            }

            switch (bd.getBankType()) {
                case Gate:
                    doGate(scratchPad, bd);
                    return;

                case Indirect:
                    doIndirect(scratchPad, bd);
                    return;

                case Queue:
                    doQueue(scratchPad, bd);
                    break;  //  NOTE: NOT return, drop through to post-switch code

                case QueueRepository:
                    doQueueBankRepository(scratchPad);
                    return;
            }

            boolean enterAccess;
            if (!scratchPad._gatedBankFlag && (bd.getBankType() != BankDescriptor.BankType.BasicMode)) {
                enterAccess = true;
            } else {
                enterAccess = checkAccess(true,
                                          false,
                                          false,
                                          scratchPad._effectiveAccessKey,
                                          bd.getAccessLock(),
                                          bd.getGeneraAccessPermissions(),
                                          bd.getSpecialAccessPermissions());
            }

            if (!enterAccess) {
                scratchPad._skipNextInstruction = false;
                scratchPad._accessDenied = true;
            } else if ((bd.getBankType() != BankDescriptor.BankType.Queue) && (bd.getGeneralFault())) {
                scratchPad._skipNextInstruction = false;
                scratchPad._generalFault = true;
            }

            scratchPad._step = 7;
        }
    }

    /**
     * Step 7 - limits check
     */
    private static class Step7Handler implements IStepHandler {

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            BankDescriptor bd = scratchPad._targetBankDescriptor;
            int offset = scratchPad._virtualAddress.getOffset();
            boolean limitsBad = (offset < bd.getLowerLimitNormalized()) || (offset > bd.getUpperLimitNormalized());
            boolean accessBad = bd.getLargeBank() && scratchPad._enterAccessRequired;
            if (limitsBad || accessBad) {
                scratchPad._skipNextInstruction = false;
                scratchPad._limitsViolation = true;
                scratchPad._accessDenied = false;
                scratchPad._invalidRealAddress = true;
            }

            scratchPad._step = limitsBad ? 9 : 8;
        }
    }

    /**
     * Step 8 - Translates the virtual address to a real address.
     */
    private static class Step8Handler implements IStepHandler {

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            //  For PP > 1, the address is never returned, and thus does not need to be determined
            if (scratchPad._addressTranslationRequested && (scratchPad._processorPrivilege < 2)) {
                scratchPad._realAddress =
                    scratchPad._targetBankDescriptor.getBaseAddress().addOffset(scratchPad._virtualAddress.getOffset());
            } else {
                scratchPad._invalidRealAddress = true;
            }

            scratchPad._step = 9;
        }
    }

    /**
     * Step 9 - write status registers if processor privilege is sufficient.
     */
    private static class Step9Handler implements IStepHandler {

        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            if (scratchPad._processorPrivilege > 1) {
                scratchPad._xReg1.setW(0);
            } else {
                long value0 = (scratchPad._accessDenied ? 0_400000_000000L : 0)
                              | (scratchPad._invalidRealAddress ? 0_200000_000000L : 0)
                              | (scratchPad._gateViolation ? 0_100000_000000L : 0)
                              | (scratchPad._limitsViolation ? 0_040000_000000L : 0)
                              | (scratchPad._generalFault ? 0_020000_000000L : 0)
                              | (scratchPad._qbrFound ? 0_004000_000000L : 0);
                long value1 = 0;
                if (scratchPad._realAddress != null) {  //  implies _invalidRealAddress is false and _addressTranslationRequested is true
                    value0 |= scratchPad._realAddress._segment & 0_001777_777777L;
                    value1 = (((long) scratchPad._realAddress._upi) << 32) | scratchPad._realAddress._offset;
                }

                scratchPad._xReg0.setW(value0);
                scratchPad._xReg1.setW(value1);
            }

            scratchPad._step = 10;
        }
    }

    /**
     * Skips NI if required
     */
    private static class Step10Handler implements IStepHandler {
        @Override
        public void invoke(
            final ScratchPad scratchPad
        ) {
            if (scratchPad._skipNextInstruction) {
                int nextAddr = scratchPad._instructionProcessor.getProgramAddressRegister().getProgramCounter() + 1;
                scratchPad._instructionProcessor.setProgramCounter(nextAddr, false);
            }

            scratchPad._done = true;
        }
    }

    /**
     * Check either GAP or SAP permissions, as determined by the comparison of the key and lock,
     * and based on the read and write flags specified.  Note that we always return true if both flags are false.
     * Corresponds to step 4, but also used elsewhere.
     */
    private static boolean checkAccess(
        final boolean enterRequired,
        final boolean readRequired,
        final boolean writeRequired,
        final AccessInfo accessKey,
        final AccessInfo accessLock,
        final AccessPermissions gapPermissions,
        final AccessPermissions sapPermissions
    ) {
        boolean useSAP =
            (accessKey._ring > accessLock._ring)
            || ((accessKey._ring == accessLock._ring) && (accessKey._domain == accessLock._domain));
        AccessPermissions selectedPermissions = useSAP ? sapPermissions : gapPermissions;
        return !((enterRequired && !selectedPermissions._enter)
                 || (readRequired && !selectedPermissions._read)
                 || (writeRequired && !selectedPermissions._write));
    }

    /**
     * Corresponds to the first part of both steps 3 and 5, and is called from the respective corresponding methods.
     * @return bank descriptor corresponding to the L,BDI in the virtual address to be tested,
     *          null if BDI < 0,32 and this is not a recursion
     * @throws AddressingExceptionInterrupt if this is a recursion and either
     *                                          a) level,bdi is < 0,32, or
     *                                          b) The BDT for the given level,bdi cannot be accessed or does not exist
     */
    private static BankDescriptor findBankDescriptor(
        final ScratchPad scratchPad
    ) throws AddressingExceptionInterrupt {
        if ((scratchPad._checkLevel == 0) && (scratchPad._checkBankDescriptorIndex < 32)) {
            if (scratchPad._recursionFlag) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       scratchPad._checkLevel,
                                                       scratchPad._checkBankDescriptorIndex);
            } else {
                return null;
            }
        }

        int bdtRegIndex = scratchPad._checkLevel + 16;
        BaseRegister bdtReg = scratchPad._instructionProcessor.getBaseRegister(bdtRegIndex);
        int bdOffset = 8 * scratchPad._checkBankDescriptorIndex;
        try {
            bdtReg.checkAccessLimits(bdOffset,
                                     8,
                                     true,
                                     false,
                                     scratchPad._instructionProcessor.getIndicatorKeyRegister().getAccessInfo());
        } catch (ReferenceViolationInterrupt ex) {
            if (scratchPad._recursionFlag) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       scratchPad._checkLevel,
                                                       scratchPad._checkBankDescriptorIndex);
            } else {
                return null;
            }
        }

        return new BankDescriptor(bdtReg._storage, bdOffset);
    }

    private static final Map<Integer, IStepHandler> _stepHandlers = new HashMap<>();
    static {
        _stepHandlers.put(1, new Step1Handler());
        _stepHandlers.put(2, new Step2Handler());
        _stepHandlers.put(3, new Step3Handler());
        _stepHandlers.put(4, new Step4Handler());
        _stepHandlers.put(5, new Step5Handler());
        //  Step 6 is implemented everywhere necessary by simply throwing an exception
        _stepHandlers.put(7, new Step7Handler());
        _stepHandlers.put(8, new Step8Handler());
        _stepHandlers.put(9, new Step9Handler());
        _stepHandlers.put(10, new Step10Handler());
    }

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Instructions like this are why CISC is a thing...
        ScratchPad sp = new ScratchPad(ip, iw);
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getBasicModeEnabled() && (sp._processorPrivilege > 0)) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  U is computed and summarily ignored
        ip.getOperand(false, false, false, false);

        while (!sp._done) {
            _stepHandlers.get(sp._step).invoke(sp);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.TVA; }
}
