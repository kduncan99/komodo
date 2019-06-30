/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.baselib.*;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.misc.*;
import com.kadware.komodo.minalib.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for all Test_InstructionProcessor_* classes
 */
class BaseFunctions {

    /**
     * Produced as a result of loadModule()
     */
    static class Processors {
        final InstrumentedInstructionProcessor _instructionProcessor;
        final InstrumentedMainStorageProcessor _mainStorageProcessor;

        Processors(
            final InstrumentedInstructionProcessor ip,
            final InstrumentedMainStorageProcessor msp
        ) {
            _instructionProcessor = ip;
            _mainStorageProcessor = msp;
        }
    }

    private static class MSPRegionAttributes implements RegionTracker.IAttributes {
        final String _bankName;
        final int _bankLevel;
        final int _bankDescriptorIndex;

        MSPRegionAttributes(
            final String bankName,
            final int bankLevel,
            final int bankDescriptorIndex
        ) {
            _bankName = bankName;
            _bankLevel = bankLevel;
            _bankDescriptorIndex = bankDescriptorIndex;
        }
    }

    //  Assembler source for the interrupt handlers
    static private final String[] IH_CODE = {
        "          $EXTEND",
        "          $INFO 1 3",
        "          $INFO 10 1",
        "",
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

    private static final Assembler.Option[] _assemblerDisplayAll = {
        Assembler.Option.EMIT_MODULE_SUMMARY,
        Assembler.Option.EMIT_DICTIONARY,
        Assembler.Option.EMIT_GENERATED_CODE,
        Assembler.Option.EMIT_SOURCE,
    };

    private static final Assembler.Option[] _assemblerDisplayNone = {};

    private static final Linker.Option[] _linkerDisplayAll = {
        Linker.Option.OPTION_EMIT_DICTIONARY,
        Linker.Option.OPTION_EMIT_GENERATED_CODE,
        Linker.Option.OPTION_EMIT_SUMMARY,
    };

    private static final Linker.Option[] _linkerDisplayNone = {};

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
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", code, display ? _assemblerDisplayAll : _assemblerDisplayNone);
        assert(relModule != null);

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
        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("I1")
                                                                 .setBankDescriptorIndex(000004)
                                                                 .setBankLevel(06)
                                                                 .setStartingAddress(022000)
                                                                 .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(12)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                                 .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("D1")
                                                                 .setBankDescriptorIndex(000005)
                                                                 .setBankLevel(06)
                                                                 .setStartingAddress(040000)
                                                                 .setPoolSpecifications(poolSpecsEven.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(13)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .build());

        Linker linker = new Linker();
        return linker.link("TEST",
                           bankDeclarations.toArray(new Linker.BankDeclaration[0]),
                           0,
                           display ? _linkerDisplayAll : _linkerDisplayNone);
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
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", code, display ? _assemblerDisplayAll : _assemblerDisplayNone);
        assert(relModule != null);

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
        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("I1")
                                                                 .setBankDescriptorIndex(000004)
                                                                 .setBankLevel(06)
                                                                 .setStartingAddress(01000)
                                                                 .setPoolSpecifications(poolSpecs04.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(12)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                                 .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("D1")
                                                                 .setBankDescriptorIndex(000005)
                                                                 .setBankLevel(06)
                                                                 .setStartingAddress(040000)
                                                                 .setPoolSpecifications(poolSpecs05.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(13)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("I2")
                                                                 .setBankDescriptorIndex(000006)
                                                                 .setBankLevel(06)
                                                                 .setStartingAddress(020000)
                                                                 .setPoolSpecifications(poolSpecs06.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(14)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                                 .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("D2")
                                                                 .setBankDescriptorIndex(000007)
                                                                 .setBankLevel(06)
                                                                 .setStartingAddress(060000)
                                                                 .setPoolSpecifications(poolSpecs07.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(15)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .build());

        Linker linker = new Linker();
        return linker.link("TEST",
                           bankDeclarations.toArray(new Linker.BankDeclaration[0]),
                           0,
                           display ? _linkerDisplayAll : _linkerDisplayNone);
    }

    /**
     * Assembles sets of code into a relocatable module, then links it such that the odd-numbered lc pools
     * are placed in an IBANK with BDI 04 and the even-number pools in a DBANK with BDI 05.
     * Initial base registers will be 0 for instructions and 2 for data.
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeExtended(
        final String[] code,
        final boolean display
    ) {
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", code, display ? _assemblerDisplayAll : _assemblerDisplayNone);
        assert(relModule != null);

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
        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("I1")
                                                                 .setBankDescriptorIndex(000004)
                                                                 .setBankLevel(06)
                                                                 .setNeedsExtendedMode(true)
                                                                 .setStartingAddress(01000)
                                                                 .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(0)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(true, true, false))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(true, true, false))
                                                                 .build());

        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                                 .setBankName("D1")
                                                                 .setBankDescriptorIndex(000005)
                                                                 .setBankLevel(06)
                                                                 .setNeedsExtendedMode(true)
                                                                 .setStartingAddress(01000)
                                                                 .setPoolSpecifications(poolSpecsEven.toArray(new Linker.LCPoolSpecification[0]))
                                                                 .setInitialBaseRegister(2)
                                                                 .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                                 .build());

        Linker linker = new Linker();
        return linker.link("TEST",
                           bankDeclarations.toArray(new Linker.BankDeclaration[0]),
                           32,
                           display ? _linkerDisplayAll : _linkerDisplayNone);
    }

    /**
     * Assembles source code into a relocatable module, then links it, producing multiple banks where:
     *  BDI 000004 contains all odd-numbered lc pools, based on B0
     *  Each unique even-numbered lc pool generates a unique bank BDI >= 5, based on B2 and up
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeExtendedMultibank(
            final String[] code,
            final boolean display
    ) {
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", code, display ? _assemblerDisplayAll : _assemblerDisplayNone);
        assert(relModule != null);

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
            if (bReg == 1) {
                ++bReg;
            }

            int bdi = entry.getKey();
            List<Linker.LCPoolSpecification> poolSpecs = entry.getValue();
            bankDeclarations.add(
                new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
                                                    .setBankName(String.format("BANK%06o", bdi))
                                                    .setBankDescriptorIndex(bdi)
                                                    .setBankLevel(6)
                                                    .setNeedsExtendedMode(true)
                                                    .setStartingAddress(01000)
                                                    .setPoolSpecifications(poolSpecs.toArray(new Linker.LCPoolSpecification[0]))
                                                    .setInitialBaseRegister(bReg++)
                                                    .setGeneralAccessPermissions(new AccessPermissions(bdi == 04,
                                                                                                       true,
                                                                                                       bReg > 0))
                                                    .setSpecialAccessPermissions(new AccessPermissions(bdi == 04,
                                                                                                       true,
                                                                                                       bReg > 0))
                                                    .build());
        }

        Linker linker = new Linker();
        return linker.link("TEST",
                           bankDeclarations.toArray(new Linker.BankDeclaration[0]),
                           32,
                           display ? _linkerDisplayAll : _linkerDisplayNone);
    }

    /**
     * Assembles source code into a relocatable module, then links it, producing multiple banks where
     * every LC number encountered is placed in a bank with a BDI which is set to LC number + 4.
     * @param code arrays of text comprising the source code we assemble
     * @param display true to display assembler/linker output
     * @return linked absolute module
     */
    static AbsoluteModule buildCodeExtendedMultibank2(
        final String[] code,
        final boolean display
    ) {
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", code, display ? _assemblerDisplayAll : _assemblerDisplayNone);
        assert(relModule != null);

        Map<Integer, List<Linker.LCPoolSpecification>> poolSpecMap = new HashMap<>(); //  keyed by BDI
        for (Integer lcIndex : relModule._storage.keySet()) {
            Linker.LCPoolSpecification poolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
            int bdi = lcIndex + 4;
            if (!poolSpecMap.containsKey(bdi)) {
                poolSpecMap.put(bdi, new LinkedList<Linker.LCPoolSpecification>());
            }
            poolSpecMap.get(bdi).add(poolSpec);
        }

        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
        int dataBReg = 2;
        for (Map.Entry<Integer, List<Linker.LCPoolSpecification>> entry : poolSpecMap.entrySet()) {
            int bdi = entry.getKey();
            List<Linker.LCPoolSpecification> poolSpecs = entry.getValue();
            Linker.BankDeclaration.Builder builder = new Linker.BankDeclaration.Builder();
            builder.setAccessInfo(new AccessInfo(3, 0));
            builder.setBankName(String.format("BANK%06o", bdi));
            builder.setBankDescriptorIndex(bdi);
            builder.setBankLevel(6);
            builder.setNeedsExtendedMode(true);
            builder.setStartingAddress(01000);
            builder.setPoolSpecifications(poolSpecs.toArray(new Linker.LCPoolSpecification[0]));
            if (bdi == 05) {
                //  first code bank
                builder.setInitialBaseRegister(0);
                builder.setSpecialAccessPermissions(new AccessPermissions(true, true, false));
                builder.setGeneralAccessPermissions(new AccessPermissions(false, false, false));
            }
            else if ((bdi & 01) == 0) {
                //  all data banks
                builder.setInitialBaseRegister(dataBReg++);
                builder.setSpecialAccessPermissions(new AccessPermissions(false, true, true));
                builder.setGeneralAccessPermissions(new AccessPermissions(false, false, false));
            } else {
                //  all other code banks
                builder.setSpecialAccessPermissions(new AccessPermissions(true, true, true));
                builder.setGeneralAccessPermissions(new AccessPermissions(false, false, false));
            }

            bankDeclarations.add(builder.build());
        }

        Linker linker = new Linker();
        return linker.link("TEST",
                           bankDeclarations.toArray(new Linker.BankDeclaration[0]),
                           32,
                           display ? _linkerDisplayAll : _linkerDisplayNone);
    }

    /**
     * Creates the banking environment absolute module
     */
    static private void createBankingModule(
    ) {
        //  Assemble banking source - there will be 8 location counter pools 0 through 7
        //  which correspond to BDT's 0 through 7 - see linking step for the reasons why
        Assembler asm = new Assembler();
        RelocatableModule bdtModule = asm.assemble("BDT", BDT_CODE, _assemblerDisplayNone);
        assert(bdtModule != null);

        //  Assemble interrupt handler code into a separate relocatable module...
        //  we have no particular expectations with respect to location counters;
        //  For simplicity, we put all of this code into one IH bank - again, see linking step.
        RelocatableModule ihModule = asm.assemble("IH", IH_CODE, _assemblerDisplayNone);
        assert(ihModule != null);

        //  Now we link - we need to create a separate bank for each of the Bank Descriptor Tables
        //  (to make loading it into the MSP easier), and one bank for the interrupt handler code.
        //  BDI's for the BDT banks are 000 through 007; BDI for the IH code is 010.
        Linker.BankDeclaration[] bankDeclarations = new Linker.BankDeclaration[9];

        for (int lcIndex = 0; lcIndex < 8; ++lcIndex) {
            Linker.LCPoolSpecification[] bdtPoolSpecs = {
                new Linker.LCPoolSpecification(bdtModule, lcIndex)
            };

            bankDeclarations[lcIndex] =
                new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 0, (short) 0))
                                                    .setBankName(String.format("BDTLEVEL%d", lcIndex))
                                                    .setBankDescriptorIndex(lcIndex)
                                                    .setBankLevel(0)
                                                    .setNeedsExtendedMode(false)
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
            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 0, (short) 0))
                                                .setBankName("IHBANK")
                                                .setBankDescriptorIndex(010)
                                                .setBankLevel(0)
                                                .setNeedsExtendedMode(true)
                                                .setStartingAddress(01000)
                                                .setPoolSpecifications(ihPoolSpecList.toArray(new Linker.LCPoolSpecification[0]))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                .build();

        Linker.Option[] options = {
            Linker.Option.OPTION_NO_ENTRY_POINT,
        };
        Linker linker = new Linker();
        _bankModule = linker.link("BDT-IH", bankDeclarations, 0, options);
        assert(_bankModule != null);
    }

    /**
     * Establishes the banking environment
     * @param ip the IP which will have its various registers set appropriately to account for the created environment
     * @param msp the MSP in which we'll create the environment
     */
    private static void establishBankingEnvironment(
        final InstrumentedInstructionProcessor ip,
        final InstrumentedMainStorageProcessor msp
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
                long bdtBankSize = bdtBank._content.getSize();
                MSPRegionAttributes bdtAttributes = new MSPRegionAttributes(bdtBank._bankName,
                                                                            0,
                                                                            bdtBank._bankDescriptorIndex);
                RegionTracker.SubRegion bdtSub = msp._regions.assign(bdtBankSize, bdtAttributes);
                msp.getStorage(0).load(bdtBank._content, (int) bdtSub._position);
                System.out.println(String.format("Loaded BDT bank at MSP offset %d for %d words",
                                                 bdtSub._position,
                                                 bdtSub._extent));

                int bankLower = bdtBank._startingAddress;
                int bankUpper = bdtBank._startingAddress + bdtBank._content.getSize() - 1;
                String attrString = String.format("BDT%d", level);

                RegionTracker.SubRegion subRegion =
                    msp._regions.assign(bdtBank._content.getSize(),
                                        new MSPRegionAttributes(attrString, 0, level));
                AbsoluteAddress absAddr = new AbsoluteAddress(msp.getUPI(), 0, (int) subRegion._position);

                BaseRegister bReg =
                    new BaseRegister(absAddr,
                                     false,
                                     bankLower,
                                     bankUpper,
                                     bdtBank._accessInfo,
                                     bdtBank._generalPermissions,
                                     bdtBank._specialPermissions);
                ip.setBaseRegister(16 + level, bReg);
                bReg._storage.load(bdtBank._content, 0);
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
                new BaseRegister(new AbsoluteAddress(msp.getUPI(), 0, (int) stackSubRegion._position),
                                 false,
                                 0,
                                 stackSize - 1,
                                 new AccessInfo((byte) 0, (short) 0),
                                 new AccessPermissions(false, false, false),
                                 new AccessPermissions(false, true, true));
            ip.setBaseRegister(InstructionProcessor.ICS_BASE_REGISTER, bReg);
            bReg._storage.load(msp.getStorage(0), (int) stackSubRegion._position, stackSize, 0);
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
        ArraySlice array = ip.getBaseRegister(baseRegisterIndex)._storage;
        long[] result = new long[array.getSize()];
        for (int ax = 0; ax < array.getSize(); ++ax) {
            result[ax] = array.get(ax);
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
     * @throws AddressingExceptionInterrupt for an invalid MSP reference
     */
    private static BankDescriptor loadBank(
        final InstructionProcessor ip,
        final InstrumentedMainStorageProcessor msp,
        final LoadableBank bank,
        final int bankLevel,
        final int bankDescriptorIndex
    ) throws AddressingExceptionInterrupt {
        assert(bankLevel >= 0) && (bankLevel < 8);

        //  Load the bank into memory
        try {
            int bankLower = bank._startingAddress;
            int bankUpper = bank._startingAddress + bank._content.getSize() - 1;
            String attrString = String.format("BDT%d", bankLevel);

            RegionTracker.SubRegion subRegion =
                msp._regions.assign(bank._content.getSize(),
                                    new MSPRegionAttributes(attrString, bankLevel, bankDescriptorIndex));
            AbsoluteAddress absAddr = new AbsoluteAddress(msp.getUPI(), 0, (int) subRegion._position);
            msp.getStorage(0).load(bank._content, absAddr._offset);

            //  Create a bank descriptor for it in the appropriate bdt
            ArraySlice bankDescriptorTable = ip.getBaseRegister(16 + bankLevel)._storage;
            BankDescriptor bd = new BankDescriptor(bankDescriptorTable, 8 * bankDescriptorIndex);
            bd.setAccessLock(bank._accessInfo);
            bd.setBankType(bank._isExtendedMode ? BankDescriptor.BankType.ExtendedMode : BankDescriptor.BankType.BasicMode);
            bd.setBaseAddress(absAddr);
            bd.setGeneralAccessPermissions(bank._generalPermissions);
            bd.setGeneralFault(false);
            bd.setLargeBank(false);
            bd.setLowerLimit(bankLower >> 9);
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
     * @throws AddressingExceptionInterrupt for an invalid MSP reference
     */
    private static void loadBanks(
        final InstructionProcessor ip,
        final InstrumentedMainStorageProcessor msp,
        final AbsoluteModule module
    ) throws AddressingExceptionInterrupt {
        for (LoadableBank loadableBank : module._loadableBanks.values()) {
            BankDescriptor bd = loadBank(ip, msp, loadableBank, loadableBank._bankLevel, loadableBank._bankDescriptorIndex);

            if (loadableBank._initialBaseRegister != null) {
                int brIndex = loadableBank._initialBaseRegister;
                ArraySlice storageSubset =
                    new ArraySlice(msp.getStorage(0),
                                   bd.getBaseAddress()._offset,
                                   bd.getUpperLimitNormalized() - bd.getLowerLimitNormalized() + 1);
                int bankLower = loadableBank._startingAddress;
                int bankUpper = loadableBank._startingAddress + loadableBank._content.getSize() - 1;
                BaseRegister bReg = new BaseRegister(bd.getBaseAddress(),
                                                     false,
                                                     bankLower,
                                                     bankUpper,
                                                     loadableBank._accessInfo,
                                                     loadableBank._generalPermissions,
                                                     loadableBank._specialPermissions);
                ip.setBaseRegister(brIndex, bReg);
                // storage is null because of limits
                if (!bReg._voidFlag) {
                    bReg._storage.load(storageSubset, 0);
                }

                if (brIndex == 0) {
                    //  based on B0 - put L,BDI in PAR
                    int lbdi = (loadableBank._bankLevel << 15) | loadableBank._bankDescriptorIndex;
                    ip.setProgramAddressRegister((long) lbdi << 18);
                } else if (brIndex == 25) {
                    //  this is a return control stack to be based on B25 - load the appropriate X register EX0
                    try {
                        long value = loadableBank._startingAddress + loadableBank._content.getSize();
                        ip.setGeneralRegister(GeneralRegisterSet.EX0, value);
                    } catch (MachineInterrupt ex) {
                    }
                } else {
                    //  based on something other than B0, set active base table
                    ActiveBaseTableEntry abte = new ActiveBaseTableEntry(loadableBank._bankLevel,
                                                                         loadableBank._bankDescriptorIndex,
                                                                         0);
                    ip.loadActiveBaseTableEntry(brIndex - 1, abte);
                }

                System.out.println(String.format("  To be based on B%d llNorm=0%o ulNorm=0%o",
                                                 loadableBank._initialBaseRegister,
                                                 bReg._lowerLimitNormalized,
                                                 bReg._upperLimitNormalized));
            }
        }
    }

    /**
     * Instantiates IP and MSP, loads a module, and sets up the processor state as needed
     * @param absoluteModule module to be loaded
     * @return Processors object so that calling code has access to the created IP and MSP
     */
    static Processors loadModule(
        final AbsoluteModule absoluteModule
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException {
        InstrumentedInstructionProcessor ip = new InstrumentedInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        InstrumentedMainStorageProcessor msp = new InstrumentedMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule);

        //  Update designator register if directed by the absolute module
        DesignatorRegister dReg = ip.getDesignatorRegister();

        if (absoluteModule._setQuarter) {
            dReg.setQuarterWordModeEnabled(true);
        } else if (absoluteModule._setThird) {
            dReg.setQuarterWordModeEnabled(false);
        }

        if (absoluteModule._afcmSet) {
            dReg.setArithmeticExceptionEnabled(true);
        } else if (absoluteModule._afcmClear) {
            dReg.setArithmeticExceptionEnabled(false);
        }

        dReg.setBasicModeEnabled(!absoluteModule._entryPointBank._isExtendedMode);
        dReg.setProcessorPrivilege(3);

        //  Set processor address
        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._entryPointAddress);

        return new Processors(ip, msp);
    }

    /**
     * Brute-force dump of almost everything we might want to know.
     * Includes elements of IP state, as well as the content of all loaded banks in the MSP
     * @param processors contains the IP and MSP of interest
     */
    static void showDebugInfo(
        final Processors processors
    ) {
        InstrumentedInstructionProcessor ip = processors._instructionProcessor;
        InstrumentedMainStorageProcessor msp = processors._mainStorageProcessor;
        DesignatorRegister dr = ip.getDesignatorRegister();
        int oldpp = dr.getProcessorPrivilege();
        dr.setProcessorPrivilege(0);

        try {
            System.out.println("Debug Info:");
            System.out.println(String.format("  PAR: %012o", ip.getProgramAddressRegister().getW()));
            System.out.println(String.format("  DR:  %012o", ip.getDesignatorRegister().getW()));
            final int regsPerLine = 4;

            System.out.println("  GRS:");
            for (int x = 0; x < 16; x += regsPerLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  X%d-X%d:", x, x + regsPerLine - 1));
                while (sb.length() < 12) { sb.append(' '); }
                for (int y = 0; y < regsPerLine; ++y) {
                    sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.X0 + x + y).getW()));
                }
                System.out.println(sb.toString());
            }

            for (int x = 0; x < 16; x += regsPerLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  A%d-A%d:", x, x + regsPerLine - 1));
                while (sb.length() < 12) { sb.append(' '); }
                for (int y = 0; y < regsPerLine; ++y) {
                    sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.A0 + x + y).getW()));
                }
                System.out.println(sb.toString());
            }

            for (int x = 0; x < 16; x += regsPerLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  R%d-R%d:", x, x + regsPerLine - 1));
                while (sb.length() < 12) { sb.append(' '); }
                for (int y = 0; y < regsPerLine; ++y) {
                    sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.R0 + x + y).getW()));
                }
                System.out.println(sb.toString());
            }

            for (int x = 0; x < 16; x += regsPerLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  EX%d-EX%d:", x, x + regsPerLine - 1));
                while (sb.length() < 12) { sb.append(' '); }
                for (int y = 0; y < regsPerLine; ++y) {
                    sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.EX0 + x + y).getW()));
                }
                System.out.println(sb.toString());
            }

            for (int x = 0; x < 16; x += regsPerLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  EA%d-EA%d:", x, x + regsPerLine - 1));
                while (sb.length() < 12) { sb.append(' '); }
                for (int y = 0; y < regsPerLine; ++y) {
                    sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.EA0 + x + y).getW()));
                }
                System.out.println(sb.toString());
            }

            for (int x = 0; x < 16; x += regsPerLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  ER%d-ER%d:", x, x + regsPerLine - 1));
                while (sb.length() < 12) { sb.append(' '); }
                for (int y = 0; y < regsPerLine; ++y) {
                    sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.ER0 + x + y).getW()));
                }
                System.out.println(sb.toString());
            }

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
                        for (int sx = 0; sx < br._storage.getSize(); sx += 8) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("      %08o:", sx));
                            for (int sy = 0; sy < 8; ++sy) {
                                if ( sx + sy < br._storage.getSize() ) {
                                    sb.append(String.format(" %012o", br._storage.get(sx + sy)));
                                }
                            }
                            System.out.println(sb.toString());
                        }
                    }
                }
            }

            for (int level = 0; level < 8; ++level) {
                System.out.println(String.format("  Level %d Banks:", level));
                BaseRegister br = ip.getBaseRegister(InstructionProcessor.L0_BDT_BASE_REGISTER + level);
                int firstBDI = (level == 0) ? 32 : 0;
                for (int bdi = firstBDI; bdi < br._storage.getSize() >> 3; ++bdi) {
                    BankDescriptor bd = new BankDescriptor(br._storage, 8 * bdi);
                    if (bd.getBaseAddress()._upi > 0) {
                        System.out.println(String.format("    BDI=%06o AbsAddr=%o:%o Lower:%o Upper:%o Type:%s",
                                                         bdi,
                                                         bd.getBaseAddress()._upi,
                                                         bd.getBaseAddress()._offset,
                                                         bd.getLowerLimitNormalized(),
                                                         bd.getUpperLimitNormalized(),
                                                         bd.getBankType().name()));
                        int len = bd.getUpperLimitNormalized() - bd.getLowerLimitNormalized() + 1;
                        for (int ix = 0; ix < len; ix += 8) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("      %08o:%08o  ", ix + bd.getLowerLimitNormalized(), ix));
                            for (int iy = 0; iy < 8; ++iy) {
                                if (ix + iy < len) {
                                    sb.append(String.format(" %012o",
                                                            msp.getStorage(bd.getBaseAddress()._segment).get(ix + iy + bd.getBaseAddress()._offset)));
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

        dr.setProcessorPrivilege(oldpp);
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
