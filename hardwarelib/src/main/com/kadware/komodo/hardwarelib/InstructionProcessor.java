/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BankType;
import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.GeneralRegister;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.IndexRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.VirtualAddress;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.Worker;
import com.kadware.komodo.hardwarelib.exceptions.AddressLimitsException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.ArithmeticExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.BreakpointInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.InitialProgramLoadInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.JumpHistoryFullInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.OperationTrapInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.QuantumTimerInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.ReferenceViolationInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.SignalInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.SoftwareBreakInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.TestAndSetInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.UPIInitialInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.UPINormalInterrupt;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.message.EntryMessage;

/**
 * Base class which models an Instruction Procesor node
 */
@SuppressWarnings("Duplicates")
public class InstructionProcessor extends Processor implements Worker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum BreakpointComparison {
        Fetch,
        Read,
        Write,
    }

    public enum RunMode {
        Normal,
        SingleInstruction,
        SingleCycle,
        Stopped,
    }

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
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Miscellaneous nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Represents an active base table entry.
     * It's basically a Word36 with special getters
     */
    public static class ActiveBaseTableEntry {

        final long _value;

        public ActiveBaseTableEntry(long value) { _value = value; }
        public ActiveBaseTableEntry(
            final int level,
            final int bankDescriptorIndex,
            final int offset
        ) {
            _value = (((long)level & 07) << 33) | (((long)bankDescriptorIndex & 077777) << 18) | (offset & 0777777);
        }

        public int getLevel() { return (int) (_value >> 33); }
        public int getBDI() { return (int) (_value >> 18) & 077777; }
        public int getLBDI() { return (int) (_value >> 18); }
        public int getSubsetOffset() { return (int) (_value & 0777777); }
    }

    /**
     * Describes a base register - there are 32 of these, each describing a based bank.
     */
    public static class BaseRegister {

        public final AccessInfo _accessLock;                        // ring and domain for this bank
        public final AbsoluteAddress _baseAddress;                  //  physical location of the described bank
        public final AccessPermissions _generalAccessPermissions;   //  ERW permissions for access from a key of lower privilege
        public final boolean _largeSizeFlag;                        //  If true, area does not exceed 2^24 bytes - else 2^18 bytes
        public final int _lowerLimitNormalized;                     //  Relative address, lower limit - 24 bits significant.
                                                                    //      Corresponds to first word/value in the storage subset
                                                                    //      one-word-granularity normalized form of the lower limit,
                                                                    //      accounting for large size flag
        public final AccessPermissions _specialAccessPermissions;   //  ERW permissions for access from a key of higher or equal privilege
        public final ArraySlice _storage;                           //  describes the extent of the storage for this bank - null for void flag
        public final int _upperLimitNormalized;                     //  Relative address, upper limit - 24 bits significant
                                                                    //      one-word-granularity normalized form of the upper limit,
                                                                    //      accounting for large size flag
        public final boolean _voidFlag;                             //  if true, this is a void bank (no storage)

        /**
         * Creates a ArraySlice to represent the bank as defined by the other attributes of this object
         * @return as described, null if void or limits indicate no storage
         * @throws AddressingExceptionInterrupt if something is wrong with the values
         */
        private ArraySlice getStorage(
        ) throws AddressingExceptionInterrupt {
            if ((_voidFlag) || (_lowerLimitNormalized > _upperLimitNormalized)) {
                return null;
            }

            try {
                int bankSize = (_upperLimitNormalized - _lowerLimitNormalized + 1);
                MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(_baseAddress._upiIndex);
                ArraySlice mspStorage = msp.getStorage(_baseAddress._segment);
                return new ArraySlice(mspStorage, _baseAddress._offset, bankSize);
            } catch (UPIProcessorTypeException | UPINotAssignedException ex) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                       0,
                                                       0);
            }
        }

        /**
         * Standard Constructor, used for Void bank
         */
        public BaseRegister(
        ) {
            _accessLock = new AccessInfo();
            _baseAddress = new AbsoluteAddress((short) 0, 0, 0);
            _generalAccessPermissions = new AccessPermissions(false, false, false);
            _largeSizeFlag = false;
            _lowerLimitNormalized = 0;
            _specialAccessPermissions = new AccessPermissions(false, false, false);
            _storage = null;
            _upperLimitNormalized = 0;
            _voidFlag = true;
        }

        /**
         * Initial value constructor for a non-void bank
         * @param baseAddress indicates UPI and offset indicating where in an MSP the storage for this bank is located
         * @param largeSizeFlag indicates a large size bank
         * @param lowerLimitNormalized actual normalized lower limit
         * @param upperLimitNormalized actual normalized upper limit
         * @param accessLock ring and domain for this bank
         * @param generalAccessPermissions access permissions for lower ring/domain
         * @param specialAccessPermissions access permissions for equal or higher ring/domain
         */
        public BaseRegister(
            final AbsoluteAddress baseAddress,
            final boolean largeSizeFlag,
            final int lowerLimitNormalized,
            final int upperLimitNormalized,
            final AccessInfo accessLock,
            final AccessPermissions generalAccessPermissions,
            final AccessPermissions specialAccessPermissions
        ) throws AddressingExceptionInterrupt {
            _baseAddress = baseAddress;
            _largeSizeFlag = largeSizeFlag;
            _lowerLimitNormalized = lowerLimitNormalized;
            _upperLimitNormalized = upperLimitNormalized;
            _accessLock = accessLock;
            _generalAccessPermissions = generalAccessPermissions;
            _specialAccessPermissions = specialAccessPermissions;
            _storage = getStorage();
            _voidFlag = _lowerLimitNormalized >_upperLimitNormalized;
        }

        /**
         * Constructor for building a BaseRegister from a bank descriptor with no subsetting
         * @param bankDescriptor source bank descriptor
         */
        public BaseRegister(
            final BankDescriptor bankDescriptor
        ) throws AddressingExceptionInterrupt {
            _accessLock = bankDescriptor.getAccessLock();
            _baseAddress = bankDescriptor.getBaseAddress();
            _generalAccessPermissions = new AccessPermissions(false,
                                                              bankDescriptor.getGeneraAccessPermissions()._read,
                                                              bankDescriptor.getGeneraAccessPermissions()._write);
            _largeSizeFlag = bankDescriptor.getLargeBank();
            _lowerLimitNormalized = bankDescriptor.getLowerLimitNormalized();
            _specialAccessPermissions = new AccessPermissions(false,
                                                              bankDescriptor.getSpecialAccessPermissions()._read,
                                                              bankDescriptor.getSpecialAccessPermissions()._write);
            _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized();
            _voidFlag = _lowerLimitNormalized > _upperLimitNormalized;
            _storage = getStorage();
        }

        /**
         * Constructor for building a BaseRegister from a bank descriptor with subsetting.
         * This occurs when the caller wishes to access a bank larger than the D-field allows, by accessing consecutive
         * sections of said bank by basing those segments on consecutive base registers.
         * In this case, we add the given offset to the base offset from the BD, and adjust the lower and upper
         * limits accordingly.  Subsequent accesses proceed as desired by virtue of the fact that we've set
         * the base address in the bank register, along with the limits, in this fashion.
         * @param bankDescriptor source bank descriptor
         * @param offset offset parameter for subsetting
         */
        public BaseRegister(
            final BankDescriptor bankDescriptor,
            final int offset
        ) throws AddressingExceptionInterrupt {
            _accessLock = bankDescriptor.getAccessLock();
            _baseAddress = bankDescriptor.getBaseAddress();
            _generalAccessPermissions = new AccessPermissions(false,
                                                              bankDescriptor.getGeneraAccessPermissions()._read,
                                                              bankDescriptor.getGeneraAccessPermissions()._write);
            _largeSizeFlag = bankDescriptor.getLargeBank();

            int bdLowerNorm = bankDescriptor.getLowerLimitNormalized();
            _lowerLimitNormalized = (bdLowerNorm > offset) ? bdLowerNorm - offset : 0;
            _specialAccessPermissions = new AccessPermissions(false,
                                                              bankDescriptor.getSpecialAccessPermissions()._read,
                                                              bankDescriptor.getSpecialAccessPermissions()._write);
            _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized() - offset;
            _voidFlag = (_upperLimitNormalized < 0) || (_lowerLimitNormalized > _upperLimitNormalized);
            _storage = getStorage();
        }

        /**
         * Constructor for load base register direct (i.e., loading from 4-word packet in memory)
         * @param values 4-word packet format as such:
         * Word 0:  bit 1:  GAP read
         *          bit 2:  GAP write
         *          bit 4:  SAP read
         *          bit 5:  SAP write
         *          bit 10: void flag
         *          bit 15: large size flag
         *          bits 18-19: Access lock ring
         *          bits 20-35: Access lock domain
         * Word 1:  bits 0-8: Lower Limit
         *          bits 18-35: Upper limit
         * Word 2:  bits 18-35: MSBits of absolute address
         * Word 3:  bits 0-36:  LSBits of absolute address
         *
         * Lower Limit - if large size, lower limit has 32,768 word granularity (15 bit shift)
         *               otherwise, it has 512 word granularity (9 bit shift)
         * Upper limit - if large size, upper limit has 64 word granularity (6 bit shift)
         *               otherwise, it has 1 word granularity (no shift)
         * Absolute address is defined by the MSP architecture
         */
        public BaseRegister(
            final long[] values
        ) throws AddressingExceptionInterrupt {
            _generalAccessPermissions = new AccessPermissions(false,
                                                              (values[0] & 0_200000_000000L) != 0,
                                                              (values[0] & 0_100000_000000L) != 0);
            _specialAccessPermissions = new AccessPermissions(false,
                                                              (values[0] & 0_020000_000000L) != 0,
                                                              (values[0] & 0_010000_000000L) != 0);
            _largeSizeFlag = (values[0] & 0_000004_000000L) != 0;
            _accessLock = new AccessInfo(values[0] & 0777777);
            _lowerLimitNormalized =(int)(((values[1] >> 27) & 0777) << (_largeSizeFlag ? 15 : 9));
            _upperLimitNormalized = (int) ((values[1] & 0777777) << (_largeSizeFlag ? 6 : 0)) | (_largeSizeFlag ? 077 : 0);
            _baseAddress = new AbsoluteAddress(values, 2);
            _voidFlag = ((values[0] & 0_000200_000000L) != 0) || (_lowerLimitNormalized > _upperLimitNormalized);
            _storage = _voidFlag ? null : getStorage();
        }

        /**
         * Checks a relative address against this bank's limits.
         * @param relativeAddress relative address of interest
         * @param fetchFlag true if this is related to an instruction fetch, else false
         * @throws ReferenceViolationInterrupt if the address is outside of limits
         */
        void checkAccessLimits(
            final long relativeAddress,
            final boolean fetchFlag
        ) throws ReferenceViolationInterrupt {
            if ((relativeAddress < _lowerLimitNormalized) || (relativeAddress > _upperLimitNormalized)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
            }
        }

        /**
         * Checks a particular key against this bank's limits.
         * @param fetchFlag true if this is related to an instruction fetch, else false
         * @param readFlag true if this will result in a read
         * @param writeFlag true if this will result in a write
         * @param accessInfo access info of the client
         * @throws ReferenceViolationInterrupt if the address is outside of limits
         */
        void checkAccessLimits(
            final boolean fetchFlag,
            final boolean readFlag,
            final boolean writeFlag,
            final AccessInfo accessInfo
        ) throws ReferenceViolationInterrupt {
            //  Choose GAP or SAP based on caller's accessInfo, but only if necessary
            if (readFlag || writeFlag) {
                boolean useSAP = ((accessInfo._ring < _accessLock._ring) || (accessInfo._domain == _accessLock._domain));
                AccessPermissions bankPermissions = useSAP ? _specialAccessPermissions : _generalAccessPermissions;

                if (readFlag && !bankPermissions._read) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, fetchFlag);
                }

                if (writeFlag && !bankPermissions._write) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, fetchFlag);
                }
            }
        }

        /**
         * Checks a relative address and a particular key against this bank's limits.
         * @param relativeAddress relative address of interest
         * @param fetchFlag true if this is related to an instruction fetch, else false
         * @param readFlag true if this will result in a read
         * @param writeFlag true if this will result in a write
         * @param accessInfo access info of the client
         * @throws ReferenceViolationInterrupt if the address is outside of limits
         */
        void checkAccessLimits(
            final long relativeAddress,
            final boolean fetchFlag,
            final boolean readFlag,
            final boolean writeFlag,
            final AccessInfo accessInfo
        ) throws ReferenceViolationInterrupt {
            checkAccessLimits(relativeAddress, fetchFlag);
            checkAccessLimits(fetchFlag, readFlag, writeFlag, accessInfo);
        }

        /**
         * Checks a range of relative addresses and a particular key against this bank's limits.
         * @param relativeAddress relative address of interest
         * @param count number of consecutive words of the access
         * @param readFlag true if this will result in a read
         * @param writeFlag true if this will result in a write
         * @param accessInfo access info of the client
         * @throws ReferenceViolationInterrupt if the address is outside of limits
         */
        void checkAccessLimits(
            final long relativeAddress,
            final int count,
            final boolean readFlag,
            final boolean writeFlag,
            final AccessInfo accessInfo
        ) throws ReferenceViolationInterrupt {
            if ((relativeAddress < _lowerLimitNormalized) || (relativeAddress + count - 1 > _upperLimitNormalized)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
            }

            checkAccessLimits(false, readFlag, writeFlag, accessInfo);
        }

        @Override
        public boolean equals(
            final Object obj
        ) {
            if (obj instanceof BaseRegister) {
                BaseRegister brobj = (BaseRegister) obj;
                if (brobj._voidFlag && _voidFlag) {
                    return true;
                }

                return (brobj._accessLock.equals(_accessLock)
                        && (brobj._baseAddress.equals(_baseAddress))
                        //  we must not compare ._enter flags, as they are undefined in the base register
                        //  so we have to compare the individual read and write flags instead
                        && (brobj._generalAccessPermissions._read == _generalAccessPermissions._read)
                        && (brobj._generalAccessPermissions._write == _generalAccessPermissions._write)
                        && (brobj._largeSizeFlag == _largeSizeFlag)
                        && (brobj._lowerLimitNormalized == _lowerLimitNormalized)
                        //  as above
                        && (brobj._specialAccessPermissions._read == _specialAccessPermissions._read)
                        && (brobj._specialAccessPermissions._write == _specialAccessPermissions._write)
                        && (brobj._upperLimitNormalized == _upperLimitNormalized));
            }

            return false;
        }

        /**
         * Retrieves the content of this base register in canonical/architecturally-correct format.
         * Format is as such:
         * Word 0:  bit 1:  GAP read
         *          bit 2:  GAP write
         *          bit 4:  SAP read
         *          bit 5:  SAP write
         *          bit 10: void flag
         *          bit 15: large size flag
         *          bits 18-19: Access lock ring
         *          bits 20-35: Access lock domain
         * Word 1:  bits 0-8: Lower Limit
         *          bits 18-35: Upper limit
         * Word 2:  bits 18-35: MSBits of absolute address
         * Word 3:  bits 0-36:  LSBits of absolute address
         *
         * Lower Limit - if large size, lower limit has 32,768 word granularity (15 bit shift)
         *               otherwise, it has 512 word granularity (9 bit shift)
         * Upper limit - if large size, upper limit has 64 word granularity (6 bit shift)
         *               otherwise, it has 1 word granularity (no shift)
         * Absolute address is defined by the MSP architecture
         * @return 4 word array of values
         */
        long[] getBaseRegisterWords(
        ) {
            long[] result = new long[4];
            for (int rx = 0; rx < 4; ++rx) { result[rx] = 0; }

            if (_generalAccessPermissions._read) { result[0] |= 0_200000_000000L; }
            if (_generalAccessPermissions._write) { result[0] |= 0_100000_000000L; }
            if (_specialAccessPermissions._read) { result[0] |= 0_020000_000000L; }
            if (_specialAccessPermissions._write) { result[0] |= 0_010000_000000L; }
            if (_voidFlag) { result[0] |= 0_000200_000000L; }
            if (_largeSizeFlag) { result[0] |= 0_000004_000000L; }
            result[0] |= (_accessLock._ring) << 16;
            result[0] |= _accessLock._domain;

            result[1] = ((long)_lowerLimitNormalized >> (_largeSizeFlag ? 15 : 9)) << 27;
            result[1] |= (long)_upperLimitNormalized >> (_largeSizeFlag ? 6 : 0);

            result[2] = _baseAddress._segment;
            result[3] = ((long) (_baseAddress._upiIndex) << 32) | _baseAddress._offset;

            return result;
        }

        /**
         * @return lower limit with granularity depending upon large size flag
         */
        public int getLowerLimit(
        ) {
            return (_largeSizeFlag ? _lowerLimitNormalized >> 15 : _lowerLimitNormalized >> 9);
        }

        /**
         * @return upper limit with granularity depending upon large size flag
         */
        public int getUpperLimit() {
            return (_largeSizeFlag ? _upperLimitNormalized >> 6 : _upperLimitNormalized);
        }

        @Override public int hashCode() { return _voidFlag ? 0 : _baseAddress.hashCode(); }

        @Override
        public String toString() {
            long[] values = getBaseRegisterWords();
            return String.format("%012o %012o %012o %012o", values[0], values[1], values[2], values[3]);
        }
    }

    /**
     * An 8-word Word36Array which describes a bank.
     * This structure is defined by the hardware PRM and exists in memory, although it may be presented from the outside
     * world for testing or setup purposes.
     */
    public static class BankDescriptor extends ArraySlice {

        /**
         * Constructor for a BankDescriptor which is located in a larger structure.
         * We do not alter the underlying array, in case it already contains a valid structure.
         * @param base base array
         * @param offset offset within the base array where the bank descriptor source code exists
         */
        public BankDescriptor(
            final ArraySlice base,
            final int offset
        ) {
            super(base, offset, 8);
        }

        /**
         * @return Access lock for this bank
         *          BD.Access_Lock:Word 0:H2
         */
        AccessInfo getAccessLock() {
            byte ring = (byte)((get(0) & 0_600000) >> 16);
            short domain = (short)(get(0) & 0_177777);
            return new AccessInfo(ring, domain);
        }

        /**
         * @return ProcessorType of bank (has some impact on the validity/meaning of other fields
         *          BD.TYPE:Word 0 Bits 8-11
         */
        public BankType getBankType() {
            int code = ((int)(get(0) >> 24)) & 017;
            return BankType.get(code);
        }

        /**
         * @return Base Address - i.e., describes the logical physical address (if that makes sense) corresponding to the
         *          first word of this bank in memory.  The actual meaning of this depends upon the hardware architecture.
         *          For us, the UPI portion is held in bits 0-3 of Word 3, the segment in H2 of Word 2,
         *          and the offset is held in bits 4-35 of Word 3.
         *          Canonical architecture requires the offset to be an unsigned value, but hardware addressing had traditionally
         *          begun at an artificially high value (such as 0400000000000 or some such) -- which allowed the base value to be
         *          less than that (but still positive) in order to account for non-zero lower-limits.
         *          Consider a lower-limit of 01000: The relative address of 01000 is valid, and refers to the first word in the bank.
         *          But if the first word in the bank is base-address == (upi)0, referring to the first word in MSP storage,
         *          the address translation algorithm (base-address + relative-address) gives us an absolute address of 01000,
         *          which is wrong - we wanted 0.
         *          To fix this, the base-address must be shifted backward/downward by the lower-limit value.
         *          This is possible if base address begin at some high value.
         *          However, for us the base address begins at zero, so to shift it down by lower-limit, we need signed values here.
         */
        public AbsoluteAddress getBaseAddress() {
            return new AbsoluteAddress(this, 2);
        }

        /**
         * @return position of this bank relative to the first bank describing a large or very large bank
         *          BD.DISP:Word 4 Bits 3-17
         */
        int getDisplacement() {
            return (int)(get(4) >> 18) & 077777;
        }

        /**
         * @return Execute, Read, and Write permissions for ring/domain at a lower level
         *          BD.GAP:Word 0 Bits 0-2
         */
        AccessPermissions getGeneraAccessPermissions() {
            return new AccessPermissions((int) (get(0) >> 33));
        }

        /**
         * @return G-flag
         *          Addressing_Exception interrupt results if this BD is accessed by normal bank-manipulation handling
         *          BD.G:Word 0 Bit 13
         */
        boolean getGeneralFault() {
            return (get(0) & 020_000000L) != 0;
        }

        /**
         * @return large bank flag:
         *          If false, this is a single bank no greater than 0_777777.
         *              BD.Lower_Limit has 512-word granularity and BD.Upper_Limit has 1-word granularity.
         *              This is required for BD.ProcessorType other than ExtendedMode.
         *          IF true, this is either a portion of a large bank no greater than 077_777777
         *              or of a very large bank no greater than 077777_777777.
         *              BD.Lower_Limit has 32768-word granularity, and BD.Upper_Limit has 64-word granularity.
         *          BD.S:Word 0 Bit 15
         */
        boolean getLargeBank() {
            return (get(0) & 04_000000L) != 0;
        }

        /**
         * @return lower limit, subject to granularity specified in the large-bank field - for non-indirect banks
         *          Word 1 Bits 0-8
         */
        public int getLowerLimit() {
            return (int)(get(1) >> 27);
        }

        /**
         * @return lower limit with granularity normalized out (making this 1-word granularity)
         */
        public int getLowerLimitNormalized() {
            return getLargeBank() ? (getLowerLimit() << 15) : (getLowerLimit() << 9);
        }

        /**
         * @return Execute, Read, and Write permissions for ring/domain at a lower level
         *          BD.SAP:Word 0 Bits 3-5
         */
        AccessPermissions getSpecialAccessPermissions() {
            return new AccessPermissions((int)(get(0) >> 30) & 07);
        }

        /**
         * @return Target bank L,BDI (only for indirect banks)
         *          L (level) is the top 3 bits in a the wrapped 18-bit field
         *          BDI is the remaining (bottom) 15 bits
         */
        int getTargetLBDI() {
            return (int)(get(0) >> 18);
        }

        /**
         * @return upper limit, subject to granularity specified in the large-bank field - for non-indirect banks
         *          Word 1 Bits 9-35 (bottom 3 quarter words)
         */
        public int getUpperLimit() {
            return (int)(get(1) & 0777_777777L);
        }

        /**
         * @return upper limit with granularity normalized out (making this 1-word granularity)
         */
        public int getUpperLimitNormalized() {
            return getLargeBank() ? (getUpperLimit() << 6) : getUpperLimit();
        }

        /**
         * @return U flag (upper limit suppression control)
         *              If clear, the upper limit is taken from the bank descriptor for populating the
         *              bank register.  If set, the bank register's upper limit is 0777777 non-normalized.
         *              Only meaningful for large banks (S is set).
         *          BD.U:Word 0 Bit 16
         */
        public boolean getUpperLimitSuppressionControl() {
            return (get(0) & 02_000000L) != 0;
        }

        public void setAccessLock(
            final AccessInfo accessInfo
        ) {
            long result = get(0) & 0_777777_000000L;
            result |= accessInfo.get() & 0777777;
            set(0, result);
        }

        public void setBankType(
            final BankType value
        ) {
            long result = get(0) & 0_776077_777777L;
            result |= (long)(value._code) << 24;
            set(0, result);
        }

        public void setBaseAddress(
            final AbsoluteAddress baseAddress
        ) {
            baseAddress.populate(this, 2);
        }

        public void setGeneralAccessPermissions(
            final AccessPermissions value
        ) {
            long result = get(0) & 0_077777_777777L;
            result |= ((long)value.get() << 33);
            set(0, result);
        }

        public void setGeneralFault(
            final boolean value
        ) {
            long result = get(0) & 0_777757_777777L;
            if (value) {
                result |= 020_000000;
            }
            set(0, result);
        }

        public void setLargeBank(
            final boolean value
        ) {
            long result = get(0) & 0_777773_777777L;
            if (value) {
                result |= 04_000000;
            }
            set(0, result);
        }

        public void setLowerLimit(
            final int value
        ) {
            long result = get(1) & 0_000777_777777L;
            result |= (long)(value & 0777) << 27;
            set(1, result);
        }

        public void setSpecialAccessPermissions(
            final AccessPermissions value
        ) {
            long result = get(0) & 0_707777_777777L;
            result |= ((long)value.get() << 30);
            set(0, result);
        }

        public void setUpperLimit(
            final int value
        ) {
            long result = get(1) & 0_777000_000000L;
            result |= value & 0777_777777L;
            set(1, result);
        }

        public void setUpperLimitSuppressionControl(
            final boolean value
        ) {
            long result = get(0) & 0_777775_777777L;
            if (value) {
                result |= 02_000000;
            }
            set(0, result);
        }
    }

    /**
     * Describes a breakpoint register.
     */
    public static class BreakpointRegister {

        final boolean _haltFlag;
        final boolean _fetchFlag;
        final boolean _readFlag;
        final boolean _writeFlag;
        final AbsoluteAddress _absoluteAddress;

        private BreakpointRegister(
            final boolean haltFlag,
            final boolean fetchFlag,
            final boolean readFlag,
            final boolean writeFlag,
            final AbsoluteAddress absoluteAddress
        ) {
            _haltFlag = haltFlag;
            _fetchFlag = fetchFlag;
            _readFlag = readFlag;
            _writeFlag = writeFlag;
            _absoluteAddress = absoluteAddress;
        }

        public static class Builder {

            private boolean _haltFlag = false;
            private boolean _fetchFlag = false;
            private boolean _readFlag = false;
            private boolean _writeFlag = false;
            private AbsoluteAddress _absoluteAddress = null;

            public Builder setHaltFlag(boolean value)                   { _haltFlag = value; return this; }
            public Builder setFetchFlag(boolean value)                  { _fetchFlag = value; return this; }
            public Builder setReadFlag(boolean value)                   { _readFlag = value; return this; }
            public Builder setWriteFlag(boolean value)                  { _writeFlag = value; return this; }
            public Builder setAbsoluteAddress(AbsoluteAddress value)    { _absoluteAddress = value; return this; }

            public BreakpointRegister build() {
                return new BreakpointRegister(_haltFlag, _fetchFlag, _readFlag, _writeFlag, _absoluteAddress);
            }
        }
    }

    /**
     * Describes a designator register
     */
    public static class DesignatorRegister {

        private static final long MASK_ActivityLevelQueueMonitorEnabled = Word36.MASK_B0;
        private static final long MASK_FaultHandlingInProgress          = Word36.MASK_B6;
        private static final long MASK_Executive24BitIndexingEnabled    = Word36.MASK_B11;
        private static final long MASK_QuantumTimerEnabled              = Word36.MASK_B12;
        private static final long MASK_DeferrableInterruptEnabled       = Word36.MASK_B13;
        private static final long MASK_ProcessorPrivilege               = Word36.MASK_B14 | Word36.MASK_B15;
        private static final long MASK_BasicModeEnabled                 = Word36.MASK_B16;
        private static final long MASK_ExecRegisterSetSelected          = Word36.MASK_B17;
        private static final long MASK_Carry                            = Word36.MASK_B18;
        private static final long MASK_Overflow                         = Word36.MASK_B19;
        private static final long MASK_CharacteristicUnderflow          = Word36.MASK_B21;
        private static final long MASK_CharacteristicOverflow           = Word36.MASK_B22;
        private static final long MASK_DivideCheck                      = Word36.MASK_B23;
        private static final long MASK_OperationTrapEnabled             = Word36.MASK_B27;
        private static final long MASK_ArithmeticExceptionEnabled       = Word36.MASK_B29;
        private static final long MASK_BasicModeBaseRegisterSelection   = Word36.MASK_B31;
        private static final long MASK_QuarterWordModeEnabled           = Word36.MASK_B32;

        private long _value = 0;

        public DesignatorRegister() {}
        public DesignatorRegister(long value) { _value = value & Word36.BIT_MASK; }

        private void changeBit(
            final long mask,
            final boolean newBit
        ) {
            _value = newBit ? Word36.logicalOr(_value, mask) : Word36.logicalAnd(_value, invertMask(mask));
        }

        private long invertMask(final long mask)                { return mask ^ Word36.BIT_MASK; }

        public void clear()                                     { _value = 0; }
        public boolean getActivityLevelQueueMonitorEnabled()    { return (_value & MASK_ActivityLevelQueueMonitorEnabled) != 0; }
        public boolean getFaultHandlingInProgress()             { return (_value & MASK_FaultHandlingInProgress) != 0; }
        public boolean getExecutive24BitIndexingEnabled()       { return (_value & MASK_Executive24BitIndexingEnabled) != 0; }
        public boolean getQuantumTimerEnabled()                 { return (_value & MASK_QuantumTimerEnabled) != 0; }
        public boolean getDeferrableInterruptEnabled()          { return (_value & MASK_DeferrableInterruptEnabled) != 0; }
        public int getProcessorPrivilege()                      { return (int)((_value & (Word36.MASK_B14 | Word36.MASK_B15)) >> 20); }
        public boolean getBasicModeEnabled()                    { return (_value & MASK_BasicModeEnabled) != 0; }
        public boolean getExecRegisterSetSelected()             { return (_value & MASK_ExecRegisterSetSelected) != 0; }
        public boolean getCarry()                               { return (_value & MASK_Carry) != 0; }
        public boolean getOverflow()                            { return (_value & MASK_Overflow) != 0; }
        public boolean getCharacteristicUnderflow()             { return (_value & MASK_CharacteristicUnderflow) != 0; }
        public boolean getCharacteristicOverflow()              { return (_value & MASK_CharacteristicOverflow) != 0; }
        public boolean getDivideCheck()                         { return (_value & MASK_DivideCheck) != 0; }
        public boolean getOperationTrapEnabled()                { return (_value & MASK_OperationTrapEnabled) != 0; }
        public boolean getArithmeticExceptionEnabled()          { return (_value & MASK_ArithmeticExceptionEnabled) != 0; }
        public boolean getBasicModeBaseRegisterSelection()      { return (_value & MASK_BasicModeBaseRegisterSelection) != 0; }
        public boolean getQuarterWordModeEnabled()              { return (_value & MASK_QuarterWordModeEnabled) != 0; }
        public long getW()                                      { return _value; }
        public long getS4()                                     { return Word36.getS4(_value); }

        public void setActivityLevelQueueMonitorEnabled(boolean flag)   { changeBit(MASK_ActivityLevelQueueMonitorEnabled, flag); }
        public void setFaultHandlingInProgress(boolean flag)            { changeBit(MASK_FaultHandlingInProgress, flag); }
        public void setExecutive24BitIndexingEnabled(boolean flag)      { changeBit(MASK_Executive24BitIndexingEnabled, flag); }
        public void setQuantumTimerEnabled(boolean flag)                { changeBit(MASK_QuantumTimerEnabled, flag); }
        public void setDeferrableInterruptEnabled(boolean flag)         { changeBit(MASK_DeferrableInterruptEnabled, flag); }
        public void setS4(long value)                                   { _value = Word36.setS4(_value, value); }
        public void setW(long value)                                    { _value = value & Word36.BIT_MASK; }

        public void setProcessorPrivilege(
            final int value
        ) {
            long cleared = _value & invertMask(MASK_ProcessorPrivilege);
            _value = cleared | ((value & 03L) << 20);
        }

        public void setBasicModeEnabled(boolean flag)                   { changeBit(MASK_BasicModeEnabled, flag); }
        public void setExecRegisterSetSelected(boolean flag)            { changeBit(MASK_ExecRegisterSetSelected, flag); }
        public void setCarry(boolean flag)                              { changeBit(MASK_Carry, flag); }
        public void setOverflow(boolean flag)                           { changeBit(MASK_Overflow, flag); }
        public void setCharacteristicUnderflow(boolean flag)            { changeBit(MASK_CharacteristicUnderflow, flag); }
        public void setCharacteristicOverflow(boolean flag)             { changeBit(MASK_CharacteristicOverflow, flag); }
        public void setDivideCheck(boolean flag)                        { changeBit(MASK_DivideCheck, flag); }
        public void setOperationTrapEnabled(boolean flag)               { changeBit(MASK_OperationTrapEnabled, flag); }
        public void setArithmeticExceptionEnabled(boolean flag)         { changeBit(MASK_ArithmeticExceptionEnabled, flag); }
        public void setBasicModeBaseRegisterSelection(boolean flag)     { changeBit(MASK_BasicModeBaseRegisterSelection, flag); }
        public void setQuarterWordModeEnabled(boolean flag)             { changeBit(MASK_QuarterWordModeEnabled, flag); }
    }

    /**
     * Contains developed absolute addresses and other information necessary for preserving values which are
     * time-consuming to calculate, and which must be used more than once in disparate locations.
     * The precipitating use case is SYSC which needs to read consecutive addresses, then write them back out.
     */
    private static class DevelopedAddresses {
        private final BaseRegister _baseRegister;
        private final int _offset;
        private final AbsoluteAddress[] _addresses;

        DevelopedAddresses(
            final BaseRegister baseRegister,
            final int offset,
            final AbsoluteAddress[] addresses
        ) {
            _baseRegister = baseRegister;
            _offset = offset;
            _addresses = addresses;
        }
    }

    /**
     * Represents a particular gate within a gate bank
     */
    private static class Gate {

        public final AccessPermissions _generalPermissions;     //  GAP.E
        public final AccessPermissions _specialPermissions;     //  SAP.E
        public final boolean _libraryGate;                      //  LIB - we don't support these currently
        public final boolean _gotoInhibit;                      //  GI
        public final boolean _designatorBitInhibit;             //  DBI
        public final boolean _accessKeyInhibit;                 //  AKI
        public final boolean _latentParameter0Inhibit;          //  LP0I
        public final boolean _latentParameter1Inhibit;          //  LP1I
        public final AccessInfo _accessLock;                    //  Lock which caller must satisfy to enter the gate
        public final int _targetBankLevel;
        public final int _targetBankDescriptorIndex;
        public final int _targetBankOffset;
        public final int _basicModeBaseRegister;                //  range is 0:3 meaning B12:B15
        public final DesignatorRegister _designatorBits12_17;   //  bits 12-17 significant (okay, maybe not 16)
        public final AccessInfo _accessKey;                     //  key to be loaded into IKR
        public final long _latentParameter0;
        public final long _latentParameter1;

        /**
         * Builds a Gate object from the 8-word storage entry representing the gate
         * @param gateDefinition represents the content of the containing gate
         */
        Gate(
            final long[] gateDefinition
        ) {
            _generalPermissions = new AccessPermissions((int) (gateDefinition[0] >> 33));
            _specialPermissions = new AccessPermissions((int) (gateDefinition[0] >> 30));
            _libraryGate = (gateDefinition[0] & 0_000040_000000L) != 0;
            _gotoInhibit = (gateDefinition[0] & 0_000020_000000L) != 0;
            _designatorBitInhibit = (gateDefinition[0] & 0_000010_000000L) != 0;
            _accessKeyInhibit = (gateDefinition[0] & 0_000004_000000L) != 0;
            _latentParameter0Inhibit = (gateDefinition[0] & 0_000002_000000L) != 0;
            _latentParameter1Inhibit = (gateDefinition[0] & 0_000001_000000L) != 0;
            _accessLock = new AccessInfo(gateDefinition[0] & 0_777777);

            _targetBankLevel = (int) (gateDefinition[1] >> 33) & 03;
            _targetBankDescriptorIndex = (int) (gateDefinition[1] >> 18) & 077777;
            _targetBankOffset = (int) (gateDefinition[1] & 0777777);

            _basicModeBaseRegister = (int) ((gateDefinition[2] >> 24) & 03);
            _designatorBits12_17 = new DesignatorRegister(gateDefinition[2] & 0_000077_000000L);
            _accessKey = new AccessInfo(gateDefinition[2] & 0_777777);

            _latentParameter0 = gateDefinition[3];
            _latentParameter1 = gateDefinition[4];
        }
    }

    /**
     * An extension of Word36 which describes an indicator key register
     */
    static class IndicatorKeyRegister {

        private long _value = 0;

        public IndicatorKeyRegister() {}
        public IndicatorKeyRegister(long value)                 { _value = value & Word36.BIT_MASK; }

        public void clear()                                     { _value = 0; }
        public long getW()                                      { return _value; }
        public int getShortStatusField()                        { return (int) Word36.getS1(_value); }
        public int getMidInstructionDescription()               { return (int) (Word36.getS2(_value) >> 3); }
        public int getPendingInterruptInformation()             { return (int) (Word36.getS2(_value) & 07); }
        public int getInterruptClassField()                     { return (int) Word36.getS3(_value); }
        public int getAccessKey()                               { return (int) Word36.getH2(_value); }
        public AccessInfo getAccessInfo()                       { return new AccessInfo(getAccessKey()); }
        public boolean getInstructionInF0()                     { return (getMidInstructionDescription() & 04) != 0; }
        public boolean getExecuteRepeatedInstruction()          { return (getMidInstructionDescription() & 02) != 0; }
        public boolean getBreakpointRegisterMatchCondition()    { return (getPendingInterruptInformation() & 04) != 0; }
        public boolean getSoftwareBreak()                       { return (getPendingInterruptInformation() & 02) != 0; }

        public void setW(long value)                            { _value = value; }
        public void setShortStatusField(int value)              { _value = Word36.setS1(_value, value); }
        public void setMidInstructionDescription(int value)     { _value = (_value & 0_770777_777777L) | ((value & 07) << 27); }
        public void setPendingInterruptInformation(int value)   { _value = (_value & 0_777077_777777L) | ((value & 07) << 24); }
        public void setInterruptClassField(int value)           { _value = Word36.setS3(_value, value); }
        public void setAccessKey(int value)                     { _value = Word36.setH2(_value, value); }

        public void setInstructionInF0(
            final boolean flag
        ) {
            _value &= 0_773777_777777L;
            if (flag) {
                _value |= 0_004000_000000L;
            }
        }

        public void setExecuteRepeatedInstruction(
            final boolean flag
        ) {
            _value &= 0_775777_777777L;
            if (flag) {
                _value |= 0_002000_000000L;
            }
        }

        public void setBreakpointRegisterMatchCondition(
            final boolean flag
        ) {
            _value &= 0_777377_777777L;
            if (flag) {
                _value |= 0_000400_000000L;
            }
        }

        public void setSoftwareBreak(
            final boolean flag
        ) {
            _value &= 0_777577_777777L;
            if (flag) {
                _value |= 0_000200_000000L;
            }
        }
    }

    /**
     * Nothing really different from the VirtualAddress class, but this is a specific hard-held register in the IP.
     */
    //TODO should we make this invariant?
    public static class ProgramAddressRegister {

        long _value = 0;

        ProgramAddressRegister()                    {}
        public long get()                           { return _value; }
        public int getLBDI()                        { return (int) (_value >> 18); }
        public int getProgramCounter()              { return (int) _value & 0_777777; }
        public void set(long value)                 { _value = value & 0_777777_777777L; }
        public void setLBDI(long value)             { _value = (_value & 0_777777) | ((value & 0_777777) << 18); }
        public void setProgramCounter(long value)   { _value = (_value & 0_777777_000000L) | (value & 0_777777); }
    }

    /**
     * Represents an RCS stack frame
     */
    private static class ReturnControlStackFrame {

        final int _reentryPointBankLevel;
        final int _reentryPointBankDescriptorIndex;
        final int _reentryPointOffset;
        final boolean _trap;
        final int _basicModeBaseRegister;                       //  Range 0:3, added to 12 signifies a base register
        final DesignatorRegister _designatorRegisterDB12To17;
        final AccessInfo _accessKey;

        /**
         * Builds a frame object from the 2-word storage entry
         */
        ReturnControlStackFrame(
            final long[] frame
        ) {
            _reentryPointBankLevel = (int) (frame[0] >> 33) & 07;
            _reentryPointBankDescriptorIndex = (int) (frame[0] >> 18) & 077777;
            _reentryPointOffset = (int) (frame[0] & 0777777);
            _trap = (frame[1] & 0_400000_000000L) != 0;
            _basicModeBaseRegister = (int) (frame[1] >> 24) & 03;
            _designatorRegisterDB12To17 = new DesignatorRegister(frame[1] & 0_000077_000000L);
            _accessKey = new AccessInfo(frame[1] & 0777777);
        }

        /**
         * Builds a frame object from the discrete components
         */
        ReturnControlStackFrame(
            final int reentryPointBankLevel,
            final int reentryPointBankDescriptorIndex,
            final int reentryPointOffset,
            final boolean trap,
            final int basicModeBaseRegister,
            final DesignatorRegister designatorRegister,
            final AccessInfo accessKey
        ) {
            _reentryPointBankLevel = reentryPointBankLevel;
            _reentryPointBankDescriptorIndex = reentryPointBankDescriptorIndex;
            _reentryPointOffset = reentryPointOffset;
            _trap = trap;
            _basicModeBaseRegister = basicModeBaseRegister & 07;
            _designatorRegisterDB12To17 = new DesignatorRegister(designatorRegister.getW() & 0_000077_000000L);
            _accessKey = accessKey;
        }

        /**
         * Returns two-word representation of the RCS frame
         */
        long[] get() {
            long[] result = new long[2];
            result[0] = ((long) _reentryPointBankLevel << 33)
                        | ((long) _reentryPointBankDescriptorIndex << 18)
                        | (result[0] |= _reentryPointOffset);

            result[1] = (_trap ? 0_400000_000000L : 0)
                        | (_basicModeBaseRegister << 24)
                        | _designatorRegisterDB12To17.getW()
                        | _accessKey.get();

            return result;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Bank Manipulation
    //  ----------------------------------------------------------------------------------------------------------------------------

    private enum TransferMode {
        BasicToBasic,
        BasicToExtended,
        ExtendedToBasic,
        ExtendedToExtended,
    }

    private interface BankManipulationStep {

        void handler(
            final BankManipulator.BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt;
    }

    /**
     * This bit of nastiness implements a state machine which implements the bank manipulation algorithm
     */
    private class BankManipulator {

        private class BankManipulationInfo {
            //  Determined at construction
            private final boolean _callOperation;                       //  true if CALL, LOCL, LxJ, LxJ/CALL, or interrupt
            private final Instruction _instruction;                     //  null if this is for interrupt handling
            private final MachineInterrupt _interrupt;                  //  null if invoked by instruction handling
            private boolean _loadInstruction = false;                   //  true if this is LAE, LBE, or LBU
            private boolean _lxjInstruction = false;                    //  true if this is for an LxJ instruction
            private int _lxjBankSelector;                               //  BDR (offset by 12) for LxJ instructions
            private int _lxjInterfaceSpec;                              //  IS for LxJ instructions
            private int _lxjXRegisterIndex;                             //  register index of X(a) for LxJ instructions
            private final long[] _operands;                             //  7 or 15 operand values for UR and LAE respectively,
            //      or 1 operand representing (U) for CALL, GOTO, LBE, LBU
            //      or 1 operand representing U for LBJ, LDJ, LIJ
            //      or null for RTN
            private boolean _returnOperation = false;                   //  true if RTN, LxJ/RTN

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
                final Instruction instruction,
                final long[] operands,
                final MachineInterrupt interrupt
            ) {
                _instruction = instruction;
                _interrupt = interrupt;
                _operands = operands;

                if (_interrupt != null) {
                    _callOperation = true;
                } else {
                    _loadInstruction = (_instruction == Instruction.LAE)
                                       || (_instruction == Instruction.LBE)
                                       || (_instruction == Instruction.LBU);
                    _lxjInstruction = (_instruction == Instruction.LBJ)
                                      || (_instruction == Instruction.LDJ)
                                      || (_instruction == Instruction.LIJ);

                    _lxjXRegisterIndex = (int) _currentInstruction.getA();

                    if (_lxjInstruction) {
                        IndexRegister xRegister = getExecOrUserXRegister(_lxjXRegisterIndex);
                        _lxjInterfaceSpec = (int) (xRegister.getW() >> 30) & 03;
                        _lxjBankSelector = (int) (xRegister.getW() >> 33) & 03;
                    }

                    _callOperation =
                        (instruction == Instruction.CALL)
                        || (instruction == Instruction.LOCL)
                        || (_lxjInstruction && (_lxjInterfaceSpec < 2));
                    //  Note that UR is not considered a return operation
                    _returnOperation =
                        (instruction == Instruction.RTN)
                        || (_lxjInstruction && (_lxjInterfaceSpec == 2));
                }
            }
        }

        /**
         * Sanity check for a couple of different categories of instructions
         */
        private class BankManipulationStep1 implements BankManipulationStep {

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
                    if ((bmInfo._instruction == Instruction.LBU)
                        && (_currentInstruction.getA() < 2)) {
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
        private class BankManipulationStep2 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._instruction != null) {
                    if (bmInfo._instruction == Instruction.CALL) {
                        bmInfo._priorBankLevel = _programAddressRegister.getLBDI() >> 15;
                        bmInfo._priorBankDescriptorIndex = _programAddressRegister.getLBDI() & 077777;
                    } else if ((bmInfo._lxjInstruction) && (bmInfo._lxjInterfaceSpec < 2)) {
                        //  We're supposed to be here for normal LxJ and for LxJ/CALL, but we also catch LxJ/GOTO
                        //  (interfaceSpec == 1 and target BD is extended with enter access, or gate)
                        //  Because we must do this for IS == 1 and source BD is basic, and it is too early in
                        //  the algorithm to know the source BD bank type.
                        int abtx;
                        if (bmInfo._instruction == Instruction.LBJ) {
                            abtx = bmInfo._lxjBankSelector + 12;
                        } else if (bmInfo._instruction == Instruction.LDJ) {
                            abtx = _designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                        } else {
                            abtx = _designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
                        }

                        bmInfo._priorBankLevel = _activeBaseTableEntries[abtx].getLevel();
                        bmInfo._priorBankDescriptorIndex = _activeBaseTableEntries[abtx].getBDI();
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
        private class BankManipulationStep3 implements BankManipulationStep {

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
                    if (_baseRegisters[InstructionProcessor.L0_BDT_BASE_REGISTER]._voidFlag) {
                        stop(InstructionProcessor.StopReason.L0BaseRegisterInvalid, 0);
                        bmInfo._nextStep = 0;
                        return;
                    }

                    //  intOffset is the offset from the start of the level 0 BDT, to the vector we're interested in.
                    ArraySlice bdtLevel0 = _baseRegisters[InstructionProcessor.L0_BDT_BASE_REGISTER]._storage;
                    if (bdtLevel0 == null) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                               0,
                                                               0);
                    }
                    int intOffset = bmInfo._interrupt.getInterruptClass().getCode();
                    if (intOffset >= bdtLevel0.getSize()) {
                        stop(InstructionProcessor.StopReason.InterruptHandlerOffsetOutOfRange, 0);
                        bmInfo._nextStep = 0;
                        return;
                    }

                    long lbdiOffset = bdtLevel0.get(intOffset);
                    bmInfo._sourceBankLevel = (int) (lbdiOffset >> 33);
                    bmInfo._sourceBankDescriptorIndex = (int) (lbdiOffset >> 18) & 077777;
                    bmInfo._sourceBankOffset = (int) lbdiOffset & 0777777;
                } else if (bmInfo._instruction == Instruction.UR) {
                    //  source L,BDI comes from operand L,BDI
                    //  offset comes from operand.PAR.PC
                    bmInfo._sourceBankLevel = (int) (bmInfo._operands[0] >> 33);
                    bmInfo._sourceBankDescriptorIndex = (int) (bmInfo._operands[0] >> 18) & 077777;
                    bmInfo._sourceBankOffset = (int) (bmInfo._operands[0] & 0777777);
                } else if (bmInfo._returnOperation) {
                    //  source L,BDI,Offset comes from RCS
                    //  This is where we pop an RCS frame and grab the relevant fields therefrom.
                    BaseRegister rcsBReg = _baseRegisters[InstructionProcessor.RCS_BASE_REGISTER];
                    if (rcsBReg._voidFlag) {
                        throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                            InstructionProcessor.RCS_BASE_REGISTER,
                                                                            0);
                    }

                    IndexRegister rcsXReg = getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
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
                    setExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER,
                                           IndexRegister.setXM(rcsXReg.getW(), framePointer + 2));

                    bmInfo._sourceBankLevel = bmInfo._rcsFrame._reentryPointBankLevel;
                    bmInfo._sourceBankDescriptorIndex = bmInfo._rcsFrame._reentryPointBankDescriptorIndex;
                    bmInfo._sourceBankOffset = bmInfo._rcsFrame._reentryPointOffset;
                } else if (bmInfo._lxjInstruction) {
                    //  source L,BDI comes from basic mode X(a) E,LS,BDI
                    //  offset comes from operand
                    long bmSpec = getExecOrUserXRegister(bmInfo._lxjXRegisterIndex).getW();
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
        private class BankManipulationStep4 implements BankManipulationStep {

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
                        stop(InstructionProcessor.StopReason.InterruptHandlerInvalidLevelBDI,
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
        private class BankManipulationStep5 implements BankManipulationStep {

            /**
             * @throws AddressingExceptionInterrupt if a void bank is specified where it is not allowed
             */
            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) throws AddressingExceptionInterrupt {
                if ((bmInfo._sourceBankLevel == 0) && (bmInfo._sourceBankDescriptorIndex == 0)) {
                    if (bmInfo._interrupt != null) {
                        stop(InstructionProcessor.StopReason.InterruptHandlerInvalidLevelBDI, 0);
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
                    } else if (bmInfo._instruction == Instruction.UR) {
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
        private class BankManipulationStep6 implements BankManipulationStep {

            /**
             * @throws AddressingExceptionInterrupt if we cannot find the bank descriptor for the source L,BDI
             */
            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) throws AddressingExceptionInterrupt {
                try {
                    bmInfo._sourceBankDescriptor = findBankDescriptor(bmInfo._sourceBankLevel, bmInfo._sourceBankDescriptorIndex);
                } catch (AddressingExceptionInterrupt ex) {
                    if (bmInfo._interrupt != null) {
                        //  this is serious - cannot continue
                        stop(InstructionProcessor.StopReason.InterruptHandlerInvalidLevelBDI,
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
        private class BankManipulationStep7 implements BankManipulationStep {

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
                            stop(InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                                 (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                            bmInfo._nextStep = 0;
                        } else if ((bmInfo._instruction == Instruction.LBU)
                                   && (_designatorRegister.getProcessorPrivilege() > 1)
                                   && !bmInfo._sourceBankDescriptor.getGeneraAccessPermissions()._enter
                                   && !bmInfo._sourceBankDescriptor.getSpecialAccessPermissions()._enter) {
                            bmInfo._targetBankDescriptor = null;
                        } else if (((bmInfo._instruction == Instruction.RTN) || bmInfo._lxjInstruction)
                                   && !bmInfo._rcsFrame._designatorRegisterDB12To17.getBasicModeEnabled()) {
                            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                                   bmInfo._sourceBankLevel,
                                                                   bmInfo._sourceBankDescriptorIndex);
                        }
                        break;

                    case Gate:
                        if (bmInfo._interrupt != null) {
                            stop(InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                                 (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                            bmInfo._nextStep = 0;
                        } else if (bmInfo._callOperation || (bmInfo._instruction == Instruction.GOTO)) {
                            bmInfo._nextStep = 9;
                        } else if (bmInfo._returnOperation || (bmInfo._instruction == Instruction.UR)) {
                            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                                   bmInfo._sourceBankLevel,
                                                                   bmInfo._sourceBankDescriptorIndex);
                        }
                        break;

                    case Indirect:
                        if (bmInfo._interrupt != null) {
                            stop(
                                InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
                                (bmInfo._sourceBankLevel << 15) | bmInfo._sourceBankDescriptorIndex);
                            bmInfo._nextStep = 0;
                        } else if ((bmInfo._callOperation) || (bmInfo._loadInstruction)){
                            bmInfo._nextStep = 8;
                        } else if (bmInfo._returnOperation
                                   || (bmInfo._instruction == Instruction.LAE)
                                   || (bmInfo._instruction == Instruction.UR)) {
                            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                                   bmInfo._sourceBankLevel,
                                                                   bmInfo._sourceBankDescriptorIndex);
                        }
                        break;

                    case QueueRepository:
                        if (bmInfo._interrupt != null) {
                            stop(InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
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
                            stop(InstructionProcessor.StopReason.InterruptHandlerInvalidBankType,
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
        private class BankManipulationStep8 implements BankManipulationStep {

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
                    bmInfo._targetBankDescriptor = findBankDescriptor(bmInfo._targetBankLevel, bmInfo._targetBankDescriptorIndex);
                } catch (AddressingExceptionInterrupt ex) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           bmInfo._targetBankLevel,
                                                           bmInfo._targetBankDescriptorIndex);
                }

                bmInfo._nextStep = 10;

                switch (bmInfo._targetBankDescriptor.getBankType()) {
                    case BasicMode:
                        //  when PP>1 and GAP.E == 0 and SAP.E == 0, do void bank (set target bd null)
                        if ((_designatorRegister.getProcessorPrivilege() > 1)
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
        private class BankManipulationStep9 implements BankManipulationStep {

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

                AccessPermissions gateBankPerms =
                    getEffectiveAccessPermissions(_indicatorKeyRegister.getAccessInfo(),
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
                    getEffectiveAccessPermissions(_indicatorKeyRegister.getAccessInfo(),
                                                  bmInfo._gateBank._accessLock,
                                                  bmInfo._gateBank._generalPermissions,
                                                  bmInfo._gateBank._specialPermissions);
                if (!gatePerms._enter) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.EnterAccessDenied,
                                                           bmInfo._sourceBankLevel,
                                                           bmInfo._sourceBankDescriptorIndex);
                }

                //  If GOTO or LxJ with X(a).IS == 1, and Gate.GI is set, throw GOTO Inhibit AddressingExceptionInterrupt
                if ((bmInfo._instruction == Instruction.GOTO)
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
                    bmInfo._targetBankDescriptor = findBankDescriptor(bmInfo._targetBankLevel, bmInfo._targetBankDescriptorIndex);
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
        private class BankManipulationStep10 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._instruction == Instruction.LAE) {
                    //  baseRegisterIndex was set a long time ago...
                    bmInfo._nextStep = 18;
                } else if (bmInfo._instruction == Instruction.LBE) {
                    bmInfo._baseRegisterIndex = (int) _currentInstruction.getA() + 16;
                    bmInfo._nextStep = 18;
                } else if (bmInfo._instruction == Instruction.LBU) {
                    bmInfo._baseRegisterIndex = (int) _currentInstruction.getA();
                    bmInfo._nextStep = 18;
                } else if ((bmInfo._instruction == Instruction.UR) || (bmInfo._interrupt != null)) {
                    bmInfo._baseRegisterIndex = 0;
                    bmInfo._nextStep = 16;
                } else {
                    //  This is a transfer operation...  Determine transfer mode
                    boolean sourceModeBasic = _designatorRegister.getBasicModeEnabled();
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
                        } else if (bmInfo._targetBankDescriptor.getBankType() == BankType.BasicMode) {
                            bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToBasic : TransferMode.ExtendedToBasic;
                        } else {
                            bmInfo._transferMode = sourceModeBasic ? TransferMode.BasicToExtended : TransferMode.ExtendedToExtended;
                        }
                    }

                    switch (bmInfo._transferMode) {
                        case BasicToBasic:
                            if (bmInfo._returnOperation) {
                                bmInfo._baseRegisterIndex = bmInfo._rcsFrame._basicModeBaseRegister + 12;
                            } else if (bmInfo._instruction == Instruction.LBJ) {
                                bmInfo._baseRegisterIndex = bmInfo._lxjBankSelector + 12;
                            } else if (bmInfo._instruction == Instruction.LDJ) {
                                bmInfo._baseRegisterIndex = _designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                            } else if (bmInfo._instruction == Instruction.LIJ) {
                                bmInfo._baseRegisterIndex = _designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
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
        private class BankManipulationStep11 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._transferMode == TransferMode.ExtendedToBasic) {
                    setBaseRegister(0, new BaseRegister());
                    _programAddressRegister.setLBDI(0L);
                } else if (bmInfo._transferMode == TransferMode.BasicToExtended) {
                    setBaseRegister(bmInfo._baseRegisterIndex, new BaseRegister());
                }

                bmInfo._nextStep++;
            }
        }

        /**
         * For calls, create an entry on the RCS.  Check for RCS overflow first...
         * Only executed for transfers.
         */
        private class BankManipulationStep12 implements BankManipulationStep {

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
                    BaseRegister rcsBReg = _baseRegisters[InstructionProcessor.RCS_BASE_REGISTER];
                    if (rcsBReg._voidFlag) {
                        throw new RCSGenericStackUnderflowOverflowInterrupt(
                            RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                            InstructionProcessor.RCS_BASE_REGISTER,
                            0);
                    }

                    IndexRegister rcsXReg = (IndexRegister) _generalRegisterSet.getRegister(InstructionProcessor.RCS_INDEX_REGISTER);

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

                    int rtnAddr = _programAddressRegister.getProgramCounter() + 1;
                    int bValue = 0;
                    switch (bmInfo._transferMode) {
                        case ExtendedToBasic:
                            if (bmInfo._gateBank != null) {
                                bValue = bmInfo._gateBank._basicModeBaseRegister;
                            }
                            break;

                        case BasicToExtended:
                            if (bmInfo._instruction == Instruction.LBJ) {
                                bValue = bmInfo._lxjBankSelector;
                            } else if (bmInfo._instruction == Instruction.LDJ) {
                                bValue = _designatorRegister.getBasicModeBaseRegisterSelection() ? 15 : 14;
                            } else if (bmInfo._instruction == Instruction.LIJ) {
                                bValue = _designatorRegister.getBasicModeBaseRegisterSelection() ? 13 : 12;
                            }
                            break;
                    }

                    ReturnControlStackFrame rcsFrame = new ReturnControlStackFrame(bmInfo._priorBankLevel,
                                                                                   bmInfo._priorBankDescriptorIndex,
                                                                                   rtnAddr,
                                                                                   false,
                                                                                   bValue,
                                                                                   _designatorRegister,
                                                                                   _indicatorKeyRegister.getAccessInfo());
                    long[] rcsData = rcsFrame.get();

                    int offset = framePointer - rcsBReg._lowerLimitNormalized;
                    rcsBReg._storage.set(offset, rcsData[0]);
                    rcsBReg._storage.set(offset + 1, rcsData[1]);
                    _generalRegisterSet.setRegister(InstructionProcessor.RCS_INDEX_REGISTER,
                                                    IndexRegister.setXM(rcsXReg.getW(), framePointer));
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
        private class BankManipulationStep13 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._lxjInstruction && (bmInfo._transferMode == TransferMode.BasicToBasic)) {
                    int parPCNext = _programAddressRegister.getProgramCounter() + 1;
                    long value = VirtualAddress.translateToBasicMode(bmInfo._priorBankLevel,
                                                                     bmInfo._priorBankDescriptorIndex,
                                                                     parPCNext);
                    value |= (long)(bmInfo._baseRegisterIndex & 03) << 33;
                    setExecOrUserXRegister(bmInfo._lxjXRegisterIndex, value);
                } else if ((bmInfo._instruction == Instruction.CALL)
                           && (bmInfo._transferMode == TransferMode.ExtendedToBasic)) {
                    setExecOrUserXRegister(11, 2L << 30);
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
        private class BankManipulationStep14 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._callOperation) {
                    long value = _designatorRegister.getBasicModeEnabled() ? 0_400000_000000L : 0;
                    value |= _indicatorKeyRegister.getAccessKey();
                    _generalRegisterSet.setRegister(GeneralRegisterSet.X0, value);
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
        private class BankManipulationStep15 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._gateBank != null) {
                    if (!bmInfo._gateBank._designatorBitInhibit) {
                        long temp = _designatorRegister.getW() & 0_777702_777777L;
                        temp |= bmInfo._gateBank._designatorBits12_17.getW() & 0_000075_000000L;
                        _designatorRegister.setW(temp);
                    }

                    if (!bmInfo._gateBank._accessKeyInhibit) {
                        _indicatorKeyRegister.setAccessKey((int) bmInfo._gateBank._accessKey.get());
                    }

                    if (!bmInfo._gateBank._latentParameter0Inhibit) {
                        setExecOrUserRRegister(0, bmInfo._gateBank._latentParameter0);
                    }

                    if (!bmInfo._gateBank._latentParameter1Inhibit) {
                        setExecOrUserRRegister(1, bmInfo._gateBank._latentParameter1);
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
        private class BankManipulationStep16 implements BankManipulationStep {

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
                    long lbdi = (bmInfo._targetBankLevel << 15) | bmInfo._targetBankDescriptorIndex;
                    _programAddressRegister.setLBDI(lbdi);
                    _programAddressRegister.setProgramCounter(bmInfo._targetBankOffset);

                    boolean hwCheck = bmInfo._interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.HardwareCheck;
                    DesignatorRegister newDR = new DesignatorRegister(0);
                    newDR.setExecRegisterSetSelected(true);
                    newDR.setArithmeticExceptionEnabled(true);
                    newDR.setBasicModeEnabled(bmInfo._targetBankDescriptor.getBankType() == BankType.BasicMode);
                    newDR.setBasicModeBaseRegisterSelection(_designatorRegister.getBasicModeBaseRegisterSelection());
                    newDR.setFaultHandlingInProgress(hwCheck);
                    _designatorRegister = newDR;

                    _indicatorKeyRegister.setW(0);
                    bmInfo._nextStep = 18;
                } else if (bmInfo._instruction == Instruction.UR) {
                    //  Entire ASP is loaded from 7 consecutive operand words.
                    //  ISW0, ISW1, and SSF of Indicator/Key register are ignored, and some Designator Bits are set-to-zero.
                    _programAddressRegister.set(bmInfo._operands[0]);
                    _designatorRegister = new DesignatorRegister(bmInfo._operands[1]);

                    IndicatorKeyRegister ikr = new IndicatorKeyRegister(bmInfo._operands[2]);
                    ikr.setShortStatusField(_indicatorKeyRegister.getShortStatusField());
                    _indicatorKeyRegister = ikr;

                    _quantumTimer = bmInfo._operands[3];
                    _currentInstruction = new InstructionWord(bmInfo._operands[4]);
                    bmInfo._nextStep = 18;
                } else if (bmInfo._returnOperation) {
                    _indicatorKeyRegister.setAccessKey((int) bmInfo._rcsFrame._accessKey.get());
                    _designatorRegister.setS4(bmInfo._rcsFrame._designatorRegisterDB12To17.getS4());
                    //  Special code for RTN instruction (per architecture document for emulated systems):
                    //  On return, we clear DB15, and if DB14 is set, we clear DB17.
                    //  What this means, is that returned-to PPrivilege 1 -> 0 and 3 -> 2,
                    //      and that we clear exec-register-set-selected if PPrivilege is returning to 2 or 3.
                    //  The latter makes sense - we don't want exec register selected for lower PPrivilege
                    //      (although the OS should ensure this), so I'm implementing that.
                    //  But the former... implies that on every RTN, the processor privilege *might* get elevated
                    //      by one step.  Why this is done is beyond me, but there it is.  In order to support
                    //      all four processor privileges properly, I'm going to NOT implement that.
                    if (_designatorRegister.getProcessorPrivilege() > 1) {
                        _designatorRegister.setExecRegisterSetSelected(false);
                    }
                } else if ((bmInfo._instruction == Instruction.GOTO)
                           || (bmInfo._instruction == Instruction.CALL)) {
                    if (bmInfo._transferMode == TransferMode.ExtendedToBasic) {
                        _designatorRegister.setBasicModeEnabled(true);
                    }
                } else if ((bmInfo._lxjInstruction) && (bmInfo._transferMode == TransferMode.BasicToExtended)) {
                    _designatorRegister.setBasicModeEnabled(false);
                }

                bmInfo._nextStep++;
            }
        }

        /**
         *  For transfer instructions, offset from step 3 (or step 9, if gated) -> PAR.PC
         */
        private class BankManipulationStep17 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._transferMode != null) {
                    setProgramCounter(bmInfo._targetBankOffset, true);
                }
                bmInfo._nextStep++;
            }
        }

        /**
         * Update Hard-held PAR.L,BDI if we loaded into B0,
         * or the appropriate ABT entry to zero for a void bank, or L,BDI otherwise.
         */
        private class BankManipulationStep18 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if (bmInfo._baseRegisterIndex == 0) {
                    //  This is already done for interrupt handling and UR
                    if ((bmInfo._interrupt == null) && (bmInfo._instruction != Instruction.UR)) {
                        _programAddressRegister.setLBDI((bmInfo._targetBankLevel << 15) | bmInfo._targetBankDescriptorIndex);
                    }
                } else if (bmInfo._baseRegisterIndex < 16) {
                    if (bmInfo._targetBankDescriptor == null) {
                        ActiveBaseTableEntry abte = new ActiveBaseTableEntry(0, 0, 0);
                        _activeBaseTableEntries[bmInfo._baseRegisterIndex] = abte;
                    } else {
                        int offset = bmInfo._loadInstruction ? bmInfo._targetBankOffset : 0;
                        ActiveBaseTableEntry abte = new ActiveBaseTableEntry(bmInfo._targetBankLevel,
                                                                             bmInfo._targetBankDescriptorIndex,
                                                                             offset);
                        _activeBaseTableEntries[bmInfo._baseRegisterIndex] = abte;
                    }
                }

                bmInfo._nextStep++;
            }
        }

        /**
         * Load the appropriate base register
         */
        private class BankManipulationStep19 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) throws AddressingExceptionInterrupt {
                if (bmInfo._targetBankDescriptor == null) {
                    _baseRegisters[bmInfo._baseRegisterIndex] = new BaseRegister();
                } else if (bmInfo._loadInstruction && (bmInfo._targetBankOffset != 0)) {
                    try {
                        _baseRegisters[bmInfo._baseRegisterIndex] =
                            new BaseRegister(bmInfo._targetBankDescriptor, bmInfo._targetBankOffset);

                    } catch (AddressingExceptionInterrupt ex) {
                        throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                               ex.getBankLevel(),
                                                               ex.getBankDescriptorIndex());
                    }
                } else {
                    try {
                        _baseRegisters[bmInfo._baseRegisterIndex] = new BaseRegister(bmInfo._targetBankDescriptor);
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
        private class BankManipulationStep20 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) {
                if ((bmInfo._transferMode == TransferMode.BasicToBasic) || (bmInfo._transferMode == TransferMode.ExtendedToBasic)) {
                    findBasicModeBank(bmInfo._targetBankOffset, true);
                }

                bmInfo._nextStep++;
            }
        }

        /**
         * Final exception checks
         */
        private class BankManipulationStep21 implements BankManipulationStep {

            @Override
            public void handler(
                final BankManipulationInfo bmInfo
            ) throws AddressingExceptionInterrupt {
                if (bmInfo._targetBankDescriptor != null) {
                    //  Check BD.G for LBU,LBE, and all transfers
                    if ((bmInfo._instruction == Instruction.LBE)
                        || (bmInfo._instruction == Instruction.LBU)
                        || (bmInfo._transferMode != null)) {
                        if (bmInfo._targetBankDescriptor.getGeneralFault()) {
                            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                                   bmInfo._targetBankLevel,
                                                                   bmInfo._targetBankDescriptorIndex);
                        }
                    }

                    AccessPermissions perms =
                        getEffectiveAccessPermissions(_indicatorKeyRegister.getAccessInfo(),
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
                        && (bmInfo._targetBankDescriptor.getBankType() == BankType.BasicMode)) {
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
                        if (bmInfo._targetBankDescriptor.getBankType() == BankType.BasicMode) {
                            BaseRegister br = _baseRegisters[bmInfo._baseRegisterIndex];
                            int relAddr = _programAddressRegister.getProgramCounter();
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
        private final BankManipulationStep[] _bankManipulationSteps = {
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
        private AccessPermissions getEffectiveAccessPermissions(
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
        private void process(
            final BankManipulationInfo bmInfo
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
            while (bmInfo._nextStep != 0) {
                _bankManipulationSteps[bmInfo._nextStep].handler(bmInfo);
            }
        }

        /**
         * An algorithm for handling bank transitions for the following instructions:
         *  CALL, GOTO, LBE, LBJ, LBU, LDJ, and LIJ
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
        private void bankManipulation(
            final Instruction instruction,
            final long operand
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
            long[] operands = { operand };
            BankManipulationInfo bmInfo = new BankManipulationInfo(instruction, operands, null);
            process(bmInfo);
        }

        /**
         * An algorithm for handling bank transitions for the UR instruction
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
        private void bankManipulation(
            final Instruction instruction,
            final long[] operands
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
            BankManipulationInfo bmInfo = new BankManipulationInfo(instruction, operands, null);
            process(bmInfo);
        }

        /**
         * An algorithm for handling bank transitions for the LAE instruction
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
        private void bankManipulation(
            final Instruction instruction,
            final int baseRegisterIndex,
            final long operand
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
            long[] operands = { operand };
            BankManipulationInfo bmInfo = new BankManipulationInfo(instruction, operands, null);
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
        private void bankManipulation(
            final MachineInterrupt interrupt
        ) throws AddressingExceptionInterrupt,
                 InvalidInstructionInterrupt,
                 RCSGenericStackUnderflowOverflowInterrupt {
            BankManipulationInfo bmInfo = new BankManipulationInfo(null, null, interrupt);
            process(bmInfo);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Function handler and function table
    //  ----------------------------------------------------------------------------------------------------------------------------

    private enum Instruction {
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
        LMA,    LMC,    LMJ,    LNA,    LNMA,   LOCL,   LPD,    LPM,
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
    }

    /**
     * Base class for a function handler (includes instruction handlers and sub function handlers)
     */
    private static abstract class FunctionHandler {

        /**
         * Handles a function (i.e., the instruction in _currentInstruction
         */
        abstract void handle() throws MachineInterrupt, UnresolvedAddressException;
    }

    /**
     * Base class for all the instruction handlers
     */
    private static abstract class InstructionHandler extends FunctionHandler {

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
        int getQuantumTimerCharge() { return 20; }
    }

    /**
     * Handles f-field function codes which require looking up the j-field in a sub-table
     */
    private class SubFunctionHandler extends FunctionHandler {

        private final FunctionHandler[] _table;

        SubFunctionHandler(FunctionHandler[] table) { _table = table; }

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            FunctionHandler handler = _table[(int) _currentInstruction.getJ()];
            if (handler == null) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
            }
            handler.handle();
        }

        /**
         * Retrieves ths sub-handler we know about which corresponds to the given j-field
         * @param jField value for j field
         * @return handler associated with a particular combination of f and j fields
         */
        FunctionHandler getHandler(
            final int jField
        ) {
            return ((jField < 0) || (jField >= _table.length)) ? null : _table[jField];
        }
    }

    /**
     * Handles f/j-field function codes which require looking up the a-field in a sub-table
     */
    private class SubSubFunctionHandler extends FunctionHandler {

        private final FunctionHandler[] _table;

        SubSubFunctionHandler(FunctionHandler[] table) { _table = table; }

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            FunctionHandler handler = _table[(int) _currentInstruction.getA()];
            if (handler == null) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
            }
            handler.handle();
        }

        /**
         * Retrieves ths sub-handler we know about which corresponds to the given a-field
         * @param aField value for the a field
         * @return handler for the function code defined by a particular combination of f, j, and a fields
         */
        FunctionHandler getHandler(
            final int aField
        ) {
            return ((aField < 0) || (aField >= _table.length)) ? null : _table[aField];
        }
    }

    /**
     * Static class which contains all the lookup information for finding a handler for
     * a particular instruction word.
     */
    private class FunctionTable {

        //TODO still want to avoid these?
        //  Note: The following instructions are not intended to be implemented:
        //      BAO
        //      CMPXCHG

        /**
         * Basic Mode function handlers for f-field 005, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION005_HANDLERS = {
            new SZFunctionHandler(),    //  000
            new SNZFunctionHandler(),   //  001
            new SP1FunctionHandler(),   //  002
            new SN1FunctionHandler(),   //  003
            new SFSFunctionHandler(),   //  004
            new SFZFunctionHandler(),   //  005
            new SASFunctionHandler(),   //  006
            new SAZFunctionHandler(),   //  007
            new INCFunctionHandler(),   //  010
            new DECFunctionHandler(),   //  011
            new INC2FunctionHandler(),  //  012
            new DEC2FunctionHandler(),  //  013
            new ENZFunctionHandler(),   //  01r
            new ADD1FunctionHandler(),  //  015
            new SUB1FunctionHandler(),  //  016
            null,           //  017
        };

        /**
         * Basic Mode function handlers for f-field 007, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION007_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            new LAQWFunctionHandler(),  //  004
            new SAQWFunctionHandler(),  //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            new LDJFunctionHandler(),   //  012
            new LIJFunctionHandler(),   //  013
            new LPDFunctionHandler(),   //  014
            new SPDFunctionHandler(),   //  015
            null,           //  016
            new LBJFunctionHandler(),   //  017
        };

        /**
         * Basic Mode function handlers for f-field 037, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION037_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            new LMCFunctionHandler(),   //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Basic Mode function handlers for f-field 071, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION071_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            new DAFunctionHandler(),    //  010
            new DANFunctionHandler(),   //  011
            new DSFunctionHandler(),    //  012
            new DLFunctionHandler(),    //  013
            new DLNFunctionHandler(),   //  014
            new DLMFunctionHandler(),   //  015
            new DJZFunctionHandler(),   //  016
            new DTEFunctionHandler(),   //  017
        };

        /**
         * Basic Mode function handlers for f-field 072, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION072_HANDLERS = {
            null,           //  000
            new SLJFunctionHandler(),   //  001
            new JPSFunctionHandler(),   //  002
            new JNSFunctionHandler(),   //  003
            new AHFunctionHandler(),    //  004
            new ANHFunctionHandler(),   //  005
            new ATFunctionHandler(),    //  006
            new ANTFunctionHandler(),   //  007
            null,           //  010
            new ERFunctionHandler(),    //  011
            null,           //  012
            null,           //  013
            null,           //  014
            new TRAFunctionHandler(),   //  015
            new SRSFunctionHandler(),   //  016
            new LRSFunctionHandler(),   //  017
        };

        /**
         * Basic Mode function handlers for f-field 073, j-field 015, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION073_015_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            new ACELFunctionHandler(),  //  003
            new DCELFunctionHandler(),  //  004
            new SPIDFunctionHandler(),  //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            new LDFunctionHandler(),    //  014
            new SDFunctionHandler(),    //  015
            new URFunctionHandler(),    //  016
            new SGNLFunctionHandler(),  //  017
        };

        /**
         * Basic Mode function handlers for f-field 073, j-field 017, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION073_017_HANDLERS = {
            new TSFunctionHandler(),    //  000
            new TSSFunctionHandler(),   //  001
            new TCSFunctionHandler(),   //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Basic Mode function handlers for f-field 073, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION073_HANDLERS = {
            new SSCFunctionHandler(),   //  000
            new DSCFunctionHandler(),   //  001
            new SSLFunctionHandler(),   //  002
            new DSLFunctionHandler(),   //  003
            new SSAFunctionHandler(),   //  004
            new DSAFunctionHandler(),   //  005
            new LSCFunctionHandler(),   //  006
            new DLSCFunctionHandler(),  //  007
            new LSSCFunctionHandler(),  //  010
            new LDSCFunctionHandler(),  //  011
            new LSSLFunctionHandler(),  //  012
            new LDSLFunctionHandler(),  //  013
            null,           //  014
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION073_015_HANDLERS), //  015
            null,           //  016
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION073_017_HANDLERS), //  017
        };

        /**
         * Basic Mode function handlers for f-field 074 j-field 004, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION074_004_HANDLERS = {
            new JFunctionHandler(),     //  000
            new JKFunctionHandler(),    //  001
            new JKFunctionHandler(),    //  002
            new JKFunctionHandler(),    //  003
            new JKFunctionHandler(),    //  004
            new JKFunctionHandler(),    //  005
            new JKFunctionHandler(),    //  006
            new JKFunctionHandler(),    //  007
            new JKFunctionHandler(),    //  010
            new JKFunctionHandler(),    //  011
            new JKFunctionHandler(),    //  012
            new JKFunctionHandler(),    //  013
            new JKFunctionHandler(),    //  014
            new JKFunctionHandler(),    //  015
            new JKFunctionHandler(),    //  016
            new JKFunctionHandler(),    //  017
        };

        /**
         * Basic Mode function handlers for f-field 074 j-field 014, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION074_014_HANDLERS = {
            new JOFunctionHandler(),    //  000
            new JFUFunctionHandler(),   //  001
            new JFOFunctionHandler(),   //  002
            new JDFFunctionHandler(),   //  003
            null,           //  004
            null,           //  005
            null,           //  006
            new PAIJFunctionHandler(),  //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Basic Mode function handlers for f-field 074 j-field 015, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION074_015_HANDLERS = {
            new JNOFunctionHandler(),   //  000
            new JNFUFunctionHandler(),  //  001
            new JNFOFunctionHandler(),  //  002
            new JNDFFunctionHandler(),  //  003
            null,           //  004
            new HLTJFunctionHandler(),  //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Basic Mode function handlers for f-field 074, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION074_HANDLERS = {
            new JZFunctionHandler(),    //  000
            new JNZFunctionHandler(),   //  001
            new JPFunctionHandler(),    //  002
            new JNFunctionHandler(),    //  003
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION074_004_HANDLERS), //  004
            new HJFunctionHandler(),    //  005
            new NOPFunctionHandler(),   //  006
            new AAIJFunctionHandler(),  //  007
            new JNBFunctionHandler(),   //  010
            new JBFunctionHandler(),    //  011
            new JMGIFunctionHandler(),  //  012
            new LMJFunctionHandler(),   //  013
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION074_014_HANDLERS), //  014
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION074_015_HANDLERS), //  015
            new JCFunctionHandler(),    //  016
            new JNCFunctionHandler(),   //  017
        };

        /**
         * Basic Mode function handlers for f-field 075, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION075_HANDLERS = {
            new LBUFunctionHandler(),   //  000
            null,           //  001
            new SBUFunctionHandler(),   //  002
            new LBEFunctionHandler(),   //  003
            new SBEDFunctionHandler(),  //  004
            new LBEDFunctionHandler(),  //  005
            new SBUDFunctionHandler(),  //  006
            new LBUDFunctionHandler(),  //  007
            new TVAFunctionHandler(),   //  010
            null,           //  011
            null,           //  012
            new LXLMFunctionHandler(),  //  013
            new LBNFunctionHandler(),   //  014
            new CRFunctionHandler(),    //  015
            null,           //  016
            new RMDFunctionHandler(),   //  017
        };

        /**
         * Basic Mode function handlers for f-field 077 j-field 017, indexed by a-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION077_017_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            new HALTFunctionHandler(),
        };

        /**
         * Basic Mode function handlers for f-field 077, indexed by j-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTION077_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION077_017_HANDLERS),
        };

        /**
         * Basic mode function handler vector indexed by the instruction f-field
         */
        private final FunctionHandler[] BASIC_MODE_FUNCTIONS = {
            null,           //  000
            new SAFunctionHandler(),    //  001
            new SNAFunctionHandler(),   //  002
            new SMAFunctionHandler(),   //  003
            new SRFunctionHandler(),    //  004
            new SubSubFunctionHandler(BASIC_MODE_FUNCTION005_HANDLERS), //  005
            new SXFunctionHandler(),    //  006
            new SubFunctionHandler(BASIC_MODE_FUNCTION007_HANDLERS), //  007
            new LAFunctionHandler(),    //  010
            new LNAFunctionHandler(),   //  011
            new LMAFunctionHandler(),   //  012
            new LNMAFunctionHandler(),  //  013
            new AAFunctionHandler(),    //  014
            new ANAFunctionHandler(),   //  015
            new AMAFunctionHandler(),   //  016
            new ANMAFunctionHandler(),  //  017
            new AUFunctionHandler(),    //  020
            new ANUFunctionHandler(),   //  021
            null,           //  022
            new LRFunctionHandler(),    //  023
            new AXFunctionHandler(),    //  024
            new ANXFunctionHandler(),   //  025
            new LXMFunctionHandler(),   //  026
            new LXFunctionHandler(),    //  027
            new MIFunctionHandler(),    //  030
            new MSIFunctionHandler(),   //  031
            new MFFunctionHandler(),    //  032
            null,           //  033
            new DIFunctionHandler(),    //  034
            new DSFFunctionHandler(),   //  035
            new DFFunctionHandler(),    //  036
            new SubFunctionHandler(BASIC_MODE_FUNCTION037_HANDLERS),    //  037
            new ORFunctionHandler(),    //  040
            new XORFunctionHandler(),   //  041
            new ANDFunctionHandler(),   //  042
            new MLUFunctionHandler(),   //  043
            new TEPFunctionHandler(),   //  044
            new TOPFunctionHandler(),   //  045
            new LXIFunctionHandler(),   //  046
            new TLEMFunctionHandler(),  //  047
            new TZFunctionHandler(),    //  050
            new TNZFunctionHandler(),   //  051
            new TEFunctionHandler(),    //  052
            new TNEFunctionHandler(),   //  053
            new TLEFunctionHandler(),   //  054
            new TGFunctionHandler(),    //  055
            new TWFunctionHandler(),    //  056
            new TNWFunctionHandler(),   //  057
            new TPFunctionHandler(),    //  060
            new TNFunctionHandler(),    //  061
            null,           //  062
            null,           //  063
            null,           //  064
            null,           //  065
            null,           //  066
            null,           //  067
            new JGDFunctionHandler(),   //  070
            new SubFunctionHandler(BASIC_MODE_FUNCTION071_HANDLERS),    //  071
            new SubFunctionHandler(BASIC_MODE_FUNCTION072_HANDLERS),    //  072
            new SubFunctionHandler(BASIC_MODE_FUNCTION073_HANDLERS),    //  073
            new SubFunctionHandler(BASIC_MODE_FUNCTION074_HANDLERS),    //  074
            new SubFunctionHandler(BASIC_MODE_FUNCTION075_HANDLERS),    //  075
            null,           //  076
            new SubFunctionHandler(BASIC_MODE_FUNCTION077_HANDLERS),    //  077
        };

        /**
         * Extended Mode function handlers for f-field 005, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION005_HANDLERS = {
            new SZFunctionHandler(),    //  000
            new SNZFunctionHandler(),   //  001
            new SP1FunctionHandler(),   //  002
            new SN1FunctionHandler(),   //  003
            new SFSFunctionHandler(),   //  004
            new SFZFunctionHandler(),   //  005
            new SASFunctionHandler(),   //  006
            new SAZFunctionHandler(),   //  007
            new INCFunctionHandler(),   //  010
            new DECFunctionHandler(),   //  011
            new INC2FunctionHandler(),  //  012
            new DEC2FunctionHandler(),  //  013
            new ENZFunctionHandler(),   //  014
            new ADD1FunctionHandler(),  //  015
            new SUB1FunctionHandler(),  //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 007, j-field 016, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION007_016_HANDLERS = {
            new LOCLFunctionHandler(),  //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            new CALLFunctionHandler(),  //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 007, j-field 017, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION007_017_HANDLERS = {
            new GOTOFunctionHandler(),  //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 007, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION007_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            new LAQWFunctionHandler(),  //  004
            new SAQWFunctionHandler(),  //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION007_016_HANDLERS),  //  016
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION007_017_HANDLERS),  //  017
        };

        /**
         * Extended Mode function handlers for f-field 033, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION033_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            new TGMFunctionHandler(),   //  013
            new DTGMFunctionHandler(),  //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 037, j-field 004, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION037_004_HANDLERS = {
            new SMDFunctionHandler(),   //  000
            new SDMNFunctionHandler(),  //  001
            new SDMFFunctionHandler(),  //  002
            new SDMSFunctionHandler(),  //  003
            new KCHGFunctionHandler(),  //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 037, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION037_HANDLERS = {
            new LRDFunctionHandler(),   //  000
            null,           //  001
            null,           //  002
            null,           //  003
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION037_004_HANDLERS),  //  004
            null,           //  005
            null,           //  006
            new LMCFunctionHandler(),   //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 050, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION050_HANDLERS = {
            new TNOPFunctionHandler(),  //  000
            new TGZFunctionHandler(),   //  001
            new TPZFunctionHandler(),   //  002
            new TPFunctionHandler(),    //  003
            new TMZFunctionHandler(),   //  004
            new TMZGFunctionHandler(),  //  005
            new TZFunctionHandler(),    //  006
            new TNLZFunctionHandler(),  //  007
            new TLZFunctionHandler(),   //  010
            new TNZFunctionHandler(),   //  011
            new TPZLFunctionHandler(),  //  012
            new TNMZFunctionHandler(),  //  013
            new TNFunctionHandler(),    //  014
            new TNPZFunctionHandler(),  //  015
            new TNGZFunctionHandler(),  //  016
            new TSKPFunctionHandler(),  //  017
        };

        /**
         * Extended Mode function handlers for f-field 071, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION071_HANDLERS = {
            new MTEFunctionHandler(),   //  000
            new MTNEFunctionHandler(),  //  001
            new MTLEFunctionHandler(),  //  002
            new MTGFunctionHandler(),   //  003
            new MTWFunctionHandler(),   //  004
            new MTNWFunctionHandler(),  //  005
            new MATLFunctionHandler(),  //  006
            new MATGFunctionHandler(),  //  007
            new DAFunctionHandler(),    //  010
            new DANFunctionHandler(),   //  011
            new DSFunctionHandler(),    //  012
            new DLFunctionHandler(),    //  013
            new DLNFunctionHandler(),   //  014
            new DLMFunctionHandler(),   //  015
            new DJZFunctionHandler(),   //  016
            new DTEFunctionHandler(),   //  017
        };

        /**
         * Extended Mode function handlers for f-field 072, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION072_HANDLERS = {
            new TRARSFunctionHandler(), //  000
            null,           //  001
            new JPSFunctionHandler(),   //  002
            new JNSFunctionHandler(),   //  003
            new AHFunctionHandler(),    //  004
            new ANHFunctionHandler(),   //  005
            new ATFunctionHandler(),    //  006
            new ANTFunctionHandler(),   //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            new TRAFunctionHandler(),   //  015
            new SRSFunctionHandler(),   //  016
            new LRSFunctionHandler(),   //  017
        };

        /**
         * Extended Mode function handlers for f-field 073, j-field 014, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION073_014_HANDLERS = {
            new NOPFunctionHandler(),   //  000
            null,           //  001
            new BUYFunctionHandler(),   //  002
            new SELLFunctionHandler(),  //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 073, j-field 015, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION073_015_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            new ACELFunctionHandler(),  //  003
            new DCELFunctionHandler(),  //  004
            new SPIDFunctionHandler(),  //  005
            new DABTFunctionHandler(),  //  006
            null,           //  007
            null,           //  010
            null,           //  011
            new LAEFunctionHandler(),   //  012
            new SKQTFunctionHandler(),  //  013
            new LDFunctionHandler(),    //  014
            new SDFunctionHandler(),    //  015
            new URFunctionHandler(),    //  016
            new SGNLFunctionHandler(),  //  017
        };

        /**
         * Extended Mode function handlers for f-field 073, j-field 017, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION073_017_HANDLERS = {
            new TSFunctionHandler(),    //  000
            new TSSFunctionHandler(),   //  001
            new TCSFunctionHandler(),   //  002
            new RTNFunctionHandler(),   //  003
            new LUDFunctionHandler(),   //  004
            new SUDFunctionHandler(),   //  005
            new IARFunctionHandler(),   //  006
            null,           //  007
            new IPCFunctionHandler(),   //  010
            null,           //  011
            new SYSCFunctionHandler(),  //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 073, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION073_HANDLERS = {
            new SSCFunctionHandler(),   //  000
            new DSCFunctionHandler(),   //  001
            new SSLFunctionHandler(),   //  002
            new DSLFunctionHandler(),   //  003
            new SSAFunctionHandler(),   //  004
            new DSAFunctionHandler(),   //  005
            new LSCFunctionHandler(),   //  006
            new DLSCFunctionHandler(),  //  007
            new LSSCFunctionHandler(),  //  010
            new LDSCFunctionHandler(),  //  011
            new LSSLFunctionHandler(),  //  012
            new LDSLFunctionHandler(),  //  013
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION073_014_HANDLERS),  //  014
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION073_015_HANDLERS),  //  015
            null,           //  016
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION073_017_HANDLERS),  //  017
        };

        /**
         * Extended Mode function handlers for f-field 074 j-field 014, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION074_014_HANDLERS = {
            new JOFunctionHandler(),    //  000
            new JFUFunctionHandler(),   //  001
            new JFOFunctionHandler(),   //  002
            new JDFFunctionHandler(),   //  003
            new JCFunctionHandler(),    //  004
            new JNCFunctionHandler(),   //  005
            new AAIJFunctionHandler(),  //  006
            new PAIJFunctionHandler(),  //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 074 j-field 015, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION074_015_HANDLERS = {
            new JNOFunctionHandler(),   //  000
            new JNFUFunctionHandler(),  //  001
            new JNFOFunctionHandler(),  //  002
            new JNDFFunctionHandler(),  //  003
            new JFunctionHandler(),     //  004
            new HLTJFunctionHandler(),  //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 074, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION074_HANDLERS = {
            new JZFunctionHandler(),    //  000
            new JNZFunctionHandler(),   //  001
            new JPFunctionHandler(),    //  002
            new JNFunctionHandler(),    //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            new JNBFunctionHandler(),   //  010
            new JBFunctionHandler(),    //  011
            new JMGIFunctionHandler(),  //  012
            new LMJFunctionHandler(),   //  013
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION074_014_HANDLERS),  //  014
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION074_015_HANDLERS),  //  015
            null,           //  016
            null,           //  017
        };

        /**
         * Extended Mode function handlers for f-field 075, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION075_HANDLERS = {
            new LBUFunctionHandler(),   //  000
            null,           //  001
            new SBUFunctionHandler(),   //  002
            new LBEFunctionHandler(),   //  003
            new SBEDFunctionHandler(),  //  004
            new LBEDFunctionHandler(),  //  005
            new SBUDFunctionHandler(),  //  006
            new LBUDFunctionHandler(),  //  007
            new TVAFunctionHandler(),   //  010
            null,           //  011
            new RDCFunctionHandler(),   //  012
            new LXLMFunctionHandler(),  //  013
            new LBNFunctionHandler(),   //  014
            new CRFunctionHandler(),    //  015
            null,           //  016
            new RMDFunctionHandler(),   //  017
        };

        /**
         * Extended Mode function handlers for f-field 077 j-field 017, indexed by a-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION077_017_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            new HALTFunctionHandler(),
        };

        /**
         * Extended Mode function handlers for f-field 077, indexed by j-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTION077_HANDLERS = {
            null,           //  000
            null,           //  001
            null,           //  002
            null,           //  003
            null,           //  004
            null,           //  005
            null,           //  006
            null,           //  007
            null,           //  010
            null,           //  011
            null,           //  012
            null,           //  013
            null,           //  014
            null,           //  015
            null,           //  016
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION077_017_HANDLERS),  //  017
        };

        /**
         * Extended mode function handler vector indexed by the instruction f-field
         */
        private final FunctionHandler[] EXTENDED_MODE_FUNCTIONS = {
            null,           //  000
            new SAFunctionHandler(),    //  001
            new SNAFunctionHandler(),   //  002
            new SMAFunctionHandler(),   //  003
            new SRFunctionHandler(),    //  004
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION005_HANDLERS), //  005
            new SXFunctionHandler(),    //  006
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION007_HANDLERS), //  007
            new LAFunctionHandler(),    //  010
            new LNAFunctionHandler(),   //  011
            new LMAFunctionHandler(),   //  012
            new LNMAFunctionHandler(),  //  013
            new AAFunctionHandler(),    //  014
            new ANAFunctionHandler(),   //  015
            new AMAFunctionHandler(),   //  016
            new ANMAFunctionHandler(),  //  017
            new AUFunctionHandler(),    //  020
            new ANUFunctionHandler(),   //  021
            null,           //  022
            new LRFunctionHandler(),    //  023
            new AXFunctionHandler(),    //  024
            new ANXFunctionHandler(),   //  025
            new LXMFunctionHandler(),   //  026
            new LXFunctionHandler(),    //  027
            new MIFunctionHandler(),    //  030
            new MSIFunctionHandler(),   //  031
            new MFFunctionHandler(),    //  032
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION033_HANDLERS), //  033
            new DIFunctionHandler(),    //  034
            new DSFFunctionHandler(),   //  035
            new DFFunctionHandler(),    //  036
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION037_HANDLERS), //  037
            new ORFunctionHandler(),    //  040
            new XORFunctionHandler(),   //  041
            new ANDFunctionHandler(),   //  042
            new MLUFunctionHandler(),   //  043
            new TEPFunctionHandler(),   //  044
            new TOPFunctionHandler(),   //  045
            new LXIFunctionHandler(),   //  046
            new TLEMFunctionHandler(),  //  047
            new SubSubFunctionHandler(EXTENDED_MODE_FUNCTION050_HANDLERS),  //  050 sub-indexed by a-field, *NOT* j-field
            new LXSIFunctionHandler(),  //  051
            new TEFunctionHandler(),    //  052
            new TNEFunctionHandler(),   //  053
            new TLEFunctionHandler(),   //  054
            new TGFunctionHandler(),    //  055
            new TWFunctionHandler(),    //  056
            new TNWFunctionHandler(),   //  057
            new LSBOFunctionHandler(),  //  060
            new LSBLFunctionHandler(),  //  061
            null,           //  062
            null,           //  063
            null,           //  064
            null,           //  065
            null,           //  066
            null,           //  067
            new JGDFunctionHandler(),   //  070
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION071_HANDLERS), //  071
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION072_HANDLERS), //  072
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION073_HANDLERS), //  073
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION074_HANDLERS), //  074
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION075_HANDLERS), //  075
            null,           //  076
            new SubFunctionHandler(EXTENDED_MODE_FUNCTION077_HANDLERS), //  077
        };

        /**
         * Retrieves the proper instruction/function handler given the instruction word
         * @param iw instruction word of interest
         * @param basicMode true if we are in basic mode, false if extended mode
         * @return InstructionHandler if found, else null
         */
        FunctionHandler lookup(
            final InstructionWord iw,
            final boolean basicMode
        ) {
            int fField = (int)iw.getF();
            FunctionHandler handler = basicMode ? BASIC_MODE_FUNCTIONS[fField] : EXTENDED_MODE_FUNCTIONS[fField];

            if (handler instanceof SubFunctionHandler) {
                handler = ((SubFunctionHandler)handler).getHandler((int)iw.getJ());
            }

            if (handler instanceof SubSubFunctionHandler) {
                handler = ((SubSubFunctionHandler)handler).getHandler((int)iw.getA());
            }

            return handler;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Actual Function Handlers
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles the AA instruction f=014
     */
    private class AAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int iaReg = (int) _currentInstruction.getA();
            long operand1 = getExecOrUserARegister(iaReg).getW();
            long operand2 = getOperand(true, true, true, true);
            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserARegister(iaReg, sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.AA; }
    }

    /**
     * Handles the AAIJ instruction f=074 j=014 a=06 for extended mode (requires PP=0),
     *                              f=074 j=07  a=not-used for basic mode (any PP)
     */
    private class AAIJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            _designatorRegister.setDeferrableInterruptEnabled(true);
            setProgramCounter(getJumpOperand(true), true);
        }

        @Override public Instruction getInstruction() { return Instruction.AAIJ; }
    }

    /**
     * Handles the ACEL instruction f=073 j=015 a=003
     * Loads the X, A, and R registers from the 48-word operand packet.
     */
    private class ACELFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 2) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long[] ops = new long[48];
            getConsecutiveOperands(false, ops, false);
            int opx = 0;

            for (int grsx = GeneralRegisterSet.X0; grsx <= GeneralRegisterSet.X11; ++grsx) {
                setGeneralRegister(grsx, ops[opx++]);
            }

            for (int grsx = GeneralRegisterSet.A0; grsx <= GeneralRegisterSet.A15 + 4; ++grsx) {
                setGeneralRegister(grsx, ops[opx++]);
            }

            for (int grsx = GeneralRegisterSet.R0; grsx <= GeneralRegisterSet.R15; ++grsx) {
                setGeneralRegister(grsx, ops[opx++]);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.ACEL; }
    }

    /**
     * Handles the ADD1 instruction f=005, a=015
     */
    private class ADD1FunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            incrementOperand(true, true, 01, twosComplement);
            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.ADD1; }
    }

    /**
     * Handles the AH instruction f=072 j=04
     */
    private class AHFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, false, false);

            long op1h1 = Word36.getSignExtended18(Word36.getH1(operand1));
            long op1h2 = Word36.getSignExtended18(Word36.getH2(operand1));
            long op2h1 = Word36.getSignExtended18(Word36.getH1(operand2));
            long op2h2 = Word36.getSignExtended18(Word36.getH2(operand2));

            long resulth1 = Word36.addSimple(op1h1, op2h1);
            long resulth2 = Word36.addSimple(op1h2, op2h2);
            long result = ((resulth1 & 0_777777) << 18) | (resulth2 & 0_777777);

            setExecOrUserARegister((int) _currentInstruction.getA(), result);
        }

        @Override public Instruction getInstruction() { return Instruction.AH; }
    }

    /**
     * Handles the AMA instruction f=016
     */
    private class AMAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);
            if (Word36.isNegative(operand2)) {
                operand2 = Word36.negate(operand2);
            }

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserARegister((int) _currentInstruction.getA(), sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.AMA; }
    }

    /**
     * Handles the ANA instruction f=015
     */
    private class ANAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = Word36.negate(getOperand(true, true, true, true));

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserARegister((int) _currentInstruction.getA(), sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.ANA; }
    }

    /**
     * Handles the AND instruction f=042
     */
    private class ANDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, operand1 & operand2);
        }

        @Override public Instruction getInstruction() { return Instruction.AND; }
    }

    /**
     * Handles the ANH instruction f=072 j=05
     */
    private class ANHFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, false, false);

            long op1h1 = Word36.getSignExtended18(Word36.getH1(operand1));
            long op1h2 = Word36.getSignExtended18(Word36.getH2(operand1));
            long op2h1 = Word36.negate(Word36.getSignExtended18(Word36.getH1(operand2)));
            long op2h2 = Word36.negate(Word36.getSignExtended18(Word36.getH2(operand2)));

            long resulth1 = Word36.addSimple(op1h1, op2h1);
            long resulth2 = Word36.addSimple(op1h2, op2h2);
            long result = ((resulth1 & 0_777777) << 18) | (resulth2 & 0_777777);

            setExecOrUserARegister((int) _currentInstruction.getA(), result);
        }

        @Override public Instruction getInstruction() { return Instruction.ANH; }
    }

    /**
     * Handles the ANMA instruction f=017
     */
    private class ANMAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);
            if (Word36.isPositive(operand2)) {
                operand2 = Word36.negate(operand2);
            }

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserARegister((int) _currentInstruction.getA(), sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.ANMA; }
    }

    /**
     * Handles the ANT instruction f=072 j=06
     */
    private class ANTFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, false, false);

            long op1t1 = Word36.getSignExtended12(Word36.getT1(operand1));
            long op1t2 = Word36.getSignExtended12(Word36.getT2(operand1));
            long op1t3 = Word36.getSignExtended12(Word36.getT3(operand1));
            long op2t1 = Word36.negate(Word36.getSignExtended12(Word36.getT1(operand2)));
            long op2t2 = Word36.negate(Word36.getSignExtended12(Word36.getT2(operand2)));
            long op2t3 = Word36.negate(Word36.getSignExtended12(Word36.getT3(operand2)));

            long resultt1 = Word36.addSimple(op1t1, op2t1) & 07777;
            long resultt2 = Word36.addSimple(op1t2, op2t2) & 07777;
            long resultt3 = Word36.addSimple(op1t3, op2t3) & 07777;
            long result = (resultt1 << 24) | (resultt2 << 12) | resultt3;

            setExecOrUserARegister((int) _currentInstruction.getA(), result);
        }

        @Override public Instruction getInstruction() { return Instruction.ANT; }
    }

    /**
     * Handles the ANU instruction f=021
     */
    private class ANUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = Word36.negate(getOperand(true, true, true, true));

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserARegister((int) _currentInstruction.getA() + 1, sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.ANU; }
    }

    /**
     * Handles the ANX instruction f=025
     */
    private class ANXFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserXRegister((int) _currentInstruction.getA()).getW();
            long operand2 = Word36.negate(getOperand(true, true, true, true));

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserXRegister((int) _currentInstruction.getA(), sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.ANX; }
    }

    /**
     * Handles the AT instruction f=072 j=07
     */
    private class ATFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, false, false);

            long op1t1 = Word36.getSignExtended12(Word36.getT1(operand1));
            long op1t2 = Word36.getSignExtended12(Word36.getT2(operand1));
            long op1t3 = Word36.getSignExtended12(Word36.getT3(operand1));
            long op2t1 = Word36.getSignExtended12(Word36.getT1(operand2));
            long op2t2 = Word36.getSignExtended12(Word36.getT2(operand2));
            long op2t3 = Word36.getSignExtended12(Word36.getT3(operand2));

            long resultt1 = Word36.addSimple(op1t1, op2t1) & 07777;
            long resultt2 = Word36.addSimple(op1t2, op2t2) & 07777;
            long resultt3 = Word36.addSimple(op1t3, op2t3) & 07777;
            long result = (resultt1 << 24) | (resultt2 << 12) | resultt3;

            setExecOrUserARegister((int) _currentInstruction.getA(), result);
        }

        @Override public Instruction getInstruction() { return Instruction.AT; }
    }

    /**
     * Handles the AU instruction f=020
     */
    private class AUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserARegister((int) _currentInstruction.getA() + 1, sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.AU; }
    }

    /**
     * Handles the AX instruction f=024
     */
    private class AXFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserXRegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);

            Word36.StaticAdditionResult sar = Word36.add(operand1, operand2);

            setExecOrUserXRegister((int) _currentInstruction.getA(), sar._value);
            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && sar._flags._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.AX; }
    }

    /**
     * Handles the BUY instruction (f=073 j=014 a=02) extended mode only
     */
    private class BUYFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean longModifier = _designatorRegister.getExecutive24BitIndexingEnabled();
            int ixReg = (int) _currentInstruction.getX();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            BaseRegister bReg = _baseRegisters[(int) _currentInstruction.getB()];

            long subtrahend = _currentInstruction.getD() + (longModifier ? xReg.getXI12() : xReg.getXI());
            long newModifier = (longModifier ? xReg.getXM24() : xReg.getXM()) - subtrahend;
            if (bReg._voidFlag
                || (newModifier < bReg._lowerLimitNormalized)
                || (newModifier > bReg._upperLimitNormalized)) {
                throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                    (int) _currentInstruction.getB(),
                                                                    (int) newModifier);
            }

            if (longModifier) {
                setExecOrUserXRegister(ixReg, IndexRegister.setXM24(xReg.getW(), newModifier));
            } else {
                setExecOrUserXRegister(ixReg, IndexRegister.setXM(xReg.getW(), newModifier));
            }
        }

        @Override public Instruction getInstruction() { return Instruction.BUY; }
    }

    /**
     * Handles the CALL instruction f=07 j=016 a=013
     */
    private class CALLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(false, true, false, false);
            new InstructionProcessor.BankManipulator().bankManipulation(Instruction.CALL, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.CALL; }
    }

    /**
     * Handles the CR instruction f=075 j=015
     * If A(a) matches the contents of U, then A(a+1) is written to U
     * Requires storage lock
     */
    private class CRFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  For basic mode, PP must be zero
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            int relAddress = calculateRelativeAddressForGRSOrStorage();
            int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
            BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
            baseRegister.checkAccessLimits(relAddress, false, true, true, _indicatorKeyRegister.getAccessInfo());
            AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
            setStorageLock(absAddress);

            long value;
            checkBreakpoint(BreakpointComparison.Read, absAddress);
            try {
                value = getStorageValue(absAddress);
            } catch (AddressLimitsException
                | UPINotAssignedException
                | UPIProcessorTypeException ex) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
            }

            if (value == getExecOrUserARegister((int) _currentInstruction.getA()).getW()) {
                checkBreakpoint(BreakpointComparison.Write, absAddress);
                long newValue = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();
                try {
                    setStorageValue(absAddress, newValue);
                    skipNextInstruction();
                } catch (AddressLimitsException
                    | UPINotAssignedException
                    | UPIProcessorTypeException ex) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
                }
            }

            incrementIndexRegisterInF0();
        }

        @Override public Instruction getInstruction() { return Instruction.CR; }
    }

    /**
     * Handles the DA instruction f=071 j=010
     */
    private class DAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand1 = {
                getExecOrUserARegister((int) _currentInstruction.getA()).getW(),
                getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW()
            };
            DoubleWord36 dwOperand1 = new DoubleWord36(operand1[0], operand1[1]);

            long[] operand2 = new long[2];
            getConsecutiveOperands(true, operand2, false);
            DoubleWord36 dwOperand2 = new DoubleWord36(operand2[0], operand2[1]);

            DoubleWord36.AdditionResult ar = dwOperand1.add(dwOperand2);

            long[] result = {
                ar._value.get().shiftRight(36).longValue() & Word36.BIT_MASK,
                ar._value.get().longValue() & Word36.BIT_MASK
            };

            setExecOrUserARegister((int) _currentInstruction.getA(), result[0]);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, result[1]);

            _designatorRegister.setCarry(ar._carry);
            _designatorRegister.setOverflow(ar._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && ar._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.DA; }
    }

    /**
     * Handles the DABT instruction f=073 j=015 a=06
     */
    private class DABTFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 1) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            //  Store all active base table entries to storage
            long[] values = new long[15];
            for (int ax = 1; ax < 16; ++ax) {
                values[ax - 1] = _activeBaseTableEntries[ax]._value;
            }

            storeConsecutiveOperands(false, values);
        }

        @Override public Instruction getInstruction() { return Instruction.DABT; }
    }

    /**
     * Handles the AA instruction f=071 j=011
     */
    private class DANFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand1 = {
                getExecOrUserARegister((int) _currentInstruction.getA()).getW(),
                getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW()
            };
            DoubleWord36 dwOperand1 = new DoubleWord36(operand1[0], operand1[1]);

            long[] operand2 = new long[2];
            getConsecutiveOperands(true, operand2, false);
            DoubleWord36 dwOperand2 = new DoubleWord36(operand2[0], operand2[1]).negate();

            DoubleWord36.AdditionResult ar = dwOperand1.add(dwOperand2);

            long[] result = {
                ar._value.get().shiftRight(36).longValue() & Word36.BIT_MASK,
                ar._value.get().longValue() & Word36.BIT_MASK
            };

            setExecOrUserARegister((int) _currentInstruction.getA(), result[0]);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, result[1]);

            _designatorRegister.setCarry(ar._carry);
            _designatorRegister.setOverflow(ar._overflow);
            if (_designatorRegister.getOperationTrapEnabled() && ar._overflow) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.DAN; }
    }

    /**
     * Handles the DCEL instruction f=073 j=015 a=004
     */
    private class DCELFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 2) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long[] ops = new long[48];
            int opx = 0;
            for (int grsx = GeneralRegisterSet.X0; grsx <= GeneralRegisterSet.X11; ++grsx) {
                ops[opx++] = _generalRegisterSet.getRegister(grsx).getW();
            }
            for (int grsx = GeneralRegisterSet.A0; grsx <= GeneralRegisterSet.A15 + 4; ++grsx) {
                ops[opx++] = _generalRegisterSet.getRegister(grsx).getW();
            }
            for (int grsx = GeneralRegisterSet.R0; grsx <= GeneralRegisterSet.R15; ++grsx) {
                ops[opx++] = _generalRegisterSet.getRegister(grsx).getW();
            }

            storeConsecutiveOperands(false, ops);
        }

        @Override public Instruction getInstruction() { return Instruction.DCEL; }
    }

    /**
     * Handles the DEC instruction f=005, a=011
     * Requires storage lock
     */
    private class DECFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            boolean skip = incrementOperand(true, true, NEGATIVE_ONE_36, twosComplement);

            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }

            if (skip) { skipNextInstruction(); }
        }

        @Override public Instruction getInstruction() { return Instruction.DEC; }
    }

    /**
     * Handles the DEC2 instruction f=005, a=013
     * Requires storage lock
     */
    private class DEC2FunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            boolean skip = incrementOperand(true, true, NEGATIVE_TWO_36, twosComplement);

            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }

            if (skip) { skipNextInstruction(); }
        }

        @Override public Instruction getInstruction() { return Instruction.DEC2; }
    }

    /**
     * Handles the DF instruction f=036
     * 72-bit signed dividend in A(a)|A(a+1) is shifted right algebraically by one bit,
     * then divided by the 36-bit divisor in U, with the 36-bit quotient stored in A(a)
     * and the 36-bit remainder in A(a+1).
     * Divide check raised if |dividend| >= ( |divisor| * 2^35 ) or divisor is +/- 0
     */
    private class DFFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] dividend = {
                getExecOrUserARegister((int) _currentInstruction.getA()).getW(),
                getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW()
            };

            long[] divisor = new long[2];
            divisor[1] = getOperand(true, true, true, true);
            divisor[0] = Word36.isNegative(divisor[1]) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO;

            long quotient = 0;
            long remainder = 0;

            DoubleWord36 dwDividend = new DoubleWord36(dividend[0], dividend[1]).rightShiftAlgebraic(1);
            DoubleWord36 dwDivisor = new DoubleWord36(divisor[0], divisor[1]);
            BigInteger compDividend = dwDividend.get().abs();
            BigInteger compDivisor = dwDivisor.get().abs().shiftLeft(35);
            if (dwDivisor.isZero() || (compDividend.compareTo(compDivisor) >= 0)) {
                _designatorRegister.setDivideCheck(true);
                if (_designatorRegister.getArithmeticExceptionEnabled() ) {
                    throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
                }
            } else {
                DoubleWord36.DivisionResult dr = dwDividend.divide(dwDivisor);
                quotient = dr._result.get().longValue() & Word36.BIT_MASK;
                remainder = dr._remainder.get().longValue() & Word36.BIT_MASK;
            }

            setExecOrUserARegister((int) _currentInstruction.getA(), quotient);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, remainder);
        }

        @Override public Instruction getInstruction() { return Instruction.DF; }
    }

    /**
     * Handles the DI instruction f=034
     * 72-bit signed dividend in A(a)|A(a+1) is divided by the 36-bit divisor in U.
     * The 36-bit quotient stored in A(a), and the 36-bit remainder in A(a+1).
     * Divide check raised if |dividend| >= ( |divisor| * 2^35 ) or divisor is +/- 0
     */
    private class DIFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] dividend = {
                getExecOrUserARegister((int) _currentInstruction.getA()).getW(),
                getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW()
            };

            long[] divisor = new long[2];
            divisor[1] = getOperand(true, true, true, true);
            divisor[0] = Word36.isNegative(divisor[1]) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO;

            long quotient = 0;
            long remainder = 0;

            DoubleWord36 dwDividend = new DoubleWord36(dividend[0], dividend[1]);
            DoubleWord36 dwDivisor = new DoubleWord36(divisor[0], divisor[1]);
            BigInteger compDividend = dwDividend.get().abs();
            BigInteger compDivisor = dwDivisor.get().abs().shiftLeft(35);
            if (dwDivisor.isZero() || (compDividend.compareTo(compDivisor) >= 0)) {
                _designatorRegister.setDivideCheck(true);
                if (_designatorRegister.getArithmeticExceptionEnabled()) {
                    throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
                }
            } else {
                DoubleWord36.DivisionResult dr = dwDividend.divide(dwDivisor);
                quotient = dr._result.get().longValue() & Word36.BIT_MASK;
                remainder = dr._remainder.get().longValue() & Word36.BIT_MASK;
            }

            setExecOrUserARegister((int) _currentInstruction.getA(), quotient);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, remainder);
        }

        @Override public Instruction getInstruction() { return Instruction.DI; }
    }

    /**
     * Handles the DJZ instruction f=071 j=016
     */
    private class DJZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            DoubleWord36 dw36 = new DoubleWord36(getExecOrUserARegister((int) _currentInstruction.getA()).getW(),
                                                 getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW());
            if (dw36.isZero()) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.DJZ; }
    }

    /**
     * Handles the DL instruction f=071 j=013
     */
    private class DLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operands = new long[2];
            getConsecutiveOperands(true, operands, false);

            int grsIndex = getExecOrUserARegisterIndex((int) _currentInstruction.getA());
            setGeneralRegister(grsIndex, operands[0]);
            setGeneralRegister(grsIndex + 1, operands[1]);
        }

        @Override public Instruction getInstruction() { return Instruction.DL; }
    }

    /**
     * Handles the DLM instruction f=071 j=015
     */
    private class DLMFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operands = new long[2];
            getConsecutiveOperands(true, operands, false);
            if (Word36.isNegative(operands[0])) {
                operands[0] = (~operands[0]) & Word36.BIT_MASK;
                operands[1] = (~operands[1]) & Word36.BIT_MASK;
            }

            int grsIndex = getExecOrUserARegisterIndex((int) _currentInstruction.getA());
            setGeneralRegister(grsIndex, operands[0]);
            setGeneralRegister(grsIndex + 1, operands[1]);
        }

        @Override public Instruction getInstruction() { return Instruction.DLM; }
    }

    /**
     * Handles the DLN instruction f=071 j=014
     */
    private class DLNFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operands = new long[2];
            getConsecutiveOperands(true, operands, false);
            operands[0] = (~operands[0]) & Word36.BIT_MASK;
            operands[1] = (~operands[1]) & Word36.BIT_MASK;

            int grsIndex = getExecOrUserARegisterIndex((int) _currentInstruction.getA());
            setGeneralRegister(grsIndex, operands[0]);
            setGeneralRegister(grsIndex + 1, operands[1]);
        }

        @Override
        public Instruction getInstruction() { return Instruction.DLN; }
    }

    /**
     * Handles the DLSC instruction f=073 j=007
     * Double left-circular-shifts the UI until bit 0 is not equal to bit 1,
     * then store that result into A(a)/A(a+1).
     * Store the shift count in A(a+2).
     */
    private class DLSCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {

            long[] operand = new long[2];
            getConsecutiveOperands(true, operand, false);
            DoubleWord36 dwOperand = new DoubleWord36(operand[0], operand[1]);

            int baseIndex = (int) _currentInstruction.getA();
            if (dwOperand.equals(DoubleWord36.DW36_POSITIVE_ZERO) || dwOperand.equals(DoubleWord36.DW36_NEGATIVE_ZERO)) {
                setExecOrUserARegister(baseIndex, dwOperand.getWords()[0].getW());
                setExecOrUserARegister(baseIndex + 1, dwOperand.getWords()[1].getW());
                setExecOrUserARegister(baseIndex + 2, 71);
            } else {
                long count = 0;
                long test = dwOperand.getWords()[0].getW() & 0_600000_000000L;
                while ((test == 0L) || (test == 0_600000_000000L)) {
                    dwOperand = dwOperand.leftShiftCircular(1);
                    test = dwOperand.getWords()[0].getW() & 0_600000_000000L;
                    ++count;
                }

                Word36[] w36 = dwOperand.getWords();
                setExecOrUserARegister((int) _currentInstruction.getA(), w36[0].getW());
                setExecOrUserARegister((int) _currentInstruction.getA() + 1, w36[1].getW());
                setExecOrUserARegister((int) _currentInstruction.getA() + 2, count);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.DLSC; }
    }

    /**
     * Handles the DS instruction f=071 j=012
     */
    private class DSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int grsIndex = getExecOrUserARegisterIndex((int) _currentInstruction.getA());
            long[] operands = new long[2];
            operands[0] = getGeneralRegister(grsIndex).getW();
            operands[1] = getGeneralRegister(grsIndex + 1).getW();
            storeConsecutiveOperands(true, operands);
        }

        @Override public Instruction getInstruction() { return Instruction.DS; }
    }

    /**
     * Handles the DSA instruction f=073 j=005
     */
    private class DSAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand = new long[2];
            operand[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            operand[1] = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            int count = (int) getImmediateOperand() & 0177;
            DoubleWord36 dw36 = new DoubleWord36(operand[0], operand[1]);
            DoubleWord36 result = dw36.rightShiftAlgebraic(count);
            Word36[] components = result.getWords();

            int baseIndex = (int) _currentInstruction.getA();
            setExecOrUserARegister(baseIndex, components[0].getW());
            setExecOrUserARegister(baseIndex + 1, components[1].getW());
            setExecOrUserARegister(baseIndex + 2, count);
        }

        @Override public Instruction getInstruction() { return Instruction.DSA; }
    }

    /**
     * Handles the DSC instruction f=073 j=001
     */
    private class DSCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand = new long[2];
            operand[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            operand[1] = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            int count = (int) getImmediateOperand() & 0177;
            DoubleWord36 dw36 = new DoubleWord36(operand[0], operand[1]);
            DoubleWord36 result = dw36.rightShiftCircular(count);
            Word36[] components = result.getWords();

            setExecOrUserARegister((int) _currentInstruction.getA(), components[0].getW());
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, components[1].getW());
        }

        @Override public Instruction getInstruction() { return Instruction.DSC; }
    }

    /**
     * Handles the DSF instruction f=035
     * A(a)||36 sign bits gets algebraically shifted right one bit, then divided by 36-bit U.
     * The 36-bit result is stored in A(a+1), the remainder is lost.
     * I have no idea how one would use this.
     * A divide check is raised if |dividend| not < |divisor| or if divisor = +/- zero
     */
    private class DSFFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] dividend = new long[2];
            dividend[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            dividend[1] = Word36.isNegative(dividend[0]) ? Word36.BIT_MASK : 0;

            long[] divisor = new long[2];
            divisor[1] = getOperand(true, true, true, true);
            divisor[0] = Word36.isNegative(divisor[1]) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO;

            long quotient = 0;
            DoubleWord36 dwDividend = new DoubleWord36(dividend[0], dividend[1]).rightShiftAlgebraic(1);
            DoubleWord36 dwDivisor = new DoubleWord36(divisor[0], divisor[1]);
            if (dwDivisor.isZero() || (dividend[0] >= divisor[1])) {
                _designatorRegister.setDivideCheck(true);
                if (_designatorRegister.getArithmeticExceptionEnabled()) {
                    throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
                }
            } else {
                DoubleWord36.DivisionResult dr = dwDividend.divide(dwDivisor);
                quotient = dr._result.get().longValue() & Word36.BIT_MASK;
            }

            setExecOrUserARegister((int) _currentInstruction.getA() + 1, quotient);
        }

        @Override public Instruction getInstruction() { return Instruction.DSF; }
    }

    /**
     * Handles the DSL instruction f=073 j=003
     */
    private class DSLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand = new long[2];
            operand[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            operand[1] = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            int count = (int) getImmediateOperand() & 0177;
            DoubleWord36 dw36 = new DoubleWord36(operand[0], operand[1]);
            DoubleWord36 result = dw36.rightShiftLogical(count);
            Word36[] components = result.getWords();

            setExecOrUserARegister((int) _currentInstruction.getA(), components[0].getW());
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, components[1].getW());
        }

        @Override public Instruction getInstruction() { return Instruction.DSL; }
    }

    /**
     * Handles the DTE instruction f=071 j=017
     */
    private class DTEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Skip NI if U,U+1 == A(a),A(a+1) - for this test, -0 is not equal to +0
            long[] uOperand = new long[2];
            long[] aOperand = new long[2];
            aOperand[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            aOperand[1] = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();
            getConsecutiveOperands(true, uOperand, false);
            if ((uOperand[0] == aOperand[0]) && (uOperand[1] == aOperand[1])) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.DTE; }
    }

    /**
     * Handles the DTGM instruction extended mode f=033 j=014
     * Skip NI if |(U,U+1)| > A(a),A(a+1)
     */
    private class DTGMFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {

            long[] uValue = new long[2];
            getConsecutiveOperands(true, uValue, false);
            DoubleWord36 dwu = new DoubleWord36(uValue[0], uValue[1]);
            if (dwu.isNegative()) { dwu = dwu.negate(); }

            DoubleWord36 dwa = new DoubleWord36(getExecOrUserARegister((int) _currentInstruction.getA()).getW(),
                                                getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW());
            if (dwu.compareTo(dwa) > 0) {
                skipNextInstruction();
            }
        }

        @Override
        public Instruction getInstruction() { return Instruction.DTGM; }
    }

    /**
     * Handles the ENZ instruction f=005, a=014
     * Requires storage lock
     */
    private class ENZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            boolean skip = incrementOperand(true, true, 0, twosComplement);

            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }

            if (skip) { skipNextInstruction(); }
        }

        @Override public Instruction getInstruction() { return Instruction.ENZ; }
    }

    /**
     * Handles the ER instruction f=072 j=011 a=unused - basic mode only
     */
    private class ERFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getImmediateOperand();
            throw new SignalInterrupt(SignalInterrupt.SignalType.ExecutiveRequest, (int) operand);
        }

        @Override public Instruction getInstruction() { return Instruction.ER; }
    }

    /**
     * Handles the GOTO instruction f=07 j=017 a=00
     */
    private class GOTOFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(false, true, false, false);
            new BankManipulator().bankManipulation(Instruction.GOTO, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.GOTO; }
    }

    /**
     * Handles the HALT instruction (f=077 j=017 a=017)
     * Causes the processor to error halt and notify the SCF.
     */
    private class HALTFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_developmentMode && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            stop(InstructionProcessor.StopReason.Debug, getImmediateOperand());
        }

        @Override public Instruction getInstruction() { return Instruction.HALT; }
    }

    /**
     * Handles the HJ instruction basic mode f=074 j=05
     */
    private class HJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Always jump
            setProgramCounter(getJumpOperand(true), true);
        }

        @Override public Instruction getInstruction() { return Instruction.HJ; }
    }

    /**
     * Handles the HLTJ instruction basic mode f=074 j=015 a=05
     */
    private class HLTJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            //  Always jump, but halt thereafter
            setProgramCounter(getJumpOperand(true), true);
            stop(InstructionProcessor.StopReason.HaltJumpExecuted, 0);
        }

        @Override public Instruction getInstruction() { return Instruction.HLTJ; }
    }

    /**
     * Handles the IAR instruction (extended mode only f=073 j=017 a=06)
     * Causes the processor to error halt and notify the SCF.
     */
    private class IARFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            stop(InstructionProcessor.StopReason.InitiateAutoRecovery, getImmediateOperand());
        }

        @Override public Instruction getInstruction() { return Instruction.IAR; }
    }

    /**
     * Handles the INC instruction f=005, a=010
     * Requires storage lock
     */
    private class INCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            boolean skip = incrementOperand(true, true, 01, twosComplement);

            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }

            if (skip) { skipNextInstruction(); }
        }

        @Override public Instruction getInstruction() { return Instruction.INC; }
    }

    /**
     * Handles the INC2 instruction f=005, a=012
     * Requires storage lock
     */
    private class INC2FunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            boolean skip = incrementOperand(true, true, 02, twosComplement);

            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }

            if (skip) { skipNextInstruction(); }
        }

        @Override public Instruction getInstruction() { return Instruction.INC2; }
    }

    /**
     * Handles the IPC instruction f=073 j=017 a=010
     */
    private class IPCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  PP has to be 0
            int procPriv = _designatorRegister.getProcessorPrivilege();
            if (procPriv > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            //  Retrieve U to get the subfunction
            long operand = getOperand(false, false, false, false);
            int subFunc = (int) Word36.getS1(operand);

            //  We do recognize a few subfunctions...
            switch (subFunc) {
                case 0: //  clear reset designator
                    //???? don't really know what this is yet...
                    //      maybe resetting the IP puts it into reset mode (see IPL interrupts)
                    //      and we need, at some point, to clear reset mode programmatically?
                    break;

                case 1: //  enable conditionalJump-history-full interrupt
                    _jumpHistoryFullInterruptEnabled = true;
                    break;

                case 2: //  disable conditionalJump-history-full interrupt
                    _jumpHistoryFullInterruptEnabled = false;
                    break;

                case 3: //  synchronize page invalidates (we don't do this, so it's a NOP)
                    //????
                    break;

                case 4: //  clear broadcast interrupt eligibility
                    _broadcastInterruptEligibility = false;
                    break;

                case 5: //  set broadcast interrupt eligibility
                    _broadcastInterruptEligibility = true;
                    break;

                default:
                    throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.IPC; }
    }

    /**
     * Handles the J instruction - extended mode f=074 j=015 a=004, basic mode f=074 j=004 a=000
     */
    private class JFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            setProgramCounter(getJumpOperand(true), true);
        }

        @Override public Instruction getInstruction() { return Instruction.J; }
    }

    /**
     * Handles the JB instruction - extended f=074 j=011
     */
    private class JBFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if ((getExecOrUserARegister((int) _currentInstruction.getA()).getW() & 0x01) == 0x01) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JB; }
    }

    /**
     * Handles the JC instruction - extended f=074 j=014 a=04, basic f=074 j=016
     */
    private class JCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getCarry()) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JC; }
    }

    /**
     * Handles the JDF instruction f=074 j=014 a=03
     */
    private class JDFFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getDivideCheck()) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
                _designatorRegister.setDivideCheck(false);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JDF; }
    }

    /**
     * Handles the JFO instruction f=074 j=014 a=02
     */
    private class JFOFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getCharacteristicOverflow()) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }
            _designatorRegister.setCharacteristicOverflow(false);
        }

        @Override public Instruction getInstruction() { return Instruction.JFO; }
    }

    /**
     * Handles the JFU instruction f=074 j=014 a=01
     */
    private class JFUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getCharacteristicUnderflow()) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }
            _designatorRegister.setCharacteristicUnderflow(false);
        }

        @Override public Instruction getInstruction() { return Instruction.JFU; }
    }

    /**
     * Handles the JGD instruction f=070
     */
    private class JGDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  right-most 3 bits of j-field concatenated to the 4 bits of a-field is a GRS index.
            //  If the associated register is greater than zero, we effect a conditionalJump to U.
            //  In any case, the register value is decremented by 1
            int regIndex = (int) (((_currentInstruction.getJ() & 07) << 4) | _currentInstruction.getA());
            GeneralRegister reg = getGeneralRegister(regIndex);
            if (reg.isPositive() && !reg.isZero()) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }

            long result = Word36.addSimple(reg.getW(), 0_777777_777776L);
            setGeneralRegister(regIndex, result);
        }

        @Override public Instruction getInstruction() { return Instruction.JGD; }
    }

    /**
     * Handles the JK instruction basic mode f=074 j=04 a=01-017
     */
    private class JKFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Get the jump operand, but don't jump.
            getJumpOperand(false);
        }

        @Override public Instruction getInstruction() { return Instruction.JK; }
    }

    /**
     * Handles the JMGI instruction f=074 j=012
     */
    private class JMGIFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  If X(a).mod > 0, effect a conditionalJump to U
            //  In any case Increment X(a).mod by X(a).inc
            //  In Basic Mode if F0.h is true (U resolution x-reg incrementation) and F0.a == F0.x, we increment only once
            //  X(0) is used for X(a) if a == 0 (contrast to F0.x == 0 -> no indexing)
            //  In Extended Mode, X(a) incrementation is always 18 bits.
            int iaReg = (int) _currentInstruction.getA();
            IndexRegister xreg = getExecOrUserXRegister(iaReg);
            long modValue = xreg.getSignedXM();
            if (Word36.isPositive(modValue) && !Word36.isZero(modValue)) {
                int counter = getJumpOperand(true);
                setProgramCounter(counter, true);
            }

            setExecOrUserXRegister(iaReg, IndexRegister.incrementModifier18(xreg.getW()));
        }

        @Override public Instruction getInstruction() { return Instruction.JMGI; }
    }

    /**
     * Handles the JN instruction f=074 j=03
     */
    private class JNFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (getExecOrUserARegister((int) _currentInstruction.getA()).isNegative()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JN; }
    }

    /**
     * Handles the JNB instruction - extended f=074 j=010
     */
    private class JNBFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if ((getExecOrUserARegister((int) _currentInstruction.getA()).getW() & 0x01) == 0x0) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JNB; }
    }

    /**
     * Handles the JNC instruction - extended f=074 j=014 a=05, basic f=074 j=017
     */
    private class JNCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_designatorRegister.getCarry()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JNC; }
    }

    /**
     * Handles the JNDF instruction f=074 j=015 a=03
     */
    private class JNDFFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_designatorRegister.getDivideCheck()) {
                setProgramCounter(getJumpOperand(true), true);
            } else {
                _designatorRegister.setDivideCheck(false);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JNDF; }
    }

    /**
     * Handles the JNFO instruction f=074 j=015 a=02
     */
    private class JNFOFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_designatorRegister.getCharacteristicOverflow()) {
                setProgramCounter(getJumpOperand(true), true);
            }
            _designatorRegister.setCharacteristicOverflow(false);
        }

        @Override public Instruction getInstruction() { return Instruction.JNFO; }
    }

    /**
     * Handles the JNFU instruction f=074 j=015 a=01
     */
    private class JNFUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_designatorRegister.getCharacteristicUnderflow()) {
                setProgramCounter(getJumpOperand(true), true);
            }
            _designatorRegister.setCharacteristicUnderflow(false);
        }

        @Override public Instruction getInstruction() { return Instruction.JNFU; }
    }

    /**
     * Handles the JNO instruction f=074 j=015 a=00
     */
    private class JNOFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!_designatorRegister.getOverflow()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JNO; }
    }

    /**
     * Handles the JNS instruction f=072 j=03
     */
    private class JNSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int iaReg = (int) _currentInstruction.getA();
            GeneralRegister reg = getExecOrUserARegister(iaReg);
            long operand = reg.getW();
            if (Word36.isNegative(operand)) {
                setProgramCounter(getJumpOperand(true), true);
            }

            setExecOrUserARegister(iaReg, Word36.leftShiftCircular(operand, 1));
        }

        @Override public Instruction getInstruction() { return Instruction.JNS; }
    }

    /**
     * Handles the JNZ instruction f=074 j=01
     */
    private class JNZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (!getExecOrUserARegister((int) _currentInstruction.getA()).isZero()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JNZ; }
    }

    /**
     * Handles the JO instruction f=074 j=014 a=00
     */
    private class JOFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getOverflow()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JO; }
    }

    /**
     * Handles the JP instruction f=074 j=02
     */
    private class JPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (getExecOrUserARegister((int) _currentInstruction.getA()).isPositive()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JP; }
    }

    /**
     * Handles the JPS instruction f=072 j=02
     */
    private class JPSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int iaReg = (int) _currentInstruction.getA();
            GeneralRegister reg = getExecOrUserARegister(iaReg);
            long operand = reg.getW();
            if (Word36.isPositive(operand)) {
                setProgramCounter(getJumpOperand(true), true);
            }

            setExecOrUserARegister(iaReg, Word36.leftShiftCircular(operand, 1));
        }

        @Override public Instruction getInstruction() { return Instruction.JPS; }
    }

    /**
     * Handles the JZ instruction f=074 j=00
     */
    private class JZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (getExecOrUserARegister((int) _currentInstruction.getA()).isZero()) {
                setProgramCounter(getJumpOperand(true), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.JZ; }
    }

    /**
     * Handles the KCHG instruction f=037 j=04 a=04
     */
    private class KCHGFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 1) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long operand = getOperand(false, true, false, false);
            setGeneralRegister(GeneralRegisterSet.X0, _indicatorKeyRegister.getAccessKey());
            _indicatorKeyRegister.setAccessKey((int) (operand >> 18));
            _designatorRegister.setQuantumTimerEnabled((operand & 040) != 0);
            _designatorRegister.setDeferrableInterruptEnabled((operand & 020) != 0);
            _designatorRegister.setExecRegisterSetSelected((operand & 01) != 0);
        }

        @Override public Instruction getInstruction() { return Instruction.KCHG; }
    }

    /**
     * Handles the LA instruction f=010
     */
    private class LAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            setExecOrUserARegister((int) _currentInstruction.getA(), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LA; }
    }

    /**
     * Handles the LAE instruction f=073 j=015 a=012
     */
    private class LAEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long[] operands = new long[15];
            getConsecutiveOperands(false, operands, false);

            BankManipulator bm = new BankManipulator();
            for (int opx = 0, brx = 1; opx < 15; ++opx, ++brx) {
                bm.bankManipulation(Instruction.LAE, brx, operands[opx]);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.LAE; }
    }

    /**
     * Handles the LAQW instruction f=07 j=04
     * Operates much like LA,[q1-q4], except that the quarter-word designation is taken from bits 4-5 of X(x).
     * X-incrementation is not supported - results are undefined.
     */
    private class LAQWFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int designator = (int) getExecOrUserXRegister((int) _currentInstruction.getX()).getS1() & 03;
            int jField = QW_J_FIELDS[designator];
            long operand = getPartialOperand(jField, true);
            setGeneralRegister(getExecOrUserARegisterIndex((int) _currentInstruction.getA()), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LAQW; }
    }

    /**
     * Handles the LBE instruction f=075 j=03
     */
    private class LBEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long operand = getOperand(true, true, false, false);
            new BankManipulator().bankManipulation(Instruction.LBE, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LBE; }
    }

    /**
     * Handles the LBED instruction f=075 j=05
     */
    private class LBEDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            int brIndex = (int) _currentInstruction.getA() + 16;
            long[] data = new long[4];
            getConsecutiveOperands(false, data, false);
            _baseRegisters[brIndex] = new BaseRegister(data);

            //  Clear any active base table entries which have a level value corresponding
            //  to the exec register being loaded (because that exec register points to the BDTable for that level).
            int level = (int) _currentInstruction.getA();
            for (int abtx = 1; abtx < 16; ++abtx) {
                if (_activeBaseTableEntries[abtx].getLevel() == level) {
                    _activeBaseTableEntries[abtx] = new ActiveBaseTableEntry(0);
                }
            }
        }

        @Override public Instruction getInstruction() { return Instruction.LBED; }
    }

    /**
     * Handles the LBJ instruction f=07 j=017
     */
    private class LBJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getJumpOperand(false);
            new BankManipulator().bankManipulation(Instruction.LBJ, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LBJ; }
    }

    /**
     * Handles the LBN instruction f=075 j=014
     * Operand is a virtual address.
     * If the virtual address is between 0,0 and 0,31 (L,BDI) then that becomes the true bank name.
     * Otherwise L,BDI indicates a bank descriptor from which we get the true bank name,
     * subject to indirect and gate banks.
     */
    private class LBNFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            boolean skip = false;
            long bankName;
            long operand = getOperand(false, true, false, false);
            VirtualAddress va = new VirtualAddress(operand);
            int origLevel = va.getLevel();
            int origBDI = va.getBankDescriptorIndex();

            if (origLevel == 0 && (va.getBankDescriptorIndex() < 32)) {
                bankName = va.getH1();
                skip = true;
            } else {
                BankDescriptor bd = getBankDescriptor(origLevel, origBDI, false);
                if (bd.getBankType() == BankType.QueueRepository) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                           origLevel,
                                                           origBDI);
                }

                bankName = origLevel << 15;
                bankName |= ((origBDI - bd.getDisplacement()) & 077777);
                if (bd.getBankType() != BankType.BasicMode) {
                    skip = true;
                }
            }

            int ixReg = (int) _currentInstruction.getA();
            setExecOrUserXRegister(ixReg, bankName << 18);

            if (skip) { skipNextInstruction(); }
        }

        @Override public Instruction getInstruction() { return Instruction.LBN; }
    }

    /**
     * Handles the LBU instruction f=075 j=00
     */
    private class LBUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long operand = getOperand(false, true, false, false);
            new BankManipulator().bankManipulation(Instruction.LBU, operand);
        }

        @Override
        public Instruction getInstruction() { return Instruction.LBU; }
    }

    /**
     * Handles the LBUD instruction f=075 j=07
     */
    private class LBUDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            int brIndex = (int) _currentInstruction.getA();
            if ((brIndex < 1) || (brIndex > 11)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidBaseRegister);
            }

            if (!_designatorRegister.getBasicModeEnabled() && (brIndex == (int) _currentInstruction.getB())) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidBaseRegister);
            }

            long[] data = new long[4];
            getConsecutiveOperands(false, data, false);
            _baseRegisters[brIndex] = new BaseRegister(data);
            _activeBaseTableEntries[brIndex] = new ActiveBaseTableEntry(0);
        }

        @Override
        public Instruction getInstruction() { return Instruction.LBUD; }
    }

    /**
     * Handles the LD instruction f=073 j=015 a=014
     */
    private class LDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long operand = getOperand(false, true, false, false);
            _designatorRegister = new DesignatorRegister(operand);
            if (_designatorRegister.getBasicModeEnabled()) {
                findBasicModeBank(_programAddressRegister.getProgramCounter(), true);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.LD; }
    }

    /**
     * Handles the LDJ instruction f=07 j=012
     */
    private class LDJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getJumpOperand(false);
            new BankManipulator().bankManipulation(Instruction.LDJ, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LDJ; }
    }

    /**
     * Handles the LDSC instruction f=073 j=011
     */
    private class LDSCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand = new long[2];
            operand[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            operand[1] = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            int count = (int) getImmediateOperand() & 0177;
            DoubleWord36 dw36 = new DoubleWord36(operand[0], operand[1]);
            DoubleWord36 result = dw36.leftShiftCircular(count);
            Word36[] components = result.getWords();

            setExecOrUserARegister((int) _currentInstruction.getA(), components[0].getW());
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, components[1].getW());
        }

        @Override public Instruction getInstruction() { return Instruction.LDSC; }
    }

    /**
     * Handles the LDSL instruction f=073 j=013
     */
    private class LDSLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long[] operand = new long[2];
            operand[0] = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            operand[1] = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            int count = (int) getImmediateOperand() & 0177;
            DoubleWord36 dw36 = new DoubleWord36(operand[0], operand[1]);
            DoubleWord36 result = dw36.leftShiftLogical(count);
            Word36[] components = result.getWords();

            setExecOrUserARegister((int) _currentInstruction.getA(), components[0].getW());
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, components[1].getW());
        }

        @Override public Instruction getInstruction() { return Instruction.LDSL; }
    }

    /**
     * Handles the LIJ instruction f=07 j=013
     */
    private class LIJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getJumpOperand(false);
            new BankManipulator().bankManipulation(Instruction.LIJ, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LIJ; }
    }

    /**
     * Handles the LMA instruction f=012
     */
    private class LMAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            if ((operand & 0_400000_000000L) != 0) {
                operand = ~operand;
            }
            setExecOrUserARegister((int) _currentInstruction.getA(), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LMA; }
    }

    /**
     * Handles the LMC instruction f=037 j=007
     */
    private class LMCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);  //  Get U, and throw it
            int regx = (int) _currentInstruction.getA();
            long micros = (getExecOrUserARegister(regx).getH2() << 36) | getExecOrUserARegister(regx + 1).getW();
            _systemProcessor.dayclockSetComparatorMicros(micros);
        }

        @Override public Instruction getInstruction() { return Instruction.LMC; }
    }

    /**
     * Handles the LMJ instruction basic mode f=074 j=013
     */
    private class LMJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Increment PAR.PC and store it in X(a)Modifier, then set PAR.PC to U
            long op = getJumpOperand(true);
            int ixReg = (int) _currentInstruction.getA();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            setExecOrUserXRegister(ixReg, IndexRegister.setH2(xReg.getW(),
                                                              _programAddressRegister.getProgramCounter() + 1));
            setProgramCounter(op, true);
        }

        @Override public Instruction getInstruction() { return Instruction.LMJ; }
    }

    /**
     * Handles the LNA instruction f=011
     */
    private class LNAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  We can just *not* the operand, because the following set will result in
            //  truncating the errant bits outside of the 36-bit word.
            long operand = ~(getOperand(true, true, true, true));
            setExecOrUserARegister((int) _currentInstruction.getA(), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LNA; }
    }

    /**
     * Handles the LNMA instruction f=013
     */
    private class LNMAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            if ((operand & 0_400000_000000L) == 0) {
                operand = ~operand;
            }
            setExecOrUserARegister((int) _currentInstruction.getA(), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LNMA; }
    }

    /**
     * Handles the LOCL instruction f=07 j=016 a=00
     */
    private class LOCLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            rcsPush(0);
            IndexRegister xReg = getExecOrUserXRegister(0);
            long newXValue = IndexRegister.setH1(xReg.getW(), _designatorRegister.getBasicModeEnabled() ? 0_400000_000000L : 0);
            newXValue = IndexRegister.setH2(newXValue, _indicatorKeyRegister.getAccessInfo().get());
            setExecOrUserXRegister(0, newXValue);

            setProgramCounter(getJumpOperand(true), true);
        }

        @Override public Instruction getInstruction() { return Instruction.LOCL; }
    }

    /**
     * Handles the LPD instruction f=07 j=014 a=not-used
     */
    private class LPDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getJumpOperand(false);
            _designatorRegister = new DesignatorRegister(operand & 0157);
        }

        @Override public Instruction getInstruction() { return Instruction.LPD; }
    }

    /**
     * Handles the LR instruction f=023
     */
    private class LRFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            setExecOrUserRRegister((int) _currentInstruction.getA(), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LR; }
    }

    /**
     * Handles the LRD instruction f=037 j=000
     */
    private class LRDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);
            int regx = (int) _currentInstruction.getA();
            long micros = (getExecOrUserARegister(regx).getH2() << 36) | getExecOrUserARegister(regx + 1).getW();
            _systemProcessor.dayclockSetMicros(micros);
        }

        @Override public Instruction getInstruction() { return Instruction.LRD; }
    }

    /**
     * Handles the LRS instruction f=072 j=017
     * This instruction loads one or two consecutive sets of values from a single buffer in memory to GRS locations.
     * The two sets are contiguous in memory; they do not have to be contiguous in the GRS.
     *
     * A(a) contains a descriptor formatted as such:
     *  Bit2-8:     count2
     *  Bit11-17:   address2
     *  Bit20-26:   count1
     *  Bit29-35:   address1
     *
     * Effective u refers to the buffer in memory.  GRS access does not apply.
     * Standard address resolution for consecutive memory addresses does apply.
     * The size of the buffer must be at least count1 + count2 words in length.
     *
     * The first set of transfers begins with the source set to effective U + 0, and the destination is the GRS register
     * at address 1.  {count1} words are transferred from consecutive locations in memory to consecutive GRS registers.
     * If at any point the GRS index reaches 0200, it is reset to 0, and the process continues.
     * The second set of transfers then begins with the source set to effective U + {count1} (or an appropriate value if
     * wraparound occurred at index 0200) and with the GRS register at address 2.  {count2} words are transferred just
     * as they were in the first set.
     *
     * If count1 or count2 are zero, the corresponding register transfer is effectively a NOP.
     *
     * A(a) may be included in the register transfer, so the content thereof will be pulled out and stored separately
     * in order to not be overwritten during the process.
     */
    private class LRSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Grab descriptor first
            int descriptorRegisterIndex = getExecOrUserARegisterIndex((int) _currentInstruction.getA());
            long descriptor = getGeneralRegister(descriptorRegisterIndex).getW();

            int address1 = (int)descriptor & 0177;
            int count1 = (int)(descriptor >> 9) & 0177;
            int address2 = (int)(descriptor >> 18) & 0177;
            int count2 = (int)(descriptor >> 27) & 0177;

            //  Go get all the operands we need for both areas
            long[] operands = new long[count1 + count2];
            getConsecutiveOperands(false, operands, false);

            //  If we got this far, we can start populating the GRS
            int ox = 0;
            int grsx = address1;
            for (int rx = 0; rx < count1; ++rx) {
                setGeneralRegister(grsx++, operands[ox++]);
                if (grsx == 0200) { grsx = 0; }
            }

            grsx = address2;
            for (int rx = 0; rx < count2; ++rx) {
                setGeneralRegister(grsx++, operands[ox++]);
                if (grsx == 0200) { grsx = 0; }
            }
        }

        @Override public Instruction getInstruction() { return Instruction.LRS; }
    }

    /**
     * Handles the LSBL instruction f=061, extended mode only
     */
    private class LSBLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            int ixReg = (int) _currentInstruction.getA();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            setExecOrUserXRegister(ixReg, Word36.setS2(xReg.getW(), operand));
        }

        @Override public Instruction getInstruction() { return Instruction.LSBL; }
    }

    /*
     * Handles the LSBO instruction f=060, extended mode only
     */
    private class LSBOFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            IndexRegister xReg = getExecOrUserXRegister((int) _currentInstruction.getA());
            setExecOrUserXRegister((int) _currentInstruction.getA(), Word36.setS1(xReg.getW(), operand));
        }

        @Override public Instruction getInstruction() { return Instruction.LSBO; }
    }

    /**
     * Handles the LSC instruction f=073 j=006 (Load shift and count)
     * Left shift operand circularly unti bit 0 != bit1, then store it in Aa.
     * Store the shift count in A(a+1).
     */
    private class LSCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, false, false);
            int count = 0;
            if (Word36.isZero(operand)) {
                count = 35;
            } else {
                long bits = operand & 0_600000_000000L;
                while ((bits == 0) || (bits == 0_600000_000000L)) {
                    operand = Word36.leftShiftCircular(operand, 1);
                    ++count;
                    bits = operand & 0_600000_000000L;
                }
            }

            setExecOrUserARegister((int) _currentInstruction.getA(), operand);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, count);
        }

        @Override public Instruction getInstruction() { return Instruction.LSC; }
    }

    /**
     * Handles the LSSC instruction f=073 j=010
     */
    private class LSSCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            int count = (int) getImmediateOperand() & 0177;
            Word36 w36 = new Word36(operand);
            Word36 result = w36.leftShiftCircular(count);
            setExecOrUserARegister((int) _currentInstruction.getA(), result.getW());
        }

        @Override public Instruction getInstruction() { return Instruction.LSSC; }
    }

    /**
     * Handles the LSSL instruction f=073 j=012
     */
    private class LSSLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            int count = (int) getImmediateOperand() & 0177;
            Word36 w36 = new Word36(operand);
            Word36 result = w36.leftShiftLogical(count);
            setExecOrUserARegister((int) _currentInstruction.getA(), result.getW());
        }

        @Override public Instruction getInstruction() { return Instruction.LSSL; }
    }

    /**
     * Handles the LUD instruction f=073 j=017 a=04
     */
    private class LUDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(false, true, false, false);
            _designatorRegister = new DesignatorRegister(operand & 0670157);
        }

        @Override public Instruction getInstruction() { return Instruction.LUD; }
    }

    /**
     * Handles the LX instruction f=027
     */
    private class LXFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            setExecOrUserXRegister((int) _currentInstruction.getA(), operand);
        }

        @Override public Instruction getInstruction() { return Instruction.LX; }
    }

    /**
     * Handles the LXI instruction f=046
     */
    private class LXIFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            int regIndex = (int) _currentInstruction.getA();
            IndexRegister xReg = getExecOrUserXRegister(regIndex);
            setExecOrUserXRegister(regIndex, IndexRegister.setXI(xReg.getW(), operand));
        }

        @Override public Instruction getInstruction() { return Instruction.LXI; }
    }

    /**
     * Handles the LXLM instruction f=075 j=013 EM any PP, BM PP = 0
     */
    private class LXLMFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if ((_designatorRegister.getProcessorPrivilege() > 0) && (_designatorRegister.getBasicModeEnabled())) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long operand = getOperand(true, true, false, false);
            int ixReg = (int) _currentInstruction.getA();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            setExecOrUserXRegister(ixReg, IndexRegister.setXM24(xReg.getW(), operand));
        }

        @Override public Instruction getInstruction() { return Instruction.LXLM; }
    }

    /**
     * Handles the LXM instruction f=026
     */
    private class LXMFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            int ixReg = (int) _currentInstruction.getA();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            setExecOrUserXRegister(ixReg, IndexRegister.setXM(xReg.getW(), operand));
        }

        @Override public Instruction getInstruction() { return Instruction.LXM; }
    }

    /**
     * Handles the LXSI instruction f=051 (extended mode only)
     */
    private class LXSIFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getOperand(true, true, true, true);
            int ixReg = (int) _currentInstruction.getA();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            setExecOrUserXRegister(ixReg, IndexRegister.setXI12(xReg.getW(), operand));
        }

        @Override public Instruction getInstruction() { return Instruction.LXSI; }
    }

    /**
     * Handles the MATG instruction extended mode f=071 j=07
     * Skip NI if ((U) AND R2) > (A(a) AND R2) - unsigned
     */
    private class MATGFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, false, false);
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long opMask = getExecOrUserRRegister(2).getW();

            if ((uValue & opMask) > (aValue & opMask)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MATG; }
    }

    /**
     * Handles the MATL instruction extended mode f=071 j=06
     * Skip NI if ((U) AND R2) <= (A(a) AND R2) - unsigned
     */
    private class MATLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, false, false);
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long opMask = getExecOrUserRRegister(2).getW();

            if ((uValue & opMask) <= (aValue & opMask)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MATL; }
    }

    /**
     * Handles the MF instruction f=032
     * This instruction is used to multiply fixed-point fractions when
     * the binary point is between bits 0 and 1; the product will then have an identically
     * positioned binary point.
     * The contents of U are fetched under j-field control and multiplied algebraically by the
     * contents of Aa. The resulting 72-bit product is then shifted left circularly by 1 bit
     * position. Subsequently, the shifted product is stored into Aa (36 most significant bits)
     * and Aa+1 (36 least significant bits).
     */
    private class MFFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            BigInteger operand1 = BigInteger.valueOf(getExecOrUserARegister((int) _currentInstruction.getA()).getW());
            BigInteger operand2 = BigInteger.valueOf(getOperand(true,
                                                                   true,
                                                                   true,
                                                                   true));
            DoubleWord36.StaticMultiplicationResult smr = DoubleWord36.multiply(operand1, operand2);
            DoubleWord36 result = new DoubleWord36(DoubleWord36.leftShiftCircular(smr._value, 1));
            Word36[] components = result.getWords();

            setExecOrUserARegister((int) _currentInstruction.getA(), components[0].getW());
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, components[1].getW());
        }

        @Override public Instruction getInstruction() { return Instruction.MF; }
    }

    /**
     * Handles the MI instruction f=030
     */
    private class MIFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            BigInteger factor1 = BigInteger.valueOf(getExecOrUserARegister((int) _currentInstruction.getA()).getW());
            BigInteger sgnExFactor1 = DoubleWord36.extendSign(factor1, 36);
            BigInteger factor2 = BigInteger.valueOf(getOperand(true, true, true, true));
            BigInteger sgnExFactor2 = DoubleWord36.extendSign(factor2, 36);
            DoubleWord36.StaticMultiplicationResult smr = DoubleWord36.multiply(sgnExFactor1, sgnExFactor2);
            Word36[] resultWords = DoubleWord36.getWords(smr._value);

            setExecOrUserARegister((int) _currentInstruction.getA(), resultWords[0].getW());
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, resultWords[1].getW());
        }

        @Override public Instruction getInstruction() { return Instruction.MI; }
    }

    /**
     * Handles the MLU instruction f=043
     */
    private class MLUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long opAa = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long opR2 = getExecOrUserRRegister(2).getW();
            long compR2 = Word36.negate(opR2);
            long opU = getOperand(true, true, true, true);
            long result = (opU & opR2) | (opAa & compR2);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, result);
        }

        @Override public Instruction getInstruction() { return Instruction.MLU; }
    }

    /**
     * Handles the MSI instruction f=031
     */
    private class MSIFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            DoubleWord36 factor1 = new DoubleWord36(0, getExecOrUserARegister((int) _currentInstruction.getA()).getW());
            DoubleWord36 factor2 = new DoubleWord36(0, getOperand(true, true, true, true));

            DoubleWord36.MultiplicationResult mr = factor1.multiply(factor2);
            Word36[] resultWords = mr._value.getWords();
            setExecOrUserARegister((int) _currentInstruction.getA(), resultWords[1].getW());

            //  check for overflow conditions.
            //  result[0] must be positive or negative zero, and the signs of result[0] and result[1] must match.
            if (!resultWords[0].isZero() || (resultWords[1].isPositive() != resultWords[0].isPositive())) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.MultiplySingleIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MSI; }
    }

    /**
     * Handles the MTE instruction extended mode f=071 j=00
     * Skip NI if (U) AND R2 == A(a) AND R2
     */
    private class MTEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long op2 = getOperand(true, true, false, false);
            long opMask = getExecOrUserRRegister(2).getW();

            if ((op1 & opMask) == (op2 & opMask)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MTE; }
    }

    /**
     * Handles the MTG instruction extended mode f=071 j=03
     * Skip NI if ((U) AND R2) > (A(a) AND R2)
     */
    private class MTGFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, false, false);
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long opMask = getExecOrUserRRegister(2).getW();

            if (Word36.compare(uValue & opMask, aValue & opMask) > 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MTG; }
    }

    /**
     * Handles the MTLE / MTNG instruction extended mode f=071 j=02
     * Skip NI if ((U) AND R2) <= (A(a) AND R2)
     */
    private class MTLEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long uValue = getOperand(true, true, false, false);
            long opMask = getExecOrUserRRegister(2).getW();

            if (Word36.compare(uValue & opMask, aValue & opMask) <= 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MTLE; }
    }

    /**
     * Handles the MTNE instruction extended mode f=071 j=01
     * Skip NI if (U) AND R2 == A(a) AND R2
     */
    private class MTNEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long op2 = getOperand(true, true, false, false);
            long opMask = getExecOrUserRRegister(2).getW();

            if ((op1 & opMask) != (op2 & opMask)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MTNE; }
    }

    /**
     * Handles the MTNW instruction extended mode f=071 j=05
     * Skip NI if (A(a) AND R2) >= ((U) AND R2)  or  ((U) AND R2) > (A(a+1) AND R2)
     */
    private class MTNWFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, false, false);
            long aValueLow = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long aValueHigh = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();
            long opMask = getExecOrUserRRegister(2).getW();

            long maskedU = uValue & opMask;
            long maskedALow = aValueLow & opMask;
            long maskedAHigh = aValueHigh & opMask;

            if ((Word36.compare(maskedALow, maskedU) >= 0)
                || (Word36.compare(maskedU, maskedAHigh) > 0)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MTNW; }
    }

    /**
     * Handles the MTW instruction extended mode f=071 j=04
     * Skip NI if (A(a) AND R2) < ((U) AND R2) <= (A(a+1) AND R2)
     */
    private class MTWFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, false, false);
            long aValueLow = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long aValueHigh = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();
            long opMask = getExecOrUserRRegister(2).getW();

            long maskedU = uValue & opMask;
            long maskedALow = aValueLow & opMask;
            long maskedAHigh = aValueHigh & opMask;

            if ((Word36.compare(maskedALow, maskedU) < 0) && (Word36.compare(maskedU, maskedAHigh) <= 0)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.MTW; }
    }

    /**
     * Handles the NOP instruction - extended mode f=073 j=014 a=00, basic mode f=074 j=06
     */
    private class NOPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            getJumpOperand(false);
        }

        @Override public Instruction getInstruction() { return Instruction.NOP; }
    }

    /**
     * Handles the OR instruction f=040
     */
    private class ORFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, operand1 | operand2);
        }

        @Override public Instruction getInstruction() { return Instruction.OR; }
    }

    /**
     * Handles the PAIJ instruction f=074 j=014 a=07
     * requires PP = 0
     */
    private class PAIJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            _designatorRegister.setDeferrableInterruptEnabled(false);
            setProgramCounter(getJumpOperand(true), true);
        }

        @Override public Instruction getInstruction() { return Instruction.PAIJ; }
    }

    /**
     * Handles the RDC instruction f=075 j=012
     */
    private class RDCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);

            long micros = _systemProcessor.dayclockGetMicros();
            int regx = (int) _currentInstruction.getA();
            setExecOrUserARegister(regx, micros >> 36);
            setExecOrUserARegister(regx + 1, micros);
        }

        @Override public Instruction getInstruction() { return Instruction.RDC; }
    }

    /**
     * Handles the RMD instruction f=075 j=017
     * Retrieves the system time as microseconds since epoch, shifted left by 5 bits and offset be a uniqueness value.
     * microseconds-since-epoch is adjusted by the system-wide dayclock offset before being shifted.
     */
    private static long _RMDLastReportedMicros = 0;
    private static int _RMDUniqueness = 0;

    private class RMDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 2) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);

            long result;
            long currentMicros = _systemProcessor.dayclockGetMicros();
            synchronized (RMDFunctionHandler.class) {
                if (currentMicros != _RMDLastReportedMicros) {
                    _RMDLastReportedMicros = currentMicros;
                    _RMDUniqueness = 0;
                    result = _RMDLastReportedMicros << 5;
                } else {
                    ++_RMDUniqueness;
                    result = (_RMDLastReportedMicros << 5) | _RMDUniqueness;
                }
            }

            int regx = (int) _currentInstruction.getA();
            setExecOrUserARegister(regx, result >> 36);
            setExecOrUserARegister(regx + 1, result);
        }

        @Override public Instruction getInstruction() { return Instruction.RMD; }
    }

    /**
     * Handles the RTN instruction f=073 j=017 a=03
     */
    private class RTNFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getJumpOperand(false);
            new BankManipulator().bankManipulation(Instruction.RTN, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.RTN; }
    }

    /**
     * Handles the SA instruction f=001
     */
    private class SAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long value = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            storeOperand(true, true, true, true, value);
        }

        @Override public Instruction getInstruction() { return Instruction.SA; }
    }

    /**
     * Handles the SAQW instruction f=07 j=05
     * Operates much like SA,[q1-q4], except that the quarter-word designation is taken from bits 4-5 of X(x).
     * X-incrementation is not supported - results are undefined.
     */
    private class SAQWFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int designator = (int) getExecOrUserXRegister((int) _currentInstruction.getX()).getS1() & 03;
            int jField = QW_J_FIELDS[designator];
            long value = getGeneralRegister(getExecOrUserARegisterIndex((int) _currentInstruction.getA())).getW();
            storePartialOperand(value, jField, true);
        }

        @Override public Instruction getInstruction() { return Instruction.SAQW; }
    }

    /**
     * Handles the SAS instruction f=005 a=006
     */
    private class SASFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, 0_040040_040040L);
        }

        @Override public Instruction getInstruction() { return Instruction.SAS; }
    }

    /**
     * Handles the SAZ instruction f=005 a=007
     */
    private class SAZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, 0_060060_060060L);
        }

        @Override public Instruction getInstruction() { return Instruction.SAZ; }
    }

    /**
     * Handles the SBED instruction f=075 j=04
     */
    private class SBEDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            BaseRegister bReg = _baseRegisters[(int) _currentInstruction.getA() + 16];
            storeConsecutiveOperands(false, bReg.getBaseRegisterWords());
        }

        @Override public Instruction getInstruction() { return Instruction.SBED; }
    }

    /**
     * Handles the SBU instruction f=075 j=02
     */
    private class SBUFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            //  For f.a == 0, take L,BDI from PAR and offset is zero.
            //  For others, take L,BDI,offset from active base table.
            long operand;
            int brIndex = (int) _currentInstruction.getA();
            if (brIndex == 0) {
                operand = _programAddressRegister.get() & 0_777777_000000L;
            } else {
                operand = _activeBaseTableEntries[brIndex]._value;
            }

            storeOperand(false, true, false, false, operand);
        }

        @Override public Instruction getInstruction() { return Instruction.SBU; }
    }

    /**
     * Handles the SBUD instruction f=075 j=06
     */
    private class SBUDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            BaseRegister bReg = _baseRegisters[(int) _currentInstruction.getA()];
            storeConsecutiveOperands(false, bReg.getBaseRegisterWords());
        }

        @Override public Instruction getInstruction() { return Instruction.SBUD; }
    }

    /**
     * Handles the SD instruction f=073 j=015 a=015
     */
    private class SDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 1) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            storeOperand(true, true, false, false, _designatorRegister.getW());
        }

        @Override public Instruction getInstruction() { return Instruction.SD; }
    }

    /**
     * Handles the SDMF instruction f=037 j=004 a=002
     */
    private class SDMFFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Extended mode only, PP==0 - SDMF is a NOP
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);
        }

        @Override public Instruction getInstruction() { return Instruction.SDMF; }
    }

    /**
     * Handles the SDMN instruction f=037 j=004 a=001
     */
    private class SDMNFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Extended mode only, PP==0 - SDMN is a NOP
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);
        }

        @Override public Instruction getInstruction() { return Instruction.SDMN; }
    }

    /**
     * Handles the SDMS instruction f=037 j=004 a=003
     */
    private class SDMSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Extended mode only, PP==0 - SDMS is a NOP
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);
        }

        @Override public Instruction getInstruction() { return Instruction.SDMS; }
    }

    /**
     * Handles the SELL instruction (f=073 j=014 a=03) extended mode only
     */
    private class SELLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            boolean longModifier = _designatorRegister.getExecutive24BitIndexingEnabled();
            int ixReg = (int) _currentInstruction.getX();
            IndexRegister xReg = getExecOrUserXRegister(ixReg);
            BaseRegister bReg = _baseRegisters[(int) _currentInstruction.getB()];
            long oldModifier = longModifier ? xReg.getXM24() : xReg.getXM();

            if (bReg._voidFlag
                || (oldModifier < bReg._lowerLimitNormalized)
                || (oldModifier > bReg._upperLimitNormalized)) {
                throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                    (int) _currentInstruction.getB(),
                                                                    (int) oldModifier);
            }

            long addend = _currentInstruction.getD() + (longModifier ? xReg.getXI12() : xReg.getXI());
            long newModifier = oldModifier + addend;
            if (longModifier) {
                setExecOrUserXRegister(ixReg, IndexRegister.setXM24(xReg.getW(), newModifier));
            } else {
                setExecOrUserXRegister(ixReg, IndexRegister.setXM(xReg.getW(), newModifier));
            }
        }

        @Override public Instruction getInstruction() { return Instruction.SELL; }
    }

    /**
     * Handles the SFS instruction f=005 a=004
     */
    private class SFSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, 0_050505_050505L);
        }

        @Override public Instruction getInstruction() { return Instruction.SFS; }
    }

    /**
     * Handles the SFZ instruction f=005 a=005
     */
    private class SFZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, 0_606060_606060L);
        }

        @Override public Instruction getInstruction() { return Instruction.SFZ; }
    }

    /**
     * Handles the SGNL instruction f=073 j=015 a=017
     */
    private class SGNLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getImmediateOperand();
            throw new SignalInterrupt(SignalInterrupt.SignalType.Signal, (int) operand);
        }

        @Override public Instruction getInstruction() { return Instruction.SGNL; }
    }

    /**
     * Handles the SKQT instruction f=073 j=015 a=013
     */
    private class SKQTFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 2) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long[] ops = { _indicatorKeyRegister.getAccessInfo().get(), _quantumTimer };
            storeConsecutiveOperands(true, ops);
        }

        @Override public Instruction getInstruction() { return Instruction.SKQT; }
    }

    /**
     * Handles the SLJ instruction basic mode f=072 j=001
     * Per architecture, U < 0200 is GRS, U+1 is always storage
     * In our implementation, U and U+1 are always storage.
     */
    private class SLJFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Increment PAR.PC, and store it in U, then update PAR.PC to reference U+1
            long returnPC = _programAddressRegister.getProgramCounter() + 1;
            storePartialOperand(returnPC, InstructionWord.H2, true);
            long newPC = getJumpOperand(true);
            setProgramCounter(newPC + 1, true);
        }

        @Override public Instruction getInstruction() { return Instruction.SLJ; }
    }

    /**
     * Handles the SMA instruction f=003
     */
    private class SMAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            if (Word36.isNegative(op)) {
                op = Word36.negate(op);
            }
            storeOperand(true, true, true, true, op);
        }

        @Override public Instruction getInstruction() { return Instruction.SMA; }
    }

    /**
     * Handles the SMD instruction f=037 j=004 a=000
     */
    private class SMDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Extended mode only, PP==0 - SMD is a NOP
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            getJumpOperand(false);
        }

        @Override public Instruction getInstruction() { return Instruction.SMD; }
    }

    /**
     * Handles the SN1 instruction f=005 a=003
     */
    private class SN1FunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, NEGATIVE_ONE_36);
        }

        @Override public Instruction getInstruction() { return Instruction.SN1; }
    }

    /**
     * Handles the SNA instruction f=002
     */
    private class SNAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            storeOperand(true, true, true, true, Word36.negate(op));
        }

        @Override public Instruction getInstruction() { return Instruction.SNA; }
    }

    /**
     * Handles the SNZ instruction f=005 a=001
     */
    private class SNZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, Word36.NEGATIVE_ZERO);
        }

        @Override public Instruction getInstruction() { return Instruction.SNZ; }
    }

    /**
     * Handles the SP1 instruction f=005 a=002
     */
    private class SP1FunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, 1L);
        }

        @Override public Instruction getInstruction() { return Instruction.SP1; }
    }

    /**
     * Handles the SPD instruction f=07 j=015 a=not-used
     */
    private class SPDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 1) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            storeOperand(true, true, false, false, _designatorRegister.getW() & 0577);
        }

        @Override public Instruction getInstruction() { return Instruction.SPD; }
    }

    /**
     * Handles the SPID instruction f=073 j=015 a=005
     */
    private class SPIDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int procPriv = _designatorRegister.getProcessorPrivilege();
            if (procPriv > 2) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long[] operands = new long[2];
            //  Operand 0:
            //      MSBit is 1
            //      T1/T2 contains feature bits (which we don't do)
            //      T3 contains UPI number (but only for pp < 2)
            operands[0] = (1L << 35) | ((procPriv < 2) ? _upiIndex : 0L);

            //  Operand 1:
            //      MSBit is 0
            //      Q1 is Series (for M series, this is zero.  That's us.)
            //      Q2 is Model - we use 3 (latest recognized - for Dorado)
            //      H2 is reserved
            operands[1] = (3L << 18);
            storeConsecutiveOperands(true, operands);
        }

        @Override public Instruction getInstruction() { return Instruction.SPID; }
    }

    /**
     * Handles the SR instruction f=004
     */
    private class SRFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, getExecOrUserRRegister((int) _currentInstruction.getA()).getW());
        }

        @Override public Instruction getInstruction() { return Instruction.SR; }
    }

    /**
     * Handles the SRS instruction f=072 j=016
     * This instruction stores one or two consecutive sets of values from GRS locations to a single buffer in memory.
     * The two sets are contiguous in memory; they do not have to be contiguous in the GRS.
     *
     * A(a) contains a descriptor formatted as such:
     *  Bit2-8:     count2
     *  Bit11-17:   address2
     *  Bit20-26:   count1
     *  Bit29-35:   address1
     *
     * Effective u refers to the buffer in memory.  GRS access does not apply.
     * Standard address resolution for consecutive memory addresses does apply.
     * The size of the buffer must be at least count1 + count2 words in length.
     *
     * The first set of transfers begins with the destination set to effective U + 0, and the source is the GRS register
     * at address 1.  {count1} words are transferred to consecutive locations in memory from consecutive GRS registers.
     * If at any point the GRS index reaches 0200, it is reset to 0, and the process continues.
     * The second set of transfers then begins with the destination set to effective U + {count1} (or an appropriate value if
     * wraparound occurred at index 0200) and with the GRS register at address 2.  {count2} words are transferred just
     * as they were in the first set.
     *
     * If count1 or count2 are zero, the corresponding register transfer is effectively a NOP.
     */
    private class SRSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Grab descriptor first
            int descriptorRegisterIndex = getExecOrUserARegisterIndex((int) _currentInstruction.getA());
            long descriptor = getGeneralRegister(descriptorRegisterIndex).getW();

            int address1 = (int)descriptor & 0177;
            int count1 = (int)(descriptor >> 9) & 0177;
            int address2 = (int)(descriptor >> 18) & 0177;
            int count2 = (int)(descriptor >> 27) & 0177;

            //  Create and populate an operands array from the indicated registers
            long[] operands = new long[count1 + count2];
            int ox = 0;
            int grsx = address1;
            for (int rx = 0; rx < count1; ++rx) {
                operands[ox++] = getGeneralRegister(grsx++).getW();
                if (grsx == 0200) {
                    grsx = 0;
                }
            }

            grsx = address2;
            for (int rx = 0; rx < count2; ++rx) {
                operands[ox++] = getGeneralRegister(grsx++).getW();
                if (grsx == 0200) {
                    grsx = 0;
                }
            }

            //  Now store them
            storeConsecutiveOperands(false, operands);
        }

        @Override public Instruction getInstruction() { return Instruction.SRS; }
    }

    /**
     * Handles the SSA instruction f=073 j=004
     */
    private class SSAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            int count = (int) getImmediateOperand() & 0177;
            Word36 w36 = new Word36(operand);
            Word36 result = w36.rightShiftAlgebraic(count);
            setExecOrUserARegister((int) _currentInstruction.getA(), result.getW());
        }

        @Override public Instruction getInstruction() { return Instruction.SSA; }
    }

    /**
     * Handles the SSC instruction f=073 j=000
     */
    public class SSCFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            int count = (int) getImmediateOperand() & 0177;
            Word36 w36 = new Word36(operand);
            Word36 result = w36.rightShiftCircular(count);
            setExecOrUserARegister((int) _currentInstruction.getA(), result.getW());
        }

        @Override public Instruction getInstruction() { return Instruction.SSC; }
    }

    /**
     * Handles the SSL instruction f=073 j=002
     */
    public class SSLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            int count = (int) getImmediateOperand() & 0177;
            Word36 w36 = new Word36(operand);
            Word36 result = w36.rightShiftLogical(count);
            setExecOrUserARegister((int) _currentInstruction.getA(), result.getW());
        }

        @Override public Instruction getInstruction() { return Instruction.SSL; }
    }

    /**
     * Handles the SUB1 instruction f=005, a=016
     */
    private class SUB1FunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            boolean twosComplement = chooseTwosComplementBasedOnJField(_currentInstruction, _designatorRegister);
            incrementOperand(true, true, NEGATIVE_ONE_36, twosComplement);
            if (_designatorRegister.getOperationTrapEnabled() && _designatorRegister.getOverflow()) {
                throw new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.SUB1; }
    }

    /**
     * Handles the SUD instruction f=073 j=017 a=05
     */
    private class SUDFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, false, false, _designatorRegister.getW() & 0777777);
        }

        @Override public Instruction getInstruction() { return Instruction.SUD; }
    }

    /**
     * Handles the SX instruction f=006
     */
    private class SXFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, getExecOrUserXRegister((int) _currentInstruction.getA()).getW());
        }

        @Override public Instruction getInstruction() { return Instruction.SX; }
    }

    /**
     * Handles the SYSC instruction f=073 j=017 a=012
     * Architecture indicates that the packet size is determined by the subfunction code,
     * so we goof around a little bit here to account for that
     */
    private class SYSCFunctionHandler extends InstructionHandler {

        private static final int SF_CONSOLE_SEND_STATUS = 030;
        private static final int SF_CONSOLE_SEND_READ_ONLY = 031;
        private static final int SF_CONSOLE_SEND_READ_REPLY = 032;
        private static final int SF_CONSOLE_CANCEL_READ_REPLY = 033;
        private static final int SF_CONSOLE_POLL_INPUT = 034;
        private static final int SF_CONSOLE_RESET = 035;

        private static final int SF_DAYCLOCK_READ = 042;
        private static final int SF_DAYCLOCK_WRITE = 043;
        private static final int SF_DAYCLOCK_WRITE_COMPARATOR = 044;

        private static final int SF_JUMPKEYS_GET = 040;
        private static final int SF_JUMPKEYS_SET = 041;

        private static final int SF_MEMORY_ALLOC = 020;
        private static final int SF_MEMORY_FREE = 021;
        private static final int SF_MEMORY_REALLOC = 022;

        private static final int SS_SUCCESSFUL = 0;
        private static final int SS_BAD_UPI = 01;
        private static final int SS_BAD_SEGMENT = 02;
        private static final int SS_INVALID_ADDRESS = 03;
        private static final int SS_INVALID_SIZE = 04;
        private static final int SS_ACCESS_DENIED = 05;
        private static final int SS_NO_DATA = 010;

        private class VirtualAddressInfo {
            final VirtualAddress _virtualAddress;
            final BankDescriptor _bankDescriptor;
            final AccessPermissions _effectivePermissions;
            final int _status;

            private VirtualAddressInfo(
                final VirtualAddress virtualAddress,
                final BankDescriptor bankDescriptor,
                final AccessPermissions effectivePermissions,
                final int status
            ) {
                _virtualAddress = virtualAddress;
                _bankDescriptor = bankDescriptor;
                _effectivePermissions = effectivePermissions;
                _status = status;
            }
        }

        //TODO can this be used elsewhere, effectively?
        private VirtualAddressInfo verifyVirtualAddress(
            final VirtualAddress virtualAddress,
            final boolean readRequested,
            final boolean writeRequested,
            final int extent
        ) {
            int status = SS_SUCCESSFUL;
            BankDescriptor bd = null;
            AccessPermissions effective = null;
            try {
                bd = findBankDescriptor(virtualAddress.getLevel(), virtualAddress.getBankDescriptorIndex());
                AccessInfo accessLock = bd.getAccessLock();
                AccessPermissions bankGAP = bd.getGeneraAccessPermissions();
                AccessPermissions bankSAP = bd.getSpecialAccessPermissions();
                AccessInfo accessKey = _indicatorKeyRegister.getAccessInfo();

                if ((accessKey._ring < accessLock._ring) || (accessKey.equals(accessLock))) {
                    effective = bankSAP;
                } else {
                    effective = bankGAP;
                }

                if ((readRequested && !effective._read) || (writeRequested && !effective._write)) {
                    status = SS_ACCESS_DENIED;
                } else {
                    if (virtualAddress.getOffset() >= bd.getSize()) {
                        status = SS_INVALID_ADDRESS;
                    } else if (virtualAddress.getOffset() + extent > bd.getSize()) {
                        status = SS_INVALID_SIZE;
                    }
                }
            } catch (AddressingExceptionInterrupt ex) {
                status = SS_INVALID_ADDRESS;
            }

            return new VirtualAddressInfo(virtualAddress, bd, effective, status);
        }

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  PP has to be 0
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long operand = getOperand(false, false, false,false);
            int subfunction = (int) Word36.getS1(operand);
            switch (subfunction) {
                case SF_MEMORY_ALLOC: {
                    //  Packet size is 3 words
                    //  U+0,S1          Subfunction
                    //  U+0,S2:         Status
                    //  U+0,S3:         UPI of target MSP
                    //  U+1,W:          Newly-assigned segment index if status is zero
                    //  U+2,W:          Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)
                    long[] operands = new long[3];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);
                    int status = SS_SUCCESSFUL;
                    try {
                        int upi = (int) Word36.getS3(operands[0]);
                        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(upi);
                        long words = operands[2] & 0_17777_777777;
                        if (words != operands[2]) {
                            status = SS_INVALID_SIZE;
                        } else {
                            operands[1] = msp.createSegment((int) words);
                        }
                    } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
                        status = SS_BAD_UPI;
                    }
                    operands[0] = Word36.setS2(operands[0], status);
                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_MEMORY_FREE: {
                    //  Packet size is 2 words
                    //  U+0,S1          Subfunction
                    //  U+0,S2:         Status
                    //  U+0,S3:         UPI of target MSP
                    //  U+1,W:          Segment index of block to be released
                    long[] operands = new long[2];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);
                    int status = SS_SUCCESSFUL;
                    try {
                        int upi = (int) Word36.getS3(operands[0]);
                        int segIndex = (int) (operands[1] & 0_37777_777777L);
                        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(upi);
                        msp.deleteSegment(segIndex);
                    } catch (AddressingExceptionInterrupt ex) {
                        status = SS_BAD_SEGMENT;
                    } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
                        status = SS_BAD_UPI;
                    }
                    operands[0] = Word36.setS2(operands[0], status);
                    //  ignore the warning - devAddr cannot be null here since we cannot be in the GRS
                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_MEMORY_REALLOC: {
                    //  Packet size is 3 words
                    //  U+0,S1          Subfunction
                    //  U+0,S2:         Status
                    //  U+0,S3:         UPI of target MSP
                    //  U+1,W:          Segment index of block to be resized
                    //  U+2,W:          Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)
                    long[] operands = new long[3];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);
                    int status = SS_SUCCESSFUL;
                    try {
                        int upi = (int) Word36.getS3(operands[0]);
                        int segIndex = (int) (operands[1] & 0_37777_777777L);
                        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(upi);
                        long words = operands[2] & 0_17777_777777;
                        if (words != operands[2]) {
                            status = SS_INVALID_SIZE;
                        } else {
                            msp.resizeSegment(segIndex, (int) words);
                        }
                    } catch (AddressingExceptionInterrupt ex) {
                        status = SS_BAD_SEGMENT;
                    } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
                        status = SS_BAD_UPI;
                    }
                    operands[0] = Word36.setS2(operands[0], status);
                    //  ignore the warning - devAddr cannot be null here since we cannot be in the GRS
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_CONSOLE_CANCEL_READ_REPLY: {
                    //TODO figure this out
                }

                case SF_CONSOLE_SEND_STATUS: {
                    //TODO this is wrong - addressing is foo'd
                    //  Packet size is 3 - 8 words depending upon the value of U+0,S3
                    //  U+0,S1          Subfunction
                    //  U+0,S2:         Status
                    //  U+0,S3:         Number of messages (at least 1, no more than 6)
                    //  U+0,Q3:         Length of first message in characters
                    //  U+0,Q4:         Length of second message in characters (if existing)
                    //  U+1,Q1:         Length of third (if existing)
                    //  U+1,Q2:         Length of fourth (if existing)
                    //  U+1,Q3:         Length of fifth (if existing)
                    //  U+1,Q4:         Length of sixth (if existing)
                    //  U+2:            Virtual address of buffer containing first message in ASCII
                    //  U+3:            Virtual address of buffer containing second message in ASCII
                    //  U+4:            Virtual address of buffer containing third message in ASCII if existing
                    //  U+5:            Virtual address of buffer containing fourth message in ASCII if existing
                    //  U+6:            Virtual address of buffer containing fifth message in ASCII if existing
                    //  U+7:            Virtual address of buffer containing sixth message in ASCII if existing
                    long[] operands = new long[4];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);

                    int status = SS_SUCCESSFUL;
                    int msgCount = (int) Word36.getS3(operands[0]);
                    int[] msgLenChars = new int[6];
                    msgLenChars[0] = (int) Word36.getQ3(operands[0]);
                    msgLenChars[1] = (int) Word36.getQ4(operands[0]);
                    msgLenChars[2] = (int) Word36.getQ1(operands[1]);
                    msgLenChars[3] = (int) Word36.getQ2(operands[1]);
                    msgLenChars[4] = (int) Word36.getQ3(operands[1]);
                    msgLenChars[5] = (int) Word36.getQ4(operands[1]);

                    VirtualAddressInfo[] vaInfos = new VirtualAddressInfo[msgCount];
                    int[] msgLenWords = new int[msgCount];
                    String[] messages = new String[msgCount];
                    for (int vax = 0; vax < msgCount; ++vax) {
                        msgLenWords[vax] = (msgLenChars[vax] / 4) + ((msgLenChars[vax] % 4 == 0) ? 0 : 1);
                        vaInfos[vax] = verifyVirtualAddress(new VirtualAddress(operands[2]), true, false, msgLenWords[vax]);
                        if (vaInfos[vax]._status != SS_SUCCESSFUL) {
                            status = vaInfos[vax]._status;
                        } else {
                            int offset = vaInfos[vax]._virtualAddress.getOffset();
                            int words = msgLenWords[vax];
                            String asciiString = vaInfos[vax]._bankDescriptor.toASCII(offset, words);
                            messages[vax] = asciiString.substring(0, msgLenChars[vax]);
                        }
                    }

                    if (status == SS_SUCCESSFUL) {
                        _systemProcessor.consoleSendStatusMessage(messages);
                    }

                    operands[0] = Word36.setS2(operands[0], status);
                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_CONSOLE_SEND_READ_ONLY: {
                    //  Packet size is 3 words
                    //  U+0,S1:         Subfunction
                    //  U+0,S2:         Status
                    //  U+0,S3:         Flags
                    //                      Bit 12-15: unused
                    //                      Bit 16:     0 normally, 1 for right-justified
                    //                      Bit 17:     0 normally, 1 to prevent caching of the message in the console multiplexor
                    //  U+0,Q3:         Length of message in characters
                    //  U+1:            Console identifier - 0 for all consoles (bottom 32 bits only)
                    //  U+2:            Virtual address of buffer containing message in ASCII
                    long[] operands = new long[3];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);

                    int chars = (int) Word36.getQ3(operands[0]);
                    int words = (chars / 4) + ((chars % 4 == 0) ? 0 : 1);
                    int flags = (int) Word36.getS3(operands[0]);
                    int consoleId = (int) operands[1];
                    VirtualAddressInfo vaInfo = verifyVirtualAddress(new VirtualAddress(operands[2]),
                                                                     true,
                                                                     false,
                                                                     words);

                    int status = vaInfo._status;
                    if (status == SS_SUCCESSFUL) {
                        try {
                            ArraySlice as = getStorageValues(vaInfo._bankDescriptor.getBaseAddress(),
                                                             vaInfo._virtualAddress.getOffset(),
                                                             words);
                            String msg =
                                "  " + as.toASCII(vaInfo._virtualAddress.getOffset(), words).substring(0, chars);
                            Boolean rightJust = (flags & 0x02) != 0;
                            Boolean cached = (flags & 0x01) == 0;
                            _systemProcessor.consoleSendReadOnlyMessage(consoleId, msg, rightJust, cached);
                        } catch (AddressLimitsException
                                 | UPINotAssignedException
                                 | UPIProcessorTypeException ex) {
                            raiseInterrupt(new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                                            vaInfo._virtualAddress.getLevel(),
                                                                            vaInfo._virtualAddress.getBankDescriptorIndex()));
                            status = SS_INVALID_ADDRESS;
                        }
                    }

                    operands[0] = Word36.setS2(operands[0], status);
                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_CONSOLE_SEND_READ_REPLY: {
                    //TODO this is wrong - addressing is foo'd
                    //  Packet size is 3 words
                    //  U+0,S1:         Subfunction
                    //  U+0,S2:         Status
                    //  U+0,S3:         Message identifier
                    //  U+0,Q3:         Length of message in characters
                    //  U+0,Q4:         Max length of reply in characters
                    //  U+1:            Console identifier - 0 for all consoles (bottom 32 bits only)
                    //  U+2:            Virtual address of buffer containing message in ASCII
                    long[] operands = new long[3];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);

                    int messageId = (int) Word36.getS3(operands[0]);
                    int chars = (int) Word36.getQ3(operands[0]);
                    int maxReplyChars = (int) Word36.getQ4(operands[0]);
                    int words = (chars / 4) + ((chars % 4 == 0) ? 0 : 1);
                    int consoleId = (int) operands[1];
                    VirtualAddressInfo vaInfo = verifyVirtualAddress(new VirtualAddress(operands[2]),
                                                                     true,
                                                                     false,
                                                                     words);
                    if (vaInfo._status == SS_SUCCESSFUL) {
                        String msg =
                            "  " + vaInfo._bankDescriptor.toASCII(vaInfo._virtualAddress.getOffset(), words).substring(0, chars);
                        _systemProcessor.consoleSendReadReplyMessage(consoleId, messageId, msg, maxReplyChars);
                        operands[0] = Word36.setS2(operands[0], vaInfo._status);
                    }

                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_CONSOLE_POLL_INPUT: {
                    //TODO this is probably wrong - addressing is foo'd
                    //  Packet size is 4 words
                    //  U+0,S1          Subfunction
                    //  U+0,S2:         Status
                    //  U+0,Q3          Size of reply buffer in words
                    //  U+0,Q4          Number of characters received if a message was read
                    //  U+1,H2:         Time to wait, in msecs - if zero, no wait
                    //  U+2,W:          Upon return, the identifier of the sending console
                    //  U+3:            Virtual address of buffer containing message in ASCII
                    long[] operands = new long[3];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);

                    int status = SS_SUCCESSFUL;
                    int words = (int) Word36.getQ3(operands[0]);
                    VirtualAddressInfo vaInfo = verifyVirtualAddress(new VirtualAddress(operands[3]),
                                                                     false,
                                                                     true,
                                                                     words);
                    if (vaInfo._status == SS_SUCCESSFUL) {
                        int waitMillis = (int) Word36.getH2(operands[1]);
                        SystemProcessorInterface.ConsoleInputMessage consInput
                            = _systemProcessor.consolePollInputMessage(waitMillis);
                        if (consInput != null) {
                            operands[0] = Word36.setQ4(operands[0], consInput._text.length());
                            operands[2] = consInput._consoleIdentifier;
                            for (int wx = 0, bdx = vaInfo._virtualAddress.getOffset(), ssx = 0, ssy = 4;
                                 wx < words && ssx < consInput._text.length();
                                 ++wx, ++bdx, ssx += 4, ssy += 4) {
                                Word36 w36 = Word36.stringToWordASCII(consInput._text.substring(ssx, ssy));
                                vaInfo._bankDescriptor.set(bdx, w36.getW());
                            }
                        } else {
                            status = SS_NO_DATA;
                        }
                    } else {
                        status = vaInfo._status;
                    }

                    operands[0] = Word36.setS2(operands[0], status);
                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_CONSOLE_RESET: {
                    //  Packet size is 1 word
                    //  U+0,S1          Subfunction
                    //  U+0,S2:         Status
                    _systemProcessor.consoleReset();
                    operand = Word36.setS2(operand, SS_SUCCESSFUL);
                    storeOperand(false,false,false,false, operand);
                    break;
                }

                case SF_DAYCLOCK_READ:
                    //  Packet size is 3 words
                    //  U+0,S1          Subfunction
                    //  U+1,2:          Dayclock value in microseconds since epoch read from our dayclock
                    //TODO
                    break;

                case SF_DAYCLOCK_WRITE:
                    //  Packet size is 3 words
                    //  U+0,S1          Subfunction
                    //  U+1,2:          Dayclock value in microseconds since epoch to be set in dayclock
                    //TODO
                    break;

                case SF_DAYCLOCK_WRITE_COMPARATOR:
                    //  Packet size is 3 words
                    //  U+0,S1          Subfunction
                    //  U+1,2:          Dayclock value in microseconds since epoch to be set in dayclock comparator
                    //TODO
                    break;

                case SF_JUMPKEYS_GET: {
                    //  Packet size is 2 words
                    //  U+0,S1          Subfunction
                    //  U+1,W           Current jump key settings are returned here
                    long[] operands = new long[2];
                    DevelopedAddresses devAddr = getConsecutiveOperands(false, operands, true);
                    operands[1] = _systemProcessor.getJumpKeys().getW();
                    //noinspection ConstantConditions
                    storeConsecutiveOperands(devAddr, operands);
                    break;
                }

                case SF_JUMPKEYS_SET: {
                    //  Packet size is 2 words
                    //  U+0,S1          Subfunction
                    //  U+1,W           Desired jump key settings
                    long[] operands = new long[2];
                    getConsecutiveOperands(false, operands, true);
                    _systemProcessor.setJumpKeys(new Word36(operands[1]));
                    break;
                }

                default:
                    throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.SYSC; }
    }

    /**
     * Handles the SZ instruction f=005 a=000
     */
    private class SZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            storeOperand(true, true, true, true, 0L);
        }

        @Override public Instruction getInstruction() { return Instruction.SZ; }
    }

    /**
     * Handles the TCS instruction f=073 j=017 a=02
     * Requires storage lock
     */
    private class TCSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            try {
                testAndStore(false);
                skipNextInstruction();
            } catch (TestAndSetInterrupt ex) {
                //  lock already clear - do nothing
            } finally {
                //  In any case, increment F0.x if/as appropriate
                incrementIndexRegisterInF0();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TCS; }
    }

    /**
     * Handles the TE instruction f=052
     * Skip NI if (U) > == A(a)
     */
    private class TEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long op2 = getOperand(true, true, true, true);
            if (op1 == op2) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TE; }
    }

    /**
     * Handles the TEP instruction f=044
     * Count the bits in the logical AND of A(a) and U.
     * If there are an even number of them, skip the next instruction.
     */
    private class TEPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long op2 = getOperand(true, true, true, true);
            int bitCount = Long.bitCount(op1 & op2);
            if ((bitCount & 0_01) == 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TEP; }
    }

    /**
     * Handles the TG instruction f=055
     * Skip NI if (U) > A(a)
     */
    private class TGFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, true, true);
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            if (Word36.compare(uValue, aValue) > 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TG; }
    }

    /**
     * Handles the TGM instruction extended mode f=033 j=013
     * Skip NI if |(U)| > A(a)
     */
    private class TGMFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, true, false);
            if (Word36.isNegative(uValue)) {
                uValue = Word36.negate(uValue);
            }

            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            if (Word36.compare(uValue, aValue) > 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TGM; }
    }

    /**
     * Handles the TGZ instruction extended mode f=050 a=01
     * Skip NI if (U) > +0
     */
    private class TGZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isPositive(op) && !Word36.isPositiveZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TGZ; }
    }

    /**
     * Handles the TLE / TNG instruction f=054
     * Skip NI if (U) <= A(a)
     */
    private class TLEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, true, true);
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            if (Word36.compare(uValue, aValue) <= 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TLE; }
    }

    /**
     * Handles the TLEM / TNGM instruction f=047
     * Skip NI if (U) <= X(a).mod
     * Always increment X(a)
     * In Basic Mode if F0.h is true (U resolution x-reg incrementation) and F0.a == F0.x, we increment only once
     * Only H2 of (U) is compared; j-field 0, 1, and 3 produce the same results. j-field 016 and 017 produce the same results.
     * X(0) is used for X(a) if a == 0 (contrast to F0.x == 0 -> no indexing)
     * In Extended Mode, X(a) incrementation is always 18 bits.
     */
    private class TLEMFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int ixReg = (int) _currentInstruction.getA();
            IndexRegister xreg = getExecOrUserXRegister(ixReg);
            long uValue = (getOperand(true, true, true, true) & 0_777777);
            long modValue = xreg.getXM();
            if (uValue <= modValue) {
                skipNextInstruction();
            }

            if (!_designatorRegister.getBasicModeEnabled()
                || (_currentInstruction.getA() != _currentInstruction.getX())
                || (_currentInstruction.getH() == 0)) {
                setExecOrUserXRegister(ixReg, IndexRegister.incrementModifier18(xreg.getW()));
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TLEM; }
    }

    /**
     * Handles the TLZ instruction extended mode f=050 a=010
     * Skip NI if (U) < -0
     */
    private class TLZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isNegative(op) && !Word36.isNegativeZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TLZ; }
    }

    /**
     * Handles the TMZ instruction - extended mode f=050 a=04
     * Skip NI if (U) == -0
     */
    private class TMZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isNegativeZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TMZ; }
    }

    /**
     * Handles the TMZG instruction - extended mode f=050 a=05
     * The designers have been smoking weed, I think.
     * Skip NI if (U) == -0 OR (U) > +0
     */
    private class TMZGFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isNegativeZero(op) || (!Word36.isPositiveZero(op) && Word36.isPositive(op))) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TMZG; }
    }

    /**
     * Handles the TN instruction - extended mode f=050 a=014, basic mode f=061
     * Skip NI if (U) > 0
     */
    private class TNFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isNegative(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TN; }
    }

    /**
     * Handles the TNE instruction f=053
     * Skip NI if (U) != A(a)
     */
    private class TNEFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long op2 = getOperand(true, true, true, true);
            if (op1 != op2) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNE; }
    }

    /**
     * Handles the TNGZ instruction extended mode f=050 a=016
     * Skip NI if (U) < 1
     */
    private class TNGZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isNegative(op) || Word36.isPositiveZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNGZ; }
    }

    /**
     * Handles the TNLZ instruction extended mode f=050 a=07
     * Skip NI if (U) >= -0
     */
    private class TNLZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isPositive(op) || Word36.isNegativeZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNLZ; }
    }

    /**
     * Handles the TNMZ instruction - extended mode f=050 a=013
     * Skip NI if (U) == -0
     */
    private class TNMZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (!Word36.isNegativeZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNMZ; }
    }

    /**
     * Handles the TNOP instruction extended mode f=050 a=00
     * Get the content of U, and toss it. Never skip NI.
     */
    private class TNOPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            getOperand(true, true, true, true);
        }

        @Override public Instruction getInstruction() { return Instruction.TNOP; }
    }

    /**
     * Handles the TNPZ instruction extended mode f=050 a=015
     * Skip NI if (U) == +0
     */
    private class TNPZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (!Word36.isPositiveZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNPZ; }
    }

    /**
     * Handles the TNW instruction f=057
     * Skip NI if (U) <= A(a) or (U) > A(a+1)
     */
    private class TNWFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, true, true);
            long aValueLow = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long aValueHigh = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            if ((Word36.compare(uValue, aValueLow) <= 0) || (Word36.compare(uValue, aValueHigh) > 0)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNW; }
    }

    /**
     * Handles the TNZ instruction - extended mode f=050 a=011, basic mode f=051
     * Skip NI if (U) != +/-0
     */
    private class TNZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (!Word36.isZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TNZ; }
    }

    /**
     * Handles the TOP instruction f=045
     * Count the bits in the logical AND of A(a) and U.
     * If there are an odd number of them, skip the next instruction.
     */
    private class TOPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long op2 = getOperand(true, true, true, true);
            int bitCount = Long.bitCount(op1 & op2);
            if ((bitCount & 0x01) != 0) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TOP; }
    }

    /**
     * Handles the TP instruction - extended mode f=050 a=03, basic mode f=060
     * Skip NI if (U) > 0
     */
    private class TPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isPositive(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TP; }
    }

    /**
     * Handles the TPZ instruction extended mode f=050 a=02
     * Skip NI if (U) == +0
     */
    private class TPZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isPositiveZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TPZ; }
    }

    /**
     * Handles the TPZL instruction extended mode f=050 a=012
     * Skip NI if (U) == +0 or (U) < -0
     */
    private class TPZLFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isPositiveZero(op)
                || (Word36.isNegative(op) && !Word36.isNegativeZero(op))) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TPZL; }
    }

    /**
     * Handles the TRA instruction f=072 j=015
     */
    private class TRAFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            int brIndex = getBasicModeBankRegisterIndex();
            long result = ((brIndex == 0) ? 0 : 0400000_000000L) | ((long) (brIndex & 03) << 33);
            setExecOrUserXRegister((int) _currentInstruction.getA(), result);

            if (brIndex != 0) {
                try {
                    BaseRegister bReg = _baseRegisters[brIndex];
                    bReg.checkAccessLimits(false, true, true, _indicatorKeyRegister.getAccessInfo());
                    skipNextInstruction();
                } catch (ReferenceViolationInterrupt ex) {
                    //  do nothing
                }
            }
        }

        @Override public Instruction getInstruction() {
            return Instruction.TRA;
        }
    }

    /**
     * Handles the TRARS instruction f=072 j=00
     */
    private class TRARSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getProcessorPrivilege() > 0) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            //  Read the 23 word packet indicated by U...
            //  Packet format:
            //  +0  Reserved
            //  +1 to +15 Active Base Table (ABT)
            //  +16 to +22 Activity Save Packet (ASP)
            //  ASP Format is:
            //      +0:     Program Address Register
            //      +1:     Designator Register
            //      +2:     Indicator / Key Register
            //      +3:     Quantum Timer
            //      +4:     F0 (instruction)
            //      +5:     ISW0 (Interrupt Status Word)
            //      +6:     ISW1 (Interrupt Status Word)
            long[] operands = new long[23];
            getConsecutiveOperands(false, operands, false);

            int abtx = 1;   //  index of first ABT word in the packet
            int aspx = 16;  //  index of first ASP word in the packet
            DesignatorRegister packetDesignatorRegister = new DesignatorRegister(operands[aspx + 2]);
            IndicatorKeyRegister packetIndicatorKeyRegister = new IndicatorKeyRegister(operands[aspx + 3]);

            int[] candidates =
                packetDesignatorRegister.getBasicModeBaseRegisterSelection() ? InstructionProcessor.BASE_REGISTER_CANDIDATES_TRUE
                                                                             : InstructionProcessor.BASE_REGISTER_CANDIDATES_FALSE;

            //  We're taking relative addresses from X(a) and X(a+1) as lower and upper limits, then checking
            //  that against the basic mode banks (B12-B15) specified in the packet, in the context specified
            //  by the packet.  See TRA - we're basically doing that, but with specified instead of extant
            //  state, and with a range of addresses rather than a single address.
            //  Docs state the relative address is X(a) (and +1) sans top 3 bits.
            //  This is still 33 bits, which is more than an int, and all our relative addresses are expected
            //  to fit in an int (which might be wrong, but there it is).  We use 31, not 33 bits for relative
            //  address (because ints are signed in idiot Java)
            IndexRegister xReg1 = getExecOrUserXRegister((int) _currentInstruction.getA());
            IndexRegister xReg2 = getExecOrUserXRegister((int) _currentInstruction.getA() + 1);
            int lowerRelAddr = (int) (xReg1.getW() & 0x7FFF);
            int upperRelAddr = (int) (xReg2.getW() & 0x7FFF);
            AccessInfo accessKey = packetIndicatorKeyRegister.getAccessInfo();

            int count = upperRelAddr - lowerRelAddr + 1;
            if (count >= 0) {
                for (int cx = 0; cx < 4; ++cx) {
                    int brIndex = candidates[cx];
                    long abtValue = operands[abtx + brIndex - 1];
                    if (abtValue != 0) {
                        ActiveBaseTableEntry abte = new ActiveBaseTableEntry(abtValue);
                        if ((abte.getLevel() > 0) || (abte.getBDI() > 31)) {
                            BaseRegister bdtBaseRegister = getBaseRegister(abte.getLevel() + 16);
                            int bdtBaseRegisterOffset = 8 * abte.getBDI();
                            try {
                                bdtBaseRegister.checkAccessLimits(bdtBaseRegisterOffset + 7, false);
                                BankDescriptor bd = new BankDescriptor(bdtBaseRegister._storage, bdtBaseRegisterOffset);
                                BaseRegister bReg = new BaseRegister(bd);

                                bReg.checkAccessLimits(lowerRelAddr, count, false, false, accessKey);

                                long value = 0_400000_000000L;
                                value |= ((long) (brIndex & 03)) << 33;
                                xReg1.setW(value);

                                try {
                                    //  If we have read/write access, skip next instruction.
                                    bReg.checkAccessLimits(false, true, true, accessKey);
                                    skipNextInstruction();
                                } catch (ReferenceViolationInterrupt ex) {
                                    //  we don't have read/write access, so do not skip
                                }

                                return;
                            } catch (ReferenceViolationInterrupt ex) {
                                //  docs are unclear as to this contingency if we are checking the BDT,
                                //  so we move on to the next thing as if this was a zero or void bank
                                //  just like we do if address limits don't match a proper bank descriptor
                            }
                        }
                    }
                }
            }

            xReg1.setW(0);
        }

        @Override public Instruction getInstruction() { return Instruction.TRARS; }
    }

    /**
     * Handles the TS instruction f=073 j=017 a=00
     * Requires storage lock
     */
    private class TSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            testAndStore(true);
            incrementIndexRegisterInF0();
        }

        @Override public Instruction getInstruction() { return Instruction.TS; }
    }

    /**
     * Handles the TSKP instruction extended mode f=050 a=017
     * Skip NI always
     */
    private class TSKPFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            getOperand(true, true, true, true);
            skipNextInstruction();
        }

        @Override public Instruction getInstruction() { return Instruction.TSKP; }
    }

    /**
     * Handles the TSS instruction f=073 j=017 a=01
     * Requires storage lock
     */
    private class TSSFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            try {
                testAndStore(true);
                skipNextInstruction();
            } catch (TestAndSetInterrupt ex) {
                //  lock already set - do nothing
            } finally {
                //  In any case, increment F0.x if/as appropriate
                incrementIndexRegisterInF0();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TSS; }
    }

    /**
     * For TVA below...
     */
    private interface TVAStepHandler {
        void invoke(TVAScratchPad scratchPad) throws MachineInterrupt, UnresolvedAddressException;
    }

    /**
     * Also for TVA
     */
    private class TVAScratchPad {
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

        TVAScratchPad(InstructionWord iw) {
            _instructionWord = iw;
            _processorPrivilege = _designatorRegister.getProcessorPrivilege();
        }
    }

    /**
     * Handles the TVA instruction f=075 j=010 - this is a gnarly one
     */
    private class TVAFunctionHandler extends InstructionHandler {

        /**
         * Sets up certain fields in the ScratchPad object for subsequent processing
         */
        private class Step1Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
                //  Get the virtual address to be checked from X(a) and the various flags and alternate key from X(a+1).
                //  If a==15, then X(a) is A(3), and X(a+1) is A(4).
                int aField = (int) scratchPad._instructionWord.getA();
                scratchPad._virtualAddress = new VirtualAddress(getExecOrUserXRegister(aField).getW());
                scratchPad._checkLevel = scratchPad._virtualAddress.getLevel();
                scratchPad._checkBankDescriptorIndex = scratchPad._virtualAddress.getBankDescriptorIndex();
                scratchPad._checkOffset = scratchPad._virtualAddress.getOffset();

                long options = (aField < 15) ?
                               getExecOrUserXRegister(aField + 1).getW() :
                               getExecOrUserARegister(4).getW();

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
                    scratchPad._effectiveAccessKey = _indicatorKeyRegister.getAccessInfo();
                }

                scratchPad._step = 2;
            }
        }

        /**
         * Step 2 - Decide whether to do step 5 (callOrQBankVAddrTranslate) or step 3 (lbuVAddrTranslateOrCheckQBRAccess)
         */
        private class Step2Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
                scratchPad._step = scratchPad._enterAccessRequired ? 5 : 3;
            }
        }

        /**
         * Step 3 - Interprets the virtual address as if we were doing an LBU, or checks QBRAccess
         */
        private class Step3Handler implements TVAStepHandler {

            private void doDefault(
                final TVAScratchPad scratchPad,
                final BankDescriptor bankDescriptor
            ) {
                if (bankDescriptor.getGeneralFault()) {
                    scratchPad._skipNextInstruction = false;
                    scratchPad._generalFault = true;
                }
                scratchPad._step = 4;
            }

            private void doIndirect(
                final TVAScratchPad scratchPad,
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
                final TVAScratchPad scratchPad
            ) {
                scratchPad._step = 4;
            }

            private void doQueueBankRepository(
                final TVAScratchPad scratchPad
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

            @Override public void invoke(final TVAScratchPad scratchPad) throws MachineInterrupt {
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
        private class Step4Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
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
        private class Step5Handler implements TVAStepHandler {

            private void doGate(
                final TVAScratchPad scratchPad,
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
                final TVAScratchPad scratchPad,
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
                final TVAScratchPad scratchPad,
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
                final TVAScratchPad scratchPad
            ) {
                scratchPad._skipNextInstruction = false;
                scratchPad._accessDenied = true;
                scratchPad._invalidRealAddress = true;
                scratchPad._qbrFound = true;
                scratchPad._step = 9;
            }

            @Override public void invoke(final TVAScratchPad scratchPad) throws MachineInterrupt {
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
                if (!scratchPad._gatedBankFlag && (bd.getBankType() != BankType.BasicMode)) {
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
                } else if ((bd.getBankType() != BankType.Queue) && (bd.getGeneralFault())) {
                    scratchPad._skipNextInstruction = false;
                    scratchPad._generalFault = true;
                }

                scratchPad._step = 7;
            }
        }

        /**
         * Step 7 - limits check
         */
        private class Step7Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
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
        private class Step8Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
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
        private class Step9Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
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
                        value1 = (((long) scratchPad._realAddress._upiIndex) << 32) | scratchPad._realAddress._offset;
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
        private class Step10Handler implements TVAStepHandler {

            @Override public void invoke(final TVAScratchPad scratchPad) {
                if (scratchPad._skipNextInstruction) { skipNextInstruction(); }
                scratchPad._done = true;
            }
        }

        /**
         * Check either GAP or SAP permissions, as determined by the comparison of the key and lock,
         * and based on the read and write flags specified.  Note that we always return true if both flags are false.
         * Corresponds to step 4, but also used elsewhere.
         */
        private boolean checkAccess(
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
        private BankDescriptor findBankDescriptor(
            final TVAScratchPad scratchPad
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
            BaseRegister bdtReg = _baseRegisters[bdtRegIndex];
            int bdOffset = 8 * scratchPad._checkBankDescriptorIndex;
            try {
                bdtReg.checkAccessLimits(bdOffset,
                                         8,
                                         true,
                                         false,
                                         _indicatorKeyRegister.getAccessInfo());
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

        private final TVAStepHandler[] _stepHandlers = {
            null,                   //  0 is not a step
            new Step1Handler(),
            new Step2Handler(),
            new Step3Handler(),
            new Step4Handler(),
            new Step5Handler(),
            null,                   //  Step 6 is simply a matter of throwing an exception, so we do that inline
            new Step7Handler(),
            new Step8Handler(),
            new Step9Handler(),
            new Step10Handler(),
        };

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            //  Instructions like this are why CISC is a thing...
            TVAScratchPad sp = new TVAScratchPad(_currentInstruction);
            if (_designatorRegister.getBasicModeEnabled() && (sp._processorPrivilege > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            //  U is computed and summarily ignored
            getOperand(false, false, false, false);

            while (!sp._done) {
                _stepHandlers[sp._step].invoke(sp);
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TVA; }
    }

    /**
     * Handles the TW instruction f=056
     * Skip NI if A(a) < (U) <= A(a+1)
     */
    private class TWFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long uValue = getOperand(true, true, true, true);
            long aValue = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long aValuePlus = getExecOrUserARegister((int) _currentInstruction.getA() + 1).getW();

            if ((Word36.compare(aValue, uValue) < 0) && (Word36.compare(uValue, aValuePlus) <= 0)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TW; }
    }

    /**
     * Handles the TZ instruction - extended mode f=050 a=06, basic mode f=050
     * Skip NI if (U) == +/-0
     */
    private class TZFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long op = getOperand(true, true, true, true);
            if (Word36.isZero(op)) {
                skipNextInstruction();
            }
        }

        @Override public Instruction getInstruction() { return Instruction.TZ; }
    }

    /**
     * Handles the UR instruction f=073 j=015 a=016
     */
    private class URFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            if (_designatorRegister.getBasicModeEnabled() && (_designatorRegister.getProcessorPrivilege() > 0)) {
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
            }

            long[] operands = new long[7];
            getConsecutiveOperands(false, operands, false);
            new BankManipulator().bankManipulation(Instruction.UR, operands);
            _preventProgramCounterIncrement = true;
        }

        @Override public Instruction getInstruction() { return Instruction.UR; }
    }

    /**
     * Handles the XOR instruction f=041
     */
    private class XORFunctionHandler extends InstructionHandler {

        @Override public void handle() throws MachineInterrupt, UnresolvedAddressException {
            long operand1 = getExecOrUserARegister((int) _currentInstruction.getA()).getW();
            long operand2 = getOperand(true, true, true, true);
            setExecOrUserARegister((int) _currentInstruction.getA() + 1, operand1 ^ operand2);
        }

        @Override
        public Instruction getInstruction() { return Instruction.XOR; }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static final int L0_BDT_BASE_REGISTER    = 16;
    public static final int ICS_BASE_REGISTER       = 26;
    public static final int ICS_INDEX_REGISTER      = GeneralRegisterSet.EX1;
    public static final int RCS_BASE_REGISTER       = 25;
    public static final int RCS_INDEX_REGISTER      = GeneralRegisterSet.EX0;
    private static final long NEGATIVE_ONE_36       = 0_777777_777776L;
    private static final long NEGATIVE_TWO_36       = 0_777777_777775L;

    //  Array of j-field values indexed by the quarter-word designator where
    //  des==0 -> Q1, des==1 -> Q2, etc. - used by LAQW and SAQW instructions
    private static final int[] QW_J_FIELDS = { 7, 4, 6, 5 };

    /**
     * Raise interrupt when this many new entries exist
     */
    private static final int JUMP_HISTORY_TABLE_THRESHOLD   = 120;

    /**
     * Size of the conditionalJump history table
     */
    private static final int JUMP_HISTORY_TABLE_SIZE        = 128;

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
     * ActiveBaseTable entries - index 1 is for B1 .. index 15 is for B15.
     * [0] is always null since the B0 BDI is held in PAR.
     */
    private final ActiveBaseTableEntry[] _activeBaseTableEntries = new ActiveBaseTableEntry[16];

    /**
     * Storage locks...
     */
    private static final Map<InstructionProcessor, HashSet<AbsoluteAddress>> _storageLocks = new HashMap<>();

    private final BaseRegister[]            _baseRegisters = new BaseRegister[32];
    private final AbsoluteAddress           _breakpointAddress = new AbsoluteAddress((short)0, 0, 0);
    private final BreakpointRegister        _breakpointRegister = null;
    private boolean                         _broadcastInterruptEligibility = false;
    protected InstructionWord                _currentInstruction = null;
    private InstructionHandler              _currentInstructionHandler = null;  //  TODO do we need this?
    private RunMode                         _currentRunMode = RunMode.Stopped;
    private DesignatorRegister              _designatorRegister = new DesignatorRegister();
    private final FunctionTable             _functionTable = new FunctionTable();
    private final GeneralRegisterSet        _generalRegisterSet = new GeneralRegisterSet();
    private IndicatorKeyRegister            _indicatorKeyRegister = new IndicatorKeyRegister();
    private boolean                         _jumpHistoryFullInterruptEnabled = false;
    private final Word36[]                  _jumpHistoryTable = new Word36[JUMP_HISTORY_TABLE_SIZE];
    private int                             _jumpHistoryTableNext = 0;
    private boolean                         _jumpHistoryThresholdReached = false;
    private MachineInterrupt                _lastInterrupt = null;    //  must always be != _pendingInterrupt
    private long                            _latestStopDetail = 0;
    private StopReason                      _latestStopReason = StopReason.Initial;
    private boolean                         _midInstructionInterruptPoint = false;
    private MachineInterrupt                _pendingInterrupt = null;
    private final ProgramAddressRegister    _preservedProgramAddressRegister = new ProgramAddressRegister();
    private boolean                         _preventProgramCounterIncrement = false;
    private final ProgramAddressRegister    _programAddressRegister = new ProgramAddressRegister();
    private long                            _quantumTimer = 0;
    private SystemProcessor                 _systemProcessor = null;

    private final Set<Processor> _pendingUPISends = new HashSet<>();

    //  debugging tools - they should be settable from the SPIF
    boolean                                 _traceExecuteInstruction = false;
    boolean                                 _developmentMode = false;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param name node name
     * @param upiIndex unique identifier for this processor
     */
    public InstructionProcessor(
        final String name,
        final int upiIndex
    ) {
        super(ProcessorType.InstructionProcessor, name, upiIndex);

        _storageLocks.put(this, new HashSet<AbsoluteAddress>());

        for (int bx = 0; bx < _baseRegisters.length; ++bx) {
            _baseRegisters[bx] = new BaseRegister();
        }

        for (int jx = 0; jx < JUMP_HISTORY_TABLE_SIZE; ++jx) {
            _jumpHistoryTable[jx] = new Word36();
        }

        for (int ax = 1; ax < _activeBaseTableEntries.length; ++ax) {
            _activeBaseTableEntries[ax] = new ActiveBaseTableEntry(0);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public ActiveBaseTableEntry[] getActiveBaseTableEntries() { return _activeBaseTableEntries; }
    public BaseRegister getBaseRegister(final int index) { return _baseRegisters[index]; }
    boolean getBroadcastInterruptEligibility() { return _broadcastInterruptEligibility; }
    public DesignatorRegister getDesignatorRegister() { return _designatorRegister; }

    public GeneralRegister getGeneralRegister(
        final int index
    ) throws MachineInterrupt {
        if (!GeneralRegisterSet.isAccessAllowed(index, _designatorRegister.getProcessorPrivilege(), false)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
        return _generalRegisterSet.getRegister(index);
    }

    public MachineInterrupt getLastInterrupt() { return _lastInterrupt; }
    public StopReason getLatestStopReason() { return _latestStopReason; }
    public long getLatestStopDetail() { return _latestStopDetail; }
    public ProgramAddressRegister getProgramAddressRegister() { return _programAddressRegister; }
    public boolean isCleared() { return (_currentRunMode == RunMode.Stopped) && (_latestStopReason == StopReason.Cleared); }
    public boolean isStopped() { return _currentRunMode == RunMode.Stopped; }

    public void setBaseRegister(
        final int index,
        final BaseRegister baseRegister
    ) {
        _baseRegisters[index] = baseRegister;
    }

    public void setGeneralRegister(
        final int index,
        final long value
    ) throws MachineInterrupt {
        if (!GeneralRegisterSet.isAccessAllowed(index, _designatorRegister.getProcessorPrivilege(), true)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
        }

        _generalRegisterSet.setRegister(index, value);
    }


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
     * Nonfault interrupts are always taken at the next interrupt point (unless classified as a pended
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
     * @return relative address for the current instruction
     */
    private int calculateRelativeAddressForGRSOrStorage(
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

        long result = Word36.addSimple(addend1, addend2);
        return (int) result;
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

        return (int) Word36.addSimple(addend1, addend2);
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
                && (((comparison == BreakpointComparison.Fetch) && _breakpointRegister._fetchFlag)
                    || ((comparison == BreakpointComparison.Read) && _breakpointRegister._readFlag)
                    || ((comparison == BreakpointComparison.Write) && _breakpointRegister._writeFlag))) {
            //TODO Per doc, 2.4.1.2 Breakpoint_Register - we need to halt if Halt Enable is set
            //      which means Stop Right Now... how do we do that for all callers of this code?
            _indicatorKeyRegister.setBreakpointRegisterMatchCondition(true);
        }
    }

    /**
     * Certain instructions (ADD1, INC1, etc) choose to do either 1's or 2's complement arithemtic based upon the
     * j-field (and apparently, the quarter-word-mode).  Such instructions call here to make that determination.
     * @param instructionWord instruction word of interest
     * @param designatorRegister designator register of interest
     * @return true if we are to do two's complement
     */
    private boolean chooseTwosComplementBasedOnJField(
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
        if (_traceExecuteInstruction) {
            String msg = String.format("Executing Instruction at %012o --> %s",
                                       getProgramAddressRegister().get(),
                                       _currentInstruction.interpret(!getDesignatorRegister().getBasicModeEnabled(),
                                                                     getDesignatorRegister().getExecRegisterSetSelected()));
            _logger.trace(msg);
        }

        FunctionHandler handler = _functionTable.lookup(_currentInstruction, _designatorRegister.getBasicModeEnabled());
        if (handler == null) {
            _midInstructionInterruptPoint = false;
            _indicatorKeyRegister.setInstructionInF0(false);
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }

        handler.handle();
        _indicatorKeyRegister.setInstructionInF0(_midInstructionInterruptPoint);
        if (!_midInstructionInterruptPoint) {
            //  instruction is done - clear storage locks
            synchronized(_storageLocks) {
                _storageLocks.get(this).clear();
            }
        }
    }

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
            case InstructionWord.W:     return source & Word36.BIT_MASK;
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
     * Fetches the next instruction based on the current program address register, placing it in the current instruction register.
     * At this point we also copy the program address register to the preserved program address register so that, even when the
     * PC gets incremented, we still know the initial PC for this instruction.
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
        long programCounter = _programAddressRegister.getProgramCounter();

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

        long pcOffset = programCounter - bReg._lowerLimitNormalized;
        _currentInstruction = new InstructionWord(bReg._storage.get((int) pcOffset));
        _indicatorKeyRegister.setInstructionInF0(true);
        _preservedProgramAddressRegister.set(_programAddressRegister.get());
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
                _currentInstruction = _currentInstruction.setXHIU(br._storage.get(wx));
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
        final long relativeAddress,
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
     * Converts a relative address to an absolute address.
     * @param baseRegister base register associated with the relative address
     * @param relativeAddress address to be converted
     * @return absolute address object
     */
    private static AbsoluteAddress getAbsoluteAddress(
        final BaseRegister baseRegister,
        final int relativeAddress
    ) {
        int upi = baseRegister._baseAddress._upiIndex;
        int actualOffset = relativeAddress - baseRegister._lowerLimitNormalized;
        int offset = baseRegister._baseAddress._offset + actualOffset;
        return new AbsoluteAddress(upi, baseRegister._baseAddress._segment, offset);
    }

    /**
     * Retrieves a BankDescriptor object representing the BD entry in a particular BDT.
     * @param bankLevel level of the bank of interest (0:7)
     * @param bankDescriptorIndex BDI of the bank of interest (0:077777)
     * @param throwFatal Set reason to FatalAddressingException if we throw an AddressingExceptionInterrupt for a bad
     *                   specified level/BDI, otherwise the reason will be InvalidSourceLevelBDI.
     * @return BankDescriptor object representing the bank descriptor in memory
     */
    private BankDescriptor getBankDescriptor(
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
        ArraySlice bdStorage = _baseRegisters[bdRegIndex]._storage;
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
     * Calculates the raw relative address (the U) for the current instruction presuming basic mode (even if it isn't set),
     * honors any indirect addressing, and returns the index of the basic mode bank (12-15) which corresponds to the
     * final address, increment the X registers if/as appropriate, but not updating the designator register.
     * Mainly for TRA instruction...
     * @return relative address for the current instruction
     */
    private int getBasicModeBankRegisterIndex(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int xx = (int) _currentInstruction.getX();
        IndexRegister xReg = (xx != 0) ? getExecOrUserXRegister(xx) : null;

        long addend1;
        long addend2 = 0;
        if (_designatorRegister.getBasicModeEnabled()) {
            addend1 = _currentInstruction.getU();
            if (xReg != null) {
                addend2 = xReg.getSignedXM();
            }

            long relativeAddress = Word36.addSimple(addend1, addend2);
            if (relativeAddress == 0777777) { relativeAddress = 0; }
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
                _currentInstruction = _currentInstruction.setXHIU(br._storage.get(wx));
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

            long relativeAddress = Word36.addSimple(addend1, addend2);
            if (relativeAddress == 0777777) { relativeAddress = 0; }
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
     * Also, we presume that we are doing full-word transfers - not partial word.
     * @param grsCheck true if we should check U to see if it is a GRS location
     * @param operands Where we store the resulting operands - the length of this array defines how many operands we retrieve
     * @param forUpdate if true, we do access checks for read and write - otherwise, only for read.
     *                  This helps us shortcut the process of reading operands, then writing them back out.
     * @return DevelopedAddresses object corresponding to the operators we retrieve - this can be used for subsequently storing.
     *              If we return null, then we've retrieved operands from GRS and no absolute addresses apply.
     * @throws MachineInterrupt if an interrupt needs to be raised
     * @throws UnresolvedAddressException if an address is not fully resolved (basic mode indirect address only)
     */
    private DevelopedAddresses getConsecutiveOperands(
        final boolean grsCheck,
        final long[] operands,
        final boolean forUpdate
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Get the relative address so we can do a grsCheck
        int relAddress = calculateRelativeAddressForGRSOrStorage();
        incrementIndexRegisterInF0();

        //  If this is a GRS reference - we do not need to look for containing banks or validate storage limits.
        if ((grsCheck)
            && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
            && (relAddress < 0200)) {
            //  For multiple accesses, advancing beyond GRS 0177 wraps back to zero.
            //  Do accessibility checks for each GRS access
            int grsIndex = relAddress;
            for (int ox = 0; ox < operands.length; ++ox, ++grsIndex) {
                if (grsIndex == 0200) { grsIndex = 0; }

                if (!GeneralRegisterSet.isAccessAllowed(grsIndex, _designatorRegister.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }

                operands[ox] = _generalRegisterSet.getRegister(grsIndex).getW();
            }

            return null;
        }

        //  Get base register and check storage and access limits
        int brIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister bReg = _baseRegisters[brIndex];
        bReg.checkAccessLimits(relAddress, operands.length, true, forUpdate, _indicatorKeyRegister.getAccessInfo());

        //  Generate abs addresses
        AbsoluteAddress[] absAddresses = new AbsoluteAddress[operands.length];
        for (int ax = 0; ax < operands.length; ++ax ) {
            absAddresses[ax] = getAbsoluteAddress(bReg, relAddress + ax);
            checkBreakpoint(BreakpointComparison.Read, absAddresses[ax]);
        }

        //  Retrieve the operands
        int initialOffset = relAddress - bReg._lowerLimitNormalized;
        int offset = initialOffset;
        for (int ox = 0; ox < operands.length; ++ox) {
            operands[ox] = bReg._storage.get(offset++);
        }

        return new DevelopedAddresses(bReg, initialOffset, absAddresses);
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
    private int getExecOrUserARegisterIndex(
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
    private GeneralRegister getExecOrUserRRegister(
        final int registerIndex
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserRRegisterIndex(registerIndex));
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
    private long getImmediateOperand(
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
            IndexRegister xReg = getExecOrUserXRegister((int) _currentInstruction.getX());

            //  24-bit indexing?
            if (!_designatorRegister.getBasicModeEnabled() && (privilege < 2) && exec24Index) {
                //  Add the 24-bit modifier
                value = Word36.addSimple(value, xReg.getXM24());
                if (_currentInstruction.getH() != 0) {
                    setExecOrUserXRegister((int) _currentInstruction.getX(), IndexRegister.incrementModifier24(xReg.getW()));
                }
            } else {
                //  Add the 18-bit modifier
                value = Word36.addSimple(value, xReg.getXM());
                if (_currentInstruction.getH() != 0) {
                    setExecOrUserXRegister((int) _currentInstruction.getX(), IndexRegister.incrementModifier18(xReg.getW()));
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
    private int getJumpOperand(
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
    private long getOperand(
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

        int relAddress = calculateRelativeAddressForGRSOrStorage();

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
    private long getPartialOperand(
        final int jField,
        final boolean quarterWordMode
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage();
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

        //  Got a hardware interrupt during hardware interrupt handling - this is very bad
        if ((interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.HardwareCheck)
            && _designatorRegister.getFaultHandlingInProgress()) {
            stop(StopReason.InterruptHandlerHardwareFailure, 0);
            return;
        }

        // Update interrupt-specific portions of the IKR
        _indicatorKeyRegister.setShortStatusField(interrupt.getShortStatusField());
        _indicatorKeyRegister.setInterruptClassField(interrupt.getInterruptClass().getCode());

        // Make sure the interrupt control stack base register is valid
        if (_baseRegisters[ICS_BASE_REGISTER]._voidFlag) {
            stop(StopReason.ICSBaseRegisterInvalid, 0);
            return;
        }

        // Acquire a stack frame, and verify limits
        IndexRegister icsXReg = (IndexRegister) _generalRegisterSet.getRegister(ICS_INDEX_REGISTER);
        icsXReg = icsXReg.decrementModifier18();
        _generalRegisterSet.setRegister(ICS_INDEX_REGISTER, icsXReg.getW());
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
        icsStorage.set(sx, _preservedProgramAddressRegister.get());
        icsStorage.set(sx + 1, _designatorRegister.getW());
        icsStorage.set(sx + 2, _indicatorKeyRegister.getW());
        icsStorage.set(sx + 3, _quantumTimer & Word36.BIT_MASK);
        icsStorage.set(sx + 4, interrupt.getInterruptStatusWord0().getW());
        icsStorage.set(sx + 5, interrupt.getInterruptStatusWord1().getW());

        //TODO other stuff which needs to be preserved - IP PRM 5.1.3
        //      e.g., results of stuff that we figure out prior to generating U in Basic Mode maybe?
        //      or does it hurt anything to just regenerate that?  We /would/ need the following two lines...
        //pStack[6].setS1( _PreservedProgramAddressRegisterValid ? 1 : 0 );
        //pStack[7].setValue( _PreservedProgramAddressRegister.getW() );

        // Create conditionalJump history table entry
        createJumpHistoryTableEntry(_preservedProgramAddressRegister.get());
        new BankManipulator().bankManipulation(interrupt);
    }

    /**
     * Increments the register indicated by the current instruction (F0) appropriately.
     * Only effective if f.x is non-zero.
     */
    private void incrementIndexRegisterInF0(
    ) {
        if ((_currentInstruction.getX() != 0) && (_currentInstruction.getH() != 0)) {
            IndexRegister iReg = getExecOrUserXRegister((int) _currentInstruction.getX());
            if (!_designatorRegister.getBasicModeEnabled()
                && (_designatorRegister.getExecutive24BitIndexingEnabled())
                && (_designatorRegister.getProcessorPrivilege() < 2)) {
                setExecOrUserXRegister((int) _currentInstruction.getX(), IndexRegister.incrementModifier24(iReg.getW()));
            } else {
                setExecOrUserXRegister((int) _currentInstruction.getX(), IndexRegister.incrementModifier18(iReg.getW()));            }
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
    private boolean incrementOperand(
        final boolean grsCheck,     //TODO do we need this?
        final boolean allowPartial, //TODO do we need this?
        final long incrementValue,
        final boolean twosComplement
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int jField = (int)_currentInstruction.getJ();
        int relAddress = calculateRelativeAddressForGRSOrStorage();

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
                sum += Word36.getTwosComplement(incrementValue);
                if (sum == 0) {
                    result = true;
                }

                _generalRegisterSet.setRegister(relAddress, sum);
                _designatorRegister.setCarry(false);
                _designatorRegister.setOverflow(false);
            } else {
                long sum = reg.getW();
                result = Word36.isZero(sum);
                Word36.StaticAdditionResult sar = Word36.add(sum, incrementValue);
                if (Word36.isZero(sar._value)) {
                    result = true;
                }

                _generalRegisterSet.setRegister(relAddress, sar._value);
                _designatorRegister.setCarry(sar._flags._carry);
                _designatorRegister.setOverflow(sar._flags._overflow);
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
            sum += Word36.getTwosComplement(incrementValue);
            if (sum == 0) {
                result = true;
            }

            _designatorRegister.setCarry(false);
            _designatorRegister.setOverflow(false);
        } else {
            if (Word36.isZero(sum)) {
                result = true;
            }
            Word36.StaticAdditionResult sar = Word36.add(sum, incrementValue);
            if (Word36.isZero(sar._value)) {
                result = true;
            }

            _designatorRegister.setCarry(sar._flags._carry);
            _designatorRegister.setOverflow(sar._flags._overflow);
            sum = sar._value;
        }

        long storageResult = allowPartial ? injectPartialWord(storageValue, sum, jField, qWordMode) : sum;
        baseRegister._storage.set(readOffset, storageResult);
        return result;
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
        final long offset
    ) {
        return !baseRegister._voidFlag
               && (offset >= baseRegister._lowerLimitNormalized)
               && (offset <= baseRegister._upperLimitNormalized);
    }

    /**
     * Stores the given ActiveBaseTableEntry in our internal table for the given base register index
     * @param baseRegisterIndex base register index 0 to 31
     * @param entry new ActiveBaseTableEntry
     */
    public void loadActiveBaseTableEntry(
        final int baseRegisterIndex,
        ActiveBaseTableEntry entry
    ) {
        _activeBaseTableEntries[baseRegisterIndex] = entry;
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
     * Sells a 2-word RCS stack frame and returns the content of said frame as a 2-word long array
     * after verifying the stack is usable and contains at least one entry.
     * @return 2-word RCS entry
     * @throws MachineInterrupt if anything goes wrong
     */
    private long[] rcsPop(
    ) throws MachineInterrupt {
        BaseRegister rcsBReg = _baseRegisters[InstructionProcessor.RCS_BASE_REGISTER];
        if (rcsBReg._voidFlag) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                                                                InstructionProcessor.RCS_BASE_REGISTER,
                                                                0);
        }

        IndexRegister rcsXReg = getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        int framePointer = (int) rcsXReg.getXM() + 2;
        if (framePointer > rcsBReg._upperLimitNormalized) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow,
                                                                InstructionProcessor.RCS_BASE_REGISTER,
                                                                framePointer);
        }
        setExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER,
                               IndexRegister.setXM(rcsXReg.getW(), framePointer));

        int offset = framePointer - rcsBReg._lowerLimitNormalized - 2;
        long[] result = new long[2];
        //  ignore the null-dereference warning in the next line
        result[0] = rcsBReg._storage.get(offset++);
        result[1] = rcsBReg._storage.get(offset);
        return result;
    }

    /**
     * Buys a 2-word RCS stack frame and populates it appropriately
     * @param bField value to be placed in the .B field of the stack frame.
     * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCStack has no more space
     */
    private void rcsPush(
        final int bField
    ) throws RCSGenericStackUnderflowOverflowInterrupt {
        rcsPush(bField, rcsPushCheck());
    }

    /**
     * Buys a 2-word RCS stack frame and populates it appropriately.
     * rcsPushCheck() must be invoked first.
     * @param bField value to be placed in the .B field of the stack frame.
     * @param framePointer where the frame will be stored (retrieved from rcsPushCheck())
     */
    private void rcsPush(
        final int bField,
        final int framePointer
    ) {
        BaseRegister rcsBReg = _baseRegisters[InstructionProcessor.RCS_BASE_REGISTER];
        IndexRegister rcsXReg = getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        setExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER, IndexRegister.setXM(rcsXReg.getW(), framePointer));

        long reentry = (long) _programAddressRegister.getLBDI() << 18;
        reentry |= (_programAddressRegister.getProgramCounter() + 1) & 0777777;

        long state = (bField & 03) << 24;
        state |= _designatorRegister.getW() & 0_000077_000000;
        state |= _indicatorKeyRegister.getAccessKey();

        int offset = framePointer - rcsBReg._lowerLimitNormalized;

        rcsBReg._storage.set(offset++, reentry);
        rcsBReg._storage.set(offset, state);
    }

    /**
     * Buys a 2-word RCS stack frame and populates it with the given data
     * rcsPushCheck() must be invoked first.
     * @param data data to be placed in the frame
     * @param framePointer where the frame will be stored (retrieved from rcsPushCheck())
     */
    private void rcsPush(
        final long[] data,
        final int framePointer
    ) {
        BaseRegister rcsBReg = _baseRegisters[InstructionProcessor.RCS_BASE_REGISTER];
        IndexRegister rcsXReg = getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);
        setExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER, IndexRegister.setXM(rcsXReg.getW(), framePointer));

        int offset = framePointer - rcsBReg._lowerLimitNormalized;

        //  ignore the null-dereference warning in the next line
        rcsBReg._storage.set(offset++, data[0]);
        rcsBReg._storage.set(offset, data[1]);
    }

    /**
     * Checks whether we can buy a 2-word RCS stack frame
     * @return framePointer pointer to the frame which will be the target of the push
     * @throws RCSGenericStackUnderflowOverflowInterrupt if the RCStack has no more space
     */
    private int rcsPushCheck(
    ) throws RCSGenericStackUnderflowOverflowInterrupt {
        // Make sure the return control stack base register is valid
        BaseRegister rcsBReg = _baseRegisters[InstructionProcessor.RCS_BASE_REGISTER];
        if (rcsBReg._voidFlag) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(
                RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                InstructionProcessor.RCS_BASE_REGISTER,
                0);
        }

        IndexRegister rcsXReg = getExecOrUserXRegister(InstructionProcessor.RCS_INDEX_REGISTER);

        int framePointer = (int) rcsXReg.getXM() - 2;
        if (framePointer < rcsBReg._lowerLimitNormalized) {
            throw new RCSGenericStackUnderflowOverflowInterrupt(
                RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow,
                InstructionProcessor.RCS_BASE_REGISTER,
                framePointer);
        }

        return framePointer;
    }

    /**
     * Sets a new value for the GeneralRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either A0 or EA0, depending on the designator register.
     * @param registerIndex A register index of interest
     * @param value new value
     */
    private void setExecOrUserARegister(
        final int registerIndex,
        final long value
    ) {
        _generalRegisterSet.setRegister(getExecOrUserARegisterIndex(registerIndex), value);
    }

    /**
     * Sets a new value for the GeneralRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either R0 or ER0, depending on the designator register.
     * @param registerIndex R register index of interest
     * @param value new value
     */
    private void setExecOrUserRRegister(
        final int registerIndex,
        final long value
    ) {
        _generalRegisterSet.setRegister(getExecOrUserRRegisterIndex(registerIndex), value);
    }

    /**
     * Sets a new value for the IndexRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either X0 or EX0, depending on the designator register.
     * @param registerIndex X register index of interest
     * @param value new value
     */
    private void setExecOrUserXRegister(
        final int registerIndex,
        final long value
    ) {
        _generalRegisterSet.setRegister(getExecOrUserXRegisterIndex(registerIndex), value);
    }

    /**
     * Updates PAR.PC and sets the prevent-increment flag according to the given parameters.
     * Used for simple conditionalJump instructions.
     * @param counter program counter value
     * @param preventIncrement true to set the prevent-increment flag
     */
    private void setProgramCounter(
        final long counter,
        final boolean preventIncrement
    ) {
        _programAddressRegister.setProgramCounter(counter);
        _preventProgramCounterIncrement = preventIncrement;
    }

    /**
     * Convenience wrapper around the following method, for locking a single address
     * @param absAddress absolute address of interest
     */
    private void setStorageLock(
        final AbsoluteAddress absAddress
    ) {
        AbsoluteAddress[] array = { absAddress };
        setStorageLocks(array);
    }

    /**
     * Set a storage lock for the given absolute address.
     * We must not enter a situation where IP0 holds A and is requesting B, while IP1 holds B and is requesting A.
     * To prevent this situation, we take care to write all code such that we never attempt to do the above.
     * To protect ourselves in case of logic errors, we check to see if the requestor already holds at least one
     * lock, and is requesting a lock on something else that is locked by another IP - if we detect that situation
     * we take a debug stop.
     * Otherwise, if the lock we are requesting is held by another IP, we wait until it is not.
     * When we can proceed, we lock the storage and return.
     * NOTE: All storage locks are cleared automatically at the conclusion of processing an instruction.
     * @param absAddresses array of addresses
     */
    private void setStorageLocks(
        final AbsoluteAddress[] absAddresses
    ) {
        boolean weHaveLocks = false;
        synchronized(_storageLocks) {
            weHaveLocks = !_storageLocks.get(this).isEmpty();
        }

        //  Grab the storage lock, and compare the requested absolute addresses against those which are locked.
        //  If any of them are locked, and not by us, bail out, yield control, then try again.
        //  If we need to bail out and we've already got at least one lock, die horribly - this situation should never occur.
        //  Otherwise, lock all the ones not already locked by us and we're done.
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
                                assert(!weHaveLocks);
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

    /**
     * Simple one-liner to effect skipping the next instruction
     */
    private void skipNextInstruction() {
        setProgramCounter(_programAddressRegister.getProgramCounter() + 1, false);
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
    private void storeConsecutiveOperands(
        final boolean grsCheck,
        long[] operands
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Get the first relative address so we can do a grsCheck
        int relAddress = calculateRelativeAddressForGRSOrStorage();

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

            AbsoluteAddress[] absAddresses = new AbsoluteAddress[operands.length];
            for (int ax = 0; ax < operands.length; ++ax ) {
                absAddresses[ax] = getAbsoluteAddress(bReg, relAddress + ax);
                checkBreakpoint(BreakpointComparison.Write, absAddresses[ax]);
            }

            //  Store the operands
            int offset = relAddress - bReg._lowerLimitNormalized;
            for (int ox = 0; ox < operands.length; ++ox) {
                bReg._storage.set(offset++, operands[ox]);
            }

            incrementIndexRegisterInF0();
        }
    }

    /**
     * As above, but for the case where the caller already has absolute addresses which have been access-checked.
     * Not for GRS storage.
     * @param addresses The absolute addresses, corresponding index-for-index with the values in the operands parameter
     * @param operands The operands to be stored
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    private void storeConsecutiveOperands(
        final DevelopedAddresses addresses,
        final long[] operands
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        assert(addresses._addresses.length == operands.length);
        int offset = addresses._offset;
        for (int ix = 0; ix < operands.length; ++ix) {
            AbsoluteAddress addr = addresses._addresses[ix];
            checkBreakpoint(BreakpointComparison.Write, addr);
            addresses._baseRegister._storage.set(offset++, operands[ix]);
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
    private void storeOperand(
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

        int relAddress = calculateRelativeAddressForGRSOrStorage();

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
    private void storePartialOperand(
        final long operand,
        final int jField,
        final boolean quarterWordMode
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage();
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
    private void testAndStore(
        final boolean flag
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage();
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
    //  Async thread entry point and helpful sub-methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Entry point for the async Worker part of this object
     */
    @Override
    public void run(
    ) {
        _isRunning = true;
        EntryMessage em = _logger.traceEntry("worker thread starting");
        synchronized(_storageLocks) {
            _storageLocks.put(this, new HashSet<AbsoluteAddress>());
        }

        _isReady = true;
        while (!_workerTerminate) {
            //  If the virtual processor is not running, then the thread only watches for UPI traffic,
            //  and otherwise sleeps slowly, waiting for a notify() which would indicate something needs done.
            if (isStopped()) {
                if (!runCheckUPI()) {
                    try {
                        synchronized (this) { wait(100); }
                    } catch (InterruptedException ex) {
                        _logger.catching(ex);
                    }
                }
            } else {
                //  This is the algorithm we execute when the processor is 'running'.
                //  Check for UPI traffic - if none...
                //  Check for pending machine interrupts - if none...
                //  Execute (or continue executing) the current instruction.
                //  If there is nothing at all to do, sleep a little bit.
                boolean somethingDone = runCheckUPI()
                                        || runCheckPendingInterruptConditions()
                                        || runCheckPendingInterrupts();
                if (!somethingDone) {
                    runCurrentInstruction();

                    // End of the cycle - should we stop?
                    if (_currentRunMode == RunMode.SingleCycle) {
                        stop(StopReason.Debug, 0);
                    }
                }
            }
        }

        synchronized(_storageLocks) {
            _storageLocks.remove(this);
        }

        _logger.traceExit(em);
        _isReady = false;
        _isRunning = false;
    }

    /**
     * Check certain conditions to see if one of several certain interrupt classes needs to be raised.
     * @return true if we did something useful, else false
     */
    private boolean runCheckPendingInterruptConditions(
    ) {
        if (_indicatorKeyRegister.getBreakpointRegisterMatchCondition() && !_midInstructionInterruptPoint) {
            if (_breakpointRegister._haltFlag) {
                stop(StopReason.Breakpoint, 0);
            } else {
                raiseInterrupt(new BreakpointInterrupt());
            }

            return true;
        }

        if ((_quantumTimer < 0) && _designatorRegister.getQuantumTimerEnabled()) {
            raiseInterrupt(new QuantumTimerInterrupt());
            return true;
        }

        if (_indicatorKeyRegister.getSoftwareBreak() && !_midInstructionInterruptPoint) {
            raiseInterrupt(new SoftwareBreakInterrupt());
            return true;
        }

        if (_jumpHistoryThresholdReached && _jumpHistoryFullInterruptEnabled && !_midInstructionInterruptPoint) {
            raiseInterrupt(new JumpHistoryFullInterrupt());
            return true;
        }

        return false;
    }

    /**
     * If an interrupt is pending, handle it.
     * If not, check certain conditions to see if one of several certain interrupt classes needs to be raised.
     * @return true if we did something useful, else false
     */
    private boolean runCheckPendingInterrupts(
    ) {
        //  Is there an interrupt pending?  If so, handle it
        if (_pendingInterrupt != null) {
            try {
                handleInterrupt();
            } catch (MachineInterrupt interrupt) {
                raiseInterrupt(interrupt);
            }
            return true;
        }

        return false;
    }

    /**
     * Checks UPI containers to see if we need to respond to any UPI traffic
     * @return true if we found something to do, else false
     */
    private boolean runCheckUPI() {
        boolean result = false;
        synchronized (_upiPendingAcknowledgements) {
            for (Processor ackSource : _upiPendingAcknowledgements) {
                if (_pendingUPISends.remove(ackSource)) {
                    result = true;
                } else {
                    _logger.error(String.format("Got unexpected ACK from %s", ackSource._name));
                }
            }
        }

        synchronized (_upiPendingInterrupts) {
            if (!_upiPendingInterrupts.isEmpty()) {
                for (Processor sendSource : _upiPendingInterrupts) {
                    switch (sendSource._Type) {
                        case InputOutputProcessor:
                            //  UPI Normal (IO completed)
                            //  Ensure we are running, and raise a class 31 interrupt.
                            //TODO - todo what?
                            if (isStopped()) {
                                _logger.error(String.format("Got a UPI SEND from %s while stopped", sendSource._name));
                            } else {
                                raiseInterrupt(new UPINormalInterrupt(MachineInterrupt.Synchrony.Broadcast, 0));
                            }
                            break;

                        case InstructionProcessor:
                            //  UPI Initial
                            //  Ensure we are stopped, raise a class 30 interrupt, and start.
                            //  TODO (how do we get the ICS and L0 BDT information?  It's a mystery...
                            if (isStopped()) {
                                raiseInterrupt(new UPIInitialInterrupt());
                                start();
                            } else {
                                _logger.error(String.format("Got a UPI SEND from %s while running", sendSource._name));
                            }
                            break;

                        case SystemProcessor:
                            //  Initial Program Load
                            //  Ensure we are stopped, raise a class 29 interrupt, and start.
                            if (isStopped()) {
                                raiseInterrupt(new InitialProgramLoadInterrupt());
                                start();
                            } else {
                                _logger.error(String.format("Got a UPI SEND from %s while running", sendSource._name));
                            }
                            break;

                        default:                    //  should never happen
                            _logger.error(String.format("Got unexpected UPI interrupt from %s", sendSource._name));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Execute (or continue executing) the instruction in F0.
     */
    private void runCurrentInstruction() {
        try {
            //  If we don't have an instruction in F0, fetch one.
            if (!_indicatorKeyRegister.getInstructionInF0()) {
                fetchInstruction();
            }

            //  Execute the instruction in F0.
            _midInstructionInterruptPoint = false;
            try {
                executeInstruction();
            } catch (UnresolvedAddressException ex) {
                //  This is not surprising - can happen for basic mode indirect addressing.
                //  Update the quantum timer so we can (eventually) interrupt a long or infinite sequence.
                _midInstructionInterruptPoint = true;
                if (_designatorRegister.getQuantumTimerEnabled()) {
                    --_quantumTimer;
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
                    _quantumTimer -= _currentInstructionHandler.getQuantumTimerCharge();
                }

                // Should we stop, given that we've completed an instruction?
                if (_currentRunMode == RunMode.SingleInstruction) {
                    stop(StopReason.Debug, 0);
                }
            }
        } catch (MachineInterrupt interrupt) {
            raiseInterrupt(interrupt);
        }
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
     * Clears the IP
     */
    @Override
    public void clear() {
        EntryMessage em = _logger.traceEntry("clear()");

        if (!isStopped()) {
            stop(StopReason.Cleared, 0);
            while (!isStopped()) {
                Thread.onSpinWait();
            }
        }

        _designatorRegister.clear();
        _programAddressRegister.set(0);

        for (int brx = 0; brx < 32; ++brx) {
            _baseRegisters[brx] = new BaseRegister();
        }

        for (int abx = 0; abx < 15; ++abx) {
            _activeBaseTableEntries[abx] = new ActiveBaseTableEntry(0, 0, 0);
        }

        _pendingUPISends.clear();
        super.clear();
        _logger.traceExit(em);
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
            _logger.catching(ex);
        }
    }

    /**
     * For debugging (eventually we should move this do dump(), above)
     */
    public void logState() {
        _logger.trace("General Register Set:");
        for (int grx = 0; grx < 128; grx += 8) {
            StringBuilder sb = new StringBuilder();
            for (int gry = 0; gry < 8; ++gry) {
                long value = _generalRegisterSet.getRegister(grx + gry).getW();
                sb.append(String.format("%012o ", value));
            }
            _logger.trace(String.format("  %06o  %s", grx, sb.toString()));
        }

        _logger.trace("Active Base Table:");
        for (int abx = 0; abx < 15; ++abx) {
            _logger.trace(String.format("  For B%d: %012o", abx + 1, _activeBaseTableEntries[abx]._value));
        }

        _logger.trace("Base Registers:");
        for (int brx = 0; brx < 32; ++brx) {
            BaseRegister br = _baseRegisters[brx];
            _logger.trace(String.format("  B%02d: %s", brx, br.toString()));
        }
    }

    /**
     * Starts the processor.
     * Since the worker thread is always running, this merely wakes it up so that it can resume instruction processing.
     * Intended to be used only when the processor is cleared. This is to be used for IPL purposes, so we need to
     * raise a class 29 interrupt.  It is up to the invoking entity to ensure the ICS base and index registers are
     * properly initialized, along with the Level 0 BDT bank register.
     */
    public boolean start() {
        EntryMessage em = _logger.traceEntry("start()");

        boolean result = false;
        synchronized(this) {
            if (isStopped()) {
                try {
                    InventoryManager im = InventoryManager.getInstance();
                    _systemProcessor = im.getSystemProcessor(InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
                    _preservedProgramAddressRegister.set(_programAddressRegister.get());
//TODO remove this                    raiseInterrupt(new InitialProgramLoadInterrupt());
                    _currentRunMode = RunMode.Normal;
                    this.notify();
                    result = true;
                } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
                    _logger.catching(ex);
                }
            }
        }

        _logger.traceExit(em, result);
        return result;
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
        EntryMessage em = _logger.traceEntry("stop()");

        synchronized(this) {
            if (!isStopped()) {
                _latestStopReason = stopReason;
                _latestStopDetail = detail;
                _currentRunMode = RunMode.Stopped;
                _logger.error(String.format("Stopping:%s Detail:%o",
                                           stopReason.toString(),
                                           _latestStopDetail));
                this.notify();
            }
        }

        _logger.traceExit(em);
    }
}
