/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;

public class BankManipulator {

    private static class BankManipulationInfo {
        //  Determined at construction
        private boolean _callOperation = false;                     //  true if CALL, LOCL, LxJ, LxJ/CALL, or interrupt
        private final InstructionHandler.Instruction _instruction;  //  null if this is for interrupt handling
        private final InstructionProcessor _instructionProcessor;   //  processor invoking this action
        private final MachineInterrupt _interrupt;                  //  null if invoked by instruction handling
        private boolean _loadInstruction = false;                   //  true if this is LAE, LBE, or LBU
        private boolean _lxjInstruction = false;                    //  true if this is for an LxJ instruction
        private int _lxjBankSelector;                               //  BDR (offset by 12) for LxJ instructions
        private int _lxjInterfaceSpec;                              //  IS for LxJ instructions
        private IndexRegister _lxjXRegister;                        //  X(a) for LxJ instructions
        private final long _operand;
        private boolean _returnOperation = false;                   //  true if RTN, LxJ/RTN

        private final ActiveBaseTableEntry[] _activeBaseTableEntries;
        private final BaseRegister[] _baseRegisters;
        private final InstructionWord _currentInstruction;
        private final DesignatorRegister _designatorRegister;
        private final IndicatorKeyRegister _indicatorKeyRegister;

        private int _nextStep = 1;

        //  Determined at some point *after* construction
        private int _baseRegisterIndex = 0;             //  base register to be loaded
        private boolean _gateProcessed = false;         //  true if we process a gate
        private int _priorBankLevel = 0;                //  For CALL and LxJ/CALL
        private int _priorBankDescriptorIndex = 0;      //  as above

        //  Following are for any returns (i.e., RTN or LxJ/RTN)
        private int _rcsBasicModeBankIndex = 0;         //  Added to 12, indicates the BR to which we return control
        private DesignatorRegister _rcsDB12_17 = null;  //  Designator register bits which should be reinstated upon return
        private int _rcsAccessKey = 0;                  //  Access Key which should be reinstated upon return
        private boolean _rcsTrap = false;               //  true if trap bit was set in the RCS

        private int _sourceBankLevel = 0;                       //  L portion of source bank L,BDI
        private int _sourceBankDescriptorIndex = 0;             //  BDI portion of source bank L,BDI
        private int _sourceBankOffset = 0;                      //  offset value accompanying source bank specification
        private BankDescriptor _sourceBankDescriptor = null;    //  BD for source bank

        private int _targetBankLevel = 0;                       //  L portion of target bank L,BDI
        private int _targetBankDescriptorIndex = 0;             //  BDI portion of target bank L,BDI
        private int _targetBankOffset = 0;                      //  offset value accompanying target bank specification
        private BankDescriptor _targetBankDescriptor = null;    //  BD for target bank - from step 7, having this null implies a void bank

        private BankManipulationInfo(
            final InstructionProcessor instructionProcessor,
            final InstructionHandler.Instruction instruction,
            final long operand,
            final MachineInterrupt interrupt
        ) {
            _instruction = instruction;
            _instructionProcessor = instructionProcessor;
            _interrupt = interrupt;
            _operand = operand;

            _activeBaseTableEntries = _instructionProcessor.getActiveBaseTableEntries();
            _baseRegisters = _instructionProcessor.getBaseRegisters();
            _currentInstruction = _instructionProcessor.getCurrentInstruction();
            _designatorRegister = _instructionProcessor.getDesignatorRegister();
            _indicatorKeyRegister = instructionProcessor.getIndicatorKeyRegister();

            if (_interrupt != null) {
                _callOperation = true;
            } else {
                _loadInstruction = (_instruction == InstructionHandler.Instruction.LAE)
                    || (_instruction == InstructionHandler.Instruction.LBE)
                    || (_instruction == InstructionHandler.Instruction.LBU);
                _lxjInstruction = (_instruction == InstructionHandler.Instruction.LBJ)
                    || (_instruction == InstructionHandler.Instruction.LDJ)
                    || (_instruction == InstructionHandler.Instruction.LIJ);

                int aField = (int) _currentInstruction.getA();

                if (_lxjInstruction) {
                    _lxjXRegister = instructionProcessor.getExecOrUserXRegister(aField);
                    _lxjInterfaceSpec = (int) (_lxjXRegister.getW() >> 30) & 03;
                    _lxjBankSelector = (int) (_lxjXRegister.getW() >> 33) & 03;
                }

                _callOperation = (instruction == InstructionHandler.Instruction.CALL)
                    || (instruction == InstructionHandler.Instruction.LOCL)
                    || (_lxjInstruction && (_lxjInterfaceSpec < 2));
                //  Note that UR is not considered a return operation
                _returnOperation = (instruction == InstructionHandler.Instruction.RTN)
                    || (_lxjInstruction && (_lxjInterfaceSpec == 2));
            }
        }
    }

    /**
     * Interface for bank manipulation step handlers
     */
    private interface BankManipulationStep {
        void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt;
    }

    /**
     * Sanity check for a couple of different categories of instructions
     */
    private static class BankManipulationStep1 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt if IS is 3 for LxJ instructions
         * @throws InvalidInstructionInterrupt if B0 or B1 are the target for an LBU instruction
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt {
            if (bmInfo._instruction != null) {
                if ((bmInfo._instruction == InstructionHandler.Instruction.LBU)
                    && (bmInfo._currentInstruction.getA() < 2)) {
                    throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidBaseRegister);
                }

                if (bmInfo._lxjInterfaceSpec == 3) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidISValue,
                                                           0,
                                                           0);
                }
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Retrieve prior L,BDI for any instruction which will result in requiring a return address/bank
     */
    private static class BankManipulationStep2 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._instruction != null) {
                if (bmInfo._instruction == InstructionHandler.Instruction.CALL) {
                    bmInfo._priorBankLevel = (int) (bmInfo._currentInstruction.getW() >> 33);
                    bmInfo._priorBankDescriptorIndex =
                        (int) (bmInfo._currentInstruction.getW() >> 18) & 077777;
                } else if ((bmInfo._lxjInstruction) && (bmInfo._lxjInterfaceSpec < 2)) {
                    //  We're supposed to be here for normal LxJ and for LxJ/CALL, but we also catch LxJ/GOTO
                    //  (interfaceSpec == 1 and target BD is extended with enter access, or gate)
                    //  Because we must do this for IS == 1 and source BD is basic, and it is too early in
                    //  the algorithm to know the source BD bank type.
                    int abtx;
                    if (bmInfo._instruction == InstructionHandler.Instruction.LBJ) {
                        abtx = bmInfo._lxjBankSelector + 12;
                    } else if (bmInfo._instruction == InstructionHandler.Instruction.LDJ) {
                        abtx = bmInfo._designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                    } else {
                        abtx = bmInfo._designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
                    }

                    bmInfo._priorBankLevel = bmInfo._activeBaseTableEntries[abtx].getLevel();
                    bmInfo._priorBankDescriptorIndex = bmInfo._activeBaseTableEntries[abtx].getBDI();
                }
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Determine source level, BDI, and offset.
     *      For transfers, this is the address to which we jump.
     *      For loads, L,BDI is the bank and offset is a subset.
     * (mostly... there's actually no requirement for LxJ instructions (for example) to jump to the bank which they base).
     * This is a little tricky as the algorithm seemingly requires us to know (in some cases) whether the
     * target bank is extended or basic mode, and we've not yet begun to determine the target bank.
     * In point of fact, the decision tree here does not actually require knowledge of the target bank type.
     */
    private static class BankManipulationStep3 implements BankManipulationStep {

        /**
         * @throws  RCSGenericStackUnderflowOverflowInterrupt if the RCS stack doesn't have a suitably-sized frame, or is void.
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws RCSGenericStackUnderflowOverflowInterrupt {
            if (bmInfo._interrupt != null) {
                //  source L,BDI,Offset comes from the interrupt vector...
                //  The bank described by B16 begins with 64 contiguous words, indexed by interrupt class (of which there are 64).
                //  Each word is a Program Address Register word, containing the L,BDI,Offset of the interrupt handling routine
                //  Make sure B16 is valid before dereferencing through it.
                if (bmInfo._baseRegisters[InstructionProcessor.L0_BDT_BASE_REGISTER]._voidFlag) {
                    bmInfo._instructionProcessor.stop(InstructionProcessor.StopReason.L0BaseRegisterInvalid, 0);
                    bmInfo._nextStep = 0;
                    return;
                }

                //  intOffset is the offset from the start of the level 0 BDT, to the vector we're interested in.
                ArraySlice bdtLevel0 = bmInfo._baseRegisters[InstructionProcessor.L0_BDT_BASE_REGISTER]._storage;
                int intOffset = bmInfo._interrupt.getInterruptClass().getCode();
                if (intOffset >= bdtLevel0.getSize()) {
                    bmInfo._instructionProcessor.stop(InstructionProcessor.StopReason.InterruptHandlerOffsetOutOfRange, 0);
                    bmInfo._nextStep = 0;
                    return;
                }

                long lbdiOffset = bdtLevel0.get(intOffset);
                bmInfo._sourceBankLevel = (int) lbdiOffset >> 33;
                bmInfo._sourceBankDescriptorIndex = (int) (lbdiOffset >> 18) & 077777;
                bmInfo._sourceBankOffset = (int) lbdiOffset & 0777777;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.UR) {
                //  source L,BDI comes from operand L,BDI
                //  offset comes from operand offset (PAR.PC)... what does this mean?
                //  TODO is this the same as the default case below?
            } else if (bmInfo._returnOperation) {
                //  source L,BDI,Offset comes from RCS
                //  This is where we pop an RCS frame and grab the relevant fields therefrom.
                BaseRegister rcsBReg = bmInfo._instructionProcessor.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
                if (rcsBReg._voidFlag) {
                    throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                        InstructionProcessor.RCS_BASE_REGISTER,
                                                                        0);
                }

                IndexRegister rcsXReg =
                    bmInfo._instructionProcessor.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
                int framePointer = (int) rcsXReg.getXM() + 2;
                if (framePointer > rcsBReg._upperLimitNormalized) {
                    throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                        InstructionProcessor.RCS_BASE_REGISTER,
                                                                        framePointer);
                }

                long frame0 = rcsBReg._storage.get(framePointer);
                long frame1 = rcsBReg._storage.get(framePointer + 1);
                rcsXReg.setXM(framePointer);

                bmInfo._sourceBankLevel = (int) (frame0 >> 33);
                bmInfo._sourceBankDescriptorIndex = (int) (frame0 >> 18) & 077777;
                bmInfo._sourceBankOffset = (int) frame0 & 0777777;
                bmInfo._rcsTrap = (frame1 & 0_400000_000000L) != 0;
                bmInfo._rcsBasicModeBankIndex = (int) (frame1 >> 24) & 03;
                bmInfo._rcsDB12_17 = new DesignatorRegister(frame1 & 077_777777);
                bmInfo._rcsAccessKey = (int) frame1 & 0777777;
            } else if (bmInfo._lxjInstruction) {
                //  source L,BDI comes from basic mode X(a) E,LS,BDI
                //  offset comes from operand
                long bmSpec = bmInfo._lxjXRegister.getW();
                boolean execFlag = (bmSpec & 0_400000_000000L) != 0;
                boolean levelSpec = (bmSpec & 0_040000_000000L) != 0;
                bmInfo._sourceBankLevel = execFlag ? (levelSpec ? 0 : 2) : (levelSpec ? 6 : 4);
                bmInfo._sourceBankDescriptorIndex = (int) ((bmSpec >> 18) & 077777);
                bmInfo._sourceBankOffset = (int) bmInfo._operand & 0777777;
            } else {
                //  source L,BDI,Offset comes from operand
                bmInfo._sourceBankLevel = (int) (bmInfo._operand >> 33) & 03;
                bmInfo._sourceBankDescriptorIndex = (int) (bmInfo._operand >> 18) & 077777;
                bmInfo._sourceBankOffset = (int) bmInfo._operand & 0777777;
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Ensure L,BDI is valid.  If L,BDI is in the range of 0,1:0,31 we throw an AddressingException.
     * If we are handling an interrupt, we stop the processor instead of throwing, and discard further processing.
     */
    private static class BankManipulationStep4 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt if the source L,BDI is invalid
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt {
            if ((bmInfo._sourceBankLevel == 0)
                && ((bmInfo._sourceBankDescriptorIndex > 0) && (bmInfo._sourceBankDescriptorIndex < 32))) {
                if (bmInfo._interrupt != null) {
                    bmInfo._instructionProcessor.stop(
                        InstructionProcessor.StopReason.InterruptHandlerInvalidLevelBDI,
                        (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                    bmInfo._nextStep = 0;
                    return;
                } else {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                           bmInfo._sourceBankLevel,
                                                           bmInfo._sourceBankDescriptorIndex);
                }
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Void bank handling.  IF void:
     *      For loads, we skip to step 10.
     *      For interrupt handling we stop the processor
     *      For transfers, we either throw an addressing exception or skip to step 10.
     */
    private static class BankManipulationStep5 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt if a void bank is specified where it is not allowed
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt {
            if ((bmInfo._sourceBankLevel == 0) && (bmInfo._sourceBankDescriptorIndex == 0)) {
                if (bmInfo._interrupt != null) {
                    bmInfo._instructionProcessor.stop(InstructionProcessor.StopReason.InterruptHandlerInvalidLevelBDI, 0);
                    bmInfo._nextStep = 0;
                    return;
                } else if (bmInfo._loadInstruction) {
                    bmInfo._nextStep = 10;
                    return;
                } else if (bmInfo._returnOperation || (bmInfo._instruction == InstructionHandler.Instruction.UR)) {
                    if (!bmInfo._rcsDB12_17.getBasicModeEnabled()) {
                        //  return to extended mode - addressing exception
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    } else {
                        //  return to basic mode - void bank
                        bmInfo._nextStep = 10;
                        return;
                    }
                }
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * At this point, source L,BDI is greater than 0,31.  Go get the corresponding bank descriptor
     */
    private static class BankManipulationStep6 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt if we cannot find the bank descriptor for the source L,BDI
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt {
            try {
                bmInfo._sourceBankDescriptor =
                    bmInfo._instructionProcessor.findBankDescriptor(bmInfo._sourceBankLevel, bmInfo._sourceBankDescriptorIndex);
            } catch (AddressingExceptionInterrupt ex) {
                if (bmInfo._interrupt != null) {
                    //  this is serious - cannot continue
                    bmInfo._instructionProcessor.stop(
                        InstructionProcessor.StopReason.InterruptHandlerInvalidLevelBDI,
                        (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                    bmInfo._nextStep = 0;
                } else {
                    throw ex;
                }
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Examines the source bank type to determine what should be done next
     */
    private static class BankManipulationStep7 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt for invalid bank type
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt {
            //  most likely case...
            bmInfo._targetBankLevel = bmInfo._sourceBankLevel;
            bmInfo._targetBankDescriptorIndex = bmInfo._sourceBankDescriptorIndex;
            bmInfo._targetBankOffset = bmInfo._sourceBankOffset;
            bmInfo._targetBankDescriptor = bmInfo._sourceBankDescriptor;
            bmInfo._nextStep = 10;

            switch (bmInfo._sourceBankDescriptor.getBankType()) {
                case ExtendedMode:
                    break;

                case BasicMode:
                    if (bmInfo._interrupt != null) {
                        bmInfo._instructionProcessor.stop(
                            InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                            (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                        bmInfo._nextStep = 0;
                    } else if ((bmInfo._instruction == InstructionHandler.Instruction.LBU)
                        && (bmInfo._designatorRegister.getProcessorPrivilege() > 1)
                        && !bmInfo._sourceBankDescriptor.getGeneraAccessPermissions()._enter
                        && !bmInfo._sourceBankDescriptor.getSpecialAccessPermissions()._enter) {
                        bmInfo._targetBankDescriptor = null;
                    } else if (((bmInfo._instruction == InstructionHandler.Instruction.RTN) || bmInfo._lxjInstruction)
                        && !bmInfo._rcsDB12_17.getBasicModeEnabled()) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    }
                    break;

                case Gate:
                    if (bmInfo._interrupt != null) {
                        bmInfo._instructionProcessor.stop(
                            InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                            (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                        bmInfo._nextStep = 0;
                    } else if (bmInfo._callOperation || (bmInfo._instruction == InstructionHandler.Instruction.GOTO)) {
                        bmInfo._nextStep = 9;
                    } else if (bmInfo._returnOperation || (bmInfo._instruction == InstructionHandler.Instruction.UR)) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    }
                    break;

                case Indirect:
                    if (bmInfo._interrupt != null) {
                        bmInfo._instructionProcessor.stop(
                            InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                            (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                        bmInfo._nextStep = 0;
                    } else if ((bmInfo._callOperation) || (bmInfo._loadInstruction)){
                        bmInfo._nextStep = 8;
                    } else if (bmInfo._returnOperation
                        || (bmInfo._instruction == InstructionHandler.Instruction.LAE)
                        || (bmInfo._instruction == InstructionHandler.Instruction.UR)) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    }
                    break;

                case QueueRepository:
                    if (bmInfo._interrupt != null) {
                        bmInfo._instructionProcessor.stop(
                            InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                            (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                        bmInfo._nextStep = 0;
                    } else {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    }
                    break;

                case Queue:
                default:    //  reserved
                    if (bmInfo._interrupt != null) {
                        bmInfo._instructionProcessor.stop(
                            InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                            (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                        bmInfo._nextStep = 0;
                    } else if (!bmInfo._loadInstruction) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    }
            }
        }
    }

    /**
     * Indirect bank processing - if we get here we need to process an indirect bank
     */
    private static class BankManipulationStep8 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt for invalid indirected-to bank type, or invalid indirected-to Level/BDI,
         *                                      or general fault set on indirected-to bank
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt {
            if (bmInfo._sourceBankDescriptor.getGeneralFault()) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GBitSetIndirect,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            } else if ((bmInfo._sourceBankLevel == 0) && (bmInfo._sourceBankDescriptorIndex < 32)) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  Assume indirected-to bank becomes target, and we move on to step 10
            try {
                int targetLBDI = bmInfo._sourceBankDescriptor.getTargetLBDI();
                bmInfo._targetBankLevel = targetLBDI >> 15;
                bmInfo._targetBankDescriptorIndex = targetLBDI & 077777;
                bmInfo._targetBankOffset = bmInfo._sourceBankOffset;
                bmInfo._targetBankDescriptor =
                    bmInfo._instructionProcessor.findBankDescriptor(bmInfo._targetBankLevel, bmInfo._targetBankDescriptorIndex);
            } catch (AddressingExceptionInterrupt ex) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       bmInfo._targetBankLevel,
                                                       bmInfo._targetBankDescriptorIndex);
            }

            bmInfo._nextStep = 10;

            switch (bmInfo._targetBankDescriptor.getBankType()) {
                case BasicMode:
                    //  when PP>1 and GAP.E == 0 and SAP.E == 0, do void bank (set target bd null)
                    if ((bmInfo._designatorRegister.getProcessorPrivilege() > 1)
                        && !bmInfo._targetBankDescriptor.getGeneraAccessPermissions()._enter
                        && !bmInfo._targetBankDescriptor.getSpecialAccessPermissions()._enter) {
                        bmInfo._targetBankDescriptor = null;
                    }
                    break;

                case Gate:
                    if (bmInfo._lxjInstruction || bmInfo._callOperation) {
                        //  do gate processing
                        bmInfo._nextStep = 9;
                        break;
                    }

                case Indirect:
                case QueueRepository:
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           bmInfo._targetBankLevel,
                                                           bmInfo._targetBankDescriptorIndex);

                case Queue:
                default:
                    if (!bmInfo._loadInstruction) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                               bmInfo._targetBankLevel,
                                                               bmInfo._targetBankDescriptorIndex);
                    }
                    break;
            }
        }
    }

    /**
     * Gate bank processing - if we get here we need to process a gate bank
     */
    private static class BankManipulationStep9 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt if the gate bank's general fault bit is set,
         *                                      or if we do not have enter access to the gate bank
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt {
            if (bmInfo._sourceBankDescriptor.getGeneralFault()) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GBitSetIndirect,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            AccessInfo key = bmInfo._indicatorKeyRegister.getAccessInfo();
            AccessPermissions perms = getEffectiveAccessPermissions(key,
                                                                    bmInfo._sourceBankDescriptor.getAccessLock(),
                                                                    bmInfo._sourceBankDescriptor.getGeneraAccessPermissions(),
                                                                    bmInfo._sourceBankDescriptor.getSpecialAccessPermissions());
            if (!perms._enter) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.EnterAccessDenied,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  Check limits of offset against gate bank to ensure the gate offset is okay
            if ((bmInfo._sourceBankOffset < bmInfo._sourceBankDescriptor.getLowerLimitNormalized())
                || (bmInfo._sourceBankOffset > bmInfo._sourceBankDescriptor.getUpperLimitNormalized())) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GateBankBoundaryViolation,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  Ensure X(a).offset is on an 8-word boundary
            //TODO AddressExceptionInterrupt

            //  Gate is found at the source offset from the start of the gate bank
            //TODO Create Gate class and load it from the offset

            //  Compare our key to the gate's lock to ensure we have enter access to the gate
            //TODO AddressExceptionInterrupt

            //  If GOTO or LBJ with X(a).IS == 1, and Gate.GI is set, throw GOTO Inhibit AddressingExceptionInterrupt
            //TODO

            //  If target L,BDI is less than 0,32 throw AddressExceptionInterrupt
            //TODO

            //  If GateBD.LIB is set, do library gate processing.
            //  We do not currently support library gates, so ignore this.

            //  Retain Designator bits, access key, latent parameters, and b field from the gate if enabled
            //TODO see 3.1.3

            //  Fetch target BD
            //TODO See steps a through d of step 6, but do fatal addressing exception

            bmInfo._gateProcessed = true;
            bmInfo._nextStep = 10;
        }
    }

    /**
     * Determine the base register to be loaded
     */
    private static class BankManipulationStep10 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._instruction == InstructionHandler.Instruction.LAE) {
                //TODO special case - we load 15 registers here B1:B15
                bmInfo._nextStep = 18;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.LBE) {
                bmInfo._baseRegisterIndex = (int) bmInfo._currentInstruction.getA() + 16;
                bmInfo._nextStep = 18;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.LBU) {
                bmInfo._baseRegisterIndex = (int) bmInfo._currentInstruction.getA();
                bmInfo._nextStep = 18;
            } else if ((bmInfo._instruction == InstructionHandler.Instruction.UR) || (bmInfo._interrupt != null)) {
                bmInfo._baseRegisterIndex = 15;
            } else {
                //  Transfer
                //TODO is this where we need a special case for LxJ to EM with no enter access?
                // EM or BM to EM - load B0
                // BM to BM: LBJ use X(a).BDR + 12, LDJ DB31 ? 15 : 14, LIJ DB31 ? 13 : 12
                //           LxJ/RTN specified by RCS.B + 12
                // EM to BM: GOTO, CALL nongated: B12, Gated specified by Gate.B + 12
                //           RTN Specified by RCS.B + 12
                bmInfo._nextStep++;
            }
        }
    }

    /**
     * Deal with prior bank - only for transfers.
     * For EM to EM or BM to BM, we do nothing.
     * For EM to BM, set B0 to void base register, and PAR.L,BDI to 0,0
     * For BM to EM LxJ/GOTO and LxJ/CALL B(_baseRegisterIndex).V is set
     *              LxJ/RTN B(RCS.B+12).V is set
     */
    private static class BankManipulationStep11 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * For calls, create an entry on the RCS.  Check for RCS overflow first...
     */
    private static class BankManipulationStep12 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Update X(a)
     * For LxJ normal, translate prior L,BDI to E,LS,BDI,
     *                  BDR field is _baseRegisterIndex & 03,
     *                  IS is zero,
     *                  PAR.PC + 1 -> X(18:35)
     * For CALL to BM, X(a).IS is set to 2, remaining fields undefined
     *                  Designator Register DB17 determines whether X(a) is exec or user register
     */
    private static class BankManipulationStep13 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Update X(0)
     * For certain transfers, User X0 contains DB16 in Bit 0, and AccessKey in Bits 17:35
     *  EM to EM GOTO, CALL
     *  BM to BM LxJ normal
     *  EM to BM GOTO, CALL
     *  BM to EM LxJ/GOTO, LxJ/CALL
     */
    private static class BankManipulationStep14 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Gate fields transfer...
     * If a gate was processed...
     *      If Gate.DBI is clear, DR.DB12:15 <- Gate.DB12:15, DB17 <- Gate.DB17
     *      If Gate.AKI is clear, Indicator/Key.AccessKey <= Gate.AccessKey
     *      If Gate.LP0I is clear, UR0 or ER0 <- Gate.LatentParameter0
     *      If Gate.LP1I is clear, UR1 or ER1 <- Gate.LatentParameter1
     *      Selection of user/exec register set is controlled by Gate.DB17 if DBI is clear, else by DR.DB17
     *      Move on to step 17 (steps 15 and 16 are mutually exclusive)
     *  Else move on to step 16
     */
    private static class BankManipulationStep15 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._gateProcessed) {
                //TODO
            }

            bmInfo._nextStep++;
        }
    }

    /**
     *  Update ASP for certain transfer instructions:
     *      EM to EM    RTN Replace AccessKey and DB12:17 with RCS fields
     *      BM to BM    LxJ/RTN as above
     *      EM to BM    GOTO, CALL set DB16
     *                  RTN AccessKey / DB12:17 as above
     *      BM to EM    LxJ/GOTO, LxJ/CALL clear DB16
     *      UR          Entire ASP is replaced with oeprand contents
     *      Interrupt   New ASP formed by hardware TODO step 3 of 5.15
     */
    private static class BankManipulationStep16 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     *  For transfer instructions, offset from step 3 (or step 9, if gated) -> PAR.PC
     */
    private static class BankManipulationStep17 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Update ABT or Hard-held PAR.L,BDI is updated
     */
    private static class BankManipulationStep18 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Load the appropriate base register
     */
    private static class BankManipulationStep19 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Toggle DB31 on transfers to basic mode
     */
    private static class BankManipulationStep20 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /**
     * Final exception checks
     */
    private static class BankManipulationStep21 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            //TODO
            bmInfo._nextStep++;
        }
    }

    /*
    TODO
    Temporary note: LxJ with IS==0 is LxJ normal (BM target) or LxJ/CALL (EM.entrer == 0 or Gate)
                        with IS==1 is LxJ normal (BM target) or LxJ/GOTO (EM.enter == 1 or Gate)
                        with IS==2 RCS.DB.16==1 is LxJ/RTN to BM
                                   RCS.DB.16==0 is LxJ/RTN to EM
     */

    /**
     * bank manipulation handlers...
     */
    private static final BankManipulationStep[] _bankManipulationSteps = {
        null,
        new BankManipulationStep1(),
        new BankManipulationStep2(),
        new BankManipulationStep3(),
        new BankManipulationStep4(),
        new BankManipulationStep5(),
        new BankManipulationStep6(),
        new BankManipulationStep7(),
        new BankManipulationStep8(),
        new BankManipulationStep9(),
        new BankManipulationStep10(),
        new BankManipulationStep11(),
        new BankManipulationStep12(),
        new BankManipulationStep13(),
        new BankManipulationStep14(),
        new BankManipulationStep15(),
        new BankManipulationStep16(),
        new BankManipulationStep17(),
        new BankManipulationStep18(),
        new BankManipulationStep19(),
        new BankManipulationStep20(),
        new BankManipulationStep21(),
    };

    /**
     * Given a lock and key, we return whichever of the general or special permissions should be observed
     */
    private static AccessPermissions getEffectiveAccessPermissions(
        final AccessInfo key,
        final AccessInfo lock,
        final AccessPermissions generalPermissions,
        final AccessPermissions specialPermissions
    ) {
        return ((key._domain > lock._domain) || (key.equals(lock))) ? specialPermissions : generalPermissions;
    }

    /**
     * An algorithm for handling bank transitions for the following instructions:
     *  CALL, GOTO, LAE, LBE, LBJ, LBU, LDJ, LIJ, RNT, TVA, and UR
     * @param instruction type of instruction or null if we are invoked for interrupt handling
     * @param operand from U for instruction processing, zero fro interrupt handling
     * @throws AddressingExceptionInterrupt if IS==3 for any LxJ instruction
     *                                      or source L,BDI is invalid
     *                                      or a void bank is specified where it is not allowed
     *                                      or for an invalid bank type in various situations
     *                                      or general fault set on destination bank
     * @throws InvalidInstructionInterrupt for LBU with B0 or B1 specified as destination
     * @throws RCSGenericStackUnderflowOverflowInterrupt for return operaions for which there is no existing stack frame
     */
    public static void bankManipulation(
        final InstructionProcessor instructionProcessor,
        final InstructionHandler.Instruction instruction,
        final long operand
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, instruction, operand, null);
        while (bmInfo._nextStep != 0) {
            _bankManipulationSteps[bmInfo._nextStep].handler(bmInfo);
        }
    }

    /**
     * An algorithm for handling bank transitions for interrupt handling
     * @param interrupt reference to the interrupt being handled, null if invoked from instruction handling
     * @throws AddressingExceptionInterrupt if IS==3 for any LxJ instruction
     *                                      or source L,BDI is invalid
     *                                      or a void bank is specified where it is not allowed
     *                                      or for an invalid bank type in various situations
     *                                      or general fault set on destination bank
     * @throws InvalidInstructionInterrupt for LBU with B0 or B1 specified as destination
     * @throws RCSGenericStackUnderflowOverflowInterrupt for return operaions for which there is no existing stack frame
     */
    public static void bankManipulation(
        final InstructionProcessor instructionProcessor,
        final MachineInterrupt interrupt
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, null, 0, interrupt);
        while (bmInfo._nextStep != 0) {
            _bankManipulationSteps[bmInfo._nextStep].handler(bmInfo);
        }
    }
}
