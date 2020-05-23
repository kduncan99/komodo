/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.VirtualAddress;
import com.kadware.komodo.baselib.Word36;
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

    private final Map<Integer, BankDeclaration> _bankDeclarations;
    private final String _moduleName;
    private final Set<LinkOption> _options;
    private final PrintStream _printStream;
    private final List<RelocatableModule> _relocatableModules;  //  some day the order might be important

    private boolean _bankImplied;
    private int _errors = 0;
    private LinkType _linkType;

    AFCMSensitivity _afcmSensitivity;
    PartialWordSensitivity _partialWordSensitivty;
    ProgramStartInfo _programStartInfo;

    /**
     * Generated bank descriptors
     */
    private final Map<Integer, BankDescriptor> _bankDescriptors = new TreeMap<>();

    /**
     * Raw content of the various banks
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
    private final Map<LCPoolSpecification, BankOffset> _poolMap = new HashMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    //TODO provide starting address override mechanism
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

        private Map<Integer, BankDeclaration> _bankDeclarations = new TreeMap<>();
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
                _bankDeclarations.put(bd._bankDescriptorIndex, bd);
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
                _bankDeclarations.put(bd._bankDescriptorIndex, bd);
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
     * Creates BankDescriptor objects for all the BankDeclaration objects we have on hand
     */
    private void createBankDescriptors() {
        for (BankDeclaration bankDecl : _bankDeclarations.values()) {
            int bdi = bankDecl._bankDescriptorIndex;
            long[] content = _bankContent.get(bdi);
            if (content == null) {
                content = new long[0];
            }

            BankType bankType = bankDecl._options.contains(BankDeclaration.BankDeclarationOption.EXTENDED_MODE)
                                ? BankType.EXTENDED_MODE
                                : BankType.BASIC_MODE;
            BankDescriptor bankDesc = new BankDescriptor(bankDecl._bankName,
                                                         bankDecl._bankLevel,
                                                         bankDecl._bankDescriptorIndex,
                                                         bankDecl._accessInfo,
                                                         bankType,
                                                         bankDecl._generalAccessPermissions,
                                                         bankDecl._specialAccessPermissions,
                                                         bankDecl._startingAddress,
                                                         bankDecl._startingAddress + content.length - 1,
                                                         content);
            _bankDescriptors.put(bdi, bankDesc);
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
                BankOffset bankOffsetInfo = _poolMap.get(lcpSpec);

                //  We'll need the containing bank's BDI...
                int bdi = bankOffsetInfo._bankDescriptorIndex;

                //  Offset from the beginning of the lcpool is the value attribute from the RelEntryPoint
                int lcpOffset = (int) rep._value;

                //  Offset from the beginning of the bank is the lcpool offset from the beginning of the bank,
                //  added to the offset from the lcpool.
                int bankOffset = bankOffsetInfo._offset + lcpOffset;

                //  Address is the bank's lowerlimit, added to the bankOffset value above
                BankDeclaration bankDecl = _bankDeclarations.get(bdi);
                int address = bankDecl._startingAddress + bankOffset;

                _programStartInfo = new ProgramStartInfo(address, bdi, bankOffset, lcpSpec, lcpOffset);
            }
        }
    }

    /**
     * Displays summary information
     */
    private void displaySummary() {
        boolean summary = false;
        boolean dictionary = false;
        boolean code = false;
        if (_options.contains(LinkOption.EMIT_SUMMARY)) {
            summary = true;
        }
        if (_options.contains(LinkOption.EMIT_DICTIONARY)) {
            summary = true;
            dictionary = true;
        }
        if (_options.contains(LinkOption.EMIT_GENERATED_CODE)) {
            code = true;
            dictionary = true;
        }

        if (summary) {
            _printStream.println("Link Module " + _moduleName + " Summary:");
            String afcmMsg = switch (_afcmSensitivity) {
                case SET -> "  AFCM-Cleared";
                case CLEARED -> "  AFCM-Set";
                case INSENSITIVE -> "  AFCM-Insensitive";
            };
            String pwMsg = switch (_partialWordSensitivty) {
                case THIRD_WORD -> "ThirdWord-Sensitive";
                case QUARTER_WORD -> "QuarterWord-Sensitive";
                case INSENSITIVE -> "PartialWord-Insensitive";
            };
            _printStream.println("  Modes: " + afcmMsg + " " + pwMsg);

            if (_programStartInfo != null) {
                _printStream.println(String.format("  Entry Point Bank:%06o Address:%08o (%s $(%d)+%d",
                                                   _programStartInfo._bankDescriptorIndex,
                                                   _programStartInfo._address,
                                                   _programStartInfo._lcpSpecification._module.getModuleName(),
                                                   _programStartInfo._lcpSpecification._lcIndex,
                                                   _programStartInfo._lcpOffset));
            }

            for (BankDescriptor bankDesc : _bankDescriptors.values()) {
                _printStream.println(String.format("  Bank %s Level:%d BDI:%06o %s Lower:%08o Upper:%08o Size:%08o",
                                                   bankDesc._bankName,
                                                   bankDesc._bankLevel,
                                                   bankDesc._bankDescriptorIndex,
                                                   bankDesc._bankType,
                                                   bankDesc._lowerLimit,
                                                   bankDesc._upperLimit,
                                                   bankDesc._upperLimit - bankDesc._lowerLimit + 1));

                if (code) {
                    int wordsPerLine = 8;
                    for (int ix = 0; ix < bankDesc._content.length; ix += wordsPerLine) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("      %08o:", ix + bankDesc._lowerLimit));
                        for (int iy = 0; iy < wordsPerLine; ++iy) {
                            if (ix + iy < bankDesc._content.length) {
                                sb.append(String.format(" %012o", bankDesc._content[ix + iy]));
                            }
                        }
                        _printStream.println(sb.toString());
                    }
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
            for (RelocatableModule.EntryPoint ep : rel.getEntryPoints()) {
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
        long tempValue = (initialValue & mask) >> shift;
        if ((tempValue & msbMask) != 0) {
            //  original field value is negative...  sign-extend it.
            tempValue |= notMask;
        }

        tempValue = Word36.addSimple(tempValue, (subtraction ? -1 : 1) * newValue);

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
            _poolMap.put(lcpSpec, new BankOffset(bankDeclaration._bankDescriptorIndex, bankOffset));
            try {
                RelocatableModule.RelocatableWord[] pool = lcpSpec._module.getLocationCounterPool(lcpSpec._lcIndex);
                bankOffset += pool.length;
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
        for (Map.Entry<LCPoolSpecification, BankOffset> entry : _poolMap.entrySet()) {
            LCPoolSpecification lcpSpec = entry.getKey();
            BankOffset bankOffset = entry.getValue();
            populateBanks(lcpSpec, bankOffset);
        }
    }

    /**
     * Populate the bank content array with resolved relocatable words for a particular lc pool.
     */
    private void populateBanks(
        final LCPoolSpecification lcpSpec,
        final BankOffset bankOffset
    ) {
        int bdi = bankOffset._bankDescriptorIndex;
        int bx = bankOffset._offset;
        long[] bankContent = _bankContent.get(bdi);

        try {
            RelocatableModule.RelocatableWord[] pool = lcpSpec._module.getLocationCounterPool(lcpSpec._lcIndex);

            //  If no content has been generated so far for the bank, create a content array.
            //  If the content array exists and is of insufficient size, resize it.
            if (bankContent == null) {
                bankContent = new long[bx + pool.length];
                _bankContent.put(bdi, bankContent);
            } else if (bankContent.length < bx + pool.length) {
                bankContent = Arrays.copyOf(bankContent, bx + pool.length);
                _bankContent.put(bdi, bankContent);
            }

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
     * Resolves a location counter reference.
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
        BankOffset bankOffset = _poolMap.get(lookupSpec);
        if (bankOffset == null) {
            raise("LC " + lcIndex
                  + " in module " + relocatableModule.getModuleName()
                  + " referenced by module " + requestingModule.getModuleName()
                  + " does not exist.");
            return 0;
        }

        BankDeclaration bd = _bankDeclarations.get(bankOffset._bankDescriptorIndex);
        return bd._startingAddress + bankOffset._offset;
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
            return resolveLocationCounterIndex(lcrse._lcPoolSpecification._module,
                                               lcrse._lcPoolSpecification._lcIndex,
                                               lcpSpecification._module);
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
        long result = relocatableWord._baseValue.getW();
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
     * Retrieve the BDI which contains the BDI$ reference
     * For LBDI$, include the level as well, in extended mode virtual address form.
     */
    private long resolveSpecialBDI(
        final LCPoolSpecification lcpSpecification,
        final boolean includeLevel
    ) {
        BankOffset bankOffset = _poolMap.get(lcpSpecification);
        if (bankOffset == null) {
            raise("Internal Error - cannot satisfy BDI$ reference");
            return 0;
        }

        int bdi = bankOffset._bankDescriptorIndex & 0077777;
        BankDeclaration bankDecl = _bankDeclarations.get(bdi);
        long result = bdi;
        if (includeLevel) {
            result |= (bankDecl._bankLevel & 03) << 15;
        }

        return result;
    }

    /**
     * Resolves a BDIREF$+symbol or LBDIREF$+symbol reference
     * BDIREF$ + bank_name retrieves the BDI of a bank defined as an absolute value
     * BDIREF$ + entrypoint retrieves the BDI for a bank containing the entry point defined as an absolute value
     * Resolve the next reference and find the BDI which contains it.
     * For EXEC mode LBDIREF$ the virtual address is in basic mode format.
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
            RelocatableModule.RelocatableItemLocationCounter lcri = (RelocatableModule.RelocatableItemLocationCounter) ri;
            LCPoolSpecification targetSpec = new LCPoolSpecification(lcpSpecification._module,
                                                                     lcri._locationCounterIndex);
            BankOffset bankOffset = _poolMap.get(targetSpec);
            if (bankOffset == null) {
                raise("Internal error cannot find bank for module "
                      + lcpSpecification._module
                      + " lc "
                      + lcri._locationCounterIndex);
                return 0;
            }

            return bankOffset._bankDescriptorIndex & 0077777;
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
                BankOffset bankOffset = _poolMap.get(lcrse._lcPoolSpecification);
                if (bankOffset == null) {
                    raise("Internal error cannot find bank for module "
                          + lcrse._lcPoolSpecification._module
                          + " lc "
                          + lcrse._lcPoolSpecification._lcIndex);
                    return 0;
                }

                int bdi = bankOffset._bankDescriptorIndex & 0077777;
                BankDeclaration bankDecl = _bankDeclarations.get(bdi);
                if (bankDecl == null) {
                    raise("Internal error cannot find bank declaration for bdi which should exist");
                    return 0;
                }

                long result;
                if (_options.contains(LinkOption.EXEC_MODE) && !includeLevel) {
                    long vaddrWord = VirtualAddress.translateToBasicMode(bankDecl._bankLevel, bdi, 0);
                    result = vaddrWord >> 18;
                } else if (includeLevel) {
                    VirtualAddress vaddr = new VirtualAddress(bankDecl._bankLevel, bdi, 0);
                    result = vaddr.getH1();
                } else {
                    result = bdi;
                }

                return result;
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
        BankOffset bankOffset = _poolMap.get(lcpSpecification);
        if (bankOffset == null) {
            raise("Internal Error - cannot satisfy FIRST$ reference");
            return 0;
        }

        BankDeclaration bankDecl = _bankDeclarations.get(bankOffset._bankDescriptorIndex);
        if (bankDecl == null) {
            raise(String.format("Internal Error - cannot find bank declaration for BDI %06o",
                                bankOffset._bankDescriptorIndex));
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
            //TODO
        } else {
            _bankImplied = false;
            if (!_relocatableModules.isEmpty()) {
                raise("Specified relocatable modules ignore for bank-specified linkage");
            }
            populateRelocatableModules();
        }

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createBankDescriptors();
        determineProgramStartAddress();
        determineModes();

        //TODO create AbsoluteModule

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
     * The SP will create
     *      * Pre-allocated banks for
     *          * Interrupt control stack (ICS)
     *          * Return control stack (RCS)
     *          * Configuration Bank
     *      * A minimal set of interrupt handlers
     *          For UPI Initial, a UR to the start address for the binary
     *          For all others, an IAR instruction
     *      * A level 0 BDT including
     *          Interrupt Vectors
     *          An entry at BDI 000040 for this bank
     *          An entry at BDI 000041 for the ICS
     *          An entry at BDI 000042 for the RCS
     *          An entry at BDI 000043 for the configuration bank
     * It will then set the appropriate registers
     *      * B0 for the code bank
     *      * B2 fo the configuration bank
     *      * B25 and EX0 for the RCS
     *      * B26 and EX1 for the ICS
     *      * DR for an extended mode quarter-word sensitive execution environment with PP = 0
     * It will then send a UPI Initial to the selected processor which will start the execution of the binary.
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
        for (RelocatableModule module : _relocatableModules) {
            for (Integer lcIndex : module.getEstablishedLocationCounterIndices()) {
                tempSpecs.add(new LCPoolSpecification(module, lcIndex));
            }
        }

        int bdi = 000040;   //  required for the bank declaration, but not used otherwise
        BankDeclaration bd = new BankDeclaration.Builder().setBankDescriptorIndex(bdi)
                                                          .setBankName(_moduleName)
                                                          .setPoolSpecifications(tempSpecs)
                                                          .build();
        _bankDeclarations.put(bd._bankDescriptorIndex, bd);

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createBankDescriptors();
        determineProgramStartAddress();
        determineModes();

        return new LinkResult(_errors, _moduleName, _bankDescriptors.values().toArray(new BankDescriptor[0]));
    }

    /**
     * A multi-banked binary, like the generic binary, is meant to be loaded by the SP into memory.
     * Unlike the generic, this binary is represented by multiple bank descriptors,
     * each containing the raw content for the associated bank.
     * Each bank descriptor defines the geometry of the bank along with access control,
     * whether (and where) the bank is initially based, and other bank attributes.
     * The SP will create all necessary bank descriptor tables.
     * It is intended primarily for development/debug/test purposes.
     * NOTE: For now, we support only basic and extended mode normal banks - no gate, indirect, queue banks
     *
     * The SP will create
     *      * Pre-allocated banks for
     *          * Interrupt control stack (ICS)
     *          * Return control stack (RCS)
     *      * A minimal set of interrupt handlers
     *          For UPI Initial, a UR to the start address for the binary
     *          For all others, an IAR instruction
     *      * A level 0 BDT including
     *          Interrupt Vectors
     *          Entries for all level-0 banks defined by the bank descriptors
     *          An entry at the first unused BDI for the ICS
     *          An entry at the next unused BDI for the RCS
     *      * Other BDTs as defined by the bank descriptors
     * It will then set the appropriate registers
     *      * B0 for the code bank
     *      * B25 and EX0 for the RCS
     *      * B26 and EX1 for the ICS
     *      * DR for an extended mode quarter-word sensitive execution environment with PP = 0
     *      * Other base registers as defined by the bank descriptors
     *          At least one bank must be initially based on B0
     *          No bank should be initially based on B25 or B26, as this will conflict with the ICS and RCS
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
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createBankDescriptors();
        determineProgramStartAddress();
        determineModes();

        return new LinkResult(_errors, _moduleName, _bankDescriptors.values().toArray(new BankDescriptor[0]));
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
            //TODO
        } else {
            _bankImplied = false;
            if (!_relocatableModules.isEmpty()) {
                raise("Specified relocatable modules ignore for bank-specified linkage");
            }
            populateRelocatableModules();
        }

        //  Collect information we need for resolving undefined references and populate the banks
        mapPools();
        establishBankNameSymbols();
        extractEntryPoints();
        populateBanks();
        createBankDescriptors();
        determineProgramStartAddress();
        determineModes();

        //TODO create ObjectModule

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

        LinkResult result = switch (linkType) {
            case ABSOLUTE -> linkAbsolute();
            case BINARY -> linkBinary();
            case MULTI_BANKED_BINARY -> linkMultiBankedBinary();
            case OBJECT -> linkObject();
        };

        if ((_programStartInfo == null) && !_options.contains(LinkOption.NO_ENTRY_POINT)) {
            raise("No program starting address");
        }

        if (!_options.contains(LinkOption.SILENT)) {
            displaySummary();
            System.out.println("Linking Ends Errors=" + _errors + " -----------------------------------");
        }
        return result;
    }
}
