/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Links one or more OldRelocatableModule objects into an AbsoluteModule object
 */
@SuppressWarnings("Duplicates")
public class Linker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Enums, inner classes, etc
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * A symbol representing an offset (again, possibly of 0) from an undefined symbol.
     * E.g., consider the following assembler code:
     *      ADDR* $EQU LIBRARY+010
     * assuming LIBRARY is undefined to the assembler, then
     *      'ADDR' would be the symbol
     *      010 would be the base value
     *      'LIBRARY' would be the undefined symbol
     */
    //TODO is this even possible? Should assembler reject the $EQU above?
    //  If we keep this around, at least raise it from being an inner class
    public static class UndefinedSymbolRelativeSymbolEntry extends SymbolEntry {

        final String _undefinedSymbol;

        UndefinedSymbolRelativeSymbolEntry(
            final String symbol,
            final long baseValue,
            final String undefinedSymbol
        ) {
            super(symbol, baseValue);
            _undefinedSymbol = undefinedSymbol;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class data
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Bank declarations keyed by L,BDI (3-bit L, 15-bit BDI)
    private final Map<Integer, BankDeclaration> _bankDeclarations;

    private final String _moduleName;
    private final Set<LinkOption> _options;
    private final PrintStream _printStream;
    private final List<RelocatableModule> _relocatableModules;  //  some day the order might be important

    private final BankGeometryMap _bankGeometries = new BankGeometryMap();
    private boolean _bankImplied;
    private int _errors = 0;
    private LinkType _linkType;

    AFCMSensitivity _afcmSensitivity;
    PartialWordSensitivity _partialWordSensitivty;
    ProgramStartInfo _programStartInfo;

    /**
     * Generated bank descriptors
     */
    private final Map<Integer, LoadableBank> _loadableBanks = new TreeMap<>();

    /**
     * Raw content of the various banks, keyed by bank L,BDI in vaddr format
     */
    private final Map<Integer, long[]> _bankContent = new TreeMap<>();

    /**
     * Symbol table
     */
    private final Map<String, SymbolEntry> _symbolTable = new HashMap<>();

    /**
     * Maps location counter pools to the banks which will eventually (or now do) contain them.
     * Also used for resolving undefined references.
     */
    private final Map<LCPoolSpecification, VirtualAddress> _poolMap = new HashMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    private Linker(
        final String moduleName,
        final Map<Integer, BankDeclaration> bankDeclarations,
        final Set<LinkOption> options,
        final PrintStream printStream,
        final List<RelocatableModule> relocatableModules
    ) {
        _bankDeclarations = bankDeclarations;
        _moduleName = moduleName;
        _options = options;
        _printStream = printStream;
        _relocatableModules = relocatableModules;
    }

    public static class Builder {

        private final Map<Integer, BankDeclaration> _bankDeclarations = new TreeMap<>();
        private String _moduleName = "<unnamed>";
        private Set<LinkOption> _options = new HashSet<>();
        private PrintStream _printStream = new PrintStream(System.out);
        private List<RelocatableModule> _relocatableModules = new LinkedList<>();

        /**
         * See setBankDeclarations() - this method adds one to the existing list
         */
        public Builder addBankDeclaration(
            final BankDeclaration bd
        ) {
            _bankDeclarations.put(bd._bankDescriptorIndex, bd);
            return this;
        }

        /**
         * See setOptions() - this method adds one to the existing list
         */
        public Builder addOption(
            final LinkOption option
        ) {
            _options.add(option);
            return this;
        }

        /**
         * See setRelocatableModule() - this method adds one to the existing list
         */
        public Builder addRelocatableModule(
            final RelocatableModule module
        ) {
            _relocatableModules.add(module);
            return this;
        }

        /**
         * For bank-declared linkages - defines the attributes and content of each bank to be created.
         * If specified for absolute, multi-banked-binary, or object linkages, the relocatable modules list is ignored.
         * Ignored for binary linkages (which are always bank-implied).
         */
        public Builder setBankDeclarations(
            final BankDeclaration[] declarations
        ) {
            _bankDeclarations.clear();
            for (BankDeclaration bd : declarations) {
                int lbdi = ((bd._bankLevel & 07) << 15) | (bd._bankDescriptorIndex & 077777);
                _bankDeclarations.put(lbdi, bd);
            }
            return this;
        }

        /**
         * For bank-declared linkages - defines the attributes and content of each bank to be created.
         * If specified for absolute, multi-banked-binary, or object linkages, the relocatable modules list is ignored.
         * Ignored for binary linkages (which are always bank-implied).
         */
        public Builder setBankDeclarations(
            final List<BankDeclaration> declarations
        ) {
            _bankDeclarations.clear();
            for (BankDeclaration bd : declarations) {
                int lbdi = ((bd._bankLevel & 07) << 15) | (bd._bankDescriptorIndex & 077777);
                _bankDeclarations.put(lbdi, bd);
            }
            return this;
        }

        /**
         * Defines the name of the absolute module
         */
        public Builder setModuleName(
            final String name
        ) {
            _moduleName = name;
            return this;
        }

        /**
         * Defines the options which control this linkage.
         */
        public Builder setOptions(
            final LinkOption[] options
        ) {
            _options.clear();
            Collections.addAll(_options, options);
            return this;
        }

        /**
         * Defines the options which control this linkage.
         */
        public Builder setOptions(
            final Collection<LinkOption> options
        ) {
            _options = new HashSet<>(options);
            return this;
        }

        /**
         * Determines where all diagnostic output is sent.
         * If unspecified, output goes to System.out.
         */
        public Builder setOutputStream(
            final OutputStream outputStream
        ) {
            _printStream = new PrintStream(outputStream);
            return this;
        }

        /**
         * Determines where all diagnostic output is sent.
         * If unspecified, output goes to System.out.
         */
        public Builder setOutputStream(
            final PrintStream printStream
        ) {
            _printStream = printStream;
            return this;
        }

        /**
         * Required for bank-implied linkages (binary linkages are always bank implied).
         * The banking scheme which will be invoked depends on the linkage type, but it
         * will use all the code from these modules for the linkage.
         * Replaces any relocatable modules already provided.
         */
        public Builder setRelocatableModules(
            final RelocatableModule[] modules
        ) {
            _relocatableModules.clear();
            Collections.addAll(_relocatableModules, modules);
            return this;
        }

        /**
         * Required for bank-implied linkages (binary linkages are always bank implied).
         * The banking scheme which will be invoked depends on the linkage type, but it
         * will use all the code from these modules for the linkage.
         * Replaces any relocatable modules already provided.
         */
        public Builder setRelocatableModules(
            final Collection<RelocatableModule> modules
        ) {
            _relocatableModules = new LinkedList<>(modules);
            return this;
        }

        /**
         * Produces a Linker object given the various attributes which have been previously set.
         */
        public Linker build() {
            return new Linker(_moduleName, _bankDeclarations, _options, _printStream, _relocatableModules);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private Methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * One of the last things we do...
     * Creates LoadableBank objects for all the BankDeclaration objects we have on hand
     */
    private void createLoadableBanks() {
        for (BankDeclaration bankDecl : _bankDeclarations.values()) {
            int lbdi = (bankDecl._bankLevel << 15) | bankDecl._bankDescriptorIndex;
            long[] content = _bankContent.get(lbdi);
            if (content == null) {
                content = new long[0];
            }

            boolean extMode = bankDecl._options.contains(BankDeclaration.BankDeclarationOption.EXTENDED_MODE);
            Map<Integer, LCPoolSpecification> contentMap = new HashMap<>();
            for (Map.Entry<LCPoolSpecification, VirtualAddress> entry : _poolMap.entrySet()) {
                VirtualAddress vaddr = entry.getValue();
                if (vaddr.getLBDI() == lbdi) {
                    LCPoolSpecification lcpSpec = entry.getKey();
                    contentMap.put(vaddr.getOffset(), lcpSpec);
                    try {
                        RelocatableModule.RelocatablePool rcPool = lcpSpec._module.getLocationCounterPool(lcpSpec._lcIndex);
                        if (rcPool._requiresExtendedMode) {
                            extMode = true;
                        }
                    } catch (ParameterException ex) {
                        raise("Cannot find relocatable pool $(" + lcpSpec._lcIndex + ") for " + lcpSpec._module.getModuleName());
                    }
                }
            }

            BankType bankType = extMode ? BankType.ExtendedMode : BankType.BasicMode;
            int llimit;
            int ulimit;
            if (content.length == 0) {
                //  void bank
                llimit = bankDecl._startingAddress == 0 ? 01000 : bankDecl._startingAddress;
                ulimit = llimit - 1;
            } else {
                BankGeometry bg = _bankGeometries.get(lbdi);
                llimit = bg._lowerLimit;
                ulimit = bg._upperLimit;
            }

            LoadableBank bankDesc = new LoadableBank(bankDecl._bankName,
                                                     bankDecl._bankLevel,
                                                     bankDecl._bankDescriptorIndex,
                                                     bankDecl._accessInfo,
                                                     bankType,
                                                     bankDecl._generalAccessPermissions,
                                                     bankDecl._specialAccessPermissions,
                                                     llimit,
                                                     ulimit,
                                                     content,
                                                     contentMap);
            _loadableBanks.put(lbdi, bankDesc);
        }
    }

    /**
     * Determine arithmetic fault mode for the resulting module
     *
     * Arithmetic fault compatibility settings (possibly not aligned with the real collector)...
     * Options:
     *     afcm set: Regardless of rel module settings, the resulting module is afcm set sensitive
     *     afcm clear: Regardless of rel module settings, the resulting module is afcm clear sensitive
     *     afcm insensitive: Regardless of rel module settings, the resulting module has no sensitivity
     *     none:
     *         if some rel modules are afcm set sensitive and some are afcm clear sensitive:
     *             if there is a start address, the rel module's sensitivity is used
     *             otherwise, the resulting module has no sensitivity
     *         if some (or all) rel modules are set-sensitive, the resulting module is afcm set sensitive
     *         if some (or all) rel modules are clear-sensitive, the resulting module is afcm clear sensitive
     *         Otherwise, the resulting module has no sensitivity
     */
    private void determineAFCMMode() {
        boolean afcmSet = _options.contains(LinkOption.ARITHMETIC_FAULT_COMPATIBILITY_MODE);
        boolean afcmClear = _options.contains(LinkOption.ARITHMETIC_FAULT_NON_INTERRUPT_MODE);
        boolean afcmInsensitive = _options.contains(LinkOption.ARITHMETIC_FAULT_INSENSITIVE);

        int afcms = (afcmSet ? 1 : 0) + (afcmClear ? 1 : 0) + (afcmInsensitive ? 1 : 0);
        if (afcms > 1) {
            raise("Conflict in arithmetic fault option specifications - only one can be specified.");
        }

        if (afcmSet) {
            _afcmSensitivity = AFCMSensitivity.SET;
        } else if (afcmClear) {
            _afcmSensitivity = AFCMSensitivity.CLEARED;
        } else if (afcmInsensitive) {
            _afcmSensitivity = AFCMSensitivity.INSENSITIVE;
        } else {
            int relAFCMSet = 0;
            int relAFCMClear = 0;
            for (RelocatableModule rel : _relocatableModules) {
                if (rel.getAFCMClearSensitivity()) {
                    relAFCMClear++;
                    if (rel.getAFCMSetSensitivity()) {
                        raise("Relocatable module in error - afcm set and clear both specified");
                    }
                } else if (rel.getAFCMSetSensitivity()) {
                    relAFCMSet++;
                }
            }

            if ((relAFCMSet > 0) && (relAFCMClear > 0)) {
                if (_programStartInfo != null) {
                    if (_programStartInfo._lcpSpecification._module.getAFCMClearSensitivity()) {
                        _afcmSensitivity = AFCMSensitivity.CLEARED;
                    } else if (_programStartInfo._lcpSpecification._module.getAFCMSetSensitivity()) {
                        _afcmSensitivity = AFCMSensitivity.SET;
                    } else {
                        _afcmSensitivity = AFCMSensitivity.INSENSITIVE;
                    }
                } else {
                    _afcmSensitivity = AFCMSensitivity.INSENSITIVE;
                }
            } else if (relAFCMSet > 0) {
                _afcmSensitivity = AFCMSensitivity.SET;
            } else if (relAFCMClear > 0) {
                _afcmSensitivity = AFCMSensitivity.CLEARED;
            } else {
                _afcmSensitivity = AFCMSensitivity.INSENSITIVE;
            }
        }
    }

    /**
     * After we've mapped LCPools to banks, we need to determine the bank geometry
     * such that each bank has proper lower and upper addressing limits.
     * This is done *before* we try to generate output, which would require knowing the
     * lower and upper limits for any location counter references (which there always are).
     * This is where we populate _bankContent (although empty for now) and _bankGeometries.
     * ...
     * Note that we do this in two stages - first for banks which are not marked for collision avoidance,
     * then for those which are. This makes our alogirhtm easier to construct.
     */
    private void determineBankGeometry() {
        try {
            boolean first = true;
            boolean done = false;
            while (!done) {
                for (BankDeclaration bankDecl : _bankDeclarations.values()) {
                    if (first != bankDecl._avoidCollision) {
                        int bankUpperAddress = -1;
                        for (Map.Entry<LCPoolSpecification, VirtualAddress> entry : _poolMap.entrySet()) {
                            VirtualAddress va = entry.getValue();
                            if ((va.getLevel() == bankDecl._bankLevel) && (va.getBankDescriptorIndex() == bankDecl._bankDescriptorIndex)) {
                                LCPoolSpecification lcpSpec = entry.getKey();
                                RelocatableModule.RelocatablePool relPool = lcpSpec._module.getLocationCounterPool(lcpSpec._lcIndex);
                                int poolUpperAddress = va.getOffset() + relPool._content.length - 1;
                                if (poolUpperAddress > bankUpperAddress) {
                                    bankUpperAddress = poolUpperAddress;
                                }
                            }
                        }

                        int contentLength = bankUpperAddress + 1;
                        _bankGeometries.mapBankDeclaration(bankDecl, contentLength);
                        int lbdi = (bankDecl._bankLevel << 15) | bankDecl._bankDescriptorIndex;
                        _bankContent.put(lbdi, new long[contentLength]);
                    }
                }

                done = !first;
                first = !first;
            }
        } catch (ParameterException ex) {
            raise("Internal Error:" + ex.getMessage());
        }
    }

    /**
     *  Determine mode settings.
     *  Must be called after we've determined the program starting address (if any),
     *  and after we've loaded the relocatable modules container (if we were *not* bank-implied).
     */
    private void determineModes() {
        determineAFCMMode();
        determinePartialWordMode();
    }

    /**
     * Determine quarter/third word sensitivity
     *
     * If specified by option, then the option is used
     * If there are both third-word and quarter-word sensitive rel modules:
     *      If there is a start address, the containing module's setting is used (third, quarter, or none)
     *      Otherwise, the resulting module is not sensitive
     * If some (or all) rel modules are third-word sensitive, the resulting module is third-word sensitive
     * If some (or all) rel modules are quarter-word sensitive, the resulting module is quarter-word sensitive
     * Finally, (if none of the modules are sensitive and no options was specified), the resulting module is not sensitive
     */
    private void determinePartialWordMode() {
        boolean qword = _options.contains(LinkOption.QUARTER_WORD_MODE);
        boolean tword = _options.contains(LinkOption.THIRD_WORD_MODE);
        if (qword && tword) {
            raise("Conflict in partial-word-sensitive option specifications - only one can be specified.");
        }

        if (qword) {
            _partialWordSensitivty = PartialWordSensitivity.QUARTER_WORD;
        } else if (tword) {
            _partialWordSensitivty = PartialWordSensitivity.THIRD_WORD;
        } else {
            int relQWord = 0;
            int relTWord = 0;
            for (RelocatableModule rel : _relocatableModules) {
                if (rel.getQuarterWordSensitivity()) {
                    relQWord++;
                    if (rel.getThirdWordSensitivity()) {
                        raise("Relocatable module in error - quarter- and third-word sensitivity specified");
                    }
                } else if (rel.getThirdWordSensitivity()) {
                    relTWord++;
                }
            }

            if ((relQWord > 0 && (relTWord) > 0)) {
                if (_programStartInfo != null) {
                    if (_programStartInfo._lcpSpecification._module.getQuarterWordSensitivity()) {
                        _partialWordSensitivty = PartialWordSensitivity.QUARTER_WORD;
                    } else if (_programStartInfo._lcpSpecification._module.getThirdWordSensitivity()) {
                        _partialWordSensitivty = PartialWordSensitivity.THIRD_WORD;
                    } else {
                        _partialWordSensitivty = PartialWordSensitivity.INSENSITIVE;
                    }
                } else {
                    _afcmSensitivity = AFCMSensitivity.INSENSITIVE;
                }
            } else if (relQWord > 0) {
                _partialWordSensitivty = PartialWordSensitivity.QUARTER_WORD;
            } else if (relTWord > 0) {
                _partialWordSensitivty = PartialWordSensitivity.THIRD_WORD;
            } else {
                _partialWordSensitivty = PartialWordSensitivity.INSENSITIVE;
            }
        }
    }

    /**
     * Must be called after we've mapped the location counter pools,
     *  and after we've loaded the relocatable modules container (if we were *not* bank-implied).
     */
    private void determineProgramStartAddress() {
        for (RelocatableModule rel : _relocatableModules) {
            RelocatableModule.RelativeEntryPoint rep = rel.getStartAddress();
            if (rep != null) {
                LCPoolSpecification lcpSpec = new LCPoolSpecification(rel, rep._locationCounterIndex);
                VirtualAddress lcpVAddr = _poolMap.get(lcpSpec);    //  L,BDI of bank, offset from start of bank of lcpool

                //  Develop the starting address.
                //      = containing_bank_lower_limit
                //          + offset_of_lcpool_from_start_of_bank
                //          + offset_of_entry_point_from_start_of_lcpool
                BankDeclaration bankDecl = _bankDeclarations.get(lcpVAddr.getLBDI());
                int bankStart = bankDecl._startingAddress;
                int offsetFromBank = lcpVAddr.getOffset();
                int offsetFromLCPool = (int) rep._value;
                int address = bankStart + offsetFromBank + offsetFromLCPool;

                VirtualAddress startAddress = new VirtualAddress(lcpVAddr.getLevel(),
                                                                 lcpVAddr.getBankDescriptorIndex(),
                                                                 address);
                _programStartInfo = new ProgramStartInfo(startAddress,
                                                         address - bankStart,
                                                         lcpSpec,
                                                         offsetFromLCPool);
            }
        }

        if ((_programStartInfo == null) && !_options.contains(LinkOption.NO_ENTRY_POINT)) {
            raise("No program starting address");
        }
    }

    /**
     * Displays summary information
     */
    private void displaySummary() {
        boolean summary = false;
        boolean dictionary = false;
        boolean code = false;
        boolean lcpoolMap = false;
        if (_options.contains(LinkOption.EMIT_SUMMARY)) {
            summary = true;
        }
        if (_options.contains(LinkOption.EMIT_DICTIONARY)) {
            summary = true;
            dictionary = true;
        }
        if (_options.contains(LinkOption.EMIT_GENERATED_CODE)) {
            summary = true;
            code = true;
            dictionary = true;
        }
        if (_options.contains(LinkOption.EMIT_LCPOOL_MAP)) {
            summary = true;
            lcpoolMap = true;
        }

        if (summary) {
            _printStream.println("Link Module " + _moduleName + " Summary:");

            String afcmMsg;
            String pwMsg;
            if (_afcmSensitivity != null) {
                afcmMsg = switch (_afcmSensitivity) {
                    case SET -> "  AFCM-Cleared";
                    case CLEARED -> "  AFCM-Set";
                    case INSENSITIVE -> "  AFCM-Insensitive";
                };
            } else {
                afcmMsg = "n/a";
            }
            if (_partialWordSensitivty != null) {
                pwMsg = switch (_partialWordSensitivty) {
                    case THIRD_WORD -> "ThirdWord-Sensitive";
                    case QUARTER_WORD -> "QuarterWord-Sensitive";
                    case INSENSITIVE -> "PartialWord-Insensitive";
                };
            } else {
                pwMsg = "n/a";
            }
            _printStream.println("  Modes: " + afcmMsg + " " + pwMsg);

            if (_programStartInfo != null) {
                _printStream.println(String.format("  Entry Point Bank Address:%s %s $(%d)+%d",
                                                   _programStartInfo._vAddress,
                                                   _programStartInfo._lcpSpecification._module.getModuleName(),
                                                   _programStartInfo._lcpSpecification._lcIndex,
                                                   _programStartInfo._lcpOffset));
            }

            for (LoadableBank bankDesc : _loadableBanks.values()) {
                _printStream.println(String.format("  Bank %s Level:%d BDI:%06o %s Lower:%08o Upper:%08o Size:%08o",
                                                   bankDesc._bankName,
                                                   bankDesc._bankLevel,
                                                   bankDesc._bankDescriptorIndex,
                                                   bankDesc._bankType,
                                                   bankDesc._lowerLimit,
                                                   bankDesc._upperLimit,
                                                   bankDesc._upperLimit - bankDesc._lowerLimit + 1));

                for (Map.Entry<Integer, LCPoolSpecification> entry : bankDesc._contentMap.entrySet()) {
                    _printStream.println(String.format("    %08o: %s", entry.getKey(), entry.getValue()));
                }

                if (code) {
                    int wordsPerLine = 8;
                    for (int ix = 0; ix < bankDesc._content.length; ix += wordsPerLine) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("      +%08o:", ix + bankDesc._lowerLimit));
                        for (int iy = 0; iy < wordsPerLine; ++iy) {
                            if (ix + iy < bankDesc._content.length) {
                                sb.append(String.format("  %012o", bankDesc._content[ix + iy]));
                            }
                        }
                        _printStream.println(sb.toString());
                    }
                }
            }

            if (lcpoolMap) {
                _printStream.println("  LCPool Map:");
                for (Map.Entry<LCPoolSpecification, VirtualAddress> entry : _poolMap.entrySet()) {
                    _printStream.println(String.format("  %16s $(%2d): Bank %06o Offset %06o",
                                                       entry.getKey()._module.getModuleName(),
                                                       entry.getKey()._lcIndex,
                                                       entry.getValue().getLBDI(),
                                                       entry.getValue().getOffset()));
                }
            }

            if (dictionary) {
                _printStream.println("  Symbols:");
                for (SymbolEntry entry : _symbolTable.values()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("    %12s: ", entry._symbol));

                    if (entry instanceof AbsoluteSymbolEntry) {
                        AbsoluteSymbolEntry ase = (AbsoluteSymbolEntry) entry;
                        sb.append(String.format("%012o", ase._baseValue));
                        if (ase._definingModule != null) {
                            sb.append(" from ");
                            sb.append(ase._definingModule.getModuleName());
                        }
                    } else if (entry instanceof LocationCounterRelativeSymbolEntry) {
                        LocationCounterRelativeSymbolEntry lcrse = (LocationCounterRelativeSymbolEntry) entry;
                        sb.append(String.format("%s:$(%d)+%012o",
                                                lcrse._lcPoolSpecification._module.getModuleName(),
                                                lcrse._lcPoolSpecification._lcIndex,
                                                lcrse._baseValue));
                    } else if (entry instanceof UndefinedSymbolRelativeSymbolEntry) {
                        UndefinedSymbolRelativeSymbolEntry usrse = (UndefinedSymbolRelativeSymbolEntry) entry;
                        sb.append(String.format("%s+%012o", usrse._undefinedSymbol, usrse._baseValue));
                    }

                    _printStream.println(sb.toString());
                }
            }
        }
    }

    /**
     * The bank names must be made available for lookup
     */
    private void establishBankNameSymbols() {
        for (BankDeclaration bankDecl : _bankDeclarations.values()) {
            _symbolTable.put(bankDecl._bankName,
                             new AbsoluteSymbolEntry(bankDecl._bankName, bankDecl._bankDescriptorIndex));
        }
    }

    /**
     * Extracts all externalized labels - both absolute and relative - from the various
     * relocatable modules so we can satisfy undefined symbol references.
     */
    private void extractEntryPoints() {
        for (RelocatableModule rel : _relocatableModules) {
            for (RelocatableModule.EntryPoint ep : rel.getEntryPoints().values()) {
                if (ep instanceof RelocatableModule.RelativeEntryPoint) {
                    RelocatableModule.RelativeEntryPoint rep = (RelocatableModule.RelativeEntryPoint) ep;
                    SymbolEntry se = new LocationCounterRelativeSymbolEntry(rep._name, rep._value, rel, rep._locationCounterIndex);
                    _symbolTable.put(rep._name, se);
                } else if (ep instanceof RelocatableModule.AbsoluteEntryPoint) {
                    RelocatableModule.AbsoluteEntryPoint aep = (RelocatableModule.AbsoluteEntryPoint) ep;
                    SymbolEntry se = new AbsoluteSymbolEntry(aep._name, aep._value, rel);
                    _symbolTable.put(aep._name, se);
                }
            }
        }
    }

    /**
     * Integrates a new value into a subfield of an existing value.
     * Raises a diagnostic if truncation occurs.
     * @param initialValue initial 36-bit value
     * @param fieldDescriptor describes the subfield of integration (could be all 36 bits)
     * @param newValue new value to be added into the subfield of the initial value
     * @param subtraction indicates the newvalue should be subtracted rather than added
     * @param lcpSpecification only for emitting diagnostics
     * @return the new integrated value
     */
    private long integrateValue(
        final long initialValue,
        final FieldDescriptor fieldDescriptor,
        final long newValue,
        final boolean subtraction,
        final LCPoolSpecification lcpSpecification
    ) {
        long mask = (1L << fieldDescriptor._fieldSize) - 1;
        long msbMask = 1L << (fieldDescriptor._fieldSize - 1);
        long notMask = mask ^ 0_777777_777777L;
        int shift = 36 - (fieldDescriptor._fieldSize + fieldDescriptor._startingBit);

        //  A special note - we recognize that the source word is in ones-complement.
        //  The reference value *might* be negative - if that is the case, we have a bit of a dilemma,
        //  as we don't know whether the field we slice out is signed or unsigned.
        //  As it turns out, it doesn't matter.  We treat it as signed, sign-extend it if it is
        //  negative, convert to twos-complement, add or subtract the reference, then convert it
        //  back to ones-complement.  This works regardless, via magic.
        //TODO I think the following is wrong, we need to shift, THEN and...
        long tempValue = (initialValue & mask) >> shift;
        if ((tempValue & msbMask) != 0) {
            //  original field value is negative...  sign-extend it.
            tempValue |= notMask;
        }

        if (subtraction) {
            tempValue = Word36.addSimple(tempValue, (Word36.negate(newValue)));
        } else {
            tempValue = Word36.addSimple(tempValue, newValue);
        }

        //  Check for field overflow...
        boolean trunc;
        if (Word36.isPositive(tempValue)) {
            trunc = (tempValue & notMask) != 0;
        } else {
            trunc = (tempValue | mask) != 0_777777_777777L;
        }

        if (trunc) {
            raise(String.format("Truncation resolving value in %s for module %s LC %d",
                                fieldDescriptor.toString(),
                                lcpSpecification._module.getModuleName(),
                                lcpSpecification._lcIndex));
        }

        //  splice it back into the discrete value
        tempValue = tempValue & mask;
        long shiftedNotMask = (mask << shift) ^ 0_777777_777777L;
        return (initialValue & shiftedNotMask) | (tempValue << shift);
    }

    /**
     * Maps the pools to where they belong so we can satisfy location counter references.
     */
    private void mapPools() {
        for (Map.Entry<Integer, BankDeclaration> entry : _bankDeclarations.entrySet()) {
            mapPools(entry.getValue());
        }
    }

    /**
     * Maps the pools to where they belong for one specific bank declaration.
     */
    private void mapPools(
        final BankDeclaration bankDeclaration
    ) {
        int bankOffset = 0;
        for (LCPoolSpecification lcpSpec : bankDeclaration._poolSpecifications) {
            VirtualAddress vAddr = new VirtualAddress(bankDeclaration._bankLevel,
                                                      bankDeclaration._bankDescriptorIndex,
                                                      bankOffset);
            _poolMap.put(lcpSpec, vAddr);
            try {
                RelocatableModule.RelocatablePool pool = lcpSpec._module.getLocationCounterPool(lcpSpec._lcIndex);
                bankOffset += pool._content.length;
            } catch (ParameterException ex) {
                raise ("Error in pool spec:LC "
                       + lcpSpec._lcIndex
                       + " not found in module "
                       + lcpSpec._module.getModuleName());
            }
        }
    }

    /**
     * Populate the bank content array with resolved relocatable words.
     */
    private void populateBanks() {
        for (Map.Entry<LCPoolSpecification, VirtualAddress> entry : _poolMap.entrySet()) {
            LCPoolSpecification lcpSpec = entry.getKey();
            VirtualAddress vAddr = entry.getValue();
            populateBanks(lcpSpec, vAddr);
        }
    }

    /**
     * Populate the bank content array with resolved relocatable words for a particular lc pool.
     */
    private void populateBanks(
        final LCPoolSpecification lcpSpec,
        final VirtualAddress virtualAddress
    ) {
        int bx = virtualAddress.getOffset();
        int lbdi = virtualAddress.getLBDI();
        long[] bankContent = _bankContent.get(lbdi);

        try {
            RelocatableModule.RelocatableWord[] pool = lcpSpec._module.getLocationCounterPool(lcpSpec._lcIndex)._content;
            for (RelocatableModule.RelocatableWord rw : pool) {
                if (rw != null) {
                    bankContent[bx] = resolveRelocatableWord(lcpSpec, rw);
                }
                ++bx;
            }
        } catch (ParameterException ex) {
            raise ("Error in pool spec:LC "
                   + lcpSpec._lcIndex
                   + " not found in module "
                   + lcpSpec._module.getModuleName());
        }
    }

    /**
     * For bank-declared linkages, the relocatable modules container should be empty.
     * We need to load it based on the content of the bank declarations container.
     * Do not invoke this for bank-implied linkages.
     */
    private void populateRelocatableModules() {
        _relocatableModules.clear();    //  just in case the client mistakenly gave us some along with the bank declaractions
        for (BankDeclaration bankDecl : _bankDeclarations.values()) {
            for (LCPoolSpecification lcPoolSpecification : bankDecl._poolSpecifications) {
                //  Not terribly efficient, but it doesn't need to be
                //  (we will add the same module once per lc pool, which isn't necessary - but it's easy)
                _relocatableModules.add(lcPoolSpecification._module);
            }
        }
    }

    /**
     * Displays an error message and kicks the counter
     * @param message message to be displayed
     */
    private void raise(
        final String message
    ) {
        if (!_options.contains(LinkOption.SILENT)) {
            _printStream.println("ERROR:" + message);
        }
        _errors++;
    }

    /**
     * Resolves a location counter reference into the proper final address which refers to the
     * first word of the particular location counter pool, accounting for the lcpool's offset
     * from the start of the bank, and the bank's starting address.
     * For the case where some module contains a reference which is provided by some (possibly other)
     * module, which is relative to a location counter.
     * @param relocatableModule module which contains the lcpool of interest
     * @param lcIndex location counter index of interest
     * @param requestingModule module which contains the reference we are trying to resolve
     * @return the final value for the reference
     */
    private long resolveLocationCounterIndex(
        final RelocatableModule relocatableModule,
        final int lcIndex,
        final RelocatableModule requestingModule
    ) {
        LCPoolSpecification lookupSpec = new LCPoolSpecification(relocatableModule, lcIndex);
        VirtualAddress bankVAddr = _poolMap.get(lookupSpec);
        if (bankVAddr == null) {
            raise(String.format("LC $%d in module %s referenced by module %s does not exist.",
                                lcIndex,
                                relocatableModule.getModuleName(),
                                requestingModule.getModuleName()));
            return 0;
        }

        BankGeometry bg = _bankGeometries.get(bankVAddr.getLBDI());
        return bg._lowerLimit + bankVAddr.getOffset();
    }

    /**
     * Resolves a location counter relocatable item reference.
     * For the case where a relocatable module has a reference which is relative to a location counter
     * within that same module.
     * @param lcpSpecification LCSpec of the location counter pool which contains the reference
     * @param relocatableItem The reference itself
     * @return the final value for the reference
     */
    private long resolveRelocatableItem(
        final LCPoolSpecification lcpSpecification,
        final RelocatableModule.RelocatableItemLocationCounter relocatableItem
    ) {
        return resolveLocationCounterIndex(lcpSpecification._module,
                                           relocatableItem._locationCounterIndex,
                                           lcpSpecification._module);
    }

    /**
     * Resolves an undefined symbol relocatable item reference
     * @param lcpSpecification LCSpec of the location counter pool which contains the reference
     * @param relocatableItem The reference itself
     * @param itemIterator The iterator over all the relocatable items for a given word -
     *                     this is necessary for some collector-defined symbols, which depend
     *                     upon the following item for resolution
     * @return the final value for the reference
     */
    private long resolveRelocatableItem(
        final LCPoolSpecification lcpSpecification,
        final RelocatableModule.RelocatableItemSymbol relocatableItem,
        final Iterator<RelocatableModule.RelocatableItem> itemIterator
    ) {
        SpecialLabel specLabel = SpecialLabel.getFrom(relocatableItem._undefinedSymbol);
        if (specLabel != null) {
            return resolveUndefinedReferenceToSpecialLabel(lcpSpecification, specLabel, itemIterator);
        }

        SymbolEntry se = _symbolTable.get(relocatableItem._undefinedSymbol);
        if (se == null) {
            raise("Undefined symbol " + relocatableItem._undefinedSymbol);
            return 0;
        }

        if (se instanceof AbsoluteSymbolEntry) {
            AbsoluteSymbolEntry ase = (AbsoluteSymbolEntry) se;
            return ase._baseValue;
        } else if (se instanceof LocationCounterRelativeSymbolEntry) {
            LocationCounterRelativeSymbolEntry lcrse = (LocationCounterRelativeSymbolEntry) se;
            long lcBase = resolveLocationCounterIndex(lcrse._lcPoolSpecification._module,
                                                      lcrse._lcPoolSpecification._lcIndex,
                                                      lcpSpecification._module);
            return lcBase + lcrse._baseValue;
        } else {
            raise("Internal error - unknown entry point type for symbol " + relocatableItem._undefinedSymbol);
            return 0;
        }
    }

    /**
     * Resolves a relocatable word by obtaining its base value, and then masking in the resulting
     * values of any attached relocatable items.
     * @param lcpSpecification LCSpec of the location counter pool which contains the reference
     * @param relocatableWord The relocatable word object to be resolved
     * @return The file resolved value of the relocatable word
     */
    private long resolveRelocatableWord(
        final LCPoolSpecification lcpSpecification,
        final RelocatableModule.RelocatableWord relocatableWord
    ) {
        long result = relocatableWord.getW();
        List<RelocatableModule.RelocatableItem> itemList = Arrays.asList(relocatableWord._relocatableItems);
        Iterator<RelocatableModule.RelocatableItem> itemIter = itemList.iterator();
        while (itemIter.hasNext()) {
            RelocatableModule.RelocatableItem ri = itemIter.next();
            long newValue = 0;
            if (ri instanceof RelocatableModule.RelocatableItemLocationCounter) {
                RelocatableModule.RelocatableItemLocationCounter lcri = (RelocatableModule.RelocatableItemLocationCounter) ri;
                newValue = resolveRelocatableItem(lcpSpecification, lcri);
            } else if (ri instanceof RelocatableModule.RelocatableItemSymbol) {
                RelocatableModule.RelocatableItemSymbol usri = (RelocatableModule.RelocatableItemSymbol) ri;
                newValue = resolveRelocatableItem(lcpSpecification, usri, itemIter);
            }

            if (newValue != 0) {
                result = integrateValue(result, ri._fieldDescriptor, newValue, ri._subtraction, lcpSpecification);
            }
        }

        return result;
    }

    /**
     * Resolves a BDI$ or LBDI$ reference
     * See collector PRM 4.5.4, 4.6.1
     * Retrieve the BDI of the code which contains the BDI$ reference
     * For LBDI$, include the level as well, in extended mode virtual address form.
     */
    private long resolveSpecialBDI(
        final LCPoolSpecification lcpSpecification,
        final boolean includeLevel
    ) {
        VirtualAddress bankVAddr = _poolMap.get(lcpSpecification);
        if (bankVAddr == null) {
            raise("Internal Error - cannot satisfy BDI$ reference");
            return 0;
        }

        if (includeLevel) {
            return bankVAddr.getLBDI();
        } else {
            return bankVAddr.getBankDescriptorIndex();
        }
    }

    /**
     * Resolves a BDIREF$+symbol or LBDIREF$+symbol reference
     * BDIREF$ + bank_name retrieves the BDI of a bank defined as an absolute value
     * BDIREF$ + entrypoint retrieves the BDI for a bank containing the entry point defined as an absolute value
     * Resolve the next reference and find the BDI which contains it.
     * We differ slightly from convention, in that we always return L,BDI in extended mode virtual address format.
     */
    private long resolveSpecialBDIREF(
        final LCPoolSpecification lcpSpecification,
        final Iterator<RelocatableModule.RelocatableItem> itemIterator,
        final boolean includeLevel
    ) {
        if (!itemIterator.hasNext()) {
            raise("Module " + lcpSpecification._module.getModuleName() + " has an incomplete BDIREF$ reference");
            return 0;
        }

        RelocatableModule.RelocatableItem ri = itemIterator.next();
        if (ri instanceof RelocatableModule.RelocatableItemLocationCounter) {
            //  module is asking for the BDI of an entry point for which it knows a location counter...
            //  The only thing unresolved is the LC offset, which we don't care about.
            //  If we're not doing an exec collection
            //  If we're doing an exec collection and includeLevel is set, return LBDI in extended mode virtual address form
            //  For exec collection and includeLevel is not set, return LBDI in basic mode virtual address form
            RelocatableModule.RelocatableItemLocationCounter lcri = (RelocatableModule.RelocatableItemLocationCounter) ri;
            LCPoolSpecification targetSpec = new LCPoolSpecification(lcpSpecification._module,
                                                                     lcri._locationCounterIndex);
            VirtualAddress bankVAddr = _poolMap.get(targetSpec);
            if (bankVAddr == null) {
                raise(String.format("Internal error cannot find bank for module %s LC $%d",
                                    lcpSpecification._module,
                                    lcri._locationCounterIndex));
                return 0;
            }

            if (includeLevel) {
                return bankVAddr.getLBDI();
            } else {
                return bankVAddr.getBankDescriptorIndex();
            }
        }

        if (ri instanceof RelocatableModule.RelocatableItemSymbol) {
            //  module is asking for BDI of a symbol which is unresolved/undefined.
            //  We look it up in our symbol table and it's either an absolute value or an entry point
            RelocatableModule.RelocatableItemSymbol usri = (RelocatableModule.RelocatableItemSymbol) ri;
            SymbolEntry se = _symbolTable.get(usri._undefinedSymbol);
            if (se == null) {
                raise("Undefined symbol for BDIREF$ in module " + lcpSpecification._module.getModuleName());
                return 0;
            }

            if (se instanceof AbsoluteSymbolEntry) {
                AbsoluteSymbolEntry ase = (AbsoluteSymbolEntry) se;
                return ase._baseValue & 0777777;
            } else if (se instanceof LocationCounterRelativeSymbolEntry) {
                LocationCounterRelativeSymbolEntry lcrse = (LocationCounterRelativeSymbolEntry) se;
                VirtualAddress bankVAddr = _poolMap.get(lcrse._lcPoolSpecification);
                if (bankVAddr == null) {
                    raise(String.format("Internal error cannot find bank for module %s LC $%d",
                                        lcrse._lcPoolSpecification._module,
                                        lcrse._lcPoolSpecification._lcIndex));
                    return 0;
                }

                if (includeLevel) {
                    return bankVAddr.getLBDI();
                } else {
                    return bankVAddr.getBankDescriptorIndex();
                }
            } else if (se instanceof UndefinedSymbolRelativeSymbolEntry) {
                //  TODO is this even a thing?
                return 0;
            }
        }

        raise("Unknown type of relocatable item in module " + lcpSpecification._module.getModuleName());
        return 0;
    }

    /**
     * Resolves a FIRST$ reference - the first address in the BDI which contains the reference
     */
    private long resolveSpecialFirst(
        final LCPoolSpecification lcpSpecification
    ) {
        VirtualAddress vAddr = _poolMap.get(lcpSpecification);
        if (vAddr == null) {
            raise("Internal Error - cannot satisfy FIRST$ reference");
            return 0;
        }

        int lbdi = vAddr.getLBDI();
        BankDeclaration bankDecl = _bankDeclarations.get(lbdi);
        if (bankDecl == null) {
            raise(String.format("Internal Error - cannot find bank declaration for L,BDI %06o", lbdi));
            return 0;
        }

        return bankDecl._startingAddress;
    }

    /**
     * Handles special identifiers such as LBDICALL$ and its ilk.
     * These identifiers are intended to be resolved by us, using information only available
     * at link time. In a traditional environment, they are handled by the collector.
     * They are not recognized by the official linker.
     * @param lcpSpecification source pool which contains the reference
     * @param labelType indicates what type of special label we are processing
     * @param itemIterator iterator over the relocatable items, in case the special handling requires multiple items
     * @return resulting value
     */
    private long resolveUndefinedReferenceToSpecialLabel(
        final LCPoolSpecification lcpSpecification,
        final SpecialLabel labelType,
        final Iterator<RelocatableModule.RelocatableItem> itemIterator
    ) {
        return switch (labelType) {
            case BDI -> resolveSpecialBDI(lcpSpecification, false);
            case BDIREF -> resolveSpecialBDIREF(lcpSpecification, itemIterator, false);
            case FIRST -> resolveSpecialFirst(lcpSpecification);
            case LBDI -> resolveSpecialBDI(lcpSpecification, true);
            case LBDIREF -> resolveSpecialBDIREF(lcpSpecification, itemIterator, true);
        };
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  link sub-methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Absolutes are intended to be loaded by the operating system.
     * An absolute is an executable which begins in basic mode.
     */
    private LinkResult linkAbsolute() {
        _linkType = LinkType.ABSOLUTE;

        //  Do we need to create default bank declarations?  If so, do that.
        if (_bankDeclarations.isEmpty()) {
            _bankImplied = true;
            if (_relocatableModules.isEmpty()) {
                raise("No bank declarations and no relocatable modules specified - output will be empty");
            }
            //  TO DO create bank declarations
        } else {
            _bankImplied = false;
            if (!_relocatableModules.isEmpty()) {
                raise("Specified relocatable modules ignore for bank-specified linkage");
            }
            populateRelocatableModules();
        }

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        determineBankGeometry();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createLoadableBanks();
        determineProgramStartAddress();
        determineModes();

        //  TO DO create AbsoluteModule

        raise("Not yet implemented");
        return new LinkResult(_errors, _moduleName);
    }

    /**
     * Binaries are directly loaded by the SP into memory.
     * The primary purpose for a binary is the Initial Load Program (ILP) which loads the first block
     * from disk or tape and transfers control thereto, as part of an official bootstrap.
     * Such entities are usually small and simple, and are contained within a single bank.
     * Since the requirements of the bank are known to the SP, no bank information is stored.
     * When loaded, control will be transfered to the first word of the bank, and the processor state
     * will be set for extended mode, ring/domain 0, and with neither stacks nor BDTs loaded.
     * The lower level address (and thus, the start address) is set at 01000.
     */
    private LinkResult linkBinary() {
        _linkType = LinkType.BINARY;

        //  Create a dummy bank declaration including pool specs for all the pools in all the supplied rel modules.
        if (_bankDeclarations.isEmpty()) {
            _bankImplied = true;
        } else {
            _bankImplied = false;
            _bankDeclarations.clear();
            raise("Provided bank declarations ignored for binary linking");
        }

        List<LCPoolSpecification> tempSpecs = new LinkedList<>();
        boolean requiresExtendedMode = false;
        for (RelocatableModule module : _relocatableModules) {
            for (Integer lcIndex : module.getEstablishedLocationCounterIndices()) {
                tempSpecs.add(new LCPoolSpecification(module, lcIndex));
                try {
                    RelocatableModule.RelocatablePool relPool = module.getLocationCounterPool(lcIndex);
                    if (relPool._requiresExtendedMode) {
                        requiresExtendedMode = true;
                    }
                } catch (ParameterException ex) {
                    //  can't happen
                }
            }
        }

        int bdi = 000040;   //  required for the bank declaration, but not used otherwise
        Set<BankDeclaration.BankDeclarationOption> bdOptions = new HashSet<>();
        bdOptions.add(BankDeclaration.BankDeclarationOption.DYNAMIC);
        if (requiresExtendedMode) {
            bdOptions.add(BankDeclaration.BankDeclarationOption.EXTENDED_MODE);
        }

        AccessInfo accInfo = new AccessInfo(0, 0);
        AccessPermissions gap = new AccessPermissions(false, false, false);
        AccessPermissions sap = new AccessPermissions(true, true, true);
        BankDeclaration bd = new BankDeclaration.Builder().setBankDescriptorIndex(bdi)
                                                          .setBankName(_moduleName)
                                                          .setPoolSpecifications(tempSpecs)
                                                          .setAccessInfo(accInfo)
                                                          .setGeneralAccessPermissions(gap)
                                                          .setSpecialAccessPermissions(sap)
                                                          .setOptions(bdOptions)
                                                          .setStartingAddress(01000)
                                                          .build();
        _bankDeclarations.put(bd._bankDescriptorIndex, bd);

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        determineBankGeometry();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createLoadableBanks();
        determineProgramStartAddress();
        determineModes();

        return new LinkResult(_errors, _moduleName, _programStartInfo, _loadableBanks.values().toArray(new LoadableBank[0]));
    }

    /**
     * A multi-banked binary, like the generic binary, is meant to be loaded by the SP into memory.
     * Unlike the generic, this binary is represented by multiple bank descriptors,
     * each containing the raw content for the associated bank.
     * Each bank descriptor defines the geometry of the bank along with access control,
     * whether (and where) the bank is initially based, and other bank attributes.
     * It is intended primarily for development/debug/test purposes.
     * NOTE: For now, we support only basic and extended mode normal banks - no gate, indirect, queue banks
     */
    private LinkResult linkMultiBankedBinary() {
        _linkType = LinkType.MULTI_BANKED_BINARY;

        if (_bankDeclarations.isEmpty()) {
            raise("Bank declarations must be specified for MultiBankedBinary linkages");
            return new LinkResult(_errors, _moduleName);
        }
        _bankImplied = false;
        if (!_relocatableModules.isEmpty()) {
            raise("Relocatable module specifications are ignore for MultiBankedBinary linkages");
        }
        populateRelocatableModules();

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        determineBankGeometry();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createLoadableBanks();
        determineProgramStartAddress();
        determineModes();

        return new LinkResult(_errors, _moduleName, _programStartInfo, _loadableBanks.values().toArray(new LoadableBank[0]));
    }

    /**
     * An object module describes an executable which begins in extended mode
     */
    private LinkResult linkObject() {
        _linkType = LinkType.OBJECT;

        //  Do we need to create default bank declarations?  If so, do that.
        if (_bankDeclarations.isEmpty()) {
            _bankImplied = true;
            if (_relocatableModules.isEmpty()) {
                raise("No bank declarations and no relocatable modules specified - output will be empty");
            }
            //  TO DO create bank declarations
        } else {
            _bankImplied = false;
            if (!_relocatableModules.isEmpty()) {
                raise("Specified relocatable modules ignore for bank-specified linkage");
            }
            populateRelocatableModules();
        }

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        determineBankGeometry();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createLoadableBanks();
        determineProgramStartAddress();
        determineModes();

        //  TO DO create ObjectModule

        raise("Not yet implemented");
        return new LinkResult(_errors, _moduleName);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public Methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    public LinkResult link(
        final LinkType linkType
    ) {
        if (!_options.contains(LinkOption.SILENT)) {
            _printStream.println("Linking module " + _moduleName + " -----------------------------------");
        }

        _bankGeometries.clear();
        _poolMap.clear();

        LinkResult result = switch (linkType) {
            case ABSOLUTE -> linkAbsolute();
            case BINARY -> linkBinary();
            case MULTI_BANKED_BINARY -> linkMultiBankedBinary();
            case OBJECT -> linkObject();
        };

        if (!_options.contains(LinkOption.SILENT)) {
            displaySummary();
            System.out.println("Linking Ends Errors=" + _errors + " -----------------------------------");
        }

        return result;
    }
}
