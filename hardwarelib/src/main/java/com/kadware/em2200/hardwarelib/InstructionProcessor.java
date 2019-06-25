/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.functions.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models an Instruction Procesor node
 */
public class InstructionProcessor extends Processor implements Worker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum RunMode {
        Normal,
        SingleInstruction,
        SingleCycle,
    };

    public enum StopReason {
        Initial,
        Cleared,
        Debug,
        Development,
        Breakpoint,
        HaltJumpExecuted,
        ICSBaseRegisterInvalid,
        ICSOverflow,
        InitiateAutoRecovery,
        L0BaseRegisterInvalid,
        PanelHalt,

        // Interrupt Handler initiated stops...
        InterruptHandlerHardwareFailure,
        InterruptHandlerOffsetOutOfRange,
        InterruptHandlerInvalidBankType,
        InterruptHandlerInvalidLevelBDI,
    };

    public enum BreakpointComparison {
        Fetch,
        Read,
        Write,
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

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

        private int _nextStep = 1;

        //  Determined at some point *after* construction
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

            if (_interrupt != null) {
                _callOperation = true;
            } else {
                _loadInstruction = (_instruction == InstructionHandler.Instruction.LAE)
                                   || (_instruction == InstructionHandler.Instruction.LBE)
                                   || (_instruction == InstructionHandler.Instruction.LBU);
                _lxjInstruction = (_instruction == InstructionHandler.Instruction.LBJ)
                                  || (_instruction == InstructionHandler.Instruction.LDJ)
                                  || (_instruction == InstructionHandler.Instruction.LIJ);

                int aField = (int) instructionProcessor._currentInstruction.getA();

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
                    && (bmInfo._instructionProcessor._currentInstruction.getA() < 2)) {
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
                    bmInfo._priorBankLevel = (int) (bmInfo._instructionProcessor._currentInstruction.getW() >> 33);
                    bmInfo._priorBankDescriptorIndex =
                        (int) (bmInfo._instructionProcessor._currentInstruction.getW() >> 18) & 077777;
                } else if ((bmInfo._lxjInstruction) && (bmInfo._lxjInterfaceSpec < 2)) {
                    //  We're supposed to be here for normal LxJ and for LxJ/CALL, but we also catch LxJ/GOTO
                    //  (interfaceSpec == 1 and target BD is extended with enter access, or gate)
                    //  Because we must do this for IS == 1 and source BD is basic, and it is too early in
                    //  the algorithm to know the source BD bank type.
                    int abtx;
                    if (bmInfo._instruction == InstructionHandler.Instruction.LBJ) {
                        abtx = bmInfo._lxjBankSelector + 12;
                    } else if (bmInfo._instruction == InstructionHandler.Instruction.LDJ) {
                        abtx = bmInfo._instructionProcessor._designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                    } else {
                        abtx = bmInfo._instructionProcessor._designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
                    }

                    bmInfo._priorBankLevel = bmInfo._instructionProcessor._activeBaseTableEntries[abtx].getLevel();
                    bmInfo._priorBankDescriptorIndex = bmInfo._instructionProcessor._activeBaseTableEntries[abtx].getBDI();
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
                if (bmInfo._instructionProcessor._baseRegisters[L0_BDT_BASE_REGISTER]._voidFlag) {
                    bmInfo._instructionProcessor.stop(StopReason.L0BaseRegisterInvalid, 0);
                    bmInfo._nextStep = 0;
                    return;
                }

                //  intOffset is the offset from the start of the level 0 BDT, to the vector we're interested in.
                ArraySlice bdtLevel0 = bmInfo._instructionProcessor._baseRegisters[L0_BDT_BASE_REGISTER]._storage;
                int intOffset = bmInfo._interrupt.getInterruptClass().getCode();
                if (intOffset >= bdtLevel0.getSize()) {
                    bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerOffsetOutOfRange, 0);
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
                    bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidLevelBDI,
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
                    bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidLevelBDI, 0);
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
                    bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidLevelBDI,
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
                        bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidBankType,
                                                          (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                        bmInfo._nextStep = 0;
                    } else if ((bmInfo._instruction == InstructionHandler.Instruction.LBU)
                               && (bmInfo._instructionProcessor._designatorRegister.getProcessorPrivilege() > 1)
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
                        bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidBankType,
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
                        bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidBankType,
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
                        bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidBankType,
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
                        bmInfo._instructionProcessor.stop(StopReason.InterruptHandlerInvalidBankType,
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
                case ExtendedMode:
                    //TODO special case - LxJ references extended mode BD without enter access
                    break;

                case BasicMode:
                    //TODO when PP>1 and GAP.E == 0 and SAP.E == 0, do void bank (set target bd null)
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
         * @throws AddressingExceptionInterrupt if the target bank's general fault bit is set
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

            //TODO if no enter access to source bank, throw

        /*
Otherwise, the Gate is fetched as follows:
a. Source Offset is limits checked against the Gate BD; if a limits violation is detected an
Addressing_Exception interrupt occurs.
b. If either (model_dependent) an absolute boundary violation is detected on the Gate
address or the Xa.Offset does not specify an 8-word Offset [implementation must detect
invalid Offset one way or the other], an Addressing_Exception interrupt occurs†. See
Section 8 for special Gate addressing rules.
c. Source Offset is applied to the Base_Address of the Gate BD and the Gate is fetched
from storage (paging is invoked on this access).
d. The current Access_Key is checked for Enter access against the Access_Lock, GAP and
SAP of the Gate (the GAP and SAP fields of the Gate correspond to the BD.GAP.E and
BD.SAP.E); an Addressing_Exception interrupt occurs if access is denied. Thus, to use a
Gate, one must have Enter access to both the Gate Bank (via the Gate BD) and the
particular Gate.
e. If a GOTO or an LBJ with Xa.IS = 1 operation is being performed, an Addressing_Exception
interrupt occurs when the Gate.GI = 1 (GOTO_Inhibit), regardless of the Target BD.
f. If the Target L,BDI is in the range 0,0 to 0,31, an Addressing_Exception interrupt occurs*.
g. If the GateBD.LIB = 1 processing continues with step 9a.
h. The Designator Bits, Access_Key, Latent Parameters and B fields from the Gate must be
retained if enabled or applicable (see 3.1.3).
i. The Target BD is fetched as described in step 6, sub-steps a through d (except that any
Addressing_Exception interrupt generated is fatal).
j. The Target BD.Type is examined and if a BD.Type  Extended_Mode and
BD.Type  Basic_Mode, instruction results are Architecturally_Undefined (any
Addressing_Exceptions associated with the Source BD must be noted as
Terminal_Addressing_Exceptions for reporting in step 21). Otherwise, processing
continues with step 10. Note: the Target BD.Type determines the resulting
environment (Basic_Mode or Extended_Mode) and that step 21 does not check Enter
access in the Target BD on gated transfers.
         */
        }
    }

    /*
    TODO
    Temporary note: LxJ with IS==0 is LxJ normal (BM target) or LxJ/CALL (EM.entrer == 0 or Gate)
                        with IS==1 is LxJ normal (BM target) or LxJ/GOTO (EM.enter == 1 or Gate)
                        with IS==2 RCS.DB.16==1 is LxJ/RTN to BM
                                   RCS.DB.16==0 is LxJ/RTN to EM
     */


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final int L0_BDT_BASE_REGISTER            = 16;
    public static final int ICS_BASE_REGISTER               = 26;
    public static final int ICS_INDEX_REGISTER              = GeneralRegisterSet.EX1;
    public static final int RCS_BASE_REGISTER               = 25;
    public static final int RCS_INDEX_REGISTER              = GeneralRegisterSet.EX0;

    /**
     * Raise interrupt when this many new entries exist
     */
    private static final int JUMP_HISTORY_TABLE_THRESHOLD   = 120;

    /**
     * Size of the conditionalJump history table
     */
    private static final int JUMP_HISTORY_TABLE_SIZE        = 128;

    private static final Logger LOGGER = LogManager.getLogger(InstructionProcessor.class);

    /**
     * Order of base register selection for Basic Mode address resolution
     * when the Basic Mode Base Register Selection Designator Register bit is false
     */
    public static final int[] BASE_REGISTER_CANDIDATES_FALSE = {12, 14, 13, 15};

    /**
     * Order of base register selection for Basic Mode address resolution
     * when the Basic Mode Base Register Selection Designator Register bit is true
     */
    public static final int[] BASE_REGISTER_CANDIDATES_TRUE = {13, 15, 12, 14};

    /**
     * ActiveBaseTable entries - index 0 is for B1 .. index 14 is for B15.  There is no entry for B0.
     */
    private final ActiveBaseTableEntry[] _activeBaseTableEntries = new ActiveBaseTableEntry[15];

    /**
     * Storage locks...
     */
    private static final Map<InstructionProcessor, HashSet<AbsoluteAddress>> _storageLocks = new HashMap<>();

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
    };

    private final BaseRegister[]            _baseRegisters = new BaseRegister[32];
    private final AbsoluteAddress           _breakpointAddress = new AbsoluteAddress((short)0, 0, 0);
    private final BreakpointRegister        _breakpointRegister = new BreakpointRegister();
    private boolean                         _broadcastInterruptEligibility = false;
    private final InstructionWord           _currentInstruction = new InstructionWord();
    private InstructionHandler              _currentInstructionHandler = null;  //  TODO do we need this?
    private RunMode                         _currentRunMode = RunMode.Normal;   //  TODO why isn't this updated?
    private final DesignatorRegister        _designatorRegister = new DesignatorRegister();
    private boolean                         _developmentMode = true;    //  TODO default this to false and provide a means of changing it
    private final GeneralRegisterSet        _generalRegisterSet = new GeneralRegisterSet();
    private final IndicatorKeyRegister      _indicatorKeyRegister = new IndicatorKeyRegister();
    private final InventoryManager          _inventoryManager = InventoryManager.getInstance();
    private boolean                         _jumpHistoryFullInterruptEnabled = false;
    private final Word36[]                  _jumpHistoryTable = new Word36[JUMP_HISTORY_TABLE_SIZE];
    private int                             _jumpHistoryTableNext = 0;
    private boolean                         _jumpHistoryThresholdReached = false;
    private MachineInterrupt                _lastInterrupt = null;    //  must always be != _pendingInterrupt
    private long                            _latestStopDetail = 0;
    private StopReason                      _latestStopReason = StopReason.Initial;
    private boolean                         _midInstructionInterruptPoint = false;
    private MachineInterrupt                _pendingInterrupt = null;
    private final ProgramAddressRegister    _preservedProgramAddressRegister = new ProgramAddressRegister();    //  TODO do we need this?
    private boolean                         _preventProgramCounterIncrement = false;
    private final ProgramAddressRegister    _programAddressRegister = new ProgramAddressRegister();
    private final Word36                    _quantumTimer = new Word36();
    private boolean                         _runningFlag = false;


    /**
     * Set this to cause the worker thread to shut down
     */
    private boolean _workerTerminate = false;

    /**
     * reference to worker thread
     */
    private final Thread _workerThread;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param name node name
     * @param upi unique identifier for this processor
     */
    public InstructionProcessor(
        final String name,
        final short upi
    ) {
        super(Processor.ProcessorType.InstructionProcessor, name, upi);

        _storageLocks.put(this, new HashSet<AbsoluteAddress>());

        for (int bx = 0; bx < _baseRegisters.length; ++bx) {
            _baseRegisters[bx] = new BaseRegister();
        }

        _workerThread = new Thread(this);
        _workerTerminate = false;

        for (int jx = 0; jx < JUMP_HISTORY_TABLE_SIZE; ++jx) {
            _jumpHistoryTable[jx] = new Word36();
        }

        for (int ax = 0; ax < _activeBaseTableEntries.length; ++ax) {
            _activeBaseTableEntries[ax] = new ActiveBaseTableEntry(0);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------


    public ActiveBaseTableEntry[] getActiveBaseTableEntries() { return _activeBaseTableEntries; }
    public BaseRegister getBaseRegister(final int index) { return _baseRegisters[index]; }
    public BaseRegister[] getBaseRegisters() { return _baseRegisters; }
    public boolean getBroadcastInterruptEligibility() { return _broadcastInterruptEligibility; }
    public InstructionWord getCurrentInstruction() { return _currentInstruction; }
    public RunMode getCurrentRunMode() { return _currentRunMode; }
    public DesignatorRegister getDesignatorRegister() { return _designatorRegister; }
    public boolean getDevelopmentMode() { return _developmentMode; }

    public GeneralRegister getGeneralRegister(
        final int index
    ) throws MachineInterrupt {
        if (!GeneralRegisterSet.isAccessAllowed(index, _designatorRegister.getProcessorPrivilege(), false)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
        return _generalRegisterSet.getRegister(index);
    }

    public IndicatorKeyRegister getIndicatorKeyRegister() { return _indicatorKeyRegister; }
    public MachineInterrupt getLastInterrupt() { return _lastInterrupt; }
    public StopReason getLatestStopReason() { return _latestStopReason; }
    public long getLatestStopDetail() { return _latestStopDetail; }
    public ProgramAddressRegister getProgramAddressRegister() { return _programAddressRegister; }
    public boolean getRunningFlag() { return _runningFlag; }

    public void loadActiveBaseTable(
        final ActiveBaseTableEntry[] source
    ) {
        for (int ax = 0; ax < source.length; ++ax) {
            if (ax < _activeBaseTableEntries.length) {
                _activeBaseTableEntries[ax] = source[ax];
            }
        }
    }

    public void loadActiveBaseTableEntry(
        final int index,
        final ActiveBaseTableEntry entry
    ) {
        _activeBaseTableEntries[index] = entry;
    }

    public void setBaseRegister(
        final int index,
        final BaseRegister baseRegister
    ) {
        _baseRegisters[index] = baseRegister;
    }

    public void setBroadcastInterruptEligibility(final boolean flag) { _broadcastInterruptEligibility = flag; }

    public void setGeneralRegister(
        final int index,
        final long value
    ) throws MachineInterrupt {
        if (!GeneralRegisterSet.isAccessAllowed(index, _designatorRegister.getProcessorPrivilege(), true)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
        }
        _generalRegisterSet.setRegister(index, value);
    }

    public void setJumpHistoryFullInterruptEnabled(final boolean flag) { _jumpHistoryFullInterruptEnabled = flag; }
    public void setProgramAddressRegister(final long value) { _programAddressRegister.setW(value); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /*
     * //TODO:Move this comment somewhere more appropriate
     * When an interrupt is raised and the IP recognizes such, it saves interrupt information and other machine state
     * information on the ICS (Interrupt Control Stack) and the Jump History table.  The Program Address Register is
     * updated from the vector for the particular interrupt, and a new hard-held ASP (Activity State Packet) is built.
     * Then instruction processing proceeds per normal (albeit in interrupt handling state).
     * See the hardware doc for instructions as to what machine state information needs to be preserved.
     *
     * Interrupts are recognized and processed only at specific points during instruction execution.
     * If an instruction is interrupted mid-execution, the state of that instruction must be preserved on the ICS
     * so that it can be resumed when interrupt processing is complete, except during hardware check interrupts
     * (in which case, the sequence of instruction(s) leading to the fault will not be resumed/retried).
     *
     * Instructions which can be interrupted mid-execution include
     *      BIML
     *      BICL
     *      BIMT
     *      BT
     *      EXR
     *      BAO
     *      All search instructions
     * Additionally, the EX instruction may be interrupted between each lookup of the next indirectly-referenced
     * instruction (in the case where the EX instruction refers to another EX instruction, then to another, etc).
     * Also, basic-mode indirect addressing, which also may have lengthy or infinite indirection must be
     * interruptable during the U-field resolution.
     *
     *  Nonfault interrupts are always taken at the next interrupt point (unless classified as a pended
     * interrupt; see Table 5–1), which may be either a between instructions or mid-execution interrupt
     * point. Note: the processor is not required to take asynchronous, nonfault interrupts at the next
     * interrupt point as long as the interrupt is not "locked out" longer than one millisecond. When taken
     * at a between instruction interrupt point, machine state reflects instruction completion and
     * ICS.INF = 0. When taken at a mid-execution interrupt point, hardware must back up PAR.PC to
     * point to the interrupted instruction, or the address of the EX or EXR instruction which led to the
     * interrupted instruction, and the remainder of the pertinent machine state (see below) must reflect
     * progress to that point. In this case, ICS.INF := 1.
     *
     * Fault interrupts detected during an instruction with no mid-execution interrupt point cause hardware
     * to back up pertinent machine state (as described below) to reflect the environment in effect at the
     * start of the instruction. Fault interrupts detected during an instruction with mid-execution interrupt
     * points cause hardware to back up pertinent machine state to reflect the environment at the last
     * interrupt point (which may be either a between instruction or a mid-execution interrupt point).
     * ICS.INF := 1 for all fault interrupts except those caused by the fetching of an instruction.
     *
     * B26 describes the base and limits of the bank which comprises the Interrupt Control Stack.
     * EX1 contains the ICS frame size in X(i) and the fram pointer in X(m).  Frame size must be a multiple of 16.
     * ICS frame:
     *  +0      Program Address Register
     *  +1      Designator Register
     *  +2,S1       Short Status Field
     *  +2,S2-S5    Indicator Key Register
     *  +3      Quantum TImer
     *  +4      If INF=1 in the Indicator Key Register (see 2.2.5)
     *  +5      Interrupt Status Word 0
     *  +6      Interrupt Status Word 1
     *  +7 - ?  Reserved for software
     */

    /**
     * Calculates the raw relative address (the U) for the current instruction.
     * Does NOT increment any x registers, even if their content contributes to the result.
     * @param offset For multiple transfer instructions which need to calculate U for each transfer,
     *                  this value increments from zero upward by one.
     * @return relative address for the current instruction
     */
    //TODO may not need offset parameter...
    private int calculateRelativeAddressForGRSOrStorage(
        final int offset
    ) {
        IndexRegister xReg = null;
        int xx = (int)_currentInstruction.getX();
        if (xx != 0) {
            xReg = getExecOrUserXRegister(xx);
        }

        long addend1;
        long addend2 = 0;
        if (_designatorRegister.getBasicModeEnabled()) {
            addend1 = _currentInstruction.getU();
            if (xReg != null) {
                addend2 = xReg.getSignedXM();
            }
        } else {
            addend1 = _currentInstruction.getD();
            if (xReg != null) {
                if (_designatorRegister.getExecutive24BitIndexingEnabled()
                        && (_designatorRegister.getProcessorPrivilege() < 2)) {
                    //  Exec 24-bit indexing is requested
                    addend2 = xReg.getSignedXM24();
                } else {
                    addend2 = xReg.getSignedXM();
                }
            }
        }

        long result = OnesComplement.add36Simple(addend1, addend2);
        if (offset != 0) {
            result = OnesComplement.add36Simple(result, offset);
        }

        return (int)result;
    }

    /**
     * Calculates the raw relative address (the U) for the current instruction.
     * Does NOT increment any x registers, even if their content contributes to the result.
     * @return relative address for the current instruction
     */
    private int calculateRelativeAddressForJump(
    ) {
        IndexRegister xReg = null;
        int xx = (int)_currentInstruction.getX();
        if (xx != 0) {
            xReg = getExecOrUserXRegister(xx);
        }

        long addend1;
        long addend2 = 0;
        if (_designatorRegister.getBasicModeEnabled()) {
            addend1 = _currentInstruction.getU();
            if (xReg != null) {
                addend2 = xReg.getSignedXM();
            }
        } else {
            addend1 = _currentInstruction.getU();
            if (xReg != null) {
                if (_designatorRegister.getExecutive24BitIndexingEnabled()
                    && (_designatorRegister.getProcessorPrivilege() < 2)) {
                    //  Exec 24-bit indexing is requested
                    addend2 = xReg.getSignedXM24();
                } else {
                    addend2 = xReg.getSignedXM();
                }
            }
        }

        return (int) OnesComplement.add36Simple(addend1, addend2);
    }

    /**
     * Checks the given absolute address and comparison type against the breakpoint register to see whether
     * we should take a breakpoint.  Updates IKR appropriately.
     * @param comparison comparison type
     * @param absoluteAddress absolute address to be compared
     */
    private void checkBreakpoint(
        final BreakpointComparison comparison,
        final AbsoluteAddress absoluteAddress
    ) {
        if (_breakpointAddress.equals(absoluteAddress)
                && (((comparison == BreakpointComparison.Fetch) && _breakpointRegister.getFetchFlag())
                    || ((comparison == BreakpointComparison.Read) && _breakpointRegister.getReadFlag())
                    || ((comparison == BreakpointComparison.Write) && _breakpointRegister.getWriteFlag()))) {
            //TODO Per doc, 2.4.1.2 Breakpoint_Register - we need to halt if Halt Enable is set
            //      which means Stop Right Now... how do we do that for all callers of this code?
            _indicatorKeyRegister.setBreakpointRegisterMatchCondition(true);
        }
    }

    /**
     * If an interrupt is pending, handle it.
     * If not, check certain conditions to see if one of several certain interrupt classes needs to be raised.
     * @return true if we did something useful, else false
     * @throws MachineInterrupt if we need to cause an interrupt to be raised
     */
    private boolean checkPendingInterrupts(
    ) throws MachineInterrupt {
        //  Is there an interrupt pending?  If so, handle it
        if (_pendingInterrupt != null) {
            handleInterrupt();
            return true;
        }

        //  Are there any pending conditions which need to be turned into interrupts?
        if (_indicatorKeyRegister.getBreakpointRegisterMatchCondition() && !_midInstructionInterruptPoint) {
            if (_breakpointRegister.getHaltFlag()) {
                stop(StopReason.Breakpoint, 0);
                return true;
            } else {
                throw new BreakpointInterrupt();
            }
        }

        if (_quantumTimer.isNegative() && _designatorRegister.getQuantumTimerEnabled()) {
            throw new QuantumTimerInterrupt();
        }

        if (_indicatorKeyRegister.getSoftwareBreak() && !_midInstructionInterruptPoint) {
            throw new SoftwareBreakInterrupt();
        }

        if (_jumpHistoryThresholdReached && _jumpHistoryFullInterruptEnabled && !_midInstructionInterruptPoint) {
            throw new JumpHistoryFullInterrupt();
        }

        return false;
    }

    /**
     * Creates a new entry in the conditionalJump history table.
     * If we cross the interrupt threshold, set the threshold-reached flag.
     * @param value absolute address to be placed into the conditionalJump history table
     */
    private void createJumpHistoryTableEntry(
        final long value
    ) {
        _jumpHistoryTable[_jumpHistoryTableNext].setW(value);

        if (_jumpHistoryTableNext > JUMP_HISTORY_TABLE_THRESHOLD ) {
            _jumpHistoryThresholdReached = true;
        }

        if (_jumpHistoryTableNext == JUMP_HISTORY_TABLE_SIZE ) {
            _jumpHistoryTableNext = 0;
        }
    }

    /**
     * Starts or continues the process of executing the instruction in _currentInstruction.
     * Don't call this if IKR.INF is not set.
     * @throws MachineInterrupt if an interrupt needs to be raised
     * @throws UnresolvedAddressException if a basic mode indirect address is not entirely resolved
     */
    protected void executeInstruction(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Call the function handler, then keep INF (instruction in F0) true if we have not completed
        //  the instruction (MidInstIntPt == true), or false if we are not (MidInstIntPt == false).
        //  It is up to the function handler to:
        //      * set or clear m_MidInstructionInterruptPoint as appropriate
        //      * properly store instruction mid-point state if it returns mid-instruction
        //      * detect and restore instruction mid-point state if it is subsequently called
        //          after returning in mid-point state.
        FunctionHandler handler = FunctionTable.lookup(_currentInstruction, _designatorRegister.getBasicModeEnabled());
        if (handler == null) {
            _midInstructionInterruptPoint = false;
            _indicatorKeyRegister.setInstructionInF0(false);
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }

        handler.handle(this, _currentInstruction);
        _indicatorKeyRegister.setInstructionInF0(_midInstructionInterruptPoint);
        if (!_midInstructionInterruptPoint) {
            //  instruction is done - clear storage locks
            synchronized(_storageLocks) {
                _storageLocks.get(this).clear();
            }
        }
    }

    /**
     * Fetches the next instruction based on the current program address register,
     * and places it in the current instruction register.
     * Basic mode:
     *  We cannot fetch from a large bank (EX, EXR can refer to an instruction in a large bank, but cannot be in one)
     *  We must check upper and lower limits unless the program counter is 0777777 (why? I don't know...)
     *  Since we use findBasicModeBank(), we are assured of this check automatically.
     *  We need to determine which base register we execute from, and read permission is needed for the corresponding bank.
     * Extended mode:
     *  We cannot fetch from a large bank (EX, EXR can refer to an instruction in a large bank, but cannot be in one)
     *  We must check upper and lower limits unless the program counter is 0777777 (why? I don't know...)
     *  Access is not checked, as GAP and SAP for enter access are applied at the time the bank is based on B0,
     *  and if that passes, GAP and SAP read access are automatically set true.
     *  EX and EXR targets still require read-access checks.
     * @throws MachineInterrupt if an interrupt needs to be raised
     */
    private void fetchInstruction(
    ) throws MachineInterrupt {
        _midInstructionInterruptPoint = false;
        boolean basicMode = _designatorRegister.getBasicModeEnabled();
        int programCounter = _programAddressRegister.getProgramCounter();

        BaseRegister bReg;
        if (basicMode) {
            int baseRegisterIndex = findBasicModeBank(programCounter, true);
            if (baseRegisterIndex == 0) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
            }

            bReg = _baseRegisters[baseRegisterIndex];
            if (!isReadAllowed(bReg)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
            }
        } else {
            bReg = _baseRegisters[0];
            bReg.checkAccessLimits(programCounter, true, false, false, _indicatorKeyRegister.getAccessInfo());
        }

        if (bReg._voidFlag || bReg._largeSizeFlag) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }

        int pcOffset = programCounter - bReg._lowerLimitNormalized;
        _currentInstruction.setW(bReg._storage.get(pcOffset));
        _indicatorKeyRegister.setInstructionInF0(true);
    }

    /**
     * Retrieves a BankDescriptor to describe the given named bank.  This is for interrupt handling.
     * The bank name is in L,BDI format.
     * @param bankLevel level of the bank, 0:7
     * @param bankDescriptorIndex BDI of the bank 0:077777
     * @return BankDescriptor object unless l,bdi is 0,0, in which case we return null
     */
    private BankDescriptor findBankDescriptor(
        final int bankLevel,
        final int bankDescriptorIndex
    ) throws AddressingExceptionInterrupt {
        // The bank descriptor tables for bank levels 0 through 7 are described by the banks based on B16 through B23.
        // The bank descriptor will be the {n}th bank descriptor in the particular bank descriptor table,
        // where {n} is the bank descriptor index.
        int bdRegIndex = bankLevel + 16;
        if (_baseRegisters[bdRegIndex]._voidFlag) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   bankLevel,
                                                   bankDescriptorIndex);
        }

        //  bdStorage contains the BDT for the given bank_name level
        //  bdTableOffset indicates the offset into the BDT, where the bank descriptor is to be found.
        ArraySlice bdStorage = _baseRegisters[bdRegIndex]._storage;
        int bdTableOffset = bankDescriptorIndex * 8;    // 8 being the size of a BD in words
        if (bdTableOffset + 8 > bdStorage.getSize()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   bankLevel,
                                                   bankDescriptorIndex);
        }

        //  Create and return a BankDescriptor object
        return new BankDescriptor(bdStorage, bdTableOffset);
    }

    /**
     * Locates the index of the base register which represents the bank which contains the given relative address.
     * Does appropriate limits checking.  Delegates to the appropriate basic or extended mode implementation.
     * @param relativeAddress relative address to be considered
     * @param updateDesignatorRegister if true and if we are in basic mode, we update the basic mode bank selection bit
     *                                 in the designator register if necessary
     * @return base register index
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    private int findBaseRegisterIndex(
        final int relativeAddress,
        final boolean updateDesignatorRegister
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (_designatorRegister.getBasicModeEnabled()) {
            //  Find the bank containing the current offset.
            //  We don't need to check for storage limits, since this is done for us by findBasicModeBank() in terms of
            //  returning a zero.
            int brIndex = findBasicModeBank(relativeAddress, updateDesignatorRegister);
            if (brIndex == 0) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
            }

            //  Are we doing indirect addressing?
            if (_currentInstruction.getI() != 0) {
                //  Increment the X register (if any) indicated by F0 (if H bit is set, of course)
                incrementIndexRegisterInF0();
                BaseRegister br = _baseRegisters[brIndex];

                //  Ensure we can read from the selected bank
                if (!isReadAllowed(br)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }
                br.checkAccessLimits(relativeAddress, false, true, false, _indicatorKeyRegister.getAccessInfo());

                //  Get xhiu fields from the referenced word, and place them into _currentInstruction,
                //  then throw UnresolvedAddressException so the caller knows we're not done here.
                int wx = relativeAddress - br._lowerLimitNormalized;
                _currentInstruction.setXHIU(br._storage.get(wx));
                throw new UnresolvedAddressException();
            }

            //  We're at our final destination
            return brIndex;
        } else {
            return getEffectiveBaseRegisterIndex();
        }
    }

    /**
     * Given a relative address, we determine which (if any) of the basic mode banks based on BDR12-15
     * are to be selected for that address.
     * We do NOT evaluate whether the bank has any particular permissions, or whether we have any access thereto.
     * @param relativeAddress relative address for which we search for a containing bank
     * @param updateDB31 set true to update DB31 if we cross primary/secondary bank pairs
     * @return the bank register index for the bank which contains the given relative address if found,
     *          else zero if the address is not within any based bank limits.
     */
    private int findBasicModeBank(
        final int relativeAddress,
        final boolean updateDB31
    ) {
        boolean db31Flag = _designatorRegister.getBasicModeBaseRegisterSelection();
        int[] table = db31Flag ? BASE_REGISTER_CANDIDATES_TRUE : BASE_REGISTER_CANDIDATES_FALSE;

        for (int tx = 0; tx < 4; ++tx) {
            //  See IP PRM 4.4.5 - select the base register from the selection table.
            //  If the bank is void, skip it.
            //  If the program counter is outside of the bank limits, skip it.
            //  Otherwise, we found the BDR we want to use.
            BaseRegister bReg = _baseRegisters[table[tx]];
            if (isWithinLimits(bReg, relativeAddress)) {
                if (updateDB31 && (tx >= 2)) {
                    //  address is found in a secondary bank, so we need to flip DB31
                    _designatorRegister.setBasicModeBaseRegisterSelection(!db31Flag);
                }

                return table[tx];
            }
        }

        return 0;
    }

    /**
     * Determines the base register to be used for an extended mode instruction,
     * using the designator bit to indicate whether to use exec or user banks,
     * and whether we are using the I bit to extend the B field.
     * (Exec base registers are B16-B31).
     * @return base register index
     */
    private int getEffectiveBaseRegisterIndex(
    ) {
        //  If PP < 2, we use the i-bit and the b-field to select the base registers from B0 to B31.
        //  For PP >= 2, we only use the b-field, to select base registers from B0 to B15 (See IP PRM 4.3.7).
        if (_designatorRegister.getProcessorPrivilege() < 2) {
            return (int)_currentInstruction.getIB();
        } else {
            return (int)_currentInstruction.getB();
        }
    }

    /**
     * Retrieves the AccessPermissions object applicable for the bank described by the given baseRegister,
     * within the context of our current key/ring.
     * @param baseRegister base register of interest
     * @return access permissions object
     */
    private AccessPermissions getEffectivePermissions(
        final BaseRegister baseRegister
    ) {
        AccessInfo tempInfo = new AccessInfo(_indicatorKeyRegister.getAccessKey());

        // If we are at a more-privileged ring than the base register's ring, use the base register's special access permissions.
        if (tempInfo._ring < baseRegister._accessLock._ring) {
            return baseRegister._specialAccessPermissions;
        }

        // If we are in the same domain as the base register, again, use the special access permissions.
        if (tempInfo._domain == baseRegister._accessLock._domain) {
            return baseRegister._specialAccessPermissions;
        }

        // Otherwise, use the general access permissions.
        return baseRegister._generalAccessPermissions;
    }

    /**
     * Retrieves the GRS index of the exec or user register indicated by the register index...
     * e.g., registerIndex == 0 returns the GRS index for either R0 or ER0, depending on the designator register.
     * @param registerIndex R register index of interest
     * @return GRS index
     */
    private int getExecOrUserRRegisterIndex(
        final int registerIndex
    ) {
        return registerIndex + (_designatorRegister.getExecRegisterSetSelected() ? GeneralRegisterSet.ER0 : GeneralRegisterSet.R0);
    }

    /**
     * Retrieves the GRS index of the exec or user register indicated by the register index...
     * e.g., registerIndex == 0 returns the GRS index for either X0 or EX0, depending on the designator register.
     * @param registerIndex X register index of interest
     * @return GRS index
     */
    private int getExecOrUserXRegisterIndex(
        final int registerIndex
    ) {
        return registerIndex + (_designatorRegister.getExecRegisterSetSelected() ? GeneralRegisterSet.EX0 : GeneralRegisterSet.X0);
    }

    /**
     * Handles the current pending interrupt.  Do not call if no interrupt is pending.
     * @throws MachineInterrupt if some other interrupt needs to be raised
     */
    private void handleInterrupt(
    ) throws MachineInterrupt {
        // Get pending interrupt, save it to lastInterrupt, and clear pending.
        MachineInterrupt interrupt = _pendingInterrupt;
        _pendingInterrupt = null;
        _lastInterrupt = interrupt;

        // Are deferrable interrupts allowed?  If not, ignore the interrupt
        if (!_designatorRegister.getDeferrableInterruptEnabled()
            && (interrupt.getDeferrability() == MachineInterrupt.Deferrability.Deferrable)) {
            return;
        }

        //TODO If the Reset Indicator is set and this is a non-initial exigent interrupt, then error halt and set an
        //      SCF readable “register” to indicate that a Reset failure occurred.

        // Update interrupt-specific portions of the IKR
        _indicatorKeyRegister.setShortStatusField(interrupt.getShortStatusField());
        _indicatorKeyRegister.setInterruptClassField(interrupt.getInterruptClass().getCode());

        // Make sure the interrupt control stack base register is valid
        if (_baseRegisters[ICS_BASE_REGISTER]._voidFlag) {
            stop(StopReason.ICSBaseRegisterInvalid, 0);
            return;
        }

        // Acquire a stack frame, and verify limits
        IndexRegister icsXReg = (IndexRegister)_generalRegisterSet.getRegister(ICS_INDEX_REGISTER);
        icsXReg.decrementModifier18();
        long stackOffset = icsXReg.getH2();
        long stackFrameSize = icsXReg.getXI();
        long stackFrameLimit = stackOffset + stackFrameSize;
        if ((stackFrameLimit - 1 > _baseRegisters[ICS_BASE_REGISTER]._upperLimitNormalized)
            || (stackOffset < _baseRegisters[ICS_BASE_REGISTER]._lowerLimitNormalized)) {
            stop(StopReason.ICSOverflow, 0);
            return;
        }

        // Populate the stack frame in storage.
        ArraySlice icsStorage = _baseRegisters[ICS_BASE_REGISTER]._storage;
        if (stackFrameLimit > icsStorage.getSize()) {
            stop(StopReason.ICSBaseRegisterInvalid, 0);
            return;
        }

        int sx = (int)stackOffset;
        icsStorage.set(sx, _programAddressRegister.getW());
        icsStorage.set(sx + 1, _designatorRegister.getW());
        icsStorage.set(sx + 2, _indicatorKeyRegister.getW());
        icsStorage.set(sx + 3, _quantumTimer.getW());
        icsStorage.set(sx + 4, interrupt.getInterruptStatusWord0().getW());
        icsStorage.set(sx + 5, interrupt.getInterruptStatusWord1().getW());

        //TODO other stuff which needs to be preserved - IP PRM 5.1.3
        //      e.g., results of stuff that we figure out prior to generating U in Basic Mode maybe?
        //      or does it hurt anything to just regenerate that?  We /would/ need the following two lines...
        //pStack[6].setS1( m_PreservedProgramAddressRegisterValid ? 1 : 0 );
        //pStack[7].setValue( m_PreservedProgramAddressRegister.getW() );

        // Create conditionalJump history table entry
        createJumpHistoryTableEntry(_programAddressRegister.getW());

        // The bank described by B16 begins with 64 contiguous words, indexed by interrupt class (of which there are 64).
        // Each word is a Program Address Register word, containing the L,BDI,Offset of the interrupt handling routine
        // Make sure B16 is valid before dereferencing through it.
        if (_baseRegisters[L0_BDT_BASE_REGISTER]._voidFlag) {
            stop(StopReason.L0BaseRegisterInvalid, 0);
            return;
        }

        //  intStorage points to level 0 BDT, the first 64 words of which comprise the interrupt vectors.
        //  intOffset is the offset from the start of the BDT, to the vector we're interested in.
        //  PAR will be set to L,BDI,Address of the appropriate interrupt handler.
        //  Note that the interrupt handler code bank is NOT YET based on B0...
        ArraySlice intStorage = _baseRegisters[L0_BDT_BASE_REGISTER]._storage;
        int intOffset = interrupt.getInterruptClass().getCode();
        if (intOffset >= icsStorage.getSize()) {
            stop(StopReason.InterruptHandlerOffsetOutOfRange, 0);
            return;
        }

        _programAddressRegister.setW(intStorage.get(intOffset));

        // Set designator register per IP PRM 5.1.5
        //  We'll set/clear Basic Mode later once we've got the interrupt handler bank
        boolean fhip = _designatorRegister.getFaultHandlingInProgress();
        _designatorRegister.clear();
        _designatorRegister.setExecRegisterSetSelected(true);
        _designatorRegister.setArithmeticExceptionEnabled(true);
        _designatorRegister.setFaultHandlingInProgress(fhip);

        if (interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.HardwareCheck) {
            if (fhip) {
                stop(StopReason.InterruptHandlerHardwareFailure, 0);
                return;
            }
            _designatorRegister.setFaultHandlingInProgress(true);
        }

        // Clear the IKR and F0
        _indicatorKeyRegister.clear();
        _currentInstruction.clear();

        // Base the PAR-indicated interrupt handler bank on B0
        //TODO WE should use standard bank-manipulation algorithm here - see hardware manual 4.6.4
        byte ihBankLevel = (byte)_programAddressRegister.getLevel();
        short ihBankDescriptorIndex = (short)_programAddressRegister.getBankDescriptorIndex();
        if ((ihBankLevel == 0) && (ihBankDescriptorIndex < 32)) {
            stop(StopReason.InterruptHandlerInvalidLevelBDI, 0);
            return;
        }

        //  Retrieve a BankDescriptor object, ensure the bank type is acceptable, and base the bank.
        BankDescriptor bankDescriptor = findBankDescriptor(ihBankLevel, ihBankDescriptorIndex);
        BankDescriptor.BankType ihBankType = bankDescriptor.getBankType();
        if ((ihBankType != BankDescriptor.BankType.ExtendedMode) && (ihBankType != BankDescriptor.BankType.BasicMode)) {
            stop(StopReason.InterruptHandlerInvalidBankType, 0);
            return;
        }

        _baseRegisters[0] = new BaseRegister(bankDescriptor);
        _designatorRegister.setBasicModeBaseRegisterSelection(ihBankType == BankDescriptor.BankType.BasicMode);//TODO is this right?
    }

    /**
     * Checks a base register to see if we can read from it, given our current key/ring
     * @param baseRegister register of interest
     * @return true if we have read permission for the bank based on the given register
     */
    private boolean isReadAllowed(
        final BaseRegister baseRegister
    ) {
        return getEffectivePermissions(baseRegister)._read;
    }

    /**
     * Indicates whether the given offset is within the addressing limits of the bank based on the given register.
     * If the bank is void, then the offset is clearly not within limits.
     * @param baseRegister register of interest
     * @param offset address offset
     * @return true if the address is within the limits of the bank based on the given register
     */
    private boolean isWithinLimits(
        final BaseRegister baseRegister,
        final int offset
    ) {
        return !baseRegister._voidFlag
               && (offset >= baseRegister._lowerLimitNormalized)
               && (offset <= baseRegister._upperLimitNormalized);
    }

    /**
     * If no other interrupt is pending, or the new interrupt is of a higher priority,
     * set the new interrupt as the pending interrupt.  Any lower-priority interrupt is dropped or ignored.
     * @param interrupt interrupt of interest
     */
    private void raiseInterrupt(
        final MachineInterrupt interrupt
    ) {
        if ((_pendingInterrupt == null)
            || (interrupt.getInterruptClass().getCode() < _pendingInterrupt.getInterruptClass().getCode())) {
            _pendingInterrupt = interrupt;
        }
    }

    /**
     * Set a storage lock for the given absolute address.
     * If this IP already has locks, we die horribly - this is how we avoid internal deadlocks
     * If the address is already locked by any other IP, then we wait until it is not.
     * Then we lock it to this IP.
     * NOTE: All storage locks are cleared automatically at the conclusion of processing an instruction.
     * @param absAddress absolute address of interest
     */
    private void setStorageLock(
        final AbsoluteAddress absAddress
    ) {
        synchronized(_storageLocks) {
            assert(_storageLocks.get(this).isEmpty());
        }

        boolean done = false;
        while (!done) {
            synchronized(_storageLocks) {
                boolean okay = true;
                Iterator<Map.Entry<InstructionProcessor, HashSet<AbsoluteAddress>>> it = _storageLocks.entrySet().iterator();
                while (okay && it.hasNext()) {
                    Map.Entry<InstructionProcessor, HashSet<AbsoluteAddress>> pair = it.next();
                    InstructionProcessor ip = pair.getKey();
                    HashSet<AbsoluteAddress> lockedAddresses = pair.getValue();
                    if (ip != this) {
                        if (lockedAddresses.contains(absAddress)) {
                            okay = false;
                            break;
                        }
                    }
                }

                if (okay) {
                    _storageLocks.get(this).add(absAddress);
                    done = true;
                }
            }

            if (!done) {
                Thread.yield();
            }
        }
    }

    /**
     * As above, but for multiple addresses.
     * NOTE: All storage locks are cleared automatically at the conclusion of processing an instruction.
     * @param absAddresses array of addresses
     */
    private void setStorageLocks(
        final AbsoluteAddress[] absAddresses
    ) {
        synchronized(_storageLocks) {
            assert(_storageLocks.get(this).isEmpty());
        }

        boolean done = false;
        while (!done) {
            synchronized(_storageLocks) {
                boolean okay = true;
                Iterator<Map.Entry<InstructionProcessor, HashSet<AbsoluteAddress>>> it = _storageLocks.entrySet().iterator();
                while (okay && it.hasNext()) {
                    Map.Entry<InstructionProcessor, HashSet<AbsoluteAddress>> pair = it.next();
                    InstructionProcessor ip = pair.getKey();
                    HashSet<AbsoluteAddress> lockedAddresses = pair.getValue();
                    if (ip != this) {
                        for (AbsoluteAddress checkAddress : absAddresses) {
                            if (lockedAddresses.contains(checkAddress)) {
                                okay = false;
                                break;
                            }
                        }
                    }
                }

                if (okay) {
                    for (AbsoluteAddress absAddr : absAddresses) {
                        _storageLocks.get(this).add(absAddr);
                    }
                    done = true;
                }
            }

            if (!done) {
                Thread.yield();
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Async thread entry point
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Entry point for the async Worker part of this object
     */
    @Override
    public void run(
    ) {
        LOGGER.info(String.format("InstructionProcessor worker %s Starting", getName()));
        synchronized(_storageLocks) {
            _storageLocks.put(this, new HashSet<AbsoluteAddress>());
        }

        while (!_workerTerminate) {
            // If the virtual processor is not running, then the thread does nothing other than sleep slowly
            if (!_runningFlag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            } else {
                //  Deal with pending interrupts, or conditions which will create a new pending interrupt.
                boolean somethingDone = false;
                try {
                    //  check for pending interrupts
                    somethingDone = checkPendingInterrupts();

                    //  If we don't have an instruction in F0, fetch one.
                    if (!somethingDone && !_indicatorKeyRegister.getInstructionInF0()) {
                        fetchInstruction();
                        somethingDone = true;
                    }

                    //  Execute the instruction in F0.
                    if (!somethingDone) {
                        _midInstructionInterruptPoint = false;
                        try {
                            executeInstruction();
                        } catch (UnresolvedAddressException ex) {
                            //  This is not surprising - can happen for basic mode indirect addressing.
                            //  Update the quantum timer so we can (eventually) interrupt a long or infinite sequence.
                            _midInstructionInterruptPoint = true;
                            if (_designatorRegister.getQuantumTimerEnabled()) {
                                _quantumTimer.add(Word36.NEGATIVE_ONE.getW());
                            }
                        }

                        if (!_midInstructionInterruptPoint) {
                            // Instruction is complete.  Maybe increment PAR.PC
                            if (_preventProgramCounterIncrement) {
                                _preventProgramCounterIncrement = false;
                            } else {
                                _programAddressRegister.setProgramCounter(_programAddressRegister.getProgramCounter() + 1);
                            }

                            //  Update IKR and (maybe) the quantum timer
                            _indicatorKeyRegister.setInstructionInF0(false);
                            if (_designatorRegister.getQuantumTimerEnabled()) {
                                _quantumTimer.add(OnesComplement.negate36(_currentInstructionHandler.getQuantumTimerCharge()));
                            }

                            // Should we stop, given that we've completed an instruction?
                            if (_currentRunMode == RunMode.SingleInstruction) {
                                stop(StopReason.Debug, 0);
                            }
                        }

                        somethingDone = true;
                    }
                } catch (MachineInterrupt interrupt) {
                    raiseInterrupt(interrupt);
                    somethingDone = true;
                }

                // End of the cycle - should we stop?
                if (_currentRunMode == RunMode.SingleCycle) {
                    stop(StopReason.Debug, 0);
                }

                if (!somethingDone) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        synchronized(_storageLocks) {
            _storageLocks.remove(this);
        }

        LOGGER.info(String.format("InstructionProcessor worker %s Terminating", getName()));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public instance methods (only for consumption by FunctionHandlers)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * An algorithm for handling bank transitions for the following instructions:
     *  CALL, GOTO, LAE, LBE, LBJ, LBU, LDJ, LIJ, RNT, TVA, and UR
     * and for interrupt handling.
     * @param instruction type of instruction or null if we are invoked for interrupt handling
     * @param operand from U for instruction processing, zero fro interrupt handling
     * @param interrupt reference to the interrupt being handled, null if invoked from instruction handling
     * @throws AddressingExceptionInterrupt if IS==3 for any LxJ instruction
     *                                      or source L,BDI is invalid
     *                                      or a void bank is specified where it is not allowed
     *                                      or for an invalid bank type in various situations
     *                                      or general fault set on destination bank
     * @throws InvalidInstructionInterrupt for LBU with B0 or B1 specified as destination
     * @throws RCSGenericStackUnderflowOverflowInterrupt for return operaions for which there is no existing stack frame
     */
    public void bankManipulation(
        final InstructionHandler.Instruction instruction,
        final long operand,
        final MachineInterrupt interrupt
    ) throws AddressingExceptionInterrupt,
             InvalidInstructionInterrupt,
             RCSGenericStackUnderflowOverflowInterrupt {
        BankManipulationInfo bmInfo = new BankManipulationInfo(this, instruction, operand, interrupt);
        while (bmInfo._nextStep != 0) {
            _bankManipulationSteps[bmInfo._nextStep].handler(bmInfo);
        }
    }

    /**
     * Protected workings of the CR instruction.
     * If A(a) matches the contents of U, then A(a+1) is written to U
     * @return true if A(a) matched the contents of U, else false
     * @throws MachineInterrupt if an interrupt needs to be raised
     * @throws UnresolvedAddressException if an address is not fully resolved (basic mode indirect address only)
     */
    public boolean conditionalReplace(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        baseRegister.checkAccessLimits(relAddress, false, true, true, _indicatorKeyRegister.getAccessInfo());
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);

        long value;
        checkBreakpoint(BreakpointComparison.Read, absAddress);
        try {
            value = _inventoryManager.getStorageValue(absAddress);
        } catch (AddressLimitsException
            | UPINotAssignedException
            | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }

        if (value == this.getExecOrUserARegister((int) _currentInstruction.getA()).getW()) {
            checkBreakpoint(BreakpointComparison.Write, absAddress);
            long newValue = this.getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();
            try {
                _inventoryManager.setStorageValue(absAddress, newValue);
                return true;
            } catch (AddressLimitsException
                | UPINotAssignedException
                | UPIProcessorTypeException ex) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
            }
        }

        return false;
    }

    /**
     * Calculates the raw relative address (the U) for the current instruction presuming basic mode (even if it isn't set),
     * honors any indirect addressing, and returns the index of the basic mode bank (12-15) which corresponds to the
     * final address, increment the X registers if/as appropriate, but not updating the designator register.
     * Mainly for TRA instruction...
     * @return relative address for the current instruction
     */
    public int getBasicModeBankRegisterIndex(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        IndexRegister xReg = null;
        int xx = (int) _currentInstruction.getX();
        if (xx != 0) {
            xReg = getExecOrUserXRegister(xx);
        }

        long addend1;
        long addend2 = 0;
        if (_designatorRegister.getBasicModeEnabled()) {
            addend1 = _currentInstruction.getU();
            if (xReg != null) {
                addend2 = xReg.getSignedXM();
            }

            long relativeAddress = OnesComplement.add36Simple(addend1, addend2);
            if (relativeAddress == 0777777) {
                relativeAddress = 0;
            }

            int brIndex = findBasicModeBank((int) relativeAddress, false);

            //  Did we find a bank, and are we doing indirect addressing?
            if ((brIndex > 0) && (_currentInstruction.getI() != 0)) {
                //  Increment the X register (if any) indicated by F0 (if H bit is set, of course)
                incrementIndexRegisterInF0();
                BaseRegister br = _baseRegisters[brIndex];

                //  Ensure we can read from the selected bank
                if (!isReadAllowed(br)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }
                br.checkAccessLimits((int) relativeAddress, false, true, false, _indicatorKeyRegister.getAccessInfo());

                //  Get xhiu fields from the referenced word, and place them into _currentInstruction,
                //  then throw UnresolvedAddressException so the caller knows we're not done here.
                int wx = (int) relativeAddress - br._lowerLimitNormalized;
                _currentInstruction.setXHIU(br._storage.get(wx));
                throw new UnresolvedAddressException();
            }

            //  We're at our final destination
            return brIndex;
        } else {
            //  We have an explicit base register - check limits
            addend1 = _currentInstruction.getD();
            if (xReg != null) {
                if (_designatorRegister.getExecutive24BitIndexingEnabled()
                    && (_designatorRegister.getProcessorPrivilege() < 2)) {
                    //  Exec 24-bit indexing is requested
                    addend2 = xReg.getSignedXM24();
                } else {
                    addend2 = xReg.getSignedXM();
                }
            }

            long relativeAddress = OnesComplement.add36Simple(addend1, addend2);
            if (relativeAddress == 0777777) {
                relativeAddress = 0;
            }

            int brIndex = (int) _currentInstruction.getB();
            BaseRegister br = _baseRegisters[brIndex];
            try {
                br.checkAccessLimits((int) relativeAddress, false);
            } catch (ReferenceViolationInterrupt ex) {
                brIndex = 0;
            }
            return brIndex;
        }
    }

    /**
     * Retrieves consecutive word values for double or multiple-word transfer operations (e.g., DL, LRS, etc).
     * The assumption is that this call is made for a single iteration of an instruction.  Per doc 9.2, effective
     * relative address (U) will be calculated only once; however, access checks must succeed for all accesses.
     * We presume we are retrieving from GRS or from storage - i.e., NOT allowing immediate addressing.
     * Also, we presume that we are doing full-word transfers - no partial word.
     * @param grsCheck true if we should check U to see if it is a GRS location
     * @param operands Where we store the resulting operands - the length of this array defines how many operands we retrieve
     * @throws MachineInterrupt if an interrupt needs to be raised
     * @throws UnresolvedAddressException if an address is not fully resolved (basic mode indirect address only)
     */
    public void getConsecutiveOperands(
        final boolean grsCheck,
        long[] operands
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Get the relative address so we can do a grsCheck
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        incrementIndexRegisterInF0();

        //  If this is a GRS reference - we do not need to look for containing banks or validate storage limits.
        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            //  For multiple accesses, advancing beyond GRS 0177 wraps back to zero.
            //  Do accessibility checks for each GRS access
            int grsIndex = relAddress;
            for (int ox = 0; ox < operands.length; ++ox, ++grsIndex) {
                if (grsIndex == 0200) {
                    grsIndex = 0;
                }

                if (!GeneralRegisterSet.isAccessAllowed(grsIndex, _designatorRegister.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }

                operands[ox] = _generalRegisterSet.getRegister(grsIndex).getW();
            }

            return;
        }

        //  Get base register and check storage and access limits
        int brIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister bReg = _baseRegisters[brIndex];
        bReg.checkAccessLimits(relAddress, operands.length, true, false, _indicatorKeyRegister.getAccessInfo());

        //  Lock the storage
        AbsoluteAddress[] absAddresses = new AbsoluteAddress[operands.length];
        for (int ax = 0; ax < operands.length; ++ax ) {
            absAddresses[ax] = getAbsoluteAddress(bReg, relAddress + ax);
        }
        setStorageLocks(absAddresses);

        //  Retrieve the operands
        int offset = relAddress - bReg._lowerLimitNormalized;
        for (int ox = 0; ox < operands.length; ++ox) {
            checkBreakpoint(BreakpointComparison.Read, absAddresses[ox]);
            operands[ox] = bReg._storage.get(offset++);
        }
    }

    /**
     * Retrieves a reference to the GeneralRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either A0 or EA0, depending on the designator register.
     * @param registerIndex A register index of interest
     * @return GRS register
     */
    public GeneralRegister getExecOrUserARegister(
        final int registerIndex
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserARegisterIndex(registerIndex));
    }

    /**
     * Retrieves the GRS index of the exec or user register indicated by the register index...
     * i.e., registerIndex == 0 returns the GRS index for either A0 or EA0, depending on the designator register.
     * @param registerIndex A register index of interest
     * @return GRS register index
     */
    public int getExecOrUserARegisterIndex(
        final int registerIndex
    ) {
        return registerIndex + (_designatorRegister.getExecRegisterSetSelected() ? GeneralRegisterSet.EA0 : GeneralRegisterSet.A0);
    }

    /**
     * Retrieves a reference to the GeneralRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either R0 or ER0, depending on the designator register.
     * @param registerIndex R register index of interest
     * @return GRS register
     */
    public GeneralRegister getExecOrUserRRegister(
        final int registerIndex
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserRRegisterIndex(registerIndex));
    }

    /**
     * Retrieves a reference to the IndexRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either X0 or EX0, depending on the designator register.
     * @param registerIndex X register index of interest
     * @return GRS register
     */
    public IndexRegister getExecOrUserXRegister(
        final int registerIndex
    ) {
        return (IndexRegister)_generalRegisterSet.getRegister(getExecOrUserXRegisterIndex(registerIndex));
    }

    /**
     * It has been determined that the u (and possibly h and i) fields comprise requested data.
     * Load the value indicated in F0 (_currentInstruction) as follows:
     *      For Processor Privilege 0,1
     *          value is 24 bits for DR.11 (exec 24bit indexing enabled) true, else 18 bits
     *      For Processor Privilege 2,3
     *          value is 24 bits for FO.i set, else 18 bits
     * If F0.x is zero, the immediate value is taken from the h,i, and u fields (unsigned), and negative zero is eliminated.
     * For F0.x nonzero, the immediate value is the sum of the u field (unsigned) with the F0.x(mod) signed field.
     *      For Extended Mode, with Processor Privilege 0,1 and DR.11 set, index modifiers are 24 bits; otherwise, they are 18 bits.
     *      For Basic Mode, index modifiers are always 18 bits.
     * In either case, the value will be left alone for j-field=016, and sign-extended for j-field=017.
     * @return immediate operand value
     */
    public long getImmediateOperand(
    ) {
        boolean exec24Index = _designatorRegister.getExecutive24BitIndexingEnabled();
        int privilege = _designatorRegister.getProcessorPrivilege();
        boolean valueIs24Bits = ((privilege < 2) && exec24Index) || ((privilege > 1) && (_currentInstruction.getI() != 0));
        long value;

        if (_currentInstruction.getX() == 0) {
            //  No indexing (x-field is zero).  Value is derived from h, i, and u fields.
            //  Get the value from h,i,u, and eliminate negative zero.
            value = _currentInstruction.getHIU();
            if (value == 0777777) {
                value = 0;
            }

            if ((_currentInstruction.getJ() == 017) && ((value & 0400000) != 0)) {
                value |= 0_777777_000000L;
            }

        } else {
            //  Value is taken only from the u field, and we eliminate negative zero at this point.
            value = _currentInstruction.getU();
            if ( value == 0177777 )
                value = 0;

            //  Add the contents of Xx(m), and do index register incrementation if appropriate.
            IndexRegister xReg = getExecOrUserXRegister((int)_currentInstruction.getX());

            //  24-bit indexing?
           if (!_designatorRegister.getBasicModeEnabled() && (privilege < 2) && exec24Index) {
                //  Add the 24-bit modifier
                value = OnesComplement.add36Simple(value, xReg.getXM24());
                if (_currentInstruction.getH() != 0) {
                    xReg.incrementModifier24();
                }
            } else {
                //  Add the 18-bit modifier
                value = OnesComplement.add36Simple(value, xReg.getXM());
                if (_currentInstruction.getH() != 0) {
                    xReg.incrementModifier18();
                }
            }
        }

        //  Truncate the result to the proper size, then sign-extend if appropriate to do so.
        boolean extend = _currentInstruction.getJ() == 017;
        if (valueIs24Bits) {
            value &= 077_777777L;
            if (extend && (value & 040_000000L) != 0) {
                value |= 0_777700_000000L;
            }
        } else {
            value &= 0_777777L;
            if (extend && (value & 0_400000) != 0) {
                value |= 0_777777_000000L;
            }
        }

        return value;
    }

    /**
     * See getImmediateOperand() above.
     * This is similar, however the calculated U field is only ever 16 or 18 bits, and is never sign-extended.
     * Also, we do not rely upon j-field for anything, as that has no meaning for conditionalJump instructions.
     * @param updateDesignatorRegister if true and if we are in basic mode, we update the basic mode bank selection bit
     *                                 in the designator register if necessary
     * @return conditionalJump operand value
     * @throws MachineInterrupt if an interrupt needs to be raised
     * @throws UnresolvedAddressException if an address is not fully resolved (basic mode indirect address only)
     */
    public int getJumpOperand(
        final boolean updateDesignatorRegister
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForJump();

        //  The following bit is how we deal with indirect addressing for basic mode.
        //  If we are doing that, it will update the U portion of the current instruction with new address information,
        //  then throw UnresolvedAddressException which will eventually route us back through here again, but this
        //  time with new address info (in reladdress), and we keep doing this until we're not doing indirect addressing.
        if (_designatorRegister.getBasicModeEnabled() && (_currentInstruction.getI() != 0)) {
            findBaseRegisterIndex(relAddress, updateDesignatorRegister);
        } else {
            incrementIndexRegisterInF0();
        }

        return relAddress;
    }

    /**
     * The general case of retrieving an operand, including all forms of addressing and partial word access.
     * Instructions which use the j-field as part of the function code will likely set allowImmediate and
     * allowPartial false.
     * @param grsDestination true if we are going to put this value into a GRS location
     * @param grsCheck true if we should consider GRS for addresses < 0200 for our source
     * @param allowImmediate true if we should allow immediate addressing
     * @param allowPartial true if we should do partial word transfers (presuming we are not in a GRS address)
     * @return operand value
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public long getOperand(
        final boolean grsDestination,
        final boolean grsCheck,
        final boolean allowImmediate,
        final boolean allowPartial
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int jField = (int)_currentInstruction.getJ();
        if (allowImmediate) {
            //  j-field is U or XU? If so, get the value from the instruction itself (immediate addressing)
            if (jField >= 016) {
                return getImmediateOperand();
            }
        }

        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        //  Loading from GRS?  If so, go get the value.
        //  If grsDestination is true, get the full value. Otherwise, honor j-field for partial-word transfer.
        //  See hardware guide section 4.3.2 - any GRS-to-GRS transfer is full-word, regardless of j-field.
        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  First, do accessibility checks
            if (!GeneralRegisterSet.isAccessAllowed(relAddress, _designatorRegister.getProcessorPrivilege(), false)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
            }

            //  If we are GRS or not allowing partial word transfers, do a full word.
            //  Otherwise, honor partial word transfering.
            if (grsDestination || !allowPartial) {
                return _generalRegisterSet.getRegister(relAddress).getW();
            } else {
                boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
                return extractPartialWord(_generalRegisterSet.getRegister(relAddress).getW(), jField, qWordMode);
            }
        }

        //  Loading from storage.  Do so, then (maybe) honor partial word handling.
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        baseRegister.checkAccessLimits(relAddress, false, true, false, _indicatorKeyRegister.getAccessInfo());

        incrementIndexRegisterInF0();

        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Read, absAddress);

        int readOffset = relAddress - baseRegister._lowerLimitNormalized;
        long value = baseRegister._storage.get(readOffset);
        if (allowPartial) {
            boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
            value = extractPartialWord(value, jField, qWordMode);
        }

        return value;
    }

    /**
     * Retrieves a partial-word operand from storage, depending upon the values of jField and quarterWordMode.
     * This is never a GRS reference, nor immediate (nor a conditionalJump or shift, for that matter).
     * @param jField not necessarily from j-field, this indicates the partial word to be stored
     * @param quarterWordMode needs to be set true for storing quarter words
     * @return operand value
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public long getPartialOperand(
        final int jField,
        final boolean quarterWordMode
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        baseRegister.checkAccessLimits(relAddress, false, true, false, _indicatorKeyRegister.getAccessInfo());

        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Read, absAddress);

        int readOffset = relAddress - baseRegister._lowerLimitNormalized;
        long value = baseRegister._storage.get(readOffset);
        return extractPartialWord(value, jField, quarterWordMode);
    }

    /**
     * Increments the register indicated by the current instruction (F0) appropriately.
     * Only effective if f.x is non-zero.
     */
    public void incrementIndexRegisterInF0(
    ) {
        if ((_currentInstruction.getX() != 0) && (_currentInstruction.getH() != 0)) {
            IndexRegister iReg = getExecOrUserXRegister((int)_currentInstruction.getX());
            if (!_designatorRegister.getBasicModeEnabled()
                    && (_designatorRegister.getExecutive24BitIndexingEnabled())
                    && (_designatorRegister.getProcessorPrivilege() < 2)) {
                iReg.incrementModifier24();
            } else {
                iReg.incrementModifier18();
            }
        }
    }

    /**
     * The general case of incrementing an operand by some value, including all forms of addressing and partial word access.
     * Instructions which use the j-field as part of the function code will likely set allowPartial false.
     * Sets carry and overflow designators if appropriate.
     * @param grsCheck true if we should consider GRS for addresses < 0200 for our source
     * @param allowPartial true if we should do partial word transfers (presuming we are not in a GRS address)
     * @param incrementValue how much we increment storage by - positive or negative, but always ones-complement
     * @param twosComplement true to use twos-complement arithmetic - otherwise use ones-complement
     * @return true if either the starting or ending value of the operand is +/- zero
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public boolean incrementOperand(
        final boolean grsCheck,
        final boolean allowPartial,
        final long incrementValue,
        final boolean twosComplement
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int jField = (int)_currentInstruction.getJ();
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        //  Loading from GRS?  If so, go get the value.
        //  If grsDestination is true, get the full value. Otherwise, honor j-field for partial-word transfer.
        //  See hardware guide section 4.3.2 - any GRS-to-GRS transfer is full-word, regardless of j-field.
        boolean result = false;
        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  This is a GRS address.  Do accessibility checks
            if (!GeneralRegisterSet.isAccessAllowed(relAddress, _designatorRegister.getProcessorPrivilege(), false)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
            }

            //  Ignore partial-word transfers.
            GeneralRegister reg = _generalRegisterSet.getRegister(relAddress);
            if (twosComplement) {
                long sum = reg.getW();
                if (sum == 0) {
                    result = true;
                }
                sum += OnesComplement.getNative36(incrementValue);
                if (sum == 0) {
                    result = true;
                }

                reg.setW(sum);
                _designatorRegister.setCarry(false);
                _designatorRegister.setOverflow(false);
            } else {
                long sum = reg.getW();
                result = OnesComplement.isZero36(sum);
                OnesComplement.Add36Result ocResult = new OnesComplement.Add36Result();
                OnesComplement.add36(sum, incrementValue, ocResult);
                if (OnesComplement.isZero36(ocResult._sum)) {
                    result = true;
                }

                reg.setW(ocResult._sum);
                _designatorRegister.setCarry(ocResult._carry);
                _designatorRegister.setOverflow(ocResult._overflow);
            }

            return result;
        }

        //  Storage operand.  Maybe do partial-word addressing
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        baseRegister.checkAccessLimits(relAddress, false, true, true, _indicatorKeyRegister.getAccessInfo());

        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Read, absAddress);

        int readOffset = relAddress - baseRegister._lowerLimitNormalized;
        long storageValue = baseRegister._storage.get(readOffset);
        boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
        long sum = allowPartial ? extractPartialWord(storageValue, jField, qWordMode) : storageValue;

        if (twosComplement) {
            if (sum == 0) {
                result = true;
            }
            sum += OnesComplement.getNative36(incrementValue);
            if (sum == 0) {
                result = true;
            }

            _designatorRegister.setCarry(false);
            _designatorRegister.setOverflow(false);
        } else {
            if (OnesComplement.isZero36(sum)) {
                result = true;
            }
            OnesComplement.Add36Result ocResult = new OnesComplement.Add36Result();
            OnesComplement.add36(sum, incrementValue, ocResult);
            if (OnesComplement.isZero36(ocResult._sum)) {
                result = true;
            }

            _designatorRegister.setCarry(ocResult._carry);
            _designatorRegister.setOverflow(ocResult._overflow);
            sum = ocResult._sum;
        }

        long storageResult = allowPartial ? injectPartialWord(storageValue, sum, jField, qWordMode) : sum;
        baseRegister._storage.set(readOffset, storageResult);
        return result;
    }

    /**
     * Updates PAR.PC and sets the prevent-increment flag according to the given parameters.
     * Used for simple conditionalJump instructions.
     * @param counter program counter value
     * @param preventIncrement true to set the prevent-increment flag
     */
    public void setProgramCounter(
        final int counter,
        final boolean preventIncrement
    ) {
        this._programAddressRegister.setProgramCounter(counter);
        this._preventProgramCounterIncrement = preventIncrement;
    }

    /**
     * Stores consecutive word values for double or multiple-word transfer operations (e.g., DS, SRS, etc).
     * The assumption is that this call is made for a single iteration of an instruction.  Per doc 9.2, effective
     * relative address (U) will be calculated only once; however, access checks must succeed for all accesses.
     * We presume that we are doing full-word transfers - no partial word.
     * @param grsCheck true if we should check U to see if it is a GRS location
     * @param operands The operands to be stored
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public void storeConsecutiveOperands(
        final boolean grsCheck,
        long[] operands
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Get the first relative address so we can do a grsCheck
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            //  For multiple accesses, advancing beyond GRS 0177 wraps back to zero.
            //  Do accessibility checks for each GRS access
            int grsIndex = relAddress;
            for (int ox = 0; ox < operands.length; ++ox, ++grsIndex) {
                if (grsIndex == 0200) {
                    grsIndex = 0;
                }

                if (!GeneralRegisterSet.isAccessAllowed(grsIndex, _designatorRegister.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }

                _generalRegisterSet.setRegister(grsIndex, operands[ox]);
            }

            incrementIndexRegisterInF0();
        } else {
            //  Get base register and check storage and access limits
            int brIndex = findBaseRegisterIndex(relAddress, false);
            BaseRegister bReg = _baseRegisters[brIndex];
            bReg.checkAccessLimits(relAddress, operands.length, false, true, _indicatorKeyRegister.getAccessInfo());

            //  Lock the storage
            AbsoluteAddress[] absAddresses = new AbsoluteAddress[operands.length];
            for (int ax = 0; ax < operands.length; ++ax ) {
                absAddresses[ax] = getAbsoluteAddress(bReg, relAddress + ax);
            }
            setStorageLocks(absAddresses);

            //  Store the operands
            int offset = relAddress - bReg._lowerLimitNormalized;
            for (int ox = 0; ox < operands.length; ++ox) {
                checkBreakpoint(BreakpointComparison.Write, absAddresses[ox]);
                bReg._storage.set(offset++, operands[ox]);
            }

            incrementIndexRegisterInF0();
        }
    }

    /**
     * General case of storing an operand either to storage or to a GRS location
     * @param grsSource true if the value came from a register, so we know whether we need to ignore partial-word transfers
     * @param grsCheck true if relative addresses < 0200 should be considered GRS locations
     * @param checkImmediate true if we should consider j-fields 016 and 017 as immediate addressing (and throw away the operand)
     * @param allowPartial true if we should allow partial-word transfers (subject to GRS-GRS transfers)
     * @param operand value to be stored (36 bits significant)
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public void storeOperand(
        final boolean grsSource,
        final boolean grsCheck,
        final boolean checkImmediate,
        final boolean allowPartial,
        final long operand
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  If we allow immediate addressing mode and j-field is U or XU... we do nothing.
        int jField = (int)_currentInstruction.getJ();
        if ((checkImmediate) && (jField >= 016)) {
            return;
        }

        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  First, do accessibility checks
            if (!GeneralRegisterSet.isAccessAllowed(relAddress, _designatorRegister.getProcessorPrivilege(), true)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, true);
            }

            //  If we are GRS or not allowing partial word transfers, do a full word.
            //  Otherwise, honor partial word transfer.
            if (!grsSource && allowPartial) {
                boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
                long originalValue = _generalRegisterSet.getRegister(relAddress).getW();
                long newValue = injectPartialWord(originalValue, operand, jField, qWordMode);
                _generalRegisterSet.setRegister(relAddress, newValue);
            } else {
                _generalRegisterSet.setRegister(relAddress, operand);
            }

            return;
        }

        //  This is going to be a storage thing...
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister bReg = _baseRegisters[baseRegisterIndex];
        bReg.checkAccessLimits(relAddress, false, false, true, _indicatorKeyRegister.getAccessInfo());

        incrementIndexRegisterInF0();

        AbsoluteAddress absAddress = getAbsoluteAddress(bReg, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Write, absAddress);

        int offset = relAddress - bReg._lowerLimitNormalized;
        if (allowPartial) {
            boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
            long originalValue = bReg._storage.get(offset);
            long newValue = injectPartialWord(originalValue, operand, jField, qWordMode);
            bReg._storage.set(offset, newValue);
        } else {
            bReg._storage.set(offset, operand);
        }
    }

    /**
     * Stores the right-most bits of an operand to a partial word in storage.
     * @param operand value to be stored (up to 36 bits significant)
     * @param jField not necessarily from j-field, this indicates the partial word to be stored
     * @param quarterWordMode needs to be set true for storing quarter words
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public void storePartialOperand(
        final long operand,
        final int jField,
        final boolean quarterWordMode
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister bReg = _baseRegisters[baseRegisterIndex];
        bReg.checkAccessLimits(relAddress, false, false, true, _indicatorKeyRegister.getAccessInfo());

        incrementIndexRegisterInF0();

        AbsoluteAddress absAddress = getAbsoluteAddress(bReg, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Write, absAddress);

        int offset = relAddress - bReg._lowerLimitNormalized;
        long originalValue = bReg._storage.get(offset);
        long newValue = injectPartialWord(originalValue, operand, jField, quarterWordMode);
        bReg._storage.set(offset, newValue);
    }

    /**
     * Updates S1 of a lock word under storage lock.
     * Does *NOT* increment the x-register in F0 (if specified), even if the h-bit is set.
     * @param flag if true, we expect the lock to be clear, and we set it.
     *              if false, we expect the lock to be set, and we clear it.
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public void testAndStore(
        final boolean flag
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister bReg = _baseRegisters[baseRegisterIndex];
        bReg.checkAccessLimits(relAddress, false, true, true, _indicatorKeyRegister.getAccessInfo());

        AbsoluteAddress absAddress = getAbsoluteAddress(bReg, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Read, absAddress);

        int offset = relAddress - bReg._lowerLimitNormalized;
        long value = bReg._storage.get(offset);
        if (flag) {
            //  we want to set the lock, so it needs to be clear
            if ((value & 0_010000_000000) != 0) {
                throw new TestAndSetInterrupt(baseRegisterIndex, relAddress);
            }

            value = injectPartialWord(value, 01, InstructionWord.S1, false);
        } else {
            //  We want to clear the lock, so it needs to be set
            if ((value & 0_010000_000000) == 0) {
                throw new TestAndSetInterrupt(baseRegisterIndex, relAddress);
            }

            value = injectPartialWord(value, 0, InstructionWord.S1, false);
        }

        checkBreakpoint(BreakpointComparison.Write, absAddress);
        bReg._storage.set(offset, value);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * IPs have no ancestors
     * @param ancestor proposed ancestor node
     * @return false always
     */
    @Override
    public boolean canConnect(
        final Node ancestor
    ) {
        return false;
    }

    /**
     * For debugging
     * @param writer where we write the dump
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write("");//TODO actually, a whole lot to do here
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Worker interface implementation
     * @return our node name
     */
    @Override
    public String getWorkerName(
    ) {
        return getName();
    }

    /**
     * Starts the instantiated thread
     */
    @Override
    public final void initialize(
    ) {
        _workerThread.start();
        while (!_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Invoked when any other node decides to signal us
     * @param source node from which the signal came
     */
    @Override
    public void signal(
        final Node source
    ) {
        //TODO IPL interrupts
    }

    /**
     * Causes the IP to skip the next instruction.  Implemented by simply incrementing the PC.
     */
    public void skipNextInstruction(
    ) {
        _programAddressRegister.setProgramCounter(_programAddressRegister.getProgramCounter() + 1);
    }

    /**
     * Starts the processor.
     * Since the worker thread is always running, this merely wakes it up so that it can resume instruction processing.
     */
    public void start(
    ) {
        synchronized(this) {
            _runningFlag = true;
            this.notify();
        }
    }

    /**
     * Stops the processor.
     * More accurately, it puts the worker thread into not-running state, such that it no longer processes instructions.
     * Rather, it will simply sleep until such time as it is placed back into running state.
     * @param stopReason enumeration indicating the reason for the stop
     * @param detail 36-bit word further describing the stop reason
     */
    public void stop(
        final StopReason stopReason,
        final long detail
    ) {
        synchronized(this) {
            if (_runningFlag) {
                _latestStopReason = stopReason;
                _latestStopDetail = detail;
                _runningFlag = false;
                System.out.println(String.format("%s Stopping:%s Detail:%o",
                                                 getName(),
                                                 stopReason.toString(),
                                                 _latestStopDetail));//TODO remove later
                LOGGER.error(String.format("%s Stopping:%s Detail:%o",
                                           getName(),
                                           stopReason.toString(),
                                           _latestStopDetail));
                this.notify();
            }
        }
    }

    /**
     * Called during config tear-down - terminate the active thread
     */
    @Override
    public void terminate(
    ) {
        _workerTerminate = true;
        synchronized(_workerThread) {
            _workerThread.notify();
        }

        while (_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Takes a 36-bit value as input, and returns a partial-word value depending upon
     * the partialWordIndicator (presumably taken from the j-field of an instruction)
     * and the quarterWordMode flag (presumably taken from the designator register).
     * @param source 36-bit source word
     * @param partialWordIndicator indicator of the desired partial word
     * @param quarterWordMode true if we're in quarter word mode, else false
     * @return partial word
     */
    private static long extractPartialWord(
        final long source,
        final int partialWordIndicator,
        final boolean quarterWordMode
    ) {
        switch (partialWordIndicator) {
            case InstructionWord.W:     return source & OnesComplement.BIT_MASK_36;
            case InstructionWord.H2:    return Word36.getH2(source);
            case InstructionWord.H1:    return Word36.getH1(source);
            case InstructionWord.XH2:   return Word36.getXH2(source);
            case InstructionWord.XH1:   // XH1 or Q2
                if (quarterWordMode) {
                    return Word36.getQ2(source);
                } else {
                    return Word36.getXH1(source);
                }
            case InstructionWord.T3:    // T3 or Q4
                if (quarterWordMode) {
                    return Word36.getQ4(source);
                } else {
                    return Word36.getXT3(source);
                }
            case InstructionWord.T2:    // T2 or Q3
                if (quarterWordMode) {
                    return Word36.getQ3(source);
                } else {
                    return Word36.getXT2(source);
                }
            case InstructionWord.T1:    // T1 or Q1
                if (quarterWordMode) {
                    return Word36.getQ1(source);
                } else {
                    return Word36.getXT1(source);
                }
            case InstructionWord.S6:    return Word36.getS6(source);
            case InstructionWord.S5:    return Word36.getS5(source);
            case InstructionWord.S4:    return Word36.getS4(source);
            case InstructionWord.S3:    return Word36.getS3(source);
            case InstructionWord.S2:    return Word36.getS2(source);
            case InstructionWord.S1:    return Word36.getS1(source);
        }

        return source;
    }

    /**
     * Converts a relative address to an absolute address.
     * @param baseRegister base register associated with the relative address
     * @param relativeAddress address to be converted
     * @return absolute address object
     */
    private static AbsoluteAddress getAbsoluteAddress(
        final BaseRegister baseRegister,
        final int relativeAddress
    ) {
        short upi = baseRegister._baseAddress._upi;
        int actualOffset = relativeAddress - baseRegister._lowerLimitNormalized;
        int offset = baseRegister._baseAddress._offset + actualOffset;
        return new AbsoluteAddress(upi, baseRegister._baseAddress._segment, offset);
    }

    /**
     * Takes 36-bit values as original and new values, and injects the new value as a partial word of the original value
     * depending upon the partialWordIndicator (presumably taken from the j-field of an instruction).
     * @param originalValue original value 36-bits significant
     * @param newValue new value right-aligned in a 6, 9, 12, 18, or 36-bit significant field
     * @param partialWordIndicator corresponds to the j-field of an instruction word
     * @param quarterWordMode true to do quarter-word mode transfers, false for third-word mode
     * @return composite value with right-most significant bits of newValue replacing a partial word portion of the
     *          original value
     */
    private static long injectPartialWord(
        final long originalValue,
        final long newValue,
        final int partialWordIndicator,
        final boolean quarterWordMode
    ) {
        switch (partialWordIndicator) {
            case InstructionWord.W:     return newValue;
            case InstructionWord.H2:    return Word36.setH2(originalValue, newValue);
            case InstructionWord.H1:    return Word36.setH1(originalValue, newValue);
            case InstructionWord.XH2:   return Word36.setH2(originalValue, newValue);
            case InstructionWord.XH1:   // XH1 or Q2
                if (quarterWordMode) {
                    return Word36.setQ2(originalValue, newValue);
                } else {
                    return Word36.setH1(originalValue, newValue);
                }
            case InstructionWord.T3:    // T3 or Q4
                if (quarterWordMode) {
                    return Word36.setQ4(originalValue, newValue);
                } else {
                    return Word36.setT3(originalValue, newValue);
                }
            case InstructionWord.T2:    // T2 or Q3
                if (quarterWordMode) {
                    return Word36.setQ3(originalValue, newValue);
                } else {
                    return Word36.setT2(originalValue, newValue);
                }
            case InstructionWord.T1:    // T1 or Q1
                if (quarterWordMode) {
                    return Word36.setQ1(originalValue, newValue);
                } else {
                    return Word36.setT1(originalValue, newValue);
                }
            case InstructionWord.S6:    return Word36.setS6(originalValue, newValue);
            case InstructionWord.S5:    return Word36.setS5(originalValue, newValue);
            case InstructionWord.S4:    return Word36.setS4(originalValue, newValue);
            case InstructionWord.S3:    return Word36.setS3(originalValue, newValue);
            case InstructionWord.S2:    return Word36.setS2(originalValue, newValue);
            case InstructionWord.S1:    return Word36.setS1(originalValue, newValue);
        }

        return originalValue;
    }
}
