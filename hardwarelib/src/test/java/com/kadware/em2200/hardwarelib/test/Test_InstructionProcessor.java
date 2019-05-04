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

    private static class MSPRegionAttributes implements RegionTracker.IAttributes {
        public final String _bankName;
        public final int _bankLevel;
        public final int _bankDescriptorIndex;

        public MSPRegionAttributes(
            final String bankName,
            final int bankLevel,
            final int bankDescriptorIndex
        ) {
            _bankName = bankName;
            _bankLevel = bankLevel;
            _bankDescriptorIndex = bankDescriptorIndex;
        }
    }

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

The Activity Save Area is used by Executive software for the saving of Activity State
Packets, Bank Descriptor Table Pointers, Return Control Stack data, Active Base Table
data, and GRS. B24 is expected to be used as the ASA pointer.

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
        "$(0)",
        "          $LIT",
        "",
        "$(1)      . Interrupt handlers",
        "IH_00*    . Interrupt 0:Reserved - Hardware Default",
        "          HALT 01000",
        "",
        "IH_01*    . Interrupt 1:Hardware Check",
        "          HALT 01001",
        "",
        "IH_02*    . Interrupt 2:Diagnostic",
        "          HALT 01002",
        "",
        "IH_03*    . Interrupt 3:Reserved",
        "          HALT 01003",
        "",
        "IH_04*    . Interrupt 4:Reserved",
        "          HALT 01004",
        "",
        "IH_05*    . Interrupt 5:Reserved",
        "          HALT 01005",
        "",
        "IH_06*    . Interrupt 6:Reserved",
        "          HALT 01006",
        "",
        "IH_07*    . Interrupt 7:Reserved",
        "          HALT 01007",
        "",
        "IH_10*    . Interrupt 8:Reference Violation",
        "          HALT 01010",
        "",
        "IH_11*    . Interrupt 9:Addressing Exception",
        "          HALT 01011",
        "",
        "IH_12*    . Interrupt 10 Terminal Addressing Exception",
        "          HALT 01012",
        "",
        "IH_13*    . Interrupt 11 RCS/Generic Stack Under/Overflow",
        "          HALT 01013",
        "",
        "IH_14*    . Interrupt 12 Signal",
        "          HALT 01014",
        "",
        "IH_15*    . Interrupt 13 Test & Set",
        "          HALT 01015",
        "",
        "IH_16*    . Interrupt 14 Invalid Instruction",
        "          HALT 01016",
        "",
        "IH_17*    . Interrupt 15 Page Exception",
        "          HALT 01017",
        "",
        "IH_20*    . Interrupt 16 Arithmetic Exception",
        "          HALT 01020",
        "",
        "IH_21*    . Interrupt 17 Data Exception",
        "          HALT 01021",
        "",
        "IH_22*    . Interrupt 18 Operation Trap",
        "          HALT 01022",
        "",
        "IH_23*    . Interrupt 19 Breakpoint",
        "          HALT 01023",
        "",
        "IH_24*    . Interrupt 20 Quantum Timer",
        "          HALT 01024",
        "",
        "IH_25*    . Interrupt 21 Reserved",
        "          HALT 01025",
        "",
        "IH_26*    . Interrupt 22 Reserved",
        "          HALT 01026",
        "",
        "IH_27*    . Interrupt 23 Page(s) Zeroed",
        "          HALT 01027",
        "",
        "IH_30*    . Interrupt 24 Software Break",
        "          HALT 01030",
        "",
        "IH_31*    . Interrupt 25 Jump History Full",
        "          HALT 01031",
        "",
        "IH_32*    . Interrupt 26 Reserved",
        "          HALT 01032",
        "",
        "IH_33*    . Interrupt 27 Dayclock",
        "          HALT 01033",
        "",
        "IH_34*    . Interrupt 28 Performance Monitoring",
        "          HALT 01034",
        "",
        "IH_35*    . Interrupt 29 Initial Program Load (IPL)",
        "          HALT 01035",
        "",
        "IH_36*    . UPI Initial",
        "          HALT 01036",
        "",
        "IH_37*    . UPI Normal",
        "          HALT 01037",
    };

    static private final String[] BDT_CODE = {
        "          $EXTEND",
        "",
        "BANKSPERLVL* $EQU 64",
        "BANKTABLESZ* $EQU 8*BANKSPERLVL . 8 words per BD",
        "",
        "$(0),BDT_LEVEL0* . Interrupt handler vectors (total of 64 vectors)",
        "                 . L,BDI is 000000+33, assuming the IH code is in bank 33",
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
        "          $RES      (8*32)-64           . Unused remainder of 32 BDs not valid",
        "          $RES      8*32                . Space for 32 exec banks, BDI 32 to 41",
        "",
        "$(1),BDT_LEVEL1* $RES BANKTABLESZ",
        "$(2),BDT_LEVEL2* $RES BANKTABLESZ",
        "$(3),BDT_LEVEL3* $RES BANKTABLESZ",
        "$(4),BDT_LEVEL4* $RES BANKTABLESZ",
        "$(5),BDT_LEVEL5* $RES BANKTABLESZ",
        "$(6),BDT_LEVEL6* $RES BANKTABLESZ",
        "$(7),BDT_LEVEL7* $RES BANKTABLESZ",
    };

    //  Absolute module for the above code...
    //  The BDT will be in banks 0 through 7, one per BDT level (0 for 0, 1 for 1, etc)
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
     * Creates the banking environment absolute module
     */
    static void createBankingModule(
    ) {
        //  Assemble banking source - there will be 8 location counter pools 0 through 7
        //  which correspond to BDT's 0 through 7 - see linking step for the reasons why
        Assembler asm = new Assembler(BDT_CODE, "BDT");
        RelocatableModule bdtModule = asm.assemble(true);
        assert(asm.getDiagnostics().isEmpty());

        //  Assemble interrupt handler code into a separate relocatable module...
        //  we have no particular expectations with respect to location counters;
        //  For simplicity, we put all of this code into one IH bank - again, see linking step.
        asm = new Assembler(IH_CODE, "IH");
        RelocatableModule ihModule = asm.assemble(true);
        assert(asm.getDiagnostics().isEmpty());

        //  Now we link - we need to create a separate bank for each of the Bank Descriptor Tables
        //  (to make loading it into the MSP easier), and one bank for the interrupt handler code.
        //  BDI's for the BDT banks are 000 through 007; BDI for the IH code is 010.
        Linker.BankDeclaration[] bankDeclarations = new Linker.BankDeclaration[9];

        for (int lcIndex = 0; lcIndex < 8; ++lcIndex) {
            Linker.LCPoolSpecification[] bdtPoolSpecs = {
                new Linker.LCPoolSpecification(bdtModule, lcIndex)
            };

            bankDeclarations[lcIndex] =
                new Linker.BankDeclaration.Builder().setBankName(String.format("BDTLEVEL%d", lcIndex))
                                                    .setBankDescriptorIndex(lcIndex)
                                                    .setBankLevel(0)
                                                    .setIsExtended(true)
                                                    .setStartingAddress(0)
                                                    .setPoolSpecifications(bdtPoolSpecs)
                                                    .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                                    .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                    .build();
        }

        List<Linker.LCPoolSpecification> ihPoolSpecList = new LinkedList<>();
        for (Integer lcIndex : ihModule._storage.keySet()) {
            ihPoolSpecList.add(new Linker.LCPoolSpecification(ihModule, lcIndex));
        }

        bankDeclarations[8] =
            new Linker.BankDeclaration.Builder().setBankName("IHBANK")
                                                .setBankDescriptorIndex(010)
                                                .setBankLevel(0)
                                                .setIsExtended(true)
                                                .setStartingAddress(01000)
                                                .setPoolSpecifications(ihPoolSpecList.toArray(new Linker.LCPoolSpecification[0]))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                .build();

        Linker linker = new Linker(bankDeclarations);
        _bankModule = linker.link("BDT-IH", true);
        assert(_bankModule != null);
    }

    /**
     * Establishes the banking environment
     * @param ip the IP which will have its various registers set appropriately to account for the created environment
     * @param msp the MSP in which we'll create the environment
     */
    static void establishBankingEnvironment(
        final ExtInstructionProcessor ip,
        final ExtMainStorageProcessor msp
    ) throws MachineInterrupt {
        //  Does the bank control absolute module already exist?  If not, create it
        if (_bankModule == null) {
            createBankingModule();
        }

        try {
            //  Load the BDT banks from the bank control module into the given MSP and set the IP registers accordingly.
            //  The BDT banks are located in banks with BDI 0 to 7 for levels 0 through 7.
            for (int level = 0; level < 8; ++level) {
                LoadableBank bdtBank = _bankModule._loadableBanks.get(level);
                long bdtBankSize = bdtBank._content.getArraySize();
                MSPRegionAttributes bdtAttributes = new MSPRegionAttributes(bdtBank._bankName,
                                                                            0,
                                                                            bdtBank._bankDescriptorIndex);
                RegionTracker.SubRegion bdtSub = msp._regions.assign(bdtBankSize, bdtAttributes);
                msp.getStorage().load((int) bdtSub._position, bdtBank._content);
                System.out.println(String.format("Loaded BDT bank at MSP offset %d for %d words",
                                                 bdtSub._position,
                                                 bdtSub._extent));

                int bankLower = bdtBank._startingAddress;
                int bankUpper = bdtBank._startingAddress + bdtBank._content.getArraySize() - 1;
                String attrString = String.format("BDT%d", level);

                RegionTracker.SubRegion subRegion =
                    msp._regions.assign(bdtBank._content.getArraySize(),
                                        new MSPRegionAttributes(attrString, 0, level));
                AbsoluteAddress absAddr = new AbsoluteAddress(msp.getUPI(), (int) subRegion._position);

                BaseRegister bReg =
                    new BaseRegister(absAddr,
                                     false,
                                     bankLower,
                                     bankUpper,
                                     bdtBank._accessInfo,
                                     bdtBank._generalPermissions,
                                     bdtBank._specialPermissions,
                                     bdtBank._content);
                ip.setBaseRegister(16 + level, bReg);
            }

            //  Now we can load the IH bank using the generic mechanism.
            //  Put it in level 0, BDI 33 (in case we want to use 32 for something)
            LoadableBank ihBank = _bankModule._loadableBanks.get(8);
            loadBank(ip, msp, ihBank, 0, 33);

            //  Establish an interrupt control stack - size is arbitrary
            int stackSize = 8 * 32;
            RegionTracker.SubRegion stackSubRegion =
                msp._regions.assign(stackSize,
                                    new MSPRegionAttributes("ICS", 0, 0));
            BaseRegister bReg =
                new BaseRegister(new AbsoluteAddress(msp.getUPI(), (int) stackSubRegion._position),
                                 false,
                                 0,
                                 stackSize - 1,
                                 new AccessInfo((byte) 0, (short) 0),
                                 new AccessPermissions(false, false, false),
                                 new AccessPermissions(false, true, true),
                                 new Word36ArraySlice(msp.getStorage(), (int) stackSubRegion._position, stackSize));
            ip.setBaseRegister(InstructionProcessor.ICS_BASE_REGISTER, bReg);
            Word36 reg = new Word36();
            reg.setH1(8);
            reg.setH2(stackSize);
            ip.setGeneralRegister(InstructionProcessor.ICS_INDEX_REGISTER, reg.getW());
        } catch (RegionTracker.OutOfSpaceException ex) {
            throw new RuntimeException("Cannot find space in MSP for bank to be loaded");
        }
    }

    /**
     * Retrieves the contents of a bank represented by a base register.
     * This is a copy of the content, so if you update the result, you don't screw up the actual content in the MSP.
     * @param ip reference to IP containing the desired BR
     * @param baseRegisterIndex index of the desired BR
     * @return reference to array of values constituting the bank
     */
    static long[] getBank(
        final InstructionProcessor ip,
        final int baseRegisterIndex
    ) {
        Word36Array array = ip.getBaseRegister(baseRegisterIndex)._storage;
        long[] result = new long[array.getArraySize()];
        for (int ax = 0; ax < array.getArraySize(); ++ax) {
            result[ax] = array.getValue(ax);
        }
        return result;
    }

    /**
     * Loads a bank into the banking environment previously established for the indicated MSP
     * and referenced by the B16 - B23 register of the indicated IP
     * @param ip IP with base registers set according to the BDT loaded into the MSP
     * @param msp the MSP into which we load the given bank
     * @param bank bank to be loaded
     * @param bankLevel BDT level where the bank is to be loaded
     * @param bankDescriptorIndex BDI of the bank within the BDT
     * @return the bank descriptor describing the bank we just loaded
     */
    static BankDescriptor loadBank(
        final InstructionProcessor ip,
        final ExtMainStorageProcessor msp,
        final LoadableBank bank,
        final int bankLevel,
        final int bankDescriptorIndex
    ) {
        assert(bankLevel >= 0) && (bankLevel < 8);

        //  Load the bank into memory
        try {
            int bankLower = bank._startingAddress;
            int bankUpper = bank._startingAddress + bank._content.getArraySize() - 1;
            String attrString = String.format("BDT%d", bankLevel);

            RegionTracker.SubRegion subRegion =
                msp._regions.assign(bank._content.getArraySize(),
                                    new MSPRegionAttributes(attrString, bankLevel, bankDescriptorIndex));
            AbsoluteAddress absAddr = new AbsoluteAddress(msp.getUPI(), (int) subRegion._position);

            //  Create a bank descriptor for it in the appropriate bdt
            Word36Array bankDescriptorTable = ip.getBaseRegister(16 + bankLevel)._storage;
            BankDescriptor bd = new BankDescriptor(bankDescriptorTable, 8 * bankDescriptorIndex);
            bd.setBankType(bank._isExtendedMode ? BankDescriptor.BankType.ExtendedMode : BankDescriptor.BankType.BasicMode);
            bd.setBaseAddress(absAddr);
            bd.setGeneralAccessPermissions(bank._generalPermissions);
            bd.setGeneralFault(false);
            bd.setLargeBank(false);
            bd.setLowerLimit(bankLower);
            bd.setSpecialAccessPermissions(bank._specialPermissions);
            bd.setUpperLimit(bankUpper);
            bd.setUpperLimitSuppressionControl(false);

            System.out.println(String.format("Loaded bank %s level %d BDI %06o MSP offset:%d length:%d",
                                             bank._bankName,
                                             bankLevel,
                                             bankDescriptorIndex,
                                             subRegion._position,
                                             subRegion._extent));

            return bd;
        } catch (RegionTracker.OutOfSpaceException ex) {
            throw new RuntimeException("Cannot find space in MSP for bank to be loaded");
        }
    }

    /**
     * Loads the various banks from the given absolute module into the given MSP
     * and applies initial base registers for the given IP as directed by that module.
     * @param ip instruction processor of interest
     * @param msp main storage processor of interest
     * @param module absolute module to be loaded
     * @param bdtLevel indicates which bdt the bank should be loaded into (0 to 7)
     */
    static void loadBanks(
        final InstructionProcessor ip,
        final ExtMainStorageProcessor msp,
        final AbsoluteModule module,
        final int bdtLevel
    ) {
        assert((bdtLevel >= 0) && (bdtLevel < 8));
        for (LoadableBank loadableBank : module._loadableBanks.values()) {
            BankDescriptor bd = loadBank(ip, msp, loadableBank, bdtLevel, loadableBank._bankDescriptorIndex);

            if (loadableBank._initialBaseRegister != null) {
                Word36ArraySlice storageSubset =
                    new Word36ArraySlice(msp.getStorage(),
                                         bd.getBaseAddress()._offset,
                                         bd.getUpperLimitNormalized() - bd.getLowerLimitNormalized() + 1);
                int bankLower = loadableBank._startingAddress;
                int bankUpper = loadableBank._startingAddress + loadableBank._content.getArraySize() - 1;
                BaseRegister bReg = new BaseRegister(bd.getBaseAddress(),
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
                                                 bReg._lowerLimitNormalized,
                                                 bReg._upperLimitNormalized));
            }
        }
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
        //TODO obsolete
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
                                                 bReg._lowerLimitNormalized,
                                                 bReg._upperLimitNormalized));
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
        //TODO obsolete
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
                                             bReg._lowerLimitNormalized,
                                             bReg._upperLimitNormalized));
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
        //TODO obsolete
        LoadBankInfo[] bankInfos = new LoadBankInfo[sourceData.length];
        for (int sx = 0; sx < sourceData.length; ++sx) {
            bankInfos[sx] = new LoadBankInfo(sourceData[sx]);
        }

        loadBanks(ip, msp, brIndex, bankInfos);
    }

    /**
     * Brute-force dump of almost everything we might want to know.
     * Includes elements of IP state, as well as the content of all loaded banks in the MSP
     * @param ip of interest
     * @param msp of interest
     */
    static void showDebugInfo(
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

            //TODO don't dump banks here, only indicate level and BDI
            //      instead, dump banks from the registers which point to the bdt's
            System.out.println("  Base Registers:");
            for (int bx = 0; bx < 32; ++bx) {
                BaseRegister br = ip.getBaseRegister(bx);
                System.out.println(String.format("    BR%d base:%s(UPI:%d Offset:%08o) lower:%d upper:%d",
                                                 bx,
                                                 br._voidFlag ? "(VOID)" : "",
                                                 br._baseAddress._upi,
                                                 br._baseAddress._offset,
                                                 br._lowerLimitNormalized,
                                                 br._upperLimitNormalized));
                if (bx >= 16 && bx < 24) {
                    System.out.println(String.format("    Base register refers to BDT level %d; BDT Content follows:",
                                                     bx - 16));
                    if (br._storage != null) {
                        for (int sx = 0; sx < br._storage.getArraySize(); sx += 8) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("      %08o:", sx));
                            for (int sy = 0; sy < 8; ++sy) {
                                if ( sx + sy < br._storage.getArraySize() ) {
                                    sb.append(String.format(" %012o", br._storage.getValue(sx + sy)));
                                }
                            }
                            System.out.println(sb.toString());
                        }
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
