/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for all Test_InstructionProcessor_* classes
 */
class Test_InstructionProcessor {

    static class LoadBankInfo {

        long[] _source;
        AccessInfo _accessInfo = new AccessInfo((byte)0, (short)0);
        AccessPermissions _generalAccessPermissions = ALL_ACCESS;
        AccessPermissions _specialAccessPermissions = ALL_ACCESS;
        int _lowerLimit = 0;

        LoadBankInfo(
            final long[] source
        ) {
            _source = source;
        }
    }

    private static final AccessPermissions ALL_ACCESS = new AccessPermissions(true, true, true);

    /**
     * Assembles sets of code into a relocatable module, then links it such that the odd-numbered lc pools
     * are placed in an IBANK with BDI 04 and the even-number pools in a DBANK with BDI 05.
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeBasic(
        final String[] code,
        final boolean display
    ) {
        Assembler asm = new Assembler(code, "TEST");
        RelocatableModule relModule = asm.assemble(display);
        List<Linker.LCPoolSpecification> poolSpecsEven = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecsOdd = new LinkedList<>();
        for (Integer lcIndex : relModule._storage.keySet()) {
            if ((lcIndex & 01) == 01) {
                Linker.LCPoolSpecification oddPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
                poolSpecsOdd.add(oddPoolSpec);
            } else {
                Linker.LCPoolSpecification evenPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
                poolSpecsEven.add(evenPoolSpec);
            }
        }

        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("I1")
                                     .setBankDescriptorIndex(000004)
                                     .setBankLevel(0)
                                     .setStartingAddress(022000)
                                     .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(12)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D1")
                                     .setBankDescriptorIndex(000005)
                                     .setBankLevel(0)
                                     .setStartingAddress(040000)
                                     .setPoolSpecifications(poolSpecsEven.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(13)
                                     .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                     .build());

        Linker linker = new Linker(bankDeclarations.toArray(new Linker.BankDeclaration[0]));
        return linker.link("TEST", display);
    }

    /**
     * Assembles multiple codesets into as a sequence of relocatable modules.
     * Odd numbered lc pools are placed into ibank I1 BDI=000004 based on B12
     * Even numbered lc pools are placed into dbank D1 BDI=000005 based on B13
     * @param code code to be assembled
     * @param display true to display assembler and linker output
     * @return absolute module
     */
    static AbsoluteModule buildCodeBasic(
        final String[][] code,
        final boolean display
    ) {
        List<RelocatableModule> relocatableModules = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecsEven = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecsOdd = new LinkedList<>();
        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();

        for (String[] codeSet : code) {
            String moduleName = String.format("TEST%d", relocatableModules.size() + 1);
            Assembler a = new Assembler(codeSet, moduleName);
            RelocatableModule relModule = a.assemble(display);
            relocatableModules.add(relModule);

            for (Integer lcIndex : relModule._storage.keySet()) {
                if ((lcIndex & 01) == 01) {
                    Linker.LCPoolSpecification oddPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
                    poolSpecsOdd.add(oddPoolSpec);
                } else {
                    Linker.LCPoolSpecification evenPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
                    poolSpecsEven.add(evenPoolSpec);
                }
            }
        }

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("I1")
                                     .setBankDescriptorIndex(000004)
                                     .setBankLevel(0)
                                     .setStartingAddress(022000)
                                     .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(12)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        if (!poolSpecsEven.isEmpty()) {
            bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                         .setBankName("D1")
                                         .setBankDescriptorIndex(000005)
                                         .setBankLevel(0)
                                         .setStartingAddress(040000)
                                         .setPoolSpecifications(poolSpecsEven.toArray(new Linker.LCPoolSpecification[0]))
                                         .setInitialBaseRegister(13)
                                         .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                         .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                         .build());
        }

        Linker linker = new Linker(bankDeclarations.toArray(new Linker.BankDeclaration[0]));
        return linker.link("TEST", display);
    }

    /**
     * Assembles source code into a relocatable module, then links it, producing four banks containing:
     *  BDI 04 IBANK:   LC pool 1 - base register 12
     *  BDI 05 DBANK:   LC pool 0 - base register 13
     *  BDI 06 IBANK:   All other odd location counter pools - base register 14
     *  BDI 07 DBANK:   All other even location counter pools - base register 15
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeBasicMultibank(
        final String[] code,
        final boolean display
    ) {
        Assembler asm = new Assembler(code, "TEST");
        RelocatableModule relModule = asm.assemble(display);
        List<Linker.LCPoolSpecification> poolSpecs04 = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecs05 = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecs06 = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecs07 = new LinkedList<>();
        for (Integer lcIndex : relModule._storage.keySet()) {
            Linker.LCPoolSpecification poolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
            if (lcIndex == 0) {
                poolSpecs05.add(poolSpec);
            } else if (lcIndex == 1) {
                poolSpecs04.add(poolSpec);
            } else if ((lcIndex & 01) == 01) {
                poolSpecs06.add(poolSpec);
            } else {
                poolSpecs07.add(poolSpec);
            }
        }

        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("I1")
                                     .setBankDescriptorIndex(000004)
                                     .setBankLevel(0)
                                     .setStartingAddress(01000)
                                     .setPoolSpecifications(poolSpecs04.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(12)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D1")
                                     .setBankDescriptorIndex(000005)
                                     .setBankLevel(0)
                                     .setStartingAddress(040000)
                                     .setPoolSpecifications(poolSpecs05.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(13)
                                     .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("I2")
                                     .setBankDescriptorIndex(000006)
                                     .setBankLevel(0)
                                     .setStartingAddress(020000)
                                     .setPoolSpecifications(poolSpecs06.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(14)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D2")
                                     .setBankDescriptorIndex(000007)
                                     .setBankLevel(0)
                                     .setStartingAddress(060000)
                                     .setPoolSpecifications(poolSpecs07.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(15)
                                     .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                     .build());

        Linker linker = new Linker(bankDeclarations.toArray(new Linker.BankDeclaration[0]));
        return linker.link("TEST", display);
    }

    /**
     * Assembles sets of code into a relocatable module, then links it such that the odd-numbered lc pools
     * are placed in an IBANK with BDI 04 and the even-number pools in a DBANK with BDI 05.
     * Initial base registers will be 0 for instructions and 1 for data.
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeExtended(
        final String[] code,
        final boolean display
    ) {
        Assembler asm = new Assembler(code, "TEST");
        RelocatableModule relModule = asm.assemble(display);
        List<Linker.LCPoolSpecification> poolSpecsEven = new LinkedList<>();
        List<Linker.LCPoolSpecification> poolSpecsOdd = new LinkedList<>();
        for (Integer lcIndex : relModule._storage.keySet()) {
            if ((lcIndex & 01) == 01) {
                Linker.LCPoolSpecification oddPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
                poolSpecsOdd.add(oddPoolSpec);
            } else {
                Linker.LCPoolSpecification evenPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
                poolSpecsEven.add(evenPoolSpec);
            }
        }

        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("I1")
                                     .setBankDescriptorIndex(000004)
                                     .setBankLevel(0)
                                     .setIsExtended(true)
                                     .setStartingAddress(01000)
                                     .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(0)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D1")
                                     .setBankDescriptorIndex(000005)
                                     .setBankLevel(0)
                                     .setIsExtended(true)
                                     .setStartingAddress(01000)
                                     .setPoolSpecifications(poolSpecsEven.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(1)
                                     .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                     .build());

        Linker linker = new Linker(bankDeclarations.toArray(new Linker.BankDeclaration[0]));
        return linker.link("TEST", display);
    }

    /**
     * Assembles source code into a relocatable module, then links it, producing multiple banks where:
     *  BDI 000004 contains all odd-numbered lc pools, based on B0
     *  Each unique even-numbered lc pool generates a unique bank BDI >= 5, based on B1 and up
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeExtendedMultibank(
            final String[] code,
            final boolean display
    ) {
        Assembler asm = new Assembler(code, "TEST");
        RelocatableModule relModule = asm.assemble(display);
        Map<Integer, List<Linker.LCPoolSpecification>> poolSpecMap = new HashMap<>(); //  keyed by BDI
        int nextDBankBDI = 05;
        for (Integer lcIndex : relModule._storage.keySet()) {
            Linker.LCPoolSpecification poolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
            if ((lcIndex & 01) == 01) {
                if (!poolSpecMap.containsKey(4)) {
                    poolSpecMap.put(4, new LinkedList<Linker.LCPoolSpecification>());
                }
                poolSpecMap.get(4).add(poolSpec);
            } else {
                poolSpecMap.put(nextDBankBDI, new LinkedList<Linker.LCPoolSpecification>());
                poolSpecMap.get(nextDBankBDI).add(poolSpec);
                ++nextDBankBDI;
            }
        }

        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
        int bReg = 0;
        for (Map.Entry<Integer, List<Linker.LCPoolSpecification>> entry : poolSpecMap.entrySet()) {
            int bdi = entry.getKey();
            List<Linker.LCPoolSpecification> poolSpecs = entry.getValue();
            bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                         .setBankName(String.format("BANK%06o", bdi))
                                         .setBankDescriptorIndex(bdi)
                                         .setBankLevel(0)
                                         .setStartingAddress(01000)
                                         .setPoolSpecifications(poolSpecs.toArray(new Linker.LCPoolSpecification[0]))
                                         .setInitialBaseRegister(bReg++)
                                         .setGeneralAccessPermissions(new AccessPermissions(bdi == 04, true, true))
                                         .setSpecialAccessPermissions(new AccessPermissions(bdi == 04, true, true))
                                         .build());
        }

        Linker linker = new Linker(bankDeclarations.toArray(new Linker.BankDeclaration[0]));
        return linker.link("TEST", display);
    }

    /**
     * Sets up the interrupt handling environment
     * @param ip the IP which will have its various registers set appropriately to account for the created environment
     * @param msp the MSP in which we'll create the environment
     * @param mspOffset the offset from the beginning of MSP storage where we create the environment
     */
    void establishInterruptEnvironment(
            final InstructionProcessor ip,
            final MainStorageProcessor msp,
            final int mspOffset
    ) throws MachineInterrupt {
        String[] code = {
                "          $EXTEND",
                "$(0)      . Interrupt handler - Reserved, Hardware Default",
                "          HALT      0",
                "",
                "$(1)      . Interrupt handler - Hardware Check",
                "          HALT      0",
                "",
                "$(2)      . Interrupt handler - Diagnostic",
                "          HALT      0",
                "",
                "$(8)      . Interrupt handler - Reference Violation",
                "          HALT      0",
                "",
                "$(9)      . Interrupt handler - Addressing Exception",
                "          HALT      0",
                "",
                "$(10)     . Interrupt handler - Terminal Addressing Exception",
                "          HALT      0",
                "",
                "$(11)     . Interrupt handler - RCS Generic Stack Under/Over Flow",
                "          HALT      0",
                "",
                "$(12)     . Interrupt handler - Signal",
                "          HALT      0",
                "",
                "$(13)     . Interrupt handler - Test And Set",
                "          HALT      0",
                "",
                "$(14)     . Interrupt handler - Invalid Instruction",
                "          HALT      01000+14",
                "",
                "$(15)     . Interrupt handler - Page Exception",
                "          HALT      0",
                "",
                "$(16)     . Interrupt handler - Arithmetic Exception",
                "          HALT      0",
                "",
                "$(17)     . Interrupt handler - Data Exception",
                "          HALT      0",
                "",
                "$(18)     . Interrupt handler - Operation Trap",
                "          HALT      0",
                "",
                "$(19)     . Interrupt handler - Breakpoint",
                "          HALT      0",
                "",
                "$(20)     . Interrupt handler - Quantum Timer",
                "          HALT      0",
                "",
                "$(23)     . Interrupt handler - Page(s) Zeroed",
                "          HALT      0",
                "",
                "$(24)     . Interrupt handler - Software Break",
                "          HALT      0",
                "",
                "$(25)     . Interrupt handler - Jump History Full",
                "          HALT      0",
                "",
                "$(27)     . Interrupt handler - Dayclock",
                "          HALT      0",
                "",
                "$(28)     . Interrupt handler - Performance Monitoring",
                "          HALT      0",
                "",
                "$(29)     . Interrupt handler - IPL",
                "          HALT      0",
                "",
                "$(30)     . Interrupt handler - UPI Initial",
                "          HALT      0",
                "",
                "$(31)     . Interrupt handler - UPI Normal",
                "          HALT      0",
        };

        Assembler asm = new Assembler(code, "IH");
        RelocatableModule relModule = asm.assemble(true);
        List<Linker.LCPoolSpecification> poolSpecs = new LinkedList<>();
        for (Integer lcIndex : relModule._storage.keySet()) {
            Linker.LCPoolSpecification poolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
            poolSpecs.add(poolSpec);
        }

        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("IH")
                                     .setBankDescriptorIndex(000004)
                                     .setBankLevel(0)
                                     .setIsExtended(true)
                                     .setStartingAddress(01000)
                                     .setPoolSpecifications(poolSpecs.toArray(new Linker.LCPoolSpecification[0]))
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        Linker linker = new Linker(bankDeclarations.toArray(new Linker.BankDeclaration[0]));
        AbsoluteModule module = linker.link("IH", true);
        setupInterrupts(ip, msp, mspOffset, module);
    }

    /**
     * Retrieves the contents of a bank represented by a base register
     * @param ip reference to IP containing the desired BR
     * @param baseRegisterIndex index of the desired BR
     * @return reference to array of values constituting the bank
     */
    static long[] getBank(
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
     * Loads the various banks from the given absolute element, into the given MSP
     * and applies initial base registers for the given IP as appropriate.
     * @param ip instruction processor of interest
     * @param msp main storage processor of interest
     * @param module absolute module to be loaded
     */
    static void loadBanks(
            final InstructionProcessor ip,
            final MainStorageProcessor msp,
            final AbsoluteModule module
    ) {
        Word36Array storage = msp.getStorage();
        short mspUpi = msp.getUPI();

        int mspOffset = 0;
        for (LoadableBank loadableBank : module._loadableBanks.values()) {
            storage.load(mspOffset, loadableBank._content);
            AbsoluteAddress absoluteAddress = new AbsoluteAddress(mspUpi, mspOffset);
            System.out.println(String.format("Loaded Bank %s BDI=%06o Starting Address=%06o Length=%06o at Absolute Address=%012o",
                                             loadableBank._bankName,
                                             loadableBank._bankDescriptorIndex,
                                             loadableBank._startingAddress,
                                             loadableBank._content.getArraySize(),
                                             absoluteAddress._offset));

            if (loadableBank._initialBaseRegister != null) {
                Word36ArraySlice storageSubset = new Word36ArraySlice(storage, mspOffset, loadableBank._content.getArraySize());
                int bankLower = loadableBank._startingAddress;
                int bankUpper = loadableBank._startingAddress + loadableBank._content.getArraySize() - 1;
                BaseRegister bReg = new BaseRegister(absoluteAddress,
                                                     false,
                                                     bankLower,
                                                     bankUpper,
                                                     loadableBank._accessInfo,
                                                     loadableBank._generalPermissions,
                                                     loadableBank._specialPermissions,
                                                     storageSubset);
                ip.setBaseRegister(loadableBank._initialBaseRegister, bReg);

                System.out.println(String.format("  To be based on B%d llNorm=0%o ulNorm=0%o",
                                                 loadableBank._initialBaseRegister,
                                                 bReg.getLowerLimitNormalized(),
                                                 bReg.getUpperLimitNormalized()));
            }

            mspOffset += loadableBank._content.getArraySize();
        }
    }

    /**
     * Given an array of LoadBankInfo objects, we load the described data as individual banks, into consecutive locations
     * into the given MainStorageProcessor.  For each bank, we create a BankRegister and establish that bank register
     * of the given InstructionProcessor as B0, B1, ...
     * @param ip reference to an IP to be used
     * @param msp reference to an MSP to be used
     * @param brIndex first base register index to be loaded (usually 0 for extended mode, 12 for basic mode)
     * @param bankInfos contains the various banks to be loaded
     */
    protected static void loadBanks(
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
     * @param ip reference to an IP to be used
     * @param msp reference to an MSP to be used
     * @param brIndex first base register index to be loaded (usually 0 for extended mode, 12 for basic mode)
     * @param sourceData array of source data arrays to be loaded
     */
    public static void loadBanks(
        final InstructionProcessor ip,
        final MainStorageProcessor msp,
        final int brIndex,
        final long[][] sourceData
    ) {
        LoadBankInfo[] bankInfos = new LoadBankInfo[sourceData.length];
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
     *              Since we are building all of this up from scratch, we'll go ahead and assign L,BDI of 0,32 to be
     *              the bank which contains all the interrupt handling code, so this entire table will be 33 * 8 words
     *              in length.  We'll also assign L,BDI of 0,33 to be the bank which contains the interrupt control stack.
     *  +n      Interrupt handling code bank - size is determined by the content of the arrays in interruptCode.
     *  +n+m    Interrupt Control Stack - we'll set this up with a particular stack frame size, with a total maximum size
     *              which will allow for some few nested interrupts.
     *
     * @param ip the IP which will have its various registers set appropriately to account for the created environment
     * @param msp the MSP in which we'll create the environment
     * @param mspOffset the offset from the beginning of MSP storage where we create the environment
     * @param module AbsoluteModule containing the interrupt code.  Code for interrupt 0 is in LC 0, for 1 in LC 1, etc
     * @return size of allocated memory in the MSP, starting at mspOffset
     * @throws MachineInterrupt if the IP throws one
     */
    int setupInterrupts(
        final InstructionProcessor ip,
        final MainStorageProcessor msp,
        final int mspOffset,
        final AbsoluteModule module
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
            //  If there is no code for the given interrupt, set the corresponding interrupt vector L,BDI,Offset to all zero.
            LoadableBank lb = module._loadableBanks.get(ihx);
            if (lb == null) {
                interruptVector.setValue(ihx, 0);
            } else {
                //  The caller actually gave us a code sequence for this interrupt.
                //  Set the interrupt vector for this code, starting immediately following the previous handler
                //  (i.e., at bank offset ihCodeSize).
                VirtualAddress vector = new VirtualAddress((byte)0, (short)0, ihCodeSize);
                interruptVector.setValue(ihx, vector.getW());

                //  Copy the code to the MSP
                msp.getStorage().load(ihCodeSize, lb._content);
                ihCodeSize += lb._content.getArraySize();
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
     * @param ip IP of interest
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
