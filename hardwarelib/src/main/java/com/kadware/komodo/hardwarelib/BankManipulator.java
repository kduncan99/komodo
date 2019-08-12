/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.baselib.*;
import com.kadware.komodo.hardwarelib.interrupts.*;

/**
 * This bit of nastiness implements a state machine which implements the bank manipulation algorithm
 */
public class BankManipulator {

    private enum TransferMode {
        BasicToBasic,
        BasicToExtended,
        ExtendedToBasic,
        ExtendedToExtended,
    }

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
        private final long[] _operands;                             //  7 or 15 operand values for UR and LAE respectively,
                                                                    //      or 1 operand representing (U) for CALL, GOTO, LBE, LBU
                                                                    //      or 1 operand representing U for LBJ, LDJ, LIJ
                                                                    //      or null for RTN
        private boolean _returnOperation = false;                   //  true if RTN, LxJ/RTN

        private final ActiveBaseTableEntry[] _activeBaseTableEntries;
        private final BaseRegister[] _baseRegisters;
        private final InstructionWord _currentInstruction;
        private final DesignatorRegister _designatorRegister;
        private final IndicatorKeyRegister _indicatorKeyRegister;

        private int _nextStep = 1;

        //  Determined at some point *after* construction
        private int _baseRegisterIndex = 0;                     //  base register to be loaded, determined in step 10
        private Gate _gateBank = null;                          //  refers to a Gate object if we processed a gate
        private int _priorBankLevel = 0;                        //  For CALL and LxJ/CALL
        private int _priorBankDescriptorIndex = 0;              //  as above
        private ReturnControlStackFrame _rcsFrame = null;       //  if non-null, this is the RCS entry for returns
        private TransferMode _transferMode = null;              //  not known until step 10, and only for LxJ, GOTO, CALL, RTN

        private int _sourceBankLevel = 0;                       //  L portion of source bank L,BDI
        private int _sourceBankDescriptorIndex = 0;             //  BDI portion of source bank L,BDI
        private int _sourceBankOffset = 0;                      //  offset value accompanying source bank specification
        private BankDescriptor _sourceBankDescriptor = null;    //  BD for source bank

        private int _targetBankLevel = 0;                       //  L portion of target bank L,BDI
        private int _targetBankDescriptorIndex = 0;             //  BDI portion of target bank L,BDI
        private int _targetBankOffset = 0;                      //  offset value accompanying target bank specification
        private BankDescriptor _targetBankDescriptor = null;    //  BD for target bank - from step 7, null implies a void bank

        private BankManipulationInfo(
            final InstructionProcessor instructionProcessor,
            final InstructionHandler.Instruction instruction,
            final long[] operands,
            final MachineInterrupt interrupt
        ) {
            _instruction = instruction;
            _instructionProcessor = instructionProcessor;
            _interrupt = interrupt;
            _operands = operands;

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

                _callOperation =
                    (instruction == InstructionHandler.Instruction.CALL)
                    || (instruction == InstructionHandler.Instruction.LOCL)
                    || (_lxjInstruction && (_lxjInterfaceSpec < 2));
                //  Note that UR is not considered a return operation
                _returnOperation =
                    (instruction == InstructionHandler.Instruction.RTN)
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
     * Retrieve prior L,BDI for any instruction which will result in acquiring a return address/bank
     */
    private static class BankManipulationStep2 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._instruction != null) {
                if (bmInfo._instruction == InstructionHandler.Instruction.CALL) {
                    bmInfo._priorBankLevel = bmInfo._instructionProcessor.getProgramAddressRegister().getLevel();
                    bmInfo._priorBankDescriptorIndex
                        = bmInfo._instructionProcessor.getProgramAddressRegister().getBankDescriptorIndex();
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
         * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCS stack doesn't have a suitably-sized frame, or is void.
         * @throws AddressingExceptionInterrupt if something is drastically wrong
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
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
                if (bdtLevel0 == null) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           0,
                                                           0);
                }
                int intOffset = bmInfo._interrupt.getInterruptClass().getCode();
                if (intOffset >= bdtLevel0.getSize()) {
                    bmInfo._instructionProcessor.stop(InstructionProcessor.StopReason.InterruptHandlerOffsetOutOfRange, 0);
                    bmInfo._nextStep = 0;
                    return;
                }

                long lbdiOffset = bdtLevel0.get(intOffset);
                bmInfo._sourceBankLevel = (int) (lbdiOffset >> 33);
                bmInfo._sourceBankDescriptorIndex = (int) (lbdiOffset >> 18) & 077777;
                bmInfo._sourceBankOffset = (int) lbdiOffset & 0777777;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.UR) {
                //  source L,BDI comes from operand L,BDI
                //  offset comes from operand.PAR.PC
                bmInfo._sourceBankLevel = (int) (bmInfo._operands[0] >> 33);
                bmInfo._sourceBankDescriptorIndex = (int) (bmInfo._operands[0] >> 18) & 077777;
                bmInfo._sourceBankOffset = (int) (bmInfo._operands[0] & 0777777);
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
                if (rcsXReg.getXM() > rcsBReg._upperLimitNormalized) {
                    throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                        InstructionProcessor.RCS_BASE_REGISTER,
                                                                        (int) rcsXReg.getXM());
                }

                if (rcsBReg._storage == null) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           0,
                                                           0);
                }

                int framePointer = (int) rcsXReg.getXM();
                int offset = framePointer - rcsBReg._lowerLimitNormalized;
                long[] frame = { rcsBReg._storage.get(offset), rcsBReg._storage.get(offset + 1) };
                bmInfo._rcsFrame = new ReturnControlStackFrame(frame);
                rcsXReg.setXM(framePointer + 2);

                bmInfo._sourceBankLevel = bmInfo._rcsFrame._reentryPointBankLevel;
                bmInfo._sourceBankDescriptorIndex = bmInfo._rcsFrame._reentryPointBankDescriptorIndex;
                bmInfo._sourceBankOffset = bmInfo._rcsFrame._reentryPointOffset;
            } else if (bmInfo._lxjInstruction) {
                //  source L,BDI comes from basic mode X(a) E,LS,BDI
                //  offset comes from operand
                long bmSpec = bmInfo._lxjXRegister.getW();
                boolean execFlag = (bmSpec & 0_400000_000000L) != 0;
                boolean levelSpec = (bmSpec & 0_040000_000000L) != 0;
                bmInfo._sourceBankLevel = execFlag ? (levelSpec ? 0 : 2) : (levelSpec ? 6 : 4);
                bmInfo._sourceBankDescriptorIndex = (int) ((bmSpec >> 18) & 077777);
                bmInfo._sourceBankOffset = (int) bmInfo._operands[0] & 0777777;
            } else {
                //  source L,BDI,Offset comes from operand
                bmInfo._sourceBankLevel = (int) (bmInfo._operands[0] >> 33) & 07;
                bmInfo._sourceBankDescriptorIndex = (int) (bmInfo._operands[0] >> 18) & 077777;
                bmInfo._sourceBankOffset = (int) bmInfo._operands[0] & 0777777;
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
                } else if (bmInfo._returnOperation) {
                    if (!bmInfo._rcsFrame._designatorRegisterDB12To17.getBasicModeEnabled()) {
                        //  return to extended mode - addressing exception
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.InvalidSourceLevelBDI,
                                                               bmInfo._sourceBankLevel,
                                                               bmInfo._sourceBankDescriptorIndex);
                    } else {
                        //  return to basic mode - void bank
                        bmInfo._nextStep = 10;
                        return;
                    }
                } else if (bmInfo._instruction == InstructionHandler.Instruction.UR) {
                    DesignatorRegister drReturn = new DesignatorRegister(bmInfo._operands[1]);
                    if (!drReturn.getBasicModeEnabled()) {
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
                        && !bmInfo._rcsFrame._designatorRegisterDB12To17.getBasicModeEnabled()) {
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
                    }
                    break;

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
            AccessPermissions gateBankPerms =
                getEffectiveAccessPermissions(key,
                                              bmInfo._sourceBankDescriptor.getAccessLock(),
                                              bmInfo._sourceBankDescriptor.getGeneraAccessPermissions(),
                                              bmInfo._sourceBankDescriptor.getSpecialAccessPermissions());
            if (!gateBankPerms._enter) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.EnterAccessDenied,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  Check limits of offset against gate bank to ensure the gate offset is within limits,
            //  and that it is a multiple of 8 words.
            if ((bmInfo._sourceBankOffset < bmInfo._sourceBankDescriptor.getLowerLimitNormalized())
                || (bmInfo._sourceBankOffset > bmInfo._sourceBankDescriptor.getUpperLimitNormalized())
                || ((bmInfo._sourceBankOffset & 07) != 0)) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GateBankBoundaryViolation,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  Gate is found at the source offset from the start of the gate bank.
            //  Create Gate class and load it from the offset.
            try {
                AbsoluteAddress gateBankAddress = bmInfo._sourceBankDescriptor.getBaseAddress();
                MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(gateBankAddress._upiIndex);
                ArraySlice mspStorage = msp.getStorage(gateBankAddress._segment);
                bmInfo._gateBank = new Gate(new ArraySlice(mspStorage, gateBankAddress._offset, 8).getAll());
            } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  Compare our key to the gate's lock to ensure we have enter access to the gate
            AccessPermissions gatePerms =
                getEffectiveAccessPermissions(key,
                                              bmInfo._gateBank._accessLock,
                                              bmInfo._gateBank._generalPermissions,
                                              bmInfo._gateBank._specialPermissions);
            if (!gatePerms._enter) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.EnterAccessDenied,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  If GOTO or LxJ with X(a).IS == 1, and Gate.GI is set, throw GOTO Inhibit AddressingExceptionInterrupt
            if ((bmInfo._instruction == InstructionHandler.Instruction.GOTO)
                || (bmInfo._lxjInstruction && (bmInfo._lxjInterfaceSpec == 1))) {
                if (bmInfo._gateBank._gotoInhibit) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.GBitSetGate,
                                                           bmInfo._sourceBankLevel,
                                                           bmInfo._sourceBankDescriptorIndex);
                }
            }

            //  If target L,BDI is less than 0,32 throw AddressExceptionInterrupt
            if ((bmInfo._gateBank._targetBankLevel == 0) && (bmInfo._gateBank._targetBankDescriptorIndex < 32)) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       bmInfo._sourceBankLevel,
                                                       bmInfo._sourceBankDescriptorIndex);
            }

            //  If GateBD.LIB is set, do library gate processing.
            //  We do not currently support library gates, so ignore this.

            //  Fetch target BD
            bmInfo._targetBankLevel = bmInfo._gateBank._targetBankLevel;
            bmInfo._targetBankDescriptorIndex = bmInfo._gateBank._targetBankDescriptorIndex;
            bmInfo._targetBankOffset = bmInfo._gateBank._targetBankOffset;
            try {
                bmInfo._targetBankDescriptor =
                    bmInfo._instructionProcessor.findBankDescriptor(bmInfo._targetBankLevel, bmInfo._targetBankDescriptorIndex);
            } catch (AddressingExceptionInterrupt ex) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       bmInfo._targetBankLevel,
                                                       bmInfo._targetBankDescriptorIndex);
            }

            bmInfo._nextStep = 10;
        }
    }

    /**
     * Finally we can define simply the source and destination execution modes for transfers.
     * Do that, then determine the base register to be loaded
     */
    private static class BankManipulationStep10 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._instruction == InstructionHandler.Instruction.LAE) {
                //  baseRegisterIndex was set a long time ago...
                bmInfo._nextStep = 18;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.LBE) {
                bmInfo._baseRegisterIndex = (int) bmInfo._currentInstruction.getA() + 16;
                bmInfo._nextStep = 18;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.LBU) {
                bmInfo._baseRegisterIndex = (int) bmInfo._currentInstruction.getA();
                bmInfo._nextStep = 18;
            } else if ((bmInfo._instruction == InstructionHandler.Instruction.UR) || (bmInfo._interrupt != null)) {
                bmInfo._baseRegisterIndex = 0;
                bmInfo._nextStep = 16;
            } else {
                //  This is a transfer operation...  Determine transfer mode
                boolean sourceModeBasic = bmInfo._designatorRegister.getBasicModeEnabled();
                if (bmInfo._returnOperation) {
                    //  destination mode is defined by DB16 in the RCS frame
                    if (bmInfo._rcsFrame._designatorRegisterDB12To17.getBasicModeEnabled()) {
                        bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToBasic : TransferMode.ExtendedToBasic;
                    } else {
                        bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToExtended : TransferMode.ExtendedToExtended;
                    }
                } else {
                    //  call or goto operation - destination mode is defined by target bank type
                    if (bmInfo._targetBankDescriptor == null) {
                        bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToBasic : TransferMode.ExtendedToExtended;
                    } else if (bmInfo._targetBankDescriptor.getBankType() == BankDescriptor.BankType.BasicMode) {
                        bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToBasic : TransferMode.ExtendedToBasic;
                    } else {
                        bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToExtended : TransferMode.ExtendedToExtended;
                    }
                }

                switch (bmInfo._transferMode) {
                    case BasicToBasic:
                        if (bmInfo._returnOperation) {
                            bmInfo._baseRegisterIndex = bmInfo._rcsFrame._basicModeBaseRegister + 12;
                        } else if (bmInfo._instruction == InstructionHandler.Instruction.LBJ) {
                            bmInfo._baseRegisterIndex = bmInfo._lxjBankSelector + 12;
                        } else if (bmInfo._instruction == InstructionHandler.Instruction.LDJ) {
                            bmInfo._baseRegisterIndex = bmInfo._designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                        } else if (bmInfo._instruction == InstructionHandler.Instruction.LIJ) {
                            bmInfo._baseRegisterIndex = bmInfo._designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
                        }
                        break;

                    case ExtendedToBasic:
                        if (bmInfo._returnOperation) {
                            bmInfo._baseRegisterIndex = bmInfo._rcsFrame._basicModeBaseRegister + 12;
                        } else {
                            if (bmInfo._gateBank == null) {
                                bmInfo._baseRegisterIndex = 12;
                            } else {
                                bmInfo._baseRegisterIndex = bmInfo._gateBank._targetBankDescriptorIndex + 12;
                            }
                        }
                        break;

                    case BasicToExtended:
                    case ExtendedToExtended:
                        bmInfo._baseRegisterIndex = 0;
                        break;
                }

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
            if (bmInfo._transferMode == TransferMode.ExtendedToBasic) {
                bmInfo._instructionProcessor.setBaseRegister(0, new BaseRegister());
                long newPar = bmInfo._instructionProcessor.getProgramAddressRegister().getProgramCounter();
                bmInfo._instructionProcessor.setProgramAddressRegister(newPar);
            } else if (bmInfo._transferMode == TransferMode.BasicToExtended) {
                bmInfo._instructionProcessor.setBaseRegister(bmInfo._baseRegisterIndex, new BaseRegister());
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * For calls, create an entry on the RCS.  Check for RCS overflow first...
     * Only executed for transfers.
     */
    private static class BankManipulationStep12 implements BankManipulationStep {

        /**
         * @throws AddressingExceptionInterrupt for bad troubles
         * @throws RCSGenericStackUnderflowOverflowInterrupt for stack overflow
         */
        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
            if (bmInfo._callOperation) {
                BaseRegister rcsBReg = bmInfo._instructionProcessor.getBaseRegister(InstructionProcessor.RCS_BASE_REGISTER);
                if (rcsBReg._voidFlag) {
                    throw new RCSGenericStackUnderflowOverflowInterrupt(
                        RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                        InstructionProcessor.RCS_BASE_REGISTER,
                        0);
                }

                IndexRegister rcsXReg = bmInfo._instructionProcessor.getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);

                int framePointer = (int) rcsXReg.getXM() - 2;
                if (framePointer < rcsBReg._lowerLimitNormalized) {
                    throw new RCSGenericStackUnderflowOverflowInterrupt(
                        RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                        InstructionProcessor.RCS_BASE_REGISTER,
                        framePointer);
                }

                if (rcsBReg._storage == null) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           0,
                                                           0);
                }

                int rtnAddr = bmInfo._instructionProcessor.getProgramAddressRegister().getProgramCounter() + 1;
                int bValue = 0;
                switch (bmInfo._transferMode) {
                    case ExtendedToBasic:
                        if (bmInfo._gateBank != null) {
                            bValue = bmInfo._gateBank._basicModeBaseRegister;
                        }
                        break;

                    case BasicToExtended:
                        if (bmInfo._instruction == InstructionHandler.Instruction.LBJ) {
                            bValue = bmInfo._lxjBankSelector;
                        } else if (bmInfo._instruction == InstructionHandler.Instruction.LDJ) {
                            bValue = bmInfo._designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                        } else if (bmInfo._instruction == InstructionHandler.Instruction.LIJ) {
                            bValue = bmInfo._designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
                        }
                        break;
                }

                ReturnControlStackFrame rcsFrame = new ReturnControlStackFrame(bmInfo._priorBankLevel,
                                                                               bmInfo._priorBankDescriptorIndex,
                                                                               rtnAddr,
                                                                               false,
                                                                               bValue,
                                                                               bmInfo._designatorRegister,
                                                                               bmInfo._indicatorKeyRegister.getAccessInfo());
                long[] rcsData = rcsFrame.get();

                int offset = framePointer - rcsBReg._lowerLimitNormalized;
                rcsBReg._storage.set(offset, rcsData[0]);
                rcsBReg._storage.set(offset + 1, rcsData[1]);
                rcsXReg.setXM(framePointer);
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Update X(a) or X11
     * For LxJ normal, translate prior L,BDI to E,LS,BDI,
     *                  BDR field is _baseRegisterIndex & 03,
     *                  IS is zero,
     *                  PAR.PC + 1 -> X(18:35)
     * For CALL to BM, X11.IS is set to 2, remaining fields undefined
     *                  Designator Register DB17 determines whether X(a) is exec or user register
     */
    private static class BankManipulationStep13 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._lxjInstruction && (bmInfo._transferMode == TransferMode.BasicToBasic)) {
                int parPCNext = bmInfo._instructionProcessor.getProgramAddressRegister().getProgramCounter() + 1;
                long value = VirtualAddress.translateToBasicMode(bmInfo._priorBankLevel,
                                                                 bmInfo._priorBankDescriptorIndex,
                                                                 parPCNext);
                value |= (long)(bmInfo._baseRegisterIndex & 03) << 33;
                bmInfo._lxjXRegister.setW(value);
            } else if ((bmInfo._instruction == InstructionHandler.Instruction.CALL)
                && (bmInfo._transferMode == TransferMode.ExtendedToBasic)) {
                bmInfo._instructionProcessor.getExecOrUserXRegister(11).setW(2L << 30);
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Update X(0) - not invoked for non-transfers
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
            if (bmInfo._callOperation) {
                try {
                    long value = bmInfo._designatorRegister.getBasicModeEnabled() ? 0_400000_000000L : 0;
                    value |= bmInfo._indicatorKeyRegister.getAccessKey();
                    bmInfo._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).setW(value);
                } catch (MachineInterrupt ex) {
                    //  cannot happen
                }
            }

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
            if (bmInfo._gateBank != null) {
                if (!bmInfo._gateBank._designatorBitInhibit) {
                    long temp = bmInfo._designatorRegister.getW() & 0_777702_777777L;
                    temp |= bmInfo._gateBank._designatorBits12_17.getW() & 0_000075_000000L;
                    bmInfo._designatorRegister.setW(temp);
                }

                if (!bmInfo._gateBank._accessKeyInhibit) {
                    bmInfo._indicatorKeyRegister.setAccessKey((int) bmInfo._gateBank._accessKey.get());
                }

                if (!bmInfo._gateBank._latentParameter0Inhibit) {
                    bmInfo._instructionProcessor.getExecOrUserRRegister(0).setW(bmInfo._gateBank._latentParameter0);
                }

                if (!bmInfo._gateBank._latentParameter1Inhibit) {
                    bmInfo._instructionProcessor.getExecOrUserRRegister(1).setW(bmInfo._gateBank._latentParameter1);
                }

                bmInfo._nextStep = 17;
            }

            bmInfo._nextStep++;
        }
    }

    /**
     *  Update ASP for certain transfer instructions (not invoked for non-transfers):
     *      EM to EM    RTN Replace AccessKey and DB12:17 with RCS fields
     *      BM to BM    LxJ/RTN as above
     *      EM to BM    GOTO, CALL set DB16
     *                  RTN AccessKey / DB12:17 as above
     *      BM to EM    LxJ/GOTO, LxJ/CALL clear DB16
     *      UR          Entire ASP is replaced with operand contents
     *      Interrupt   New ASP formed by hardware
     */
    private static class BankManipulationStep16 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._interrupt != null) {
                //  PAR is loaded from the values in _targetBankLevel, _targetBankDescriptorIndex,
                //      and _targetOffset which were established previously in this algorithm
                //  Designator Register is cleared excepting the following bits:
                //      DB17 (Exec Register Set Selection) set to 1
                //      DB29 (Arithmetic Exception Enable) set to 1
                //      DB1 (Performance Monitoring Counter Enabled) is set to DB2 - not supported here
                //      DB2 (PerfMon Counter Interrupt Control) and DB31 (Basic Mode BaseRegister Selection) are not changed
                //      DB6 is set if this is a HardwareCheck interrupt
                //  Indicator/Key register is zeroed out.
                //  Quantum timer is undefined, and the rest of the ASP is not relevant.
                long par = (long) bmInfo._targetBankLevel << 33;
                par |= (long) bmInfo._targetBankDescriptorIndex << 18;
                par |= bmInfo._targetBankOffset;
                bmInfo._instructionProcessor.setProgramAddressRegister(par);

                boolean hwCheck = bmInfo._interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.HardwareCheck;
                DesignatorRegister newDR = new DesignatorRegister(0);
                newDR.setExecRegisterSetSelected(true);
                newDR.setArithmeticExceptionEnabled(true);
                newDR.setBasicModeEnabled(bmInfo._targetBankDescriptor.getBankType() == BankDescriptor.BankType.BasicMode);
                newDR.setBasicModeBaseRegisterSelection(bmInfo._designatorRegister.getBasicModeBaseRegisterSelection());
                newDR.setFaultHandlingInProgress(hwCheck);
                bmInfo._instructionProcessor.setDesignatorRegister(newDR);

                bmInfo._indicatorKeyRegister.setW(0);
                bmInfo._nextStep = 18;
            } else if (bmInfo._instruction == InstructionHandler.Instruction.UR) {
                //  Entire ASP is loaded from 7 consecutive operand words.
                //  ISW0, ISW1, and SSF of Indicator/Key register are ignored, and some Designator Bits are set-to-zero.
                bmInfo._instructionProcessor.setProgramAddressRegister(bmInfo._operands[0]);
                bmInfo._instructionProcessor.setDesignatorRegister(new DesignatorRegister(bmInfo._operands[1]));
                IndicatorKeyRegister ikr = new IndicatorKeyRegister(bmInfo._operands[2]);
                ikr.setShortStatusField(bmInfo._indicatorKeyRegister.getShortStatusField());
                bmInfo._instructionProcessor.setIndicatorKeyRegister(ikr.getW());
                bmInfo._instructionProcessor.setQuantumTimer(bmInfo._operands[3]);
                bmInfo._instructionProcessor.setCurrentInstruction(bmInfo._operands[4]);
                bmInfo._nextStep = 18;
            } else if (bmInfo._returnOperation) {
                bmInfo._indicatorKeyRegister.setAccessKey((int) bmInfo._rcsFrame._accessKey.get());
                bmInfo._designatorRegister.setS4(bmInfo._rcsFrame._designatorRegisterDB12To17.getS4());
                //  Special code for RTN instruction (per architecture document for emulated systems):
                //  On return, we clear DB15, and if DB14 is set, we clear DB17.
                //  What this means, is that returned-to PPrivilege 1 -> 0 and 3 -> 2,
                //      and that we clear exec-register-set-selected if PPrivilege is returning to 2 or 3.
                //  The latter makes sense - we don't want exec register selected for lower PPrivilege
                //      (although the OS should ensure this), so I'm implementing that.
                //  But the former... implies that on every RTN, the processor privilege *might* get elevated
                //      by one step.  Why this is done is beyond me, but there it is.  In order to support
                //      all four processor privileges properly, I'm going to NOT implement that.
                if (bmInfo._designatorRegister.getProcessorPrivilege() > 1) {
                    bmInfo._designatorRegister.setExecRegisterSetSelected(false);
                }
            } else if ((bmInfo._instruction == InstructionHandler.Instruction.GOTO)
                || (bmInfo._instruction == InstructionHandler.Instruction.CALL)) {
                if (bmInfo._transferMode == TransferMode.ExtendedToBasic) {
                    bmInfo._designatorRegister.setBasicModeEnabled(true);
                }
            } else if ((bmInfo._lxjInstruction) && (bmInfo._transferMode == TransferMode.BasicToExtended)) {
                bmInfo._designatorRegister.setBasicModeEnabled(false);
            }

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
            if (bmInfo._transferMode != null) {
                bmInfo._instructionProcessor.setProgramCounter(bmInfo._targetBankOffset, true);
            }
            bmInfo._nextStep++;
        }
    }

    /**
     * Update Hard-held PAR.L,BDI if we loaded into B0,
     * or the appropriate ABT entry to zero for a void bank, or L,BDI otherwise.
     */
    private static class BankManipulationStep18 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if (bmInfo._baseRegisterIndex == 0) {
                //  This is already done for interrupt handling and UR
                if ((bmInfo._interrupt == null) && (bmInfo._instruction != InstructionHandler.Instruction.UR)) {
                    long newPar = bmInfo._instructionProcessor.getProgramAddressRegister().getH2();
                    newPar |= (long) bmInfo._targetBankLevel << 33 | (long) bmInfo._targetBankDescriptorIndex << 18;
                    bmInfo._instructionProcessor.setProgramAddressRegister(newPar);
                }
            } else if (bmInfo._baseRegisterIndex < 16) {
                if (bmInfo._targetBankDescriptor == null) {
                    ActiveBaseTableEntry abte = new ActiveBaseTableEntry(0, 0, 0);
                    bmInfo._instructionProcessor.getActiveBaseTableEntries()[bmInfo._baseRegisterIndex - 1] = abte;
                } else {
                    int offset = bmInfo._loadInstruction ? bmInfo._targetBankOffset : 0;
                    ActiveBaseTableEntry abte = new ActiveBaseTableEntry(bmInfo._targetBankLevel,
                                                                         bmInfo._targetBankDescriptorIndex,
                                                                         offset);
                    bmInfo._instructionProcessor.getActiveBaseTableEntries()[bmInfo._baseRegisterIndex - 1] = abte;
                }
            }

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
        ) throws AddressingExceptionInterrupt {
            if (bmInfo._targetBankDescriptor == null) {
                bmInfo._instructionProcessor.setBaseRegister(bmInfo._baseRegisterIndex, new BaseRegister());
            } else if (bmInfo._loadInstruction && (bmInfo._targetBankOffset != 0)) {
                try {
                    BaseRegister br = new BaseRegister(bmInfo._targetBankDescriptor, bmInfo._targetBankOffset);
                    bmInfo._instructionProcessor.setBaseRegister(bmInfo._baseRegisterIndex, br);
                } catch (AddressingExceptionInterrupt ex) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           ex.getBankLevel(),
                                                           ex.getBankDescriptorIndex());
                }
            } else {
                try {
                    BaseRegister br = new BaseRegister(bmInfo._targetBankDescriptor);
                    bmInfo._instructionProcessor.setBaseRegister(bmInfo._baseRegisterIndex, br);
                } catch (AddressingExceptionInterrupt ex) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           ex.getBankLevel(),
                                                           ex.getBankDescriptorIndex());
                }
            }

            bmInfo._nextStep++;
        }
    }

    /**
     * Toggle DB31 on transfers to basic mode
     * Find out which register pair we are jumping to, and set DB31 appropriately
     */
    private static class BankManipulationStep20 implements BankManipulationStep {

        @Override
        public void handler(
            final BankManipulationInfo bmInfo
        ) {
            if ((bmInfo._transferMode == TransferMode.BasicToBasic) || (bmInfo._transferMode == TransferMode.ExtendedToBasic)) {
                bmInfo._instructionProcessor.findBasicModeBank(bmInfo._targetBankOffset, true);
            }

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
        ) throws AddressingExceptionInterrupt {
            if (bmInfo._targetBankDescriptor != null) {
                //  Check BD.G for LBU,LBE, and all transfers
                if ((bmInfo._instruction == InstructionHandler.Instruction.LBE)
                    || (bmInfo._instruction == InstructionHandler.Instruction.LBU)
                    || (bmInfo._transferMode != null)) {
                    if (bmInfo._targetBankDescriptor.getGeneralFault()) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                               bmInfo._targetBankLevel,
                                                               bmInfo._targetBankDescriptorIndex);
                    }
                }

                AccessPermissions perms =
                    getEffectiveAccessPermissions(bmInfo._instructionProcessor.getIndicatorKeyRegister().getAccessInfo(),
                                                  bmInfo._targetBankDescriptor.getAccessLock(),
                                                  bmInfo._targetBankDescriptor.getGeneraAccessPermissions(),
                                                  bmInfo._targetBankDescriptor.getSpecialAccessPermissions());

                //  Non RTN transfer to extended mode bank with no enter access,
                //  non-gated (of course - targets of gate banks should always have no enter access)
                if ((bmInfo._transferMode != null)
                    && (bmInfo._gateBank == null)
                    && !bmInfo._returnOperation) {
                    if ((bmInfo._transferMode == TransferMode.BasicToExtended)
                        || (bmInfo._transferMode == TransferMode.ExtendedToExtended)) {
                        if (!perms._enter) {
                            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                                   bmInfo._targetBankLevel,
                                                                   bmInfo._targetBankDescriptorIndex);
                        }
                    }
                }

                //  Did we attempt a non-gated transfer to a basic mode bank with enter access denied,
                //  with relative address not set to the lower limit of the target BD?
                if ((bmInfo._transferMode != null)
                    && (bmInfo._gateBank == null)
                    && (bmInfo._targetBankDescriptor.getBankType() == BankDescriptor.BankType.BasicMode)) {
                    if ((!perms._enter)
                        && (bmInfo._targetBankOffset != bmInfo._targetBankDescriptor.getLowerLimitNormalized())) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                               bmInfo._targetBankLevel,
                                                               bmInfo._targetBankDescriptorIndex);
                    }
                }

                //  Did we do gated transfer, or non-gated with no enter access, to a basic mode bank,
                //  while the new PAR.PC does not refer to that bank?
                if ((bmInfo._transferMode != null)
                    && ((bmInfo._gateBank != null) || !perms._enter)) {
                    if (bmInfo._targetBankDescriptor.getBankType() == BankDescriptor.BankType.BasicMode) {
                        BaseRegister br = bmInfo._instructionProcessor.getBaseRegister(bmInfo._baseRegisterIndex);
                        int relAddr = bmInfo._instructionProcessor.getProgramAddressRegister().getProgramCounter();
                        try {
                            br.checkAccessLimits(relAddr, false);
                        } catch (ReferenceViolationInterrupt ex) {
                            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                                   bmInfo._targetBankLevel,
                                                                   bmInfo._targetBankDescriptorIndex);
                        }
                    }
                }

                //  Check for RCS.Trap (only if there is an RCS frame)
                if ((bmInfo._rcsFrame != null) && (bmInfo._rcsFrame._trap)) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           bmInfo._targetBankLevel,
                                                           bmInfo._targetBankDescriptorIndex);
                }
            }

            //  All done.
            bmInfo._nextStep = 0;
        }
    }

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
        return ((key._ring < lock._ring) || (key.equals(lock))) ? specialPermissions : generalPermissions;
    }

    /**
     * This is the main processing loop for the state machine
     */
    private static void process(
        final BankManipulationInfo bmInfo
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        while (bmInfo._nextStep != 0) {
            int currentStep = bmInfo._nextStep;//TODO
            _bankManipulationSteps[bmInfo._nextStep].handler(bmInfo);
            //TODO eventually remove the following sanity check
            if (bmInfo._nextStep == currentStep) {
                System.out.println("Stuck at step " + String.valueOf(currentStep));
                assert(false);
            }
        }
    }

    /**
     * An algorithm for handling bank transitions for the following instructions:
     *  CALL, GOTO, LBE, LBJ, LBU, LDJ, and LIJ
     * @param instructionProcessor IP of interest
     * @param instruction type of instruction or null if we are invoked for interrupt handling
     * @param operand from (U) for CALL, GOTO, LBE, and LBU - from U for LBJ, LDJ, LIJ
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
        long[] operands = { operand };
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, instruction, operands, null);
        process(bmInfo);
    }

    /**
     * An algorithm for handling bank transitions for the RTN instruction
     * @param instructionProcessor IP of interest
     * @param instruction type of instruction or null if we are invoked for interrupt handling
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
        final InstructionHandler.Instruction instruction
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, instruction, null, null);
        process(bmInfo);
    }

    /**
     * An algorithm for handling bank transitions for the UR instruction
     * @param instructionProcessor IP of interest
     * @param instruction type of instruction or null if we are invoked for interrupt handling
     * @param operands array of 7 operand values for UR, or 15 values for LAE
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
        final long[] operands
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, instruction, operands, null);
        process(bmInfo);
    }

    /**
     * An algorithm for handling bank transitions for the LAE instruction
     * @param instructionProcessor IP of interest
     * @param instruction type of instruction or null if we are invoked for interrupt handling
     * @param baseRegisterIndex indicates the register upon which the bank is to be based
     * @param operand L,BDI,OFFSET for bank to be based
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
        final int baseRegisterIndex,
        final long operand
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        long[] operands = { operand };
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, instruction, operands, null);
        bmInfo._baseRegisterIndex = baseRegisterIndex;
        process(bmInfo);
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
        BankManipulationInfo bmInfo = new BankManipulationInfo(instructionProcessor, null, null, interrupt);
        process(bmInfo);
    }
}
