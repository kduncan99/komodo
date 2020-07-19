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
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.klink.BankDeclaration;
import com.kadware.komodo.kex.klink.LCPoolSpecification;
import com.kadware.komodo.kex.klink.LinkOption;
import com.kadware.komodo.kex.klink.LinkResult;
import com.kadware.komodo.kex.klink.LinkType;
import com.kadware.komodo.kex.klink.Linker;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
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

    private static final String[] GEN_DEFS_SOURCE = {
        ". ................................",
        ". Generally Useful Definitions",
        ".",
        "DR$SETPP*   $PROC",
        ". P(1,1) is the requested privilege, 0 to 3",
        "PPRIV     $EQU      DR$SETPP(1, 0)",
        "VALUE     $EQU      PPRIV*/20",
        "",
        "          SD        A13",
        "          AND       A13,(0777763777777)",
        "          OR        A14,(VALUE)",
        "          LD        A15",
        "          $END",
        "",
        ".",
        "DR$SETQWORD* $PROC",
        "          SD        A14",
        "          OR,U      A14,010",
        "          LD        A15",
        "          $END",
        ".",
        "DR$CLRQWORD* $PROC",
        "          SD        A14",
        "          AND       A14,(0777777,0777767)",
        "          LD        A15",
        "          $END",
        ".",
        "          $END"
    };

    private static final String[] CHP_DEFS_SOURCE = {
        ". ................................",
        ". Channel Program Definitions",
        ".",
        "CHP_IOPUPI   $EQUF 0,,S1",
        "CHP_CHMOD    $EQUF 0,,S2",
        "CHP_DEVNUM   $EQUF 0,,S3",
        "CHP_FUNCTION $EQUF 0,,S4",
        "CHP_FORMAT   $EQUF 0,,S5",
        "CHP_ACWS     $EQUF 0,,S6",
        "CHP_BLKADDR  $EQUF 1,,W",
        "CHP_CHANSTAT $EQUF 2,,S1",
        "CHP_DEVSTAT  $EQUF 2,,S2",
        "CHP_RESBYTES $EQUF 2,,S3",
        "CHP_WORDS    $EQUF 3,,H2",
        "CHP_ACWS     $EQUF 4",
        ".",
        "             $END"
    };

    private static final String[] SYSC_DEFS_SOURCE = {
        ". ................................",
        ". SYSC Instruction Definitions",
        ".",
        "SYSC$CREATE*  $EQU 020",
        "SYSC$DELETE*  $EQU 021",
        "SYSC$RESIZE*  $EQU 022",
        ".",
        "SYSC$CNSTAT*  $EQU 030",
        "SYSC$CNREAD*  $EQU 031",
        "SYSC$CNRDREP* $EQU 032",
        "SYSC$CNPOLL*  $EQU 033",
        "SYSC$CNRESET* $EQU 034",
        ".",
        "SYSC$OK*      $EQU 0    . Request successful",
        "SYSC$BADUPI*  $EQU 01   . Given UPI is not a main storage processor",
        "SYSC$BADSEG*  $EQU 02   . Given segment index is unknown to the given MSP",
        "SYSC$INVADDR* $EQU 03   . Given address is invalid or does not exist",
        "SYSC$INVSIZE* $EQU 04   . Requested size is out of range or invalid",
        "SYSC$NACCESS* $EQU 05   . Access denied",
        ".",
        "SYSC$FORM*    $FORM 6,6,6,18",
        "SYSC$SUBFUNC* $EQUF 0,,S1",
        "SYSC$STATUS*  $EQUF 0,,S2",
        "SYSC$MSPUPI*  $EQUF 0,,S3",
        "SYSC$MEMSEG*  $EQUF 1,,W",
        "SYSC$MEMSIZE* $EQUF 2,,W",
        ".",
        "DEFAULT$MSP*  $EQU 01",
        ".",
        "             $END"
    };

    private static final Map<String, Dictionary> _definitionSets = new HashMap<>();

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
        "          . Do be in extended mode with PPriv < 2.",
        "          . Do not be in exec-register-set-selection, nor 24-bit index mode.",
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

    //  Prologue and epilogue code for most test cases

    private static final String[] BASIC_MODE_SETUP = {
        "          $EXTEND",
        "          $INFO 1 3",
        "          $INFO 10 1",
        ".",
        "$(0),LCZERO$",     //  data space for individual tests
        "",
        "$(2)",
        ". RETURN CONTROL STACK",
        "RCDEPTH   $EQU      32",
        "RCSSIZE   $EQU      2*RCDEPTH",
        "RCSTACK   $RES      RCSSIZE",
        ".",
        "$(1)      $LIT",
        "START",
        "          LD        (000001,000000)     . ext mode, exec regs, pp=0",
        "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
        "          LXI,U     EX0,0",
        "          LXM,U     EX0,RCSTACK+RCSSIZE",
        ".",
        "          LD        (0,0)               . ext mode, user regs, pp=0",
        "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
        ".",
        "          LBU       B15,(LBDIREF$+LCZERO$, 0)",
        "          GOTO      (LBDIREF$+BMSTART, BMSTART)",
        ".",
        "          $BASIC",
        "$(3)      $LIT",
        "BMSTART"           //  code space for individual tests
    };

    private static final String[] EXTENDED_MODE_SETUP = {
        "          $EXTEND",
        "          $INFO 1 3",
        "          $INFO 10 1",
        "",
        "$(0),LCZERO$",     //  data space for individual tests
        "",
        "$(2)",
        ". RETURN CONTROL STACK",
        "RCDEPTH   $EQU      32",
        "RCSSIZE   $EQU      2*RCDEPTH",
        "RCSTACK   $RES      RCSSIZE",
        ".",
        "$(1)      $LIT",
        "START",
        "          LD        (000001,000000) . ext mode, exec regs, pp=0",
        "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
        "          LXI,U     EX0,0",
        "          LXM,U     EX0,RCSTACK+RCSSIZE",
        ".",
        "          LD        (0,0)           . ext mode, user regs, pp=0",
        "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
        ".",
        "          LBU       B2,(LBDIREF$+LCZERO$, 0)",
        ".",                //   code space for individual tests
    };

    private static final String[] END = {
        "",
        "          $END      START"
    };

    String[] wrapForBasicMode(
        final String[] source
    ) {
        String[] result = new String[BASIC_MODE_SETUP.length + source.length + END.length];
        System.arraycopy(BASIC_MODE_SETUP, 0, result, 0, BASIC_MODE_SETUP.length);
        System.arraycopy(source, 0, result, BASIC_MODE_SETUP.length, source.length);
        System.arraycopy(END, 0, result, BASIC_MODE_SETUP.length + source.length, END.length);
        return result;
    }

    String[] wrapForExtendedMode(
        final String[] source
    ) {
        String[] result = new String[EXTENDED_MODE_SETUP.length + source.length + END.length];
        System.arraycopy(EXTENDED_MODE_SETUP, 0, result, 0, EXTENDED_MODE_SETUP.length);
        System.arraycopy(source, 0, result, EXTENDED_MODE_SETUP.length, source.length);
        System.arraycopy(END, 0, result, EXTENDED_MODE_SETUP.length + source.length, END.length);
        return result;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private things
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Builds a particular definition set and inserts it into our container of definition sets
     */
    private static void buildDefinitionSet(
        final String name,
        final String[] source
    ) {
        Set<AssemblerOption> defOptions = new HashSet<>(DEFAULT_ASSEMBLER_OPTIONS);
        defOptions.add(AssemblerOption.DEFINITION_MODE);
        Assembler asm = new Assembler.Builder().setModuleName(name)
                                               .setOptions(defOptions)
                                               .setSource(source)
                                               .build();
        AssemblerResult asmResult = asm.assemble();
        Assert.assertFalse(asmResult._diagnostics.hasError());
        _definitionSets.put(name, asmResult._definitions);
    }

    /**
     * Builds all the canned definition sets for future $INCLUDE in test code
     */
    private static Map<String, Dictionary> buildDefinitionSets() {
        if (_definitionSets.isEmpty()) {
            buildDefinitionSet("GEN$DEFS", GEN_DEFS_SOURCE);
            buildDefinitionSet("CHP$DEFS", CHP_DEFS_SOURCE);
            buildDefinitionSet("SYSC$DEFS", SYSC_DEFS_SOURCE);
        }

        return _definitionSets;
    }

    /**
     * Builds the interrupt handler relocatable module for any unit test needing to use it
     */
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
     * Common configuration creation code
     * Creates the following configuration:
     *      1 System Processor
     *      1 Instruction Processor
     *      1 Input Output Processor with
     *          1 Byte Channel Module at index 0 with
     *              1 Scratch Disk Device at index 0
     *          1 Word Channel Module at index 1
     *      1 Main Storage Processor
     */
    void createConfiguration(
    ) throws CannotConnectException,
             ChannelModuleIndexConflictException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPIInvalidException {
        createConfiguration(1, 1, 1);
    }

    /**
     * Common processor creation code
     * Creates 1 system processor, a number of other processors as indicated by the parameters,
     * and the following other nodes:
     *      1 byte channel module CHMBx0 per IOP, at index 0
     *      1 word channel module CHMWx1 per IOP, at index 1
     *      1 scratch disk device on all IOPs, CHMBx0 at index 0
     * @param ipCount number of IPs to create
     * @param iopCount number of IOPs to create
     * @param mspCount number of MSPs to create
     */
    void createConfiguration(
        final int ipCount,
        final int iopCount,
        final int mspCount
    ) throws CannotConnectException,
             ChannelModuleIndexConflictException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPIInvalidException {
        assertFalse(ipCount >= InventoryManager.MAX_IPS);
        assertFalse(iopCount >= InventoryManager.MAX_IOPS);
        assertFalse(mspCount >= InventoryManager.MAX_MSPS);

        InventoryManager im = InventoryManager.getInstance();
        im.createSystemProcessor("SP0",
                                 8080,
                                 null,
                                 new Credentials("test", "test"));

        for (int px = 0; px < mspCount; ++px) {
            MainStorageProcessor msp = new InstrumentedMainStorageProcessor(String.format("MSP%d", px),
                                                                            (short) 1,
                                                                            8 * 1024 * 1024);
            im.addMainStorageProcessor(msp);
        }

        for (int px = 0; px < ipCount; ++px) {
            im.createInstructionProcessor(String.format("IP%d", px));
        }

        Set<ChannelModule> byteChannelModules = new HashSet<>();
        Set<ChannelModule> wordChannelModules = new HashSet<>();
        for (int px = 0; px < iopCount; ++px) {
            InputOutputProcessor iop = im.createInputOutputProcessor(String.format("IOP%d", px));
            ChannelModule cm0 = im.createChannelModule(ChannelModule.ChannelModuleType.Byte,
                                                       String.format("CHMB%d0", px),
                                                       iop,
                                                       0);
            byteChannelModules.add(cm0);
            ChannelModule cm1 = im.createChannelModule(ChannelModule.ChannelModuleType.Word,
                                                       String.format("CHMW%d1", px),
                                                       iop,
                                                       1);
            wordChannelModules.add(cm1);
        }

        //TODO create disk
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Package-private things for unit tests to invoke
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Builds a binary executable consisting of a code bank and a data bank, which contains all the code generated from source.
     * All even location counter pools go in the data bank, all odd location counter pools go in the code bank.
     * @param source source code to be built
     */
    void buildDualBank(
        final String[] source
    ) {
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(_assemblerOptions)
                                            .setModuleName("BINARY-REL")
                                            .setDefinitionSets(buildDefinitionSets())
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
    }

    /**
     * Builds a binary executable consisting of one bank per location counter.
     * Includes an interrupt handler bank.
     * Odd-number BDIs are static read-only
     * Even-number BDIs are dynamic dbanks, read-write, no enter
     * @param source source code to be built
     * @param avoidDBankCollision databanks are adjusted upwards to avoid addressing collision - usually only useful for basic mode
     * @param writeableCodeBanks true if code banks are to be marked as writeable (for SLJ instruction tests)
     */
    void buildMultiBank(
        final String[] source,
        final boolean avoidDBankCollision,
        final boolean writeableCodeBanks
    ) {
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(_assemblerOptions)
                                            .setModuleName("BINARY-REL")
                                            .setDefinitionSets(buildDefinitionSets())
                                            .build();
        _assemblerResult = _assembler.assemble();
        assertNotNull(_assemblerResult._relocatableModule);
        assertFalse(_assemblerResult._diagnostics.hasError());

        List<BankDeclaration> bankDeclarations = new LinkedList<>();

        Set<BankDeclaration.BankDeclarationOption> codeBankOptsSet = new HashSet<>();
        if (!writeableCodeBanks) {
            codeBankOptsSet.add(BankDeclaration.BankDeclarationOption.WRITE_PROTECT);
        }
        BankDeclaration.BankDeclarationOption[] codeBankOpts =
            codeBankOptsSet.toArray(new BankDeclaration.BankDeclarationOption[0]);

        BankDeclaration.BankDeclarationOption[] dataBankOpts = {
            BankDeclaration.BankDeclarationOption.DBANK,
            BankDeclaration.BankDeclarationOption.DYNAMIC
        };

        AccessPermissions codeSAP = new AccessPermissions(true, true, writeableCodeBanks);
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
                                                                        .setAvoidCollision(avoidDBankCollision)
                                                                        .setAccessInfo(accessLock)
                                                                        .setOptions(dataBankOpts)
                                                                        .build();
                bankDeclarations.add(bankDecl);
            }

            bdIndex++;
        }

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

        _linker = new Linker.Builder().setModuleName("BINARY")
                                      .setOptions(_linkOptions)
                                      .setBankDeclarations(bankDeclarations)
                                      .build();
        _linkResult = _linker.link(LinkType.MULTI_BANKED_BINARY);
        assertNotNull(_linkResult._loadableBanks);
        assertNotEquals(0, _linkResult._loadableBanks.length);
        assertEquals(0, _linkResult._errorCount);
    }

    /**
     * Builds a binary executable consisting of a single bank, which contains all the code generated from source
     * @param source source code to be built
     */
    void buildSimple(
        final String[] source
    ) {
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(_assemblerOptions)
                                            .setModuleName("BINARY-REL")
                                            .setDefinitionSets(buildDefinitionSets())
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
    }

    /**
     * Clears the class state for a subsequent build/ipl process
     */
    void clear(
    ) {
        InventoryManager.getInstance().clearConfiguration();

        _assembler = null;
        _assemblerOptions = DEFAULT_ASSEMBLER_OPTIONS;
        _assemblerResult = null;

        _linker = null;
        _linkOptions = DEFAULT_LINK_OPTIONS;
        _linkResult = null;
    }

    /**
     * Retrieves the contents of a bank represented by a base register.
     * This is a copy of the content, so if you update the result, you don't screw up the actual content in the MSP.
     */
    long[] getBankByBaseRegister(
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

    InputOutputProcessor getFirstIOP() {
        try {
            return InventoryManager.getInstance().getInputOutputProcessor(InventoryManager.FIRST_IOP_UPI_INDEX);
        } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
            return null;
        }
    }

    InstructionProcessor getFirstIP() {
        try {
            return InventoryManager.getInstance().getInstructionProcessor(InventoryManager.FIRST_IP_UPI_INDEX);
        } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
            return null;
        }
    }

    MainStorageProcessor getFirstMSP() {
        try {
            return InventoryManager.getInstance().getMainStorageProcessor(InventoryManager.FIRST_MSP_UPI_INDEX);
        } catch (UPINotAssignedException | UPIProcessorTypeException ex) {
            return null;
        }
    }

    /**
     * Using the results of a previous build, we call on the SP to load the created banks and start the IP
     * (for multi-IP configurations, we start the first one, and load into the first MSP)
     * @param wait true to wait until the IP halts, false to return immediately after starting the IP
     */
    void ipl(
        final boolean wait
    ) throws BinaryLoadException,
             MachineInterrupt,
             UPINotAssignedException,
             UPIProcessorTypeException {
        assertNotNull(_linkResult);
        assertNotNull(_linkResult._loadableBanks);
        assertNotNull(_linkResult._programStartInfo);

        InventoryManager im = InventoryManager.getInstance();
        InstructionProcessor ip0 = im.getInstructionProcessor(InventoryManager.FIRST_IP_UPI_INDEX);
        MainStorageProcessor msp0 = im.getMainStorageProcessor(InventoryManager.FIRST_MSP_UPI_INDEX);
        SystemProcessor sp0 = im.getSystemProcessor(InventoryManager.FIRST_SP_UPI_INDEX);

        assertNotNull(ip0);
        assertNotNull(msp0);
        assertNotNull(sp0);

        ip0.setDevelopmentMode(true);
        ip0.setTraceInstructions(true);
        sp0.iplBinary("TEST",
                      _linkResult._loadableBanks,
                      _linkResult._programStartInfo._vAddress,
                      msp0._upiIndex,
                      ip0._upiIndex,
                      false,
                      false);

        //  wait for IP to start
        while (ip0.isStopped()) {
            Thread.onSpinWait();
        }

        //  maybe wait for IP to stop
        if (wait) {
            while (!ip0.isStopped()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    //  do nothing
                }
            }
        }
    }

    /**
     * Brute-force dump of almost everything we might want to know.
     * Includes elements of IP state, as well as the content of all loaded banks in the MSP
     */
    void showDebugInfo() {
        System.out.println("Debug Info:");
        InventoryManager im = InventoryManager.getInstance();
        for (int upi = InventoryManager.FIRST_IP_UPI_INDEX; upi < InventoryManager.LAST_IP_UPI_INDEX; ++upi) {
            try {
                InstructionProcessor ip = im.getInstructionProcessor(upi);
                DesignatorRegister dr = ip.getDesignatorRegister();
                int oldpp = dr.getProcessorPrivilege();
                dr.setProcessorPrivilege(0);

                System.out.println("  " + ip._name);
                System.out.println(String.format("    PAR: %012o", ip.getProgramAddressRegister().get()));
                System.out.println(String.format("    DR:  %012o", ip.getDesignatorRegister().getW()));
                final int regsPerLine = 4;

                System.out.println("    GRS:");
                for (int x = 0; x < 16; x += regsPerLine) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    X%d-X%d:", x, x + regsPerLine - 1));
                    while (sb.length() < 12) { sb.append(' '); }
                    for (int y = 0; y < regsPerLine; ++y) {
                        sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.X0 + x + y).getW()));
                    }
                    System.out.println(sb.toString());
                }

                for (int x = 0; x < 16; x += regsPerLine) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    A%d-A%d:", x, x + regsPerLine - 1));
                    while (sb.length() < 12) { sb.append(' '); }
                    for (int y = 0; y < regsPerLine; ++y) {
                        sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.A0 + x + y).getW()));
                    }
                    System.out.println(sb.toString());
                }

                for (int x = 0; x < 16; x += regsPerLine) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    R%d-R%d:", x, x + regsPerLine - 1));
                    while (sb.length() < 12) { sb.append(' '); }
                    for (int y = 0; y < regsPerLine; ++y) {
                        sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.R0 + x + y).getW()));
                    }
                    System.out.println(sb.toString());
                }

                for (int x = 0; x < 16; x += regsPerLine) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    EX%d-EX%d:", x, x + regsPerLine - 1));
                    while (sb.length() < 12) { sb.append(' '); }
                    for (int y = 0; y < regsPerLine; ++y) {
                        sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.EX0 + x + y).getW()));
                    }
                    System.out.println(sb.toString());
                }

                for (int x = 0; x < 16; x += regsPerLine) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    EA%d-EA%d:", x, x + regsPerLine - 1));
                    while (sb.length() < 12) { sb.append(' '); }
                    for (int y = 0; y < regsPerLine; ++y) {
                        sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.EA0 + x + y).getW()));
                    }
                    System.out.println(sb.toString());
                }

                for (int x = 0; x < 16; x += regsPerLine) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    ER%d-ER%d:", x, x + regsPerLine - 1));
                    while (sb.length() < 12) { sb.append(' '); }
                    for (int y = 0; y < regsPerLine; ++y) {
                        sb.append(String.format(" %012o", ip.getGeneralRegister(GeneralRegisterSet.ER0 + x + y).getW()));
                    }
                    System.out.println(sb.toString());
                }

                System.out.println("    Base Registers:");
                for (int bx = 0; bx < 32; ++bx) {
                    BaseRegister br = ip.getBaseRegister(bx);
                    System.out.println(String.format("      B%d base:%s(%s) lower:%d upper:%d",
                                                     bx,
                                                     br._voidFlag ? "(VOID)" : "",
                                                     br._baseAddress.toString(),
                                                     br._lowerLimitNormalized,
                                                     br._upperLimitNormalized));
                    if (bx >= 16 && bx < 24) {
                        System.out.println(String.format("      Base register refers to BDT level %d; BDT Content follows:",
                                                         bx - 16));
                        if (br._storage != null) {
                            for (int sx = 0; sx < br._storage.getSize(); sx += 8) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(String.format("        %08o:", sx));
                                for (int sy = 0; sy < 8; ++sy) {
                                    if (sx + sy < br._storage.getSize()) {
                                        sb.append(String.format(" %012o", br._storage.get(sx + sy)));
                                    }
                                }
                                System.out.println(sb.toString());
                            }
                        }
                    } else if (bx == 25) {
                        System.out.println("      Base register describes the RCS stack; Content follows:");
                        if (br._storage != null) {
                            for (int sx = 0; sx < br._storage.getSize(); sx += 8) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(String.format("        %08o:", sx));
                                for (int sy = 0; sy < 8; ++sy) {
                                    if (sx + sy < br._storage.getSize()) {
                                        sb.append(String.format(" %012o", br._storage.get(sx + sy)));
                                    }
                                }
                                System.out.println(sb.toString());
                            }
                        }
                    } else if (bx == 26) {
                        System.out.println("      Base register describes the ICS stack; Content follows:");
                        if (br._storage != null) {
                            for (int sx = 0; sx < br._storage.getSize(); sx += 8) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(String.format("        %08o:", sx));
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
                    BaseRegister br = ip.getBaseRegister(InstructionProcessor.L0_BDT_BASE_REGISTER + level);
                    System.out.println(String.format("    Level %d Banks:%s", level, (br == null) ? "<BR not set>" : ""));
                    if (br != null) {
                        int firstBDI = (level == 0) ? 32 : 0;
                        if (br._storage == null) {
                            System.out.println("      no storage");
                        } else {
                            for (int bdi = firstBDI; bdi < br._storage.getSize() >> 3; ++bdi) {
                                BankDescriptor bd =
                                    new BankDescriptor(br._storage, 8 * bdi);
                                if (bd.getBaseAddress()._upiIndex > 0) {
                                    System.out.println(String.format("      BDI=%06o AbsAddr=%s Lower:%o Upper:%o ProcessorType:%s",
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
                                                AbsoluteAddress baseAddr = bd.getBaseAddress();
                                                MainStorageProcessor msp = im.getMainStorageProcessor(baseAddr._upiIndex);
                                                ArraySlice mspStorage = msp.getStorage(baseAddr._segment);
                                                long value = mspStorage.get(ix + iy + baseAddr._offset);
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

                dr.setProcessorPrivilege(oldpp);
            } catch (UPINotAssignedException ex) {
                //  not notable
            } catch (MachineInterrupt | UPIProcessorTypeException ex) {
                System.out.println("Caught:" + ex.getMessage());
            }
        }
    }
}
