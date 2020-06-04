/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.*;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.*;
import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.klink.BankDeclaration;
import com.kadware.komodo.kex.klink.LCPoolSpecification;
import com.kadware.komodo.kex.klink.LinkOption;
import com.kadware.komodo.kex.klink.LinkResult;
import com.kadware.komodo.kex.klink.LinkType;
import com.kadware.komodo.kex.klink.Linker;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Base class for all Test_InstructionProcessor_* classes
 */
class BaseFunctions {

    static final Set<AssemblerOption> DEFAULT_ASSEMBLER_OPTIONS = new HashSet<>();
    static {
        DEFAULT_ASSEMBLER_OPTIONS.add(AssemblerOption.EMIT_SOURCE);
        DEFAULT_ASSEMBLER_OPTIONS.add(AssemblerOption.EMIT_MODULE_SUMMARY);
        DEFAULT_ASSEMBLER_OPTIONS.add(AssemblerOption.EMIT_DICTIONARY);
        DEFAULT_ASSEMBLER_OPTIONS.add(AssemblerOption.EMIT_GENERATED_CODE);
    }

    static final Set<LinkOption> DEFAULT_LINK_OPTIONS = new HashSet<>();
    static {
        DEFAULT_LINK_OPTIONS.add(LinkOption.EMIT_SUMMARY);
        DEFAULT_LINK_OPTIONS.add(LinkOption.EMIT_DICTIONARY);
        DEFAULT_LINK_OPTIONS.add(LinkOption.EMIT_LCPOOL_MAP);
        DEFAULT_LINK_OPTIONS.add(LinkOption.EMIT_GENERATED_CODE);
    }

    Assembler _assembler = null;
    Set<AssemblerOption> _assemblerOptions = DEFAULT_ASSEMBLER_OPTIONS;
    AssemblerResult _assemblerResult = null;

    Linker _linker = null;
    Set<LinkOption> _linkOptions = DEFAULT_LINK_OPTIONS;
    LinkResult _linkResult = null;

    InventoryManager _inventoryManager = null;
    InstructionProcessor _instructionProcessor = null;
    InstrumentedMainStorageProcessor _mainStorageProcessor = null;
    SystemProcessor _systemProcessor = null;


    //  Assembler source for the interrupt handlers
    static private final String[] IH_CODE = {
        "          $EXTEND",
        "          $INFO 1 3",
        "          $INFO 10 1",
        "",
        "$(1)      . Interrupt handlers",
        "IH_00     . Interrupt 0:Reserved - Hardware Default",
        "          HALT 01000",
        "",
        "IH_01     . Interrupt 1:Hardware Check",
        "          HALT 01001",
        "",
        "IH_02     . Interrupt 2:Diagnostic",
        "          HALT 01002",
        "",
        "IH_03     . Interrupt 3:Reserved",
        "          HALT 01003",
        "",
        "IH_04     . Interrupt 4:Reserved",
        "          HALT 01004",
        "",
        "IH_05     . Interrupt 5:Reserved",
        "          HALT 01005",
        "",
        "IH_06     . Interrupt 6:Reserved",
        "          HALT 01006",
        "",
        "IH_07     . Interrupt 7:Reserved",
        "          HALT 01007",
        "",
        "IH_10     . Interrupt 8:Reference Violation",
        "          HALT 01010",
        "",
        "IH_11     . Interrupt 9:Addressing Exception",
        "          HALT 01011",
        "",
        "IH_12     . Interrupt 10 Terminal Addressing Exception",
        "          HALT 01012",
        "",
        "IH_13     . Interrupt 11 RCS/Generic Stack Under/Overflow",
        "          HALT 01013",
        "",
        "IH_14     . Interrupt 12 Signal",
        "          HALT 01014",
        "",
        "IH_15     . Interrupt 13 Test & Set",
        "          HALT 01015",
        "",
        "IH_16     . Interrupt 14 Invalid Instruction",
        "          HALT 01016",
        "",
        "IH_17     . Interrupt 15 Page Exception",
        "          HALT 01017",
        "",
        "IH_20     . Interrupt 16 Arithmetic Exception",
        "          HALT 01020",
        "",
        "IH_21     . Interrupt 17 Data Exception",
        "          HALT 01021",
        "",
        "IH_22     . Interrupt 18 Operation Trap",
        "          HALT 01022",
        "",
        "IH_23     . Interrupt 19 Breakpoint",
        "          HALT 01023",
        "",
        "IH_24     . Interrupt 20 Quantum Timer",
        "          HALT 01024",
        "",
        "IH_25     . Interrupt 21 Reserved",
        "          HALT 01025",
        "",
        "IH_26     . Interrupt 22 Reserved",
        "          HALT 01026",
        "",
        "IH_27     . Interrupt 23 Page(s) Zeroed",
        "          HALT 01027",
        "",
        "IH_30     . Interrupt 24 Software Break",
        "          HALT 01030",
        "",
        "IH_31     . Interrupt 25 Jump History Full",
        "          HALT 01031",
        "",
        "IH_32     . Interrupt 26 Reserved",
        "          HALT 01032",
        "",
        "IH_33     . Interrupt 27 Dayclock",
        "          HALT 01033",
        "",
        "IH_34     . Interrupt 28 Performance Monitoring",
        "          HALT 01034",
        "",
        "IH_35     . Interrupt 29 Initial Program Load (IPL)",
        "          HALT 01035",
        "",
        "IH_36     . UPI Initial",
        "          HALT 01036",
        "",
        "IH_37     . UPI Normal",
        "          HALT 01037",
        "",
        "IH$INIT*  . CALL this to establish interrupt handler vectors",
        "          . which we expect to be based on B16.",
        "          . Do not be in exec-register-set-selection, nor 24-bit index mode.",
        "          . Also, be in PPriv < 2.",
        "          . It is highly recommended to be in PAIJ, but not required.",
        "          LXI,U     X1,1",
        "          LXM,U     X1,0",
        "          LXI,U     A0,LBDIREF$+IH_00",
        "          LXM,U     A0,IH_00",
        "          LA,U      A1,31",
        "LOOP      SA        A0,0,*X1,B16",
        "          AA,U      A0,1",
        "          JGD       A1,LOOP",
        "          RTN       0",
    };

    private static RelocatableModule _ihRelocatable = null;


    /**
     * Builds a binary executable consisting of a code bank and a data bank, which contains all the code generated from source.
     * All even location counter pools go in the data bank, all odd location counter pools go in the code bank.
     */
    void buildDualBank(
        final String[] source
    ) throws MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(_assemblerOptions)
                                            .setModuleName("BINARY-REL")
                                            .build();
        _assemblerResult = _assembler.assemble();
        assertNotNull(_assemblerResult._relocatableModule);
        assertFalse(_assemblerResult._diagnostics.hasError());

        List<LCPoolSpecification> poolSpecsCode = new LinkedList<>();
        List<LCPoolSpecification> poolSpecsData = new LinkedList<>();
        for (int lcIndex : _assemblerResult._relocatableModule.getEstablishedLocationCounterIndices()) {
            LCPoolSpecification lcpSpec = new LCPoolSpecification(_assemblerResult._relocatableModule, lcIndex);
            if ((lcIndex & 1) == 0) {
                poolSpecsData.add(lcpSpec);
            } else {
                poolSpecsCode.add(lcpSpec);
            }
        }

        BankDeclaration.BankDeclarationOption[] codeBankOpts = {
//            BankDeclaration.BankDeclarationOption.EXTENDED_MODE,
            BankDeclaration.BankDeclarationOption.WRITE_PROTECT
        };
        BankDeclaration.BankDeclarationOption[] dataBankOpts = {
            BankDeclaration.BankDeclarationOption.DBANK,
            BankDeclaration.BankDeclarationOption.DYNAMIC
        };

        BankDeclaration[] bankDeclarations = {
            new BankDeclaration.Builder().setBankName("CODE")
                                         .setStartingAddress(01000)
                                         .setBankLevel(1)
                                         .setBankDescriptorIndex(4)
                                         .setOptions(codeBankOpts)
                                         .setAccessInfo(new AccessInfo(0, 0))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, false))
                                         .setPoolSpecifications(poolSpecsCode)
                                         .build(),
            new BankDeclaration.Builder().setBankName("DATA")
                                         .setStartingAddress(0)
                                         .setBankLevel(1)
                                         .setBankDescriptorIndex(5)
                                         .setOptions(dataBankOpts)
                                         .setAccessInfo(new AccessInfo(0, 0))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                         .setPoolSpecifications(poolSpecsData)
                                         .build(),
        };

        _linker = new Linker.Builder().setModuleName("BINARY")
                                      .setOptions(_linkOptions)
                                      .setBankDeclarations(bankDeclarations)
                                      .build();
        _linkResult = _linker.link(LinkType.MULTI_BANKED_BINARY);
        assertNotNull(_linkResult._loadableBanks);
        assertNotEquals(0, _linkResult._loadableBanks.length);
        assertEquals(0, _linkResult._errorCount);

        createProcessors();
    }

    private void buildInterruptHandlerModule() {
        if (_ihRelocatable == null) {
            Assembler asm = new Assembler.Builder().setModuleName("IH")
                                                   .setSource(IH_CODE)
                                                   .setOptions(DEFAULT_ASSEMBLER_OPTIONS)
                                                   .build();
            AssemblerResult asmResult = asm.assemble();
            assertNotNull(asmResult._relocatableModule);
            _ihRelocatable = asmResult._relocatableModule;
        }
    }

    /**
     * Builds a binary executable consisting of one bank per location counter.
     * Odd-number BDIs are static read-only
     * Even-number BDIs are dynamic dbanks, read-write, no enter
     */
    void buildMultiBank(
        final String[] source,
        final boolean includeInterruptHandlers
    ) throws MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(_assemblerOptions)
                                            .setModuleName("BINARY-REL")
                                            .build();
        _assemblerResult = _assembler.assemble();
        assertNotNull(_assemblerResult._relocatableModule);
        assertFalse(_assemblerResult._diagnostics.hasError());

        List<BankDeclaration> bankDeclarations = new LinkedList<>();

        BankDeclaration.BankDeclarationOption[] codeBankOpts = {
//            BankDeclaration.BankDeclarationOption.EXTENDED_MODE,
            BankDeclaration.BankDeclarationOption.WRITE_PROTECT,
        };
        BankDeclaration.BankDeclarationOption[] dataBankOpts = {
            BankDeclaration.BankDeclarationOption.DBANK,
            BankDeclaration.BankDeclarationOption.DYNAMIC
        };

        AccessPermissions codeSAP = new AccessPermissions(true, true, false);
        AccessPermissions dataSAP = new AccessPermissions(false, true, true);
        AccessPermissions gap = new AccessPermissions(false, false, false);
        AccessInfo accessLock = new AccessInfo(0, 0);

        int bdIndex = 000004;
        for (int lcIndex : _assemblerResult._relocatableModule.getEstablishedLocationCounterIndices()) {
            LCPoolSpecification[] lcpSpecs = { new LCPoolSpecification(_assemblerResult._relocatableModule, lcIndex) };
            if ((lcIndex & 1) == 1) {
                String bankName = String.format("IBANK%06o", bdIndex);
                BankDeclaration bankDecl = new BankDeclaration.Builder().setBankDescriptorIndex(bdIndex)
                                                                        .setBankLevel(1)
                                                                        .setBankName(bankName)
                                                                        .setPoolSpecifications(lcpSpecs)
                                                                        .setSpecialAccessPermissions(codeSAP)
                                                                        .setGeneralAccessPermissions(gap)
                                                                        .setStartingAddress(01000)
                                                                        .setAccessInfo(accessLock)
                                                                        .setOptions(codeBankOpts)
                                                                        .build();
                bankDeclarations.add(bankDecl);
            } else {
                String bankName = String.format("DBANK%06o", bdIndex);
                BankDeclaration bankDecl = new BankDeclaration.Builder().setBankDescriptorIndex(bdIndex)
                                                                        .setBankLevel(1)
                                                                        .setBankName(bankName)
                                                                        .setPoolSpecifications(lcpSpecs)
                                                                        .setSpecialAccessPermissions(dataSAP)
                                                                        .setGeneralAccessPermissions(gap)
                                                                        .setStartingAddress(0)
                                                                        .setAccessInfo(accessLock)
                                                                        .setOptions(dataBankOpts)
                                                                        .build();
                bankDeclarations.add(bankDecl);
            }

            bdIndex++;
        }

        if (includeInterruptHandlers) {
            buildInterruptHandlerModule();
            LCPoolSpecification[] lcpSpecs = { new LCPoolSpecification(_ihRelocatable, 1) };
            BankDeclaration bankDecl = new BankDeclaration.Builder().setBankDescriptorIndex(bdIndex)
                                                                    .setBankLevel(1)
                                                                    .setBankName("IHBANK")
                                                                    .setPoolSpecifications(lcpSpecs)
                                                                    .setSpecialAccessPermissions(codeSAP)
                                                                    .setGeneralAccessPermissions(gap)
                                                                    .setStartingAddress(01000)
                                                                    .setAccessInfo(new AccessInfo(0, 0))
                                                                    .setOptions(codeBankOpts)
                                                                    .build();
            bankDeclarations.add(bankDecl);
        }

        _linker = new Linker.Builder().setModuleName("BINARY")
                                      .setOptions(_linkOptions)
                                      .setBankDeclarations(bankDeclarations)
                                      .build();
        _linkResult = _linker.link(LinkType.MULTI_BANKED_BINARY);
        assertNotNull(_linkResult._loadableBanks);
        assertNotEquals(0, _linkResult._loadableBanks.length);
        assertEquals(0, _linkResult._errorCount);

        createProcessors();
    }

    /**
     * Builds a binary executable consisting of a single bank, which contains all the code generated from source
     */
    void buildSimple(
        final String[] source
    ) throws MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(_assemblerOptions)
                                            .setModuleName("BINARY-REL")
                                            .build();
        _assemblerResult = _assembler.assemble();
        assertNotNull(_assemblerResult._relocatableModule);
        assertFalse(_assemblerResult._diagnostics.hasError());

        RelocatableModule[] relModules = { _assemblerResult._relocatableModule };
        Linker linker = new Linker.Builder().setRelocatableModules(relModules)
                                            .setOptions(_linkOptions)
                                            .setModuleName("BINARY")
                                            .build();
        _linkResult = linker.link(LinkType.BINARY);
        assertNotNull(_linkResult._loadableBanks);
        assertNotEquals(0, _linkResult._loadableBanks.length);
        assertEquals(0, _linkResult._errorCount);

        createProcessors();
    }

    void ipl(
        final boolean wait
    ) throws BinaryLoadException,
             MachineInterrupt,
             UPINotAssignedException,
             UPIProcessorTypeException {
        assertNotNull(_linkResult);
        assertNotNull(_linkResult._loadableBanks);
        assertNotNull(_linkResult._programStartInfo);
        assertNotNull(_instructionProcessor);
        assertNotNull(_mainStorageProcessor);
        assertNotNull(_systemProcessor);

        _instructionProcessor.setDevelopmentMode(true);
        _instructionProcessor.setTraceInstructions(true);
        _systemProcessor.iplBinary("TEST",
                                   _linkResult._loadableBanks,
                                   _linkResult._programStartInfo._vAddress,
                                   _mainStorageProcessor._upiIndex,
                                   _instructionProcessor._upiIndex,
                                   false,
                                   false);

        //  wait for IP to start
        while (_instructionProcessor.isStopped()) {
            Thread.onSpinWait();
        }

        //  maybe wait for IP to stop
        if (wait) {
            while (!_instructionProcessor.isStopped()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    //  do nothing
                }
            }
        }
    }


//
//    static private final String[] BDT_CODE = {
//        "          $EXTEND",
//        "",
//        "BANKSPERLVL* $EQU 64",
//        "BANKTABLESZ* $EQU 8*BANKSPERLVL . 8 words per BD",
//        "",
//        "$(0),BDT_LEVEL0* . Interrupt handler vectors (total of 64 vectors)",
//        "                 . L,BDI is 000000+33, assuming the IH code is in bank 33",
//        "          + 33,IH_00",
//        "          + 33,IH_01",
//        "          + 33,IH_02",
//        "          + 33,IH_03",
//        "          + 33,IH_04",
//        "          + 33,IH_05",
//        "          + 33,IH_06",
//        "          + 33,IH_07",
//        "          + 33,IH_10",
//        "          + 33,IH_11",
//        "          + 33,IH_12",
//        "          + 33,IH_13",
//        "          + 33,IH_14",
//        "          + 33,IH_15",
//        "          + 33,IH_16",
//        "          + 33,IH_17",
//        "          + 33,IH_20",
//        "          + 33,IH_21",
//        "          + 33,IH_22",
//        "          + 33,IH_23",
//        "          + 33,IH_24",
//        "          + 33,IH_25",
//        "          + 33,IH_26",
//        "          + 33,IH_27",
//        "          + 33,IH_30",
//        "          + 33,IH_31",
//        "          + 33,IH_32",
//        "          + 33,IH_33",
//        "          + 33,IH_34",
//        "          + 33,IH_35",
//        "          + 33,IH_36",
//        "          + 33,IH_37",
//        "          $RES      32                  . Interrupts 32-63 are not defined",
//        "          $RES      (8*32)-64           . Unused remainder of 32 BDs not valid",
//        "          $RES      8*32                . Space for 32 exec banks, BDI 32 to 41",
//        "",
//        "$(1),BDT_LEVEL1* $RES BANKTABLESZ",
//        "$(2),BDT_LEVEL2* $RES BANKTABLESZ",
//        "$(3),BDT_LEVEL3* $RES BANKTABLESZ",
//        "$(4),BDT_LEVEL4* $RES BANKTABLESZ",
//        "$(5),BDT_LEVEL5* $RES BANKTABLESZ",
//        "$(6),BDT_LEVEL6* $RES BANKTABLESZ",
//        "$(7),BDT_LEVEL7* $RES BANKTABLESZ",
//    };
//
//    //  Absolute module for the above code...
//    //  The BDT will be in banks 0 through 7, one per BDT level (0 for 0, 1 for 1, etc)
//    //  IH code will be in bank 33 (2nd BD in level 0 BDT) (our convention, it'll work)
//    static private AbsoluteModule _bankModule = null;
//
//    private static final Assembler.Option[] _assemblerDisplayAll = {
//        Assembler.Option.EMIT_MODULE_SUMMARY,
//        Assembler.Option.EMIT_DICTIONARY,
//        Assembler.Option.EMIT_GENERATED_CODE,
//        Assembler.Option.EMIT_SOURCE,
//    };
//
//    private static final Assembler.Option[] _assemblerDisplayNone = {};
//
//    private static final Linker.Option[] _linkerDisplayAll = {
//        Linker.Option.OPTION_EMIT_DICTIONARY,
//        Linker.Option.OPTION_EMIT_GENERATED_CODE,
//        Linker.Option.OPTION_EMIT_SUMMARY,
//    };
//
//    private static final Linker.Option[] _linkerDisplayNone = {};
//
//    /**
//     * Assembles sets of code into a relocatable module, then links it such that the odd-numbered lc pools
//     * are placed in an IBANK with BDI 04 and the even-number pools in a DBANK with BDI 05.
//     * @param code arrays of text comprising the source code we assemble
//     * @param display true to display assembler/linker output
//     * @return linked absolute module
//     */
//    static AbsoluteModule buildCodeBasic(
//        final String[] code,
//        final boolean display
//    ) {
//        Assembler asm = new Assembler();
//        OldRelocatableModule relModule = asm.assemble("TEST", code, display ? _assemblerDisplayAll : _assemblerDisplayNone);
//        assert(relModule != null);
//
//        List<Linker.LCPoolSpecification> poolSpecsEven = new LinkedList<>();
//        List<Linker.LCPoolSpecification> poolSpecsOdd = new LinkedList<>();
//        for (Integer lcIndex : relModule._storage.keySet()) {
//            if ((lcIndex & 01) == 01) {
//                Linker.LCPoolSpecification oddPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
//                poolSpecsOdd.add(oddPoolSpec);
//            } else {
//                Linker.LCPoolSpecification evenPoolSpec = new Linker.LCPoolSpecification(relModule, lcIndex);
//                poolSpecsEven.add(evenPoolSpec);
//            }
//        }
//
//        List<Linker.BankDeclaration> bankDeclarations = new LinkedList<>();
//        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
//                                                                 .setBankName("I1")
//                                                                 .setBankDescriptorIndex(000004)
//                                                                 .setBankLevel(06)
//                                                                 .setStartingAddress(022000)
//                                                                 .setPoolSpecifications(poolSpecsOdd.toArray(new Linker.LCPoolSpecification[0]))
//                                                                 .setInitialBaseRegister(12)
//                                                                 .setGeneralAccessPermissions(new AccessPermissions(true, true, true))
//                                                                 .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
//                                                                 .build());
//
//        bankDeclarations.add(new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 0))
//                                                                 .setBankName("D1")
//                                                                 .setBankDescriptorIndex(000005)
//                                                                 .setBankLevel(06)
//                                                                 .setStartingAddress(040000)
//                                                                 .setPoolSpecifications(poolSpecsEven.toArray(new Linker.LCPoolSpecification[0]))
//                                                                 .setInitialBaseRegister(13)
//                                                                 .setGeneralAccessPermissions(new AccessPermissions(false, true, true))
//                                                                 .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
//                                                                 .build());
//
//        Linker linker = new Linker();
//        return linker.link("TEST",
//                           bankDeclarations.toArray(new Linker.BankDeclaration[0]),
//                           0,
//                           display ? _linkerDisplayAll : _linkerDisplayNone);
//    }
//
    /**
     * Clears the class state for a subsequent build/ipl process
     */
    void clear(
    ) throws UPINotAssignedException {
        if (_instructionProcessor != null) {
            _instructionProcessor.stop(InstructionProcessor.StopReason.Cleared, 0);
            while (!_instructionProcessor.isStopped()) {
                Thread.onSpinWait();
            }
            _instructionProcessor.terminate();
            InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
            _instructionProcessor = null;
        }

        if (_mainStorageProcessor != null) {
            _mainStorageProcessor.terminate();
            InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
            _mainStorageProcessor = null;
        }

        if (_systemProcessor != null) {
            _systemProcessor.terminate();
            InventoryManager.getInstance().deleteProcessor(_systemProcessor._upiIndex);
            _systemProcessor = null;
        }

        _assembler = null;
        _assemblerOptions = DEFAULT_ASSEMBLER_OPTIONS;
        _assemblerResult = null;

        _linker = null;
        _linkOptions = DEFAULT_LINK_OPTIONS;
        _linkResult = null;
    }

    /**
     * Common processor creation code
     */
    private void createProcessors(
    ) throws MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        _inventoryManager = InventoryManager.getInstance();
        _systemProcessor = _inventoryManager.createSystemProcessor("SP0",
                                                                   8080,
                                                                   null,
                                                                   new Credentials("test", "test"));
        _instructionProcessor = _inventoryManager.createInstructionProcessor("IP0");
        _mainStorageProcessor = new InstrumentedMainStorageProcessor("MSP0",
                                                                     (short) 1,
                                                                     8 * 1024 * 1024);
        _inventoryManager.addMainStorageProcessor(_mainStorageProcessor);
    }

    /**
     * Retrieves the contents of a bank represented by a base register.
     * This is a copy of the content, so if you update the result, you don't screw up the actual content in the MSP.
     */
    long[] getBankByBaseRegister(
        final int baseRegisterIndex
    ) {
        ArraySlice array = _instructionProcessor.getBaseRegister(baseRegisterIndex)._storage;
        long[] result = new long[array.getSize()];
        for (int ax = 0; ax < array.getSize(); ++ax) {
            result[ax] = array.get(ax);
        }
        return result;
    }

    /**
     * Brute-force dump of almost everything we might want to know.
     * Includes elements of IP state, as well as the content of all loaded banks in the MSP
     */
    void showDebugInfo() {
        InstructionProcessor ip = _instructionProcessor;
        InstrumentedMainStorageProcessor msp = _mainStorageProcessor;
        InstructionProcessor.DesignatorRegister dr = ip.getDesignatorRegister();
        int oldpp = dr.getProcessorPrivilege();
        dr.setProcessorPrivilege(0);

        try {
            System.out.println("Debug Info:");
            System.out.println(String.format("  PAR: %012o", ip.getProgramAddressRegister().get()));
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
                InstructionProcessor.BaseRegister br = ip.getBaseRegister(bx);
                System.out.println(String.format("    B%d base:%s(%s) lower:%d upper:%d",
                                                 bx,
                                                 br._voidFlag ? "(VOID)" : "",
                                                 br._baseAddress.toString(),
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
                                if (sx + sy < br._storage.getSize()) {
                                    sb.append(String.format(" %012o", br._storage.get(sx + sy)));
                                }
                            }
                            System.out.println(sb.toString());
                        }
                    }
                } else if (bx == 25) {
                    System.out.println("    Base register describes the RCS stack; Content follows:");
                    if (br._storage != null) {
                        for (int sx = 0; sx < br._storage.getSize(); sx += 8) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("      %08o:", sx));
                            for (int sy = 0; sy < 8; ++sy) {
                                if (sx + sy < br._storage.getSize()) {
                                    sb.append(String.format(" %012o", br._storage.get(sx + sy)));
                                }
                            }
                            System.out.println(sb.toString());
                        }
                    }
                } else if (bx == 26) {
                    System.out.println("    Base register describes the ICS stack; Content follows:");
                    if (br._storage != null) {
                        for (int sx = 0; sx < br._storage.getSize(); sx += 8) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("      %08o:", sx));
                            for (int sy = 0; sy < 8; ++sy) {
                                if (sx + sy < br._storage.getSize()) {
                                    sb.append(String.format(" %012o", br._storage.get(sx + sy)));
                                }
                            }
                            System.out.println(sb.toString());
                        }
                    }
                }
            }

            for (int level = 0; level < 8; ++level) {
                InstructionProcessor.BaseRegister br = ip.getBaseRegister(InstructionProcessor.L0_BDT_BASE_REGISTER + level);
                System.out.println(String.format("  Level %d Banks:%s", level, (br == null) ? "<BR not set>" : ""));
                if (br != null) {
                    int firstBDI = (level == 0) ? 32 : 0;
                    if (br._storage == null) {
                        System.out.println("    no storage");
                    } else {
                        for (int bdi = firstBDI; bdi < br._storage.getSize() >> 3; ++bdi) {
                            BankDescriptor bd =
                                new BankDescriptor(br._storage, 8 * bdi);
                            if (bd.getBaseAddress()._upiIndex > 0) {
                                System.out.println(String.format("    BDI=%06o AbsAddr=%s Lower:%o Upper:%o ProcessorType:%s",
                                                                 bdi,
                                                                 bd.getBaseAddress().toString(),
                                                                 bd.getLowerLimitNormalized(),
                                                                 bd.getUpperLimitNormalized(),
                                                                 bd.getBankType().name()));
                                int len = bd.getUpperLimitNormalized() - bd.getLowerLimitNormalized() + 1;
                                for (int ix = 0; ix < len; ix += 8) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(String.format("      %08o:%08o  ", ix + bd.getLowerLimitNormalized(), ix));
                                    for (int iy = 0; iy < 8; ++iy) {
                                        if (ix + iy < len) {
                                            ArraySlice mspStorage = msp.getStorage(bd.getBaseAddress()._segment);
                                            long value = mspStorage.get(ix + iy + bd.getBaseAddress()._offset);
                                            sb.append(String.format(" %012o", value));
                                        }
                                    }
                                    System.out.println(sb.toString());
                                }
                            }
                        }
                    }
                }
            }
        } catch (MachineInterrupt ex) {
            System.out.println("Caught:" + ex.getMessage());
        }

        dr.setProcessorPrivilege(oldpp);
    }
}
