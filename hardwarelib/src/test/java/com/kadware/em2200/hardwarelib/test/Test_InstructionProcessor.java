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

    /*

2.1.2 Base_Register Conventions
The following table lists the architecturally defined conventions for Base_Register usage.
Base_Register Usage
0 Current Extended_Mode Code Bank, if any
1 Reserved for activity-local storage stack
2–15 Unassigned User, available in Extended_Mode
12–15 Unassigned User, available in Basic_Mode
16 L0 BDT Pointer and Interrupt_Vector_Area Pointer (see 3.1.1 and 3.6)
17 L1 BDT Pointer (see 3.1.1)
18 L2 BDT Pointer (see 3.1.1)
19 L3 BDT Pointer (see 3.1.1)
20 L4 BDT Pointer (see 3.1.1)
21 L5 BDT Pointer (see 3.1.1)
22 L6 BDT Pointer (see 3.1.1)
23 L7 BDT Pointer (see 3.1.1)
24 ASA Pointer (see 3.5)
25 Return_Control_Stack (see 3.3.1)
26 Interrupt_Control_Stack (see 3.3.2)
27–31 Unassigned Exec

The Bank_Descriptor_Table (BDT) is a series of 8-word Bank_Descriptors (BD). Up to eight such
tables may be active at any one time, and each of the eight BDT Pointer Base_Registers, B16–B23,
points to one of these. These eight parts of the address space are called level (L) 0–7, respectively.
BDTs are referenced by the L-field (bits 0–2) of a Virtual_Address as described in 4.2.2.
The L1–L7 BDTs may contain up to 32,768 BDs each. Because B16 is also used as the
Interrupt_Vector_Area pointer (see 3.6), the L0 BDT may contain up to 32,736 BDs. Section 8
contains the special addressing rules for BDTs.

Version 3H models, hardware requires that B16 addresses 0120-0137 be available (that at least the
limits support this address) for hardware read and write. Sometimes hardware needs to read and
write a place in memory to control internal operations.

Software Note: It is intended that L indicate scope, with L0 most global and L7 most local. No other
architectural definition of L is made.

The Interrupt_Vector_Area is an array of 64 contiguous words (entries), starting at word 0 of the
Bank described by B16, which are in the format of the Program_Address_Register (PAR; see 2.2.1).
The Interrupt_Vector_Area entries provide a unique software entry point for each class of interrupt.
The Interrupt_Vector_Area entries are ordered by interrupt class number, as shown in Table 5–1. As
part of the interrupt sequence (see 5.1.5), the appropriate entry is fetched from the
Interrupt_Vector_Area (using the interrupt class as an offset) and loaded into the hard-held PAR.
There is no conflict between the Interrupt_Vector_Area and the Level 0 Bank_Descriptors because
L,BDI 0,0 through 0,31 do not reference the BDT.
 */

    //  Assembler source for the interrupt handlers
    static private final String[] IH_CODE = {
        "          $EXTEND",
        "          $LIT(0)",
        "",
        "$(1)      . Interrupt handlers",
        "IH_00*    HALT 01000",
        "IH_01*    HALT 01001",
        "IH_02*    HALT 01002",
        "IH_03*    HALT 01003",
        "IH_04*    HALT 01004",
        "IH_05*    HALT 01005",
        "IH_06*    HALT 01006",
        "IH_07*    HALT 01007",
        "IH_10*    HALT 01010",
        "IH_11*    HALT 01011",
        "IH_12*    HALT 01012",
        "IH_13*    HALT 01013",
        "IH_14*    HALT 01014",
        "IH_15*    HALT 01015",
        "IH_16*    HALT 01016",
        "IH_17*    HALT 01017",
        "IH_20*    HALT 01020",
        "IH_21*    HALT 01021",
        "IH_22*    HALT 01022",
        "IH_23*    HALT 01023",
        "IH_24*    HALT 01024",
        "IH_25*    HALT 01025",
        "IH_26*    HALT 01026",
        "IH_27*    HALT 01027",
        "IH_30*    HALT 01030",
        "IH_31*    HALT 01031",
        "IH_32*    HALT 01032",
        "IH_33*    HALT 01033",
        "IH_34*    HALT 01034",
        "IH_35*    HALT 01035",
        "IH_36*    HALT 01036",
        "IH_37*    HALT 01037",
    };

    static private final String[] BDT_CODE = {
        "          $EXTEND",
        "$(0)",
        "",
        "BANKS_PER_LEVEL* $EQU 64",
        "BANK_TABLE_SIZE* $EQU 8*BANKS_PER_LEVEL . 8 words per BD",
        "",
        "BDT_LEVEL0* . Interrupt handler vectors (total of 64 vectors)",
        "            . L,BDI is 000000+33, assuming the IH code is in bank 33",
        "          + 33,IH_00",
        "          + 33,IH_01",
        "          + 33,IH_02",
        "          + 33,IH_03",
        "          + 33,IH_04",
        "          + 33,IH_05",
        "          + 33,IH_06",
        "          + 33,IH_07",
        "          + 33,IH_10",
        "          + 33,IH_11",
        "          + 33,IH_12",
        "          + 33,IH_13",
        "          + 33,IH_14",
        "          + 33,IH_15",
        "          + 33,IH_16",
        "          + 33,IH_17",
        "          + 33,IH_20",
        "          + 33,IH_21",
        "          + 33,IH_22",
        "          + 33,IH_23",
        "          + 33,IH_24",
        "          + 33,IH_25",
        "          + 33,IH_26",
        "          + 33,IH_27",
        "          + 33,IH_30",
        "          + 33,IH_31",
        "          + 33,IH_32",
        "          + 33,IH_33",
        "          + 33,IH_34",
        "          + 33,IH_35",
        "          + 33,IH_36",
        "          + 33,IH_37",
        "          $RES      32                  . Interrupts 32-63 are not defined",
        "          $RES      (8*32)-64           . Unused",
        "          $RES      8*32                . Space for 32 exec banks, BDI 32 to 41",
        "",
        "BDT_LEVEL1* $RES BANK_TABLE_SIZE",
        "BDT_LEVEL2* $RES BANK_TABLE_SIZE",
        "BDT_LEVEL3* $RES BANK_TABLE_SIZE",
        "BDT_LEVEL4* $RES BANK_TABLE_SIZE",
        "BDT_LEVEL5* $RES BANK_TABLE_SIZE",
        "BDT_LEVEL6* $RES BANK_TABLE_SIZE",
        "BDT_LEVEL7* $RES BANK_TABLE_SIZE",
    };

    //  Absolute module for the above code...
    //  The BDT will be in bank 31 (it gets special loading consideration, and is never referenced in the BDT)
    //  IH code will be in bank 33 (2nd BD in level 0 BDT) (our convention, it'll work)
    static private AbsoluteModule _bankModule = null;

    //  Class which describes a bank to be loaded into the existing BDT environment
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
                                     .setBankLevel(06)
                                     .setStartingAddress(022000)
                                     .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(12)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D1")
                                     .setBankDescriptorIndex(000005)
                                     .setBankLevel(06)
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
                                     .setBankLevel(06)
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
                                         .setBankLevel(06)
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
                                     .setBankLevel(06)
                                     .setStartingAddress(01000)
                                     .setPoolSpecifications(poolSpecs04.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(12)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D1")
                                     .setBankDescriptorIndex(000005)
                                     .setBankLevel(06)
                                     .setStartingAddress(040000)
                                     .setPoolSpecifications(poolSpecs05.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(13)
                                     .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("I2")
                                     .setBankDescriptorIndex(000006)
                                     .setBankLevel(06)
                                     .setStartingAddress(020000)
                                     .setPoolSpecifications(poolSpecs06.toArray(new Linker.LCPoolSpecification[0]))
                                     .setInitialBaseRegister(14)
                                     .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                     .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                     .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder()
                                     .setBankName("D2")
                                     .setBankDescriptorIndex(000007)
                                     .setBankLevel(06)
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
                                     .setBankLevel(06)
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
                                     .setBankLevel(06)
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
            bankDeclarations.add(
                new Linker.BankDeclaration.Builder()
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
     * Establishes the banking environment
     * @param ip the IP which will have its various registers set appropriately to account for the created environment
     * @param msp the MSP in which we'll create the environment
     */
    static void establishBankingEnvironment(
            final InstructionProcessor ip,
            final MainStorageProcessor msp
    ) {
        //  Does the bank control absolute module already exist?  If not, create it
        if (_bankModule == null) {
            //TODO
        }

        //TODO load the bank control module into the given MSP and set the IP registers accordingly

        //TODO establish an interrupt control stack

        //TODO note the next available absolute memory location for future bank loading
    }

//    /**
//     * Sets up the interrupt handling environment
//     * @param ip the IP which will have its various registers set appropriately to account for the created environment
//     * @param msp the MSP in which we'll create the environment
//     * @param mspOffset the offset from the beginning of MSP storage where we create the environment
//     */
//    void establishInterruptEnvironment(
//            final InstructionProcessor ip,
//            final MainStorageProcessor msp,
//            final int mspOffset
//    ) throws MachineInterrupt {
//        String[] code = {
//            "          $EXTEND",
//            "$(1)",
//            "IH00      . Interrupt handler - Reserved, Hardware Default",
//            "          HALT      01000+0",
//            "",
//            "IH01      . Interrupt handler - Hardware Check",
//            "          HALT      01000+1",
//            "",
//            "IH02      . Interrupt handler - Diagnostic",
//            "          HALT      01000+2",
//            "",
//            "IH08      . Interrupt handler - Reference Violation",
//            "          HALT      01000+8",
//            "",
//            "IH09      . Interrupt handler - Addressing Exception",
//            "          HALT      01000+9",
//            "",
//            "IH10      . Interrupt handler - Terminal Addressing Exception",
//            "          HALT      01000+10",
//            "",
//            "IH11      . Interrupt handler - RCS Generic Stack Under/Over Flow",
//            "          HALT      01000+11",
//            "",
//            "IH12      . Interrupt handler - Signal",
//            "          HALT      01000+12",
//            "",
//            "IH13      . Interrupt handler - Test And Set",
//            "          HALT      01000+13",
//            "",
//            "IH14      . Interrupt handler - Invalid Instruction",
//            "          HALT      01000+14",
//            "",
//            "IH15      . Interrupt handler - Page Exception",
//            "          HALT      01000+15",
//            "",
//            "IH16      . Interrupt handler - Arithmetic Exception",
//            "          HALT      01000+16",
//            "",
//            "IH17      . Interrupt handler - Data Exception",
//            "          HALT      01000+17",
//            "",
//            "IH18      . Interrupt handler - Operation Trap",
//            "          HALT      01000+18",
//            "",
//            "IH19      . Interrupt handler - Breakpoint",
//            "          HALT      01000+19",
//            "",
//            "IH20      . Interrupt handler - Quantum Timer",
//            "          HALT      01000+20",
//            "",
//            "IH23      . Interrupt handler - Page(s) Zeroed",
//            "          HALT      01000+23",
//            "",
//            "IH24      . Interrupt handler - Software Break",
//            "          HALT      01000+24",
//            "",
//            "IH25      . Interrupt handler - Jump History Full",
//            "          HALT      01000+25",
//            "",
//            "IH27      . Interrupt handler - Dayclock",
//            "          HALT      01000+27",
//            "",
//            "IH28      . Interrupt handler - Performance Monitoring",
//            "          HALT      01000+28",
//            "",
//            "IH29      . Interrupt handler - IPL",
//            "          HALT      01000+29",
//            "",
//            "IH30      . Interrupt handler - UPI Initial",
//            "          HALT      01000+30",
//            "",
//            "IH31      . Interrupt handler - UPI Normal",
//            "          HALT      01000+31",
//            "",
//            "$(0) . Vectors",
//            "          . Table of PAR vectors in L,BDI,PC format",
//            "          . Presumes all interrupt handlers are in bank with BDI of 020 and level 0",
//            "          + $BDI(1),IH00",
//            "          + $BDI(1),IH01",
//            "          + $BDI(1),IH02",
//            "          + 0",
//            "          + 0",
//            "          + 0",
//            "          + 0",
//            "          + 0",
//            "          + $BDI(1),IH08",
//            "          + $BDI(1),IH09",
//            "          + $BDI(1),IH10",
//            "          + $BDI(1),IH11",
//            "          + $BDI(1),IH12",
//            "          + $BDI(1),IH13",
//            "          + $BDI(1),IH14",
//            "          + $BDI(1),IH15",
//            "          + $BDI(1),IH16",
//            "          + $BDI(1),IH17",
//            "          + $BDI(1),IH18",
//            "          + $BDI(1),IH19",
//            "          + $BDI(1),IH20",
//            "          + 0",
//            "          + 0",
//            "          + $BDI(1),IH23",
//            "          + $BDI(1),IH24",
//            "          + $BDI(1),IH25",
//            "          + 0",
//            "          + $BDI(1),IH27",
//            "          + $BDI(1),IH28",
//            "          + $BDI(1),IH29",
//            "          + $BDI(1),IH30",
//            "          + $BDI(1),IH31",
//            "          $RES 32"
//        };
//
//        Assembler asm = new Assembler(code, "IH");
//        RelocatableModule relModule = asm.assemble(true);
//
//        Linker.LCPoolSpecification codePoolSpec = new Linker.LCPoolSpecification(relModule, 1);
//        Linker.LCPoolSpecification vectorPoolSpec = new Linker.LCPoolSpecification(relModule, 0);
//        Linker.LCPoolSpecification[] codePoolSpecs = { codePoolSpec };
//        Linker.LCPoolSpecification[] vectorPoolSpecs = { vectorPoolSpec };
//
//        Linker.BankDeclaration codeBankDecl =
//            new Linker.BankDeclaration.Builder().setBankName("IHCODE")
//                                                .setBankDescriptorIndex(0)
//                                                .setBankLevel(0)
//                                                .setIsExtended(true)
//                                                .setStartingAddress(01000)
//                                                .setPoolSpecifications(codePoolSpecs)
//                                                .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
//                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
//                                                .build();
//
//        Linker.BankDeclaration vectorBankDecl =
//            new Linker.BankDeclaration.Builder().setBankName("IHVECTORS")
//                                                .setBankDescriptorIndex(1)
//                                                .setBankLevel(0)
//                                                .setIsExtended(true)
//                                                .setStartingAddress(0)
//                                                .setPoolSpecifications(vectorPoolSpecs)
//                                                .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
//                                                .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
//                                                .build();
//
//        Linker.BankDeclaration[] bankDeclarations = {
//            codeBankDecl,
//            vectorBankDecl
//        };
//        Linker linker = new Linker(bankDeclarations);
//        AbsoluteModule module = linker.link("IH", true);
//        setupInterrupts(ip, msp, mspOffset, module);
//    }

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

//    /**
//     * Sets up the interrupt environment at the given offset in the given MSP, and adjusts the given IP's registers
//     * accordingly.  A number of structures are created in contiguous memory beginning at mspOffset, as such:
//     *  +0      Level 0 Bank Descriptor Table (which we create)
//     *              This table is indexed by BDI for virtual addresses which have an L-value of zero.
//     *              Each entry is an 8-word bank descriptor (see hardware manual)
//     *              However, the first 64 words of this table comprise the interrupt vector table, consisting of
//     *              64 contiguous words indexed by interrupt class.  Each word is the L,BDI,Offset of the code
//     *              which handles the particular interrupt.
//     *              L,BDI of 0,0 through 0,31 do not refer to actual banks (for architectural reasons or something),
//     *              so these first 64 words do not conflict with any BD's.
//     *              Since we are building all of this up from scratch, we'll go ahead and assign L,BDI of 0,32 to be
//     *              the bank which contains all the interrupt handling code, so this entire table will be 33 * 8 words
//     *              in length.  We'll also assign L,BDI of 0,33 to be the bank which contains the interrupt control stack.
//     *  +n      Interrupt handling code bank - size is determined by the content of the arrays in interruptCode.
//     *  +n+m    Interrupt Control Stack - we'll set this up with a particular stack frame size, with a total maximum size
//     *              which will allow for some few nested interrupts.
//     *
//     * @param ip the IP which will have its various registers set appropriately to account for the created environment
//     * @param msp the MSP in which we'll create the environment
//     * @param mspOffset the offset from the beginning of MSP storage where we create the environment
//     * @param module AbsoluteModule containing the interrupt code.  Code for interrupt 0 is in LC 0, for 1 in LC 1, etc
//     * @return size of allocated memory in the MSP, starting at mspOffset
//     * @throws MachineInterrupt if the IP throws one
//     */
//    int setupInterrupts(
//        final InstructionProcessor ip,
//        final MainStorageProcessor msp,
//        final int mspOffset,
//        final AbsoluteModule module
//    ) throws MachineInterrupt {
//        //TODO
//        return 0;
//    }

    public static void showDebugInfo(
        final InstructionProcessor ip,
        final MainStorageProcessor msp
    ) {
        try {
            System.out.println("Debug Info:");
            System.out.println("  Processor Registers:");
            for (int x = 0; x < 16; ++x) {
                GeneralRegister gr = ip.getGeneralRegister(GeneralRegisterSet.X0 + x);
                System.out.println(String.format("    X%d %012o", x, gr.getW()));
            }
            for (int x = 0; x < 16; ++x) {
                GeneralRegister gr = ip.getGeneralRegister(GeneralRegisterSet.A0 + x);
                System.out.println(String.format("    A%d %012o", x, gr.getW()));
            }
            for (int x = 0; x < 16; ++x) {
                GeneralRegister gr = ip.getGeneralRegister(GeneralRegisterSet.R0 + x);
                System.out.println(String.format("    R%d %012o", x, gr.getW()));
            }
            for (int x = 0; x < 16; ++x) {
                GeneralRegister gr = ip.getGeneralRegister(GeneralRegisterSet.EX0 + x);
                System.out.println(String.format("    EX%d %012o", x, gr.getW()));
            }
            for (int x = 0; x < 16; ++x) {
                GeneralRegister gr = ip.getGeneralRegister(GeneralRegisterSet.EA0 + x);
                System.out.println(String.format("    EA%d %012o", x, gr.getW()));
            }
            for (int x = 0; x < 16; ++x) {
                GeneralRegister gr = ip.getGeneralRegister(GeneralRegisterSet.ER0 + x);
                System.out.println(String.format("    ER%d %012o", x, gr.getW()));
            }

            System.out.println("  Base Registers:");
            for (int bx = 0; bx < 32; ++bx) {
                BaseRegister br = ip.getBaseRegister(bx);
                System.out.println(String.format("    BR%d base:(UPI:%d Offset:%08o) lower:%d upper:%d",
                                                 bx,
                                                 br.getBaseAddress()._upi,
                                                 br.getBaseAddress()._offset,
                                                 br.getLowerLimitNormalized(),
                                                 br.getUpperLimitNormalized()));
                System.out.println("    Content:");
                Word36Array storage = br.getStorage();
                if (storage != null) {
                    for ( int sx = 0; sx < storage.getArraySize(); sx += 8 ) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("      %08o:", sx));
                        for ( int sy = 0; sy < 8; ++sy ) {
                            if ( sx + sy < storage.getArraySize() ) {
                                sb.append(String.format(" %012o", storage.getValue(sx + sy)));
                            }
                        }
                        System.out.println(sb.toString());
                    }
                }
            }
        } catch (MachineInterrupt ex) {
            System.out.println("Caught:" + ex.getMessage());
        }
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
