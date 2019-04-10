/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.IndexRegister;
import com.kadware.em2200.baselib.Word36Array;
import com.kadware.em2200.baselib.Word36ArraySlice;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.AbsoluteAddress;
import com.kadware.em2200.hardwarelib.misc.AccessInfo;
import com.kadware.em2200.hardwarelib.misc.AccessPermissions;
import com.kadware.em2200.hardwarelib.misc.BankDescriptor;
import com.kadware.em2200.hardwarelib.misc.BaseRegister;
import com.kadware.em2200.hardwarelib.misc.VirtualAddress;

/**
 * Base class for all Test_InstructionProcessor_* classes
 */
public class Test_InstructionProcessor {

    protected static class LoadBankInfo {

        public long[] _source;
        public AccessInfo _accessInfo = new AccessInfo((byte)0, (short)0);
        public AccessPermissions _generalAccessPermissions = ALL_ACCESS;
        public AccessPermissions _specialAccessPermissions = ALL_ACCESS;
        public int _lowerLimit = 0;

        public LoadBankInfo(
            final long[] source
        ) {
            _source = source;
        }
    }

    private static final AccessPermissions ALL_ACCESS = new AccessPermissions(true, true, true);

    /**
     * Retrieves the contents of a bank represented by a base register
     * <p>
     * @param ip
     * @param baseRegisterIndex
     * <p>
     * @return
     */
    public static long[] getBank(
        final InstructionProcessor ip,
        final int baseRegisterIndex
    ) {
        Word36Array array = ip.getBaseRegister(baseRegisterIndex).getStorage();
        long[] result = new long[array.getArraySize()];
        for (int ax = 0; ax < array.getArraySize(); ++ax) {
            result[ax] = array.getValue(ax);
        }
        return result;
    }

    /**
     * Given an array of LoadBankInfo objects, we load the described data as individual banks, into consecutive locations
     * into the given MainStorageProcessor.  For each bank, we create a BankRegister and establish that bank register
     * of the given InstructionProcessor as B0, B1, ...
     * <p>
     * @param ip
     * @param msp
     * @param brIndex first base register index to be loaded (usually 0 for extended mode, 12 for basic mode)
     * @param bankInfos
     */
    public static void loadBanks(
        final InstructionProcessor ip,
        final MainStorageProcessor msp,
        final int brIndex,
        final LoadBankInfo[] bankInfos
    ) {
        Word36Array storage = msp.getStorage();
        short mspUpi = msp.getUPI();

        int mspOffset = 0;
        for (int sx = 0; sx < bankInfos.length; ++sx) {
            storage.load(mspOffset, bankInfos[sx]._source);
            AbsoluteAddress absoluteAddress = new AbsoluteAddress(mspUpi, mspOffset);
            Word36ArraySlice storageSubset = new Word36ArraySlice(storage, mspOffset, bankInfos[sx]._source.length);

            BaseRegister bReg = new BaseRegister(absoluteAddress,
                                                 false,
                                                 bankInfos[sx]._lowerLimit,
                                                 bankInfos[sx]._lowerLimit + bankInfos[sx]._source.length - 1,
                                                 bankInfos[sx]._accessInfo,
                                                 bankInfos[sx]._generalAccessPermissions,
                                                 bankInfos[sx]._specialAccessPermissions,
                                                 storageSubset);
            ip.setBaseRegister(brIndex + sx, bReg);
            mspOffset += bankInfos[sx]._source.length;
            System.out.println(String.format("Loaded Bank B%d Abs=%s llNorm=0%o ulNorm=0%o",
                                             brIndex + sx,
                                             absoluteAddress,
                                             bReg.getLowerLimitNormalized(),
                                             bReg.getUpperLimitNormalized()));
        }
    }

    /**
     * Given an array of long arrays, we treat each of the long arrays as a source of 36-bit values (wrapped in longs).
     * Each of the arrays of longs represents data which is to be stored consecutively into an area of memory and which will
     * subsequently be considered a bank.  Each successive source array loaded into unique non-overlapping areas of
     * storage in the given MainStorageProcessor, and for each, a BankRegister is created and established in the given
     * InstructionProcessor at B0, B1, etc..
     * <p>
     * @param ip
     * @param msp
     * @param brIndex first base register index to be loaded (usually 0 for extended mode, 12 for basic mode)
     * @param sourceData
     */
    public static void loadBanks(
        final InstructionProcessor ip,
        final MainStorageProcessor msp,
        final int brIndex,
        final long sourceData[][]
    ) {
        LoadBankInfo bankInfos[] = new LoadBankInfo[sourceData.length];
        for (int sx = 0; sx < sourceData.length; ++sx) {
            bankInfos[sx] = new LoadBankInfo(sourceData[sx]);
        }

        loadBanks(ip, msp, brIndex, bankInfos);
    }

    /**
     * Sets up the interrupt environment at the given offset in the given MSP, and adjusts the given IP's registers
     * accordingly.  A number of structures are created in contiguous memory beginning at mspOffset, as such:
     *  +0      Level 0 Bank Descriptor Table (which we create)
     *              This table is indexed by BDI for virtual addresses which have an L-value of zero.
     *              Each entry is an 8-word bank descriptor (see hardware manual)
     *              However, the first 64 words of this table comprise the interrupt vector table, consisting of
     *              64 contiguous words indexed by interrupt class.  Each word is the L,BDI,Offset of the code
     *              which handles the particular interrupt.
     *              L,BDI of 0,0 through 0,31 do not refer to actual banks (for architectural reasons or something),
     *              so these first 64 words do not conflict with any BD's.
     *              Since we are building all of this up from scratch, we'll go ahead an assign L,BDI of 0,32 to be
     *              the bank which contains all the interrupt handling code, so this entire table will be 33 * 8 words
     *              in length.  We'll also assign L,BDI of 0,33 to be the bank which contains the interrupt control stack.
     *  +n      Interrupt handling code bank - size is determined by the content of the arrays in interruptCode.
     *  +n+m    Interrupt Control Stack - we'll set this up with a particular stack frame size, with a total maximum size
     *              which will allow for some few nested interrupts.
     *
     * @param ip the IP which will have its various registers set appropriately to account for the created environment
     * @param msp the MSP in which we'll create the environment
     * @param mspOffset the offset from the beginning of MSP storage where we create the environment
     * @param interruptCode an array of 64 pointers to code arrays - each code array contains the instructions necessary for
     *                      handling a particular interrupt.  Each code array is indexed (by the major index) by the class
     *                      of the interrupt which the code handles.  If any major reference is null, it is assumed that the
     *                      caller has no preference for handling the particular interrupt, and we will set the interrupt vectort
     *                      to point to a generic interrupt handler that we will create here.
     * <p>
     * @return size of allocated memory in the MSP, starting at mspOffset
     * <p>
     * @throws MachineInterrupt
     */
    public int setupInterrupts(
        final InstructionProcessor ip,
        final MainStorageProcessor msp,
        final int mspOffset,
        final long interruptCode[][]
    ) throws MachineInterrupt {
        //  Stake out slices of the level 0 bank descriptor table
        Word36ArraySlice interruptVector = new Word36ArraySlice(msp.getStorage(), mspOffset, 64);
        BankDescriptor ihBankDescriptor = new BankDescriptor(msp.getStorage(), 8 * mspOffset);
        BankDescriptor icsBankDescriptor = new BankDescriptor(msp.getStorage(), 8 * (mspOffset + 1));
        int bdtSize = interruptVector.getArraySize() + ihBankDescriptor.getArraySize() + icsBankDescriptor.getArraySize();

        int ihCodeMSPOffset = mspOffset + bdtSize;  //  This is where the interrupt handler code bank is found,
                                                    //      relative to the start of MSP storage
        int ihCodeSize = 0;                         //  This is the size of the interrupt handler code bank (so far)

        //  Iterate over the provided interrupt code arrays
        for (int ihx = 0; ihx < 63; ++ihx) {
            //  If the given code sequence is a null reference, or if we've run over the end of the array,
            //  set the corresponding interrupt vector L,BDI,Offset to all zero.  Eventually, we want to handle this
            //  by pointing the corrdsponding vector to some fixed default interrupt handling code which stores something
            //  and then returns.  However, we don't yet have mechanisms to allow this, so for now...  //????
            if ((ihx >= interruptCode.length) || (interruptCode[ihx] == null)) {
                interruptVector.setValue(ihx, 0);
            } else {
                //  The caller actually gave us a code sequence for this interrupt.
                //  Set the interrupt vector for this code, starting immediately following the previous handler
                //  (i.e., at bank offset ihCodeSize).
                VirtualAddress vector = new VirtualAddress((byte)0, (short)0, ihCodeSize);
                interruptVector.setValue(ihx, vector.getW());

                //  Copy the code to the MSP
                for (int cx = 0; cx < interruptCode[ihx].length; ++cx) {
                    msp.getStorage().setValue(ihCodeMSPOffset + ihCodeSize, interruptCode[ihx][cx]);
                    ++ihCodeSize;
                }
            }
        }

        //  Create a slice for the interrupt handler code bank in storage
        Word36ArraySlice ihSlice = new Word36ArraySlice(msp.getStorage(), ihCodeMSPOffset, ihCodeSize);

        //  Set up the interrupt handler code bank descriptor
        ihBankDescriptor.setBankType(BankDescriptor.BankType.ExtendedMode);
        ihBankDescriptor.setBaseAddress(new AbsoluteAddress(msp.getUPI(), ihCodeMSPOffset));
        ihBankDescriptor.setGeneralAccessPermissions(ALL_ACCESS);
        ihBankDescriptor.setGeneralFault(false);
        ihBankDescriptor.setLargeBank(false);
        ihBankDescriptor.setLowerLimit(0);
        ihBankDescriptor.setSpecialAccessPermissions(ALL_ACCESS);
        ihBankDescriptor.setUpperLimit(ihCodeSize - 1);
        ihBankDescriptor.setUpperLimitSuppressionControl(false);

        //  Set up the base register for the level 0 BDT
        BaseRegister ihBaseRegister = new BaseRegister(ihBankDescriptor.getBaseAddress(),
                                                       false,
                                                       ihBankDescriptor.getLowerLimitNormalized(),
                                                       ihBankDescriptor.getUpperLimitNormalized(),
                                                       ihBankDescriptor.getAccessLock(),
                                                       ihBankDescriptor.getGeneraAccessPermissions(),
                                                       ihBankDescriptor.getSpecialAccessPermissions(),
                                                       ihSlice);
        ip.setBaseRegister(InstructionProcessor.L0_BDT_BASE_REGISTER, ihBaseRegister);

        //  Set up the interrupt control stack bank descriptor
        int icsMSPOffset = ihCodeMSPOffset + ihCodeSize;
        int icsFrameSize = 16;
        int icsEntries = 8;
        int icsStackSize = icsEntries * icsFrameSize;
        icsBankDescriptor.setBankType(BankDescriptor.BankType.ExtendedMode);
        icsBankDescriptor.setBaseAddress(new AbsoluteAddress(msp.getUPI(), icsMSPOffset));
        icsBankDescriptor.setGeneralAccessPermissions(ALL_ACCESS);
        icsBankDescriptor.setGeneralFault(false);
        icsBankDescriptor.setLargeBank(false);
        icsBankDescriptor.setLowerLimit(0);
        icsBankDescriptor.setSpecialAccessPermissions(ALL_ACCESS);
        //???? check the setUpperLimit() ... I think it might be wonky
        icsBankDescriptor.setUpperLimit(icsStackSize - 1);
        icsBankDescriptor.setUpperLimitSuppressionControl(false);

        //  Need a slice object for the ICS
        Word36ArraySlice icsSlice = new Word36ArraySlice(msp.getStorage(), icsMSPOffset, icsStackSize);

        //  Set up the Interrupt Control Stack registers on the IP
        BaseRegister icsBaseRegister = new BaseRegister(icsBankDescriptor.getBaseAddress(),
                                                        false,
                                                        icsBankDescriptor.getLowerLimitNormalized(),
                                                        icsBankDescriptor.getUpperLimitNormalized(),
                                                        icsBankDescriptor.getAccessLock(),
                                                        icsBankDescriptor.getGeneraAccessPermissions(),
                                                        icsBankDescriptor.getSpecialAccessPermissions(),
                                                        icsSlice);
        ip.setBaseRegister(InstructionProcessor.ICS_BASE_REGISTER, icsBaseRegister);

        IndexRegister icsIndexRegister = new IndexRegister();
        icsIndexRegister.setXI(icsFrameSize);
        icsIndexRegister.setXM(icsStackSize);
        ip.setGeneralRegister(InstructionProcessor.ICS_INDEX_REGISTER, icsIndexRegister.getW());

        return bdtSize + ihCodeSize + icsStackSize;
    }

    /**
     * Starts an IP and waits for it to stop on its own
     * <p>
     * @param ip
     */
    public static void startAndWait(
        final InstructionProcessor ip
    ) {
        ip.start();
        while (ip.getRunningFlag()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                //  do nothing
            }
        }
    }
}
