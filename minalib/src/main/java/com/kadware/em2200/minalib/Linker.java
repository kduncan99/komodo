/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Links one or more RelocatableModule objects into an AbsoluteModule object
 */
@SuppressWarnings("Duplicates")
public class Linker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public enums, classes, etc
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static enum Option {
        OPTION_NO_ENTRY_POINT,
        OPTION_QUARTER_WORD_MODE,
        OPTION_THIRD_WORD_MODE,
        OPTION_ARITHMETIC_FAULT_COMPATIBILITY_MODE,
        OPTION_ARITHMETIC_FAULT_NON_INTERRUPT_MODE,
        OPTION_EMIT_SUMMARY,
        OPTION_EMIT_DICTIONARY,
        OPTION_EMIT_GENERATED_CODE,
    }

    /**
     * Specifies a particular lc pool within a relocatable module -
     * one or more of these are encapsulated into the BankDeclaration object
     */
    public static class LCPoolSpecification {
        final RelocatableModule _module;
        final int _lcIndex;

        public LCPoolSpecification(
            final RelocatableModule module,
            final int lcIndex
        ) {
            _module = module;
            _lcIndex = lcIndex;
        }

        @Override
        public boolean equals(
            final Object obj
        ) {
            if (obj instanceof LCPoolSpecification) {
                LCPoolSpecification lcps = (LCPoolSpecification) obj;
                return ((lcps._module == _module) && (lcps._lcIndex == _lcIndex));
            }
            return false;
        }
    }

    /**
     * Class provided by clients for declaring how banks are to be created
     */
    public static class BankDeclaration {
        private final AccessInfo _accessInfo;
        private final int _bankDescriptorIndex;
        private final int _bankLevel;
        private final String _bankName;
        private final AccessPermissions _generalAccessPermissions;
        private final Integer _initialBaseRegister;
        private final boolean _needsExtendedMode;
        private final LCPoolSpecification[] _poolSpecifications;
        private final AccessPermissions _specialAccessPermissions;
        private final int _startingAddress;

        /**
         * Constructor
         * @param bankName name of the bank
         * @param bankDescriptorIndex BDI for the bank
         * @param bankLevel level 0-7 for the bank
         * @param accessInfo ring and domain for the bank
         * @param generalAccessPermissions GAP permissions
         * @param specialAccessPermissions SAP permissions
         * @param startingAddress beginning address for the bank - i.e., 01000, 022000, etc
         * @param initialBaseRegister indicates a base register if the bank is to be initially based
         * @param poolSpecifications set of LCPoolSpecification objects indicating the LCPools to be included in this bank
         * @param needsExtendedMode true to use extended mode even if no relocatable pools have the flag set
         */
        private BankDeclaration(
            final String bankName,
            final int bankDescriptorIndex,
            final int bankLevel,
            final AccessInfo accessInfo,
            final AccessPermissions generalAccessPermissions,
            final AccessPermissions specialAccessPermissions,
            final int startingAddress,
            final Integer initialBaseRegister,
            final LCPoolSpecification[] poolSpecifications,
            final boolean needsExtendedMode
        ) {
            _accessInfo = accessInfo;
            _bankDescriptorIndex = bankDescriptorIndex;
            _bankLevel = bankLevel;
            _bankName = bankName;
            _generalAccessPermissions = generalAccessPermissions;
            _initialBaseRegister = initialBaseRegister;
            _poolSpecifications = poolSpecifications;
            _specialAccessPermissions = specialAccessPermissions;
            _startingAddress = startingAddress;
            _needsExtendedMode = needsExtendedMode;
        }

        public static class Builder{
            private AccessInfo _accessInfo = new AccessInfo((byte) 0,(short) 0);
            private int _bankDescriptorIndex;
            private int _bankLevel;
            private String _bankName = null;
            private AccessPermissions _generalAccessPermissions = new AccessPermissions();
            private Integer _initialBaseRegister = null;
            private LCPoolSpecification[] _poolSpecifications = null;
            private AccessPermissions _specialAccessPermissions = new AccessPermissions();
            private int _startingAddress;
            private boolean _needsExtendedMode = false;

            public Builder setAccessInfo(final AccessInfo value) { _accessInfo = value; return this; }
            public Builder setBankDescriptorIndex(final int value) { _bankDescriptorIndex = value; return this; }
            public Builder setBankLevel(final int value) { _bankLevel = value; return this; }
            public Builder setBankName(final String value) { _bankName = value; return this; }
            public Builder setGeneralAccessPermissions(final AccessPermissions value) { _generalAccessPermissions = value; return this; }
            public Builder setInitialBaseRegister(final Integer value) { _initialBaseRegister = value; return this; }
            public Builder setNeedsExtendedMode(final boolean value) { _needsExtendedMode = value; return this; }
            public Builder setPoolSpecifications(final LCPoolSpecification[] values) { _poolSpecifications = values; return this; }
            public Builder setSpecialAccessPermissions(final AccessPermissions value) { _specialAccessPermissions = value; return this; }
            public Builder setStartingAddress(final int value) { _startingAddress = value; return this; }

            public BankDeclaration build() {
                return new BankDeclaration(_bankName,
                                           _bankDescriptorIndex,
                                           _bankLevel,
                                           _accessInfo,
                                           _generalAccessPermissions,
                                           _specialAccessPermissions,
                                           _startingAddress,
                                           _initialBaseRegister,
                                           _poolSpecifications,
                                           _needsExtendedMode);
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private enums, classes, etc
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Describes entry point information for absolute modules with starting addresses
     */
    private class EntryPointInfo {

        private Integer _entryPointAddress = null;                      //  virtual address relative to bank limits
        private LoadableBank _entryPointBank = null;                    //  bank containing the entry point
        private RelocatableModule _entryPointRelocatableModule = null;  //  Module containing the entry point
        private boolean _foundEntryPoint = false;

        /**
         * Find START$ label if it exists.
         * If it does, it should have an undefined LC which tells us the LC pool containing the label,
         * and a discrete value indicating the lc pool offset.  With that, we can find the containing bank.
         */
        private EntryPointInfo(
            final LoadableBank[] loadableBanks
        ) {
            for (RelocatableModule module : _moduleSet._set) {
                if (module._externalLabels.containsKey("START$")) {
                    IntegerValue iv = module._externalLabels.get("START$");
                    UndefinedReference[] urs = iv._undefinedReferences;
                    if (urs.length != 1) {
                        raise("Improper START$ label - wrong number of undef refs");
                        continue;
                    }

                    UndefinedReference ur = urs[0];
                    if (!(ur instanceof UndefinedReferenceToLocationCounter)) {
                        raise("Improper START$ label - wrong undef ref");
                        continue;
                    }

                    if (_foundEntryPoint) {
                        raise("Duplicate START$ label");
                        continue;
                    }

                    //  We have the relocatable module and pool
                    UndefinedReferenceToLocationCounter lcRef = (UndefinedReferenceToLocationCounter) ur;
                    _entryPointRelocatableModule = module;
                    _foundEntryPoint = true;
                    long lcBase = findLocationCounterAddress(module, lcRef._locationCounterIndex);
                    _entryPointAddress = (int) iv._value + (int) lcBase;

                    for (BankDeclaration bd : _bankDeclarations) {
                        if (_entryPointBank != null) {
                            break;
                        }

                        for (LCPoolSpecification poolSpec : bd._poolSpecifications) {
                            if (_entryPointBank != null) {
                                break;
                            }

                            if ((poolSpec._module == _entryPointRelocatableModule)
                                && (poolSpec._lcIndex == lcRef._locationCounterIndex)) {
                                for (LoadableBank bank : loadableBanks) {
                                    if (bank._bankDescriptorIndex == bd._bankDescriptorIndex) {
                                        _entryPointBank = bank;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Maintains an IntegerValue along with information regarding where this value was sourced from
     */
    private static class LinkerValue extends IntegerValue {

        private final Integer _lcIndex;                         //  null if n/a
        private final RelocatableModule _relocatableModule;

        private LinkerValue(
            final long value,
            final UndefinedReference[] undefinedReferences,
            final Integer lcIndex,
            final RelocatableModule relocatableModule
        ) {
            super(false, value, undefinedReferences);
            _lcIndex = lcIndex;
            _relocatableModule = relocatableModule;
        }
    }

    /**
     *  Maps an LCPoolSpecification to its starting virtual address
     *  (which is within the addressing range of the containing bank)
     */
    private class LocationCounterPoolMap {
        private final Map<LCPoolSpecification, Integer> _map = new HashMap<>();

        /**
         * Generates a map which assigns base addresses to all the LC pools.
         * Also generates a set of unique references to the relocatable modules.
         * Finally, we create labels to represent the location counter base addresses.
         */
        private LocationCounterPoolMap (
            final BankDeclaration[] bankDeclarations
        ) {
            for (BankDeclaration bd : bankDeclarations) {
                int address = bd._startingAddress;
                for (LCPoolSpecification lcps : bd._poolSpecifications) {
                    _map.put(lcps, address);
                    address += lcps._module._storage.get(lcps._lcIndex)._storage.length;
                }
            }
        }
    }

    /**
     * Simple block which describes possible modes
     */
    private static class Modes {
        private boolean _quarterWordMode = false;
        private boolean _thirdWordMode = false;
        private boolean _afCompatibilityMode = false;
        private boolean _afNonInterruptMode = false;

        private Modes() {}

        private Modes(
            final ModuleSet moduleSet
        ) {
            for (RelocatableModule module : moduleSet._set) {
                if (module._requiresQuarterWordMode) {
                    _quarterWordMode = true;
                }
                if (module._requiresThirdWordMode) {
                    _thirdWordMode = true;
                }
                if (module._requiresArithmeticFaultCompatibilityMode) {
                    _afCompatibilityMode = true;
                }
                if (module._requiresArithmeticFaultNonInterruptMode) {
                    _afNonInterruptMode = true;
                }
            }
        }
    }

    /**
     * Where we store a set of unique references to modules
     */
    private static class ModuleSet {
        private final Set<RelocatableModule> _set = new HashSet<>();

        private ModuleSet(
            final BankDeclaration[] bankDeclarations
        ) {
            for (BankDeclaration bd : bankDeclarations) {
                for (LCPoolSpecification poolSpec : bd._poolSpecifications) {
                    _set.add(poolSpec._module);
                }
            }
        }
    }

    /**
     * Describes the options controlling this linkage
     */
    private class Options {
        private final Modes _modes = new Modes();
        private boolean _noEntryPoint = false;
        private boolean _emitDictionary = false;
        private boolean _emitSummary = false;
        private boolean _emitGeneratedCode = false;

        private Options(
            final Option[] optionSet
        ) {
            for (Option opt : optionSet) {
                switch (opt) {
                    case OPTION_NO_ENTRY_POINT:
                        _noEntryPoint = true;
                        break;

                    case OPTION_ARITHMETIC_FAULT_COMPATIBILITY_MODE:
                        _modes._afCompatibilityMode = true;
                        break;

                    case OPTION_ARITHMETIC_FAULT_NON_INTERRUPT_MODE:
                        _modes._afNonInterruptMode = true;
                        break;

                    case OPTION_QUARTER_WORD_MODE:
                        _modes._quarterWordMode = true;
                        break;

                    case OPTION_THIRD_WORD_MODE:
                        _modes._thirdWordMode = true;
                        break;

                    case OPTION_EMIT_DICTIONARY:
                        _emitDictionary = true;
                        break;

                    case OPTION_EMIT_GENERATED_CODE:
                        _emitGeneratedCode = true;
                        break;

                    case OPTION_EMIT_SUMMARY:
                        _emitSummary = true;
                        break;
                }
            }
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class data
    //  ----------------------------------------------------------------------------------------------------------------------------

    private BankDeclaration[] _bankDeclarations = null;
    private Dictionary _dictionary = null;
    private int _errors = 0;
    private LocationCounterPoolMap _locationCounterPoolMap = null;
    private ModuleSet _moduleSet = null;
    private Options _options = null;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Extracts code from the Relocatable Modules, satisfies any lingering undefined references,
     * and generates the storage for the various banks.
     */
    private LoadableBank createLoadableBank(
        BankDeclaration bankDeclaration
    ) {
        boolean needsExtended = bankDeclaration._needsExtendedMode;
        int bankSize = 0;
        for (LCPoolSpecification lcps : bankDeclaration._poolSpecifications) {
            bankSize += lcps._module._storage.get(lcps._lcIndex)._storage.length;
        }

        ArraySlice slice = new ArraySlice(new long[bankSize]);

        for (LCPoolSpecification lcps : bankDeclaration._poolSpecifications) {
            //  Does the pool need extended mode?
            LocationCounterPool lcp = lcps._module._storage.get(lcps._lcIndex);
            if (lcp._needsExtendedMode) {
                needsExtended = true;
            }

            loadPool(bankDeclaration, lcps, slice);
        }

        try {
            return new LoadableBank.Builder().setBankDescriptorIndex(bankDeclaration._bankDescriptorIndex)
                                             .setBankLevel(bankDeclaration._bankLevel)
                                             .setBankName(bankDeclaration._bankName)
                                             .setContent(slice)
                                             .setIsExtendedMode(needsExtended)
                                             .setStartingAddress(bankDeclaration._startingAddress)
                                             .setInitialBaseRegister(bankDeclaration._initialBaseRegister)
                                             .setAccessInfo(bankDeclaration._accessInfo)
                                             .setGeneralPermissions(bankDeclaration._generalAccessPermissions)
                                             .setSpecialPermissions(bankDeclaration._specialAccessPermissions)
                                             .build();
        } catch (InvalidParameterException ex) {
            raise("Internal Error:" + ex.getMessage());
            return null;
        }
    }

    /**
     * Creates a bank to contain a return control stack
     * @param bankLevel level of the bank to be created
     * @param bankDescriptorIndex of the bank to be created
     * @param size size of the stack
     * @param accessInfo ring/domain for the stack
     * @return LoadableBank representing the RCS
     */
    private LoadableBank createRCSBank(
        int bankLevel,
        int bankDescriptorIndex,
        final int size,
        final AccessInfo accessInfo
    ) {
        ArraySlice slice = new ArraySlice(new long[size]);

        try {
            return new LoadableBank.Builder().setBankDescriptorIndex(bankDescriptorIndex)
                                             .setBankLevel(bankLevel)
                                             .setBankName("RCSTACK")
                                             .setContent(slice)
                                             .setIsExtendedMode(true)
                                             .setStartingAddress(0)
                                             .setInitialBaseRegister(25)
                                             .setAccessInfo(accessInfo)
                                             .setGeneralPermissions(new AccessPermissions(false, true, true))
                                             .setSpecialPermissions(new AccessPermissions(false, true, true))
                                             .build();
        } catch (InvalidParameterException ex) {
            raise("Internal Error:" + ex.getMessage());
            return null;
        }
    }

    /**
     *  Determine mode settings.
     *  If specified in the optionSet, then that setting overrides everything else.
     *  If not in the optionSet, we look at the settings on the relocatable elements.
     *  If those options do not exist or they conflict, then go find the pool which contains
     *  the starting address (if any), and use the settings from that pool.
     *  If there is no starting address, then it doesn't matter and we leave the modes unset.
     * @param entryPointRelModule rel module containing the entry point - null if none
     * @return Modes object describing the final mode settings to be used for the absolute module
     */
    private Modes determineModes(
        final RelocatableModule entryPointRelModule
    ) {
        Modes result = new Modes();
        Modes relModes = new Modes(_moduleSet);

        if (_options._modes._quarterWordMode || _options._modes._thirdWordMode) {
            result._quarterWordMode = _options._modes._quarterWordMode;
            result._thirdWordMode = _options._modes._thirdWordMode;
        } else {
            if (relModes._thirdWordMode && relModes._quarterWordMode) {
                if (entryPointRelModule != null) {
                    result._quarterWordMode = entryPointRelModule._requiresQuarterWordMode;
                    result._thirdWordMode = entryPointRelModule._requiresThirdWordMode;
                }
            } else {
                result._thirdWordMode = relModes._thirdWordMode;
                result._quarterWordMode = relModes._quarterWordMode;
            }
        }

        if (_options._modes._afCompatibilityMode || _options._modes._afNonInterruptMode) {
            result._afCompatibilityMode = _options._modes._afCompatibilityMode;
            result._afNonInterruptMode = _options._modes._afNonInterruptMode;
        } else {
            if (relModes._afNonInterruptMode && relModes._afCompatibilityMode) {
                if (entryPointRelModule != null) {
                    result._afCompatibilityMode = entryPointRelModule._requiresArithmeticFaultCompatibilityMode;
                    result._afNonInterruptMode = entryPointRelModule._requiresArithmeticFaultNonInterruptMode;
                }
            } else {
                result._afCompatibilityMode = relModes._afCompatibilityMode;
                result._afNonInterruptMode = relModes._afNonInterruptMode;
            }
        }

        return result;
    }

    /**
     * Displays summary information
     * @param module AbsoluteModule to be displayed
     */
    private void display(
        final AbsoluteModule module
    ) {
        if (_options._emitSummary || _options._emitDictionary || _options._emitGeneratedCode) {
            System.out.println(String.format("Absolute Module %s %s%s%s%s",
                                             module._name,
                                             module._setQuarter ? "Qtr " : "",
                                             module._setThird ? "Third " : "",
                                             module._afcmClear ? "AFCMClear " : "",
                                             module._afcmSet ? "AFCMSet " : ""));
            if (module._entryPointBank != null) {
                System.out.println(String.format("  Entry Point Bank:%06o Address:%08o",
                                                 module._entryPointBank._bankDescriptorIndex,
                                                 module._entryPointAddress));
            }
            for (LoadableBank bank : module._loadableBanks.values()) {
                System.out.println(String.format("    Bank %s Level:%d BDI:%06o %s BaseAddr:%08o Size:%08o",
                                                 bank._bankName,
                                                 bank._bankLevel,
                                                 bank._bankDescriptorIndex,
                                                 bank._isExtendedMode ? "Extended" : "Basic",
                                                 bank._startingAddress,
                                                 bank._content.getSize()));
                System.out.println(String.format("      Lock:%s GAP:%s SAP%s",
                                                 bank._accessInfo,
                                                 bank._generalPermissions,
                                                 bank._specialPermissions));
                if (_options._emitGeneratedCode) {
                    int wordsPerLine = 8;
                    for (int ix = 0; ix < bank._content.getSize(); ix += wordsPerLine) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("      %08o:", ix + bank._startingAddress));
                        for (int iy = 0; iy < wordsPerLine; ++iy) {
                            if (ix + iy < bank._content.getSize()) {
                                sb.append(String.format(" %012o", bank._content.get(ix + iy)));
                            }
                        }
                        System.out.println(sb.toString());
                    }
                }
            }

            if (_options._emitDictionary) {
                System.out.println("Dictionary:");
                for (String label : _dictionary.getLabels()) {
                    try {
                        IntegerValue iv = (IntegerValue) _dictionary.getValue(label);
                        System.out.println(String.format("  %12s: %s", label, iv.toString()));
                    } catch (NotFoundException e) {
                        //  can't happen.
                    }
                }
            }
        }
    }

    /**
     * Extracts labels exported by the various relocatable modules and copies them to our local dictionary
     */
    private void extractLabels(
    ) {
        for (RelocatableModule module : _moduleSet._set) {
            for (Map.Entry<String, IntegerValue> externalLabelEntry : module._externalLabels.entrySet()) {
                String label = externalLabelEntry.getKey();
                IntegerValue iv = externalLabelEntry.getValue();
                //  We have to resolve LC references now, or we will lose the module locality.
                //  We will *not* resolve label references, as the resolved values for them are
                //  being built in this loop, and some of them likely are not yet located.
                long discreteValue = iv._value;
                List<UndefinedReference> newRefs = new LinkedList<>();
                Integer lcIndex = null;
                for (UndefinedReference ur : iv._undefinedReferences) {
                    if (ur instanceof UndefinedReferenceToLabel) {
                        newRefs.add(ur);
                    } else if (ur instanceof UndefinedReferenceToLocationCounter) {
                        UndefinedReferenceToLocationCounter lcRef = (UndefinedReferenceToLocationCounter) ur;
                        long address = findLocationCounterAddress(module, lcRef._locationCounterIndex);
                        discreteValue = integrateValue(discreteValue,
                                                       ur._fieldDescriptor,
                                                       address,
                                                       module._name,
                                                       lcRef._locationCounterIndex);
                        if (lcIndex == null) {
                            lcIndex = lcRef._locationCounterIndex;
                        }
                    } else {
                        raise(String.format("Unknown undefined reference type encountered in module %s", module._name));
                    }
                }

                LinkerValue lv = new LinkerValue(discreteValue,
                                                 newRefs.toArray(new UndefinedReference[0]),
                                                 lcIndex,
                                                 module);
                _dictionary.addValue(0, label, lv);
            }
        }
    }

    /**
     * Finds the L,BDI of the bank containing the given module/lcIndex:
     *      (level << 15) | (bdi)
     * Results are indeterminate if the lc is included in more than one bank
     */
    private int findBankDescriptorIndex(
        final RelocatableModule relocatableModule,
        final int locationCounterIndex
    ) {
        for (BankDeclaration bd : _bankDeclarations) {
            for (LCPoolSpecification pSpec : bd._poolSpecifications) {
                if ((pSpec._module == relocatableModule) && (pSpec._lcIndex == locationCounterIndex)) {
                    return (bd._bankLevel << 15) |  bd._bankDescriptorIndex;
                }
            }
        }

        assert(false);//????
        return 0;
    }

    /**
     * Finds the starting address for a particular location counter pool from the given lc map.
     * @param module module containing the location counter pool of interest
     * @param lcIndex index of the location counter pool of interest
     * @return virtual address where the given lc pool begins
     */
    private long findLocationCounterAddress(
        final RelocatableModule module,
        final int lcIndex
    ) {
        for (Map.Entry<LCPoolSpecification, Integer> entry : _locationCounterPoolMap._map.entrySet()) {
            LCPoolSpecification poolSpec = entry.getKey();
            if ((poolSpec._module == module) && (poolSpec._lcIndex == lcIndex)) {
                return entry.getValue();
            }
        }

        raise(String.format("Internal error - cannot find address entry for %s LC %d",
                            module._name,
                            lcIndex));
        return 0;
    }

    /**
     * Handles special identifiers such as LBDICALL$ and it's ilk
     * @param initialValue value into which we are integrating
     * @param spRef UndefinedReferenceSpecial which contains the type and target label
     * @param sourcePoolSpec source pool which contains the reference
     * @return integrated value
     */
    private long handleSpecialIdentifier(
        final long initialValue,
        final UndefinedReferenceSpecial spRef,
        final LCPoolSpecification sourcePoolSpec
    ) {
        long newValue = 0;
        try {
            switch (spRef._type) {
                case LBDI:
                    //TODO does not take a subject label, so we need to go back and do more work between this and the assembler
                    break;

                case LBDICALL: {
                    //  We don't do absolute numbers, only label references
                    //  We're not doing bank-implied (at least not yet), but we do check to see if the
                    //  BDI for the label matches the BDI which contains the reference to it.
                    LinkerValue linkerValue = (LinkerValue) _dictionary.getValue(spRef._subjectLabel);
                    if ((sourcePoolSpec._module != linkerValue._relocatableModule)
                        || (sourcePoolSpec._lcIndex != linkerValue._lcIndex)) {
                        newValue = findBankDescriptorIndex(linkerValue._relocatableModule, linkerValue._lcIndex);
                    }
                    break;
                }

                case LBDIREF: {
                    //  We don't do absolute numbers, only label references
                    LinkerValue linkerValue = (LinkerValue) _dictionary.getValue(spRef._subjectLabel);
                    newValue = findBankDescriptorIndex(linkerValue._relocatableModule, linkerValue._lcIndex);
                    break;
                }
            }
        } catch (NotFoundException ex) {
            raise(String.format("Undefined reference for %s$ subject:%s",
                                spRef._type.toString(),
                                spRef._subjectLabel));
        }

        return integrateValue(initialValue,
                              spRef._fieldDescriptor,
                              newValue,
                              sourcePoolSpec._module._name,
                              sourcePoolSpec._lcIndex);
    }

    /**
     * Integrates a new value into a subfield of an existing value.
     * Raises a diagnostic if truncation occurs.
     * @param initialValue initial 36-bit value
     * @param fieldDescriptor describes the subfield of integration (could be all 36 bits)
     * @param newValue new value to be added into the subfield of the initial value
     * @param moduleName only for emitting diagnostics
     * @param lcIndex also only for emitting diagnostics
     * @return the new integrated value
     */
    private long integrateValue(
        final long initialValue,
        final FieldDescriptor fieldDescriptor,
        final long newValue,
        final String moduleName,
        final int lcIndex
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

        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
        OnesComplement.getOnesComplement36(tempValue, ocr);
        ocr._result += newValue;
        tempValue = OnesComplement.getNative36(ocr._result);

        //  Check for field overflow...
        boolean trunc;
        if (OnesComplement.isPositive36(tempValue)) {
            trunc = (tempValue & notMask) != 0;
        } else {
            trunc = (tempValue | mask) != 0_777777_777777L;
        }
        if (trunc) {
            raise(String.format("Truncation resolving value in %s for module %s LC %d",
                                fieldDescriptor.toString(),
                                moduleName,
                                lcIndex));
        }

        //  splice it back into the discrete value
        tempValue = tempValue & mask;
        long shiftedNotMask = (mask << shift) ^ 0_777777_777777L;
        return (initialValue & shiftedNotMask) | (tempValue << shift);
    }

    /**
     * Loads a pool for createLoadableBank()
     */
    private void loadPool(
        final BankDeclaration bankDeclaration,
        final LCPoolSpecification poolSpec,
        final ArraySlice slice
    ) {
        //  For each source word in the relocatable element's location counter pool,
        //  get the discrete integer value, update it if appropriate, and move it
        //  to the Word36Array which eventually becomes the storage for the bank.
        LocationCounterPool lcp = poolSpec._module._storage.get(poolSpec._lcIndex);
        for (int rwx = 0, wax = _locationCounterPoolMap._map.get(poolSpec) - bankDeclaration._startingAddress;
             rwx < lcp._storage.length;
             ++rwx, ++wax) {
            RelocatableWord36 rw36 = lcp._storage[rwx];

            //  Check for null - that can happen due to $RES in the assembler
            if (rw36 != null) {
                long discreteValue = rw36.getW();

                //  If there are any undefined references in the source word from the relocatable module,
                //  iterate over them.  For each undefined reference, lookup the value for the reference,
                //  slice out the particular field of the discrete value, add the reference value thereto,
                //  check for truncation, and splice the resulting value back into the discrete value.
                if (rw36._undefinedReferences.length > 0) {
                    for (UndefinedReference ur : rw36._undefinedReferences) {
                        if (ur instanceof UndefinedReferenceToLabel) {
                            UndefinedReferenceToLabel lRef = (UndefinedReferenceToLabel) ur;
                            try {
                                IntegerValue iv = (IntegerValue) _dictionary.getValue(lRef._label);
                                discreteValue = integrateValue(discreteValue,
                                                               ur._fieldDescriptor,
                                                               iv._value,
                                                               poolSpec._module._name,
                                                               poolSpec._lcIndex);
                            } catch (NotFoundException ex) {
                                raise(String.format("Undefined reference:%s", lRef._label));
                            }
                        } else if (ur instanceof UndefinedReferenceToLocationCounter) {
                            //  This is a reference from a relocatable element to one of its own location counters.
                            UndefinedReferenceToLocationCounter lcRef = (UndefinedReferenceToLocationCounter) ur;
                            long addr = findLocationCounterAddress(poolSpec._module, lcRef._locationCounterIndex);
                            discreteValue = integrateValue(discreteValue,
                                                           ur._fieldDescriptor,
                                                           addr,
                                                           poolSpec._module._name,
                                                           poolSpec._lcIndex);
                        } else if (ur instanceof UndefinedReferenceSpecial) {
                            UndefinedReferenceSpecial spRef = (UndefinedReferenceSpecial) ur;
                            discreteValue = handleSpecialIdentifier(discreteValue, spRef, poolSpec);
                        } else {
                            raise("Internal error:Undefined reference of unknown type");
                        }
                    }
                }

                slice.set(wax, discreteValue);
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
        System.out.println("ERROR:" + message);
        _errors++;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Performs the linkage
     * @param moduleName name of the absolute module to be created
     * @param bankDeclarations array of BankDeclaration objects
     * @param stackDepth RCS stack depth - zero for no stack
     * @param optionSet options controlling the linkage
     * @return newly created AbsoluteModule
     */
    public AbsoluteModule link(
        final String moduleName,
        final BankDeclaration[] bankDeclarations,
        final int stackDepth,
        final Option[] optionSet
    ) {
        _bankDeclarations = bankDeclarations;
        _errors = 0;

        System.out.println(String.format("Linking module %s -----------------------------------", moduleName));
        _options = new Options(optionSet);
        _moduleSet = new ModuleSet(bankDeclarations);
        _locationCounterPoolMap = new LocationCounterPoolMap(bankDeclarations);
        _dictionary = new Dictionary();
        extractLabels();

        //  Create LoadableBank objects to contain the code from the relocable module(s)
        List<LoadableBank> loadableBanks = new LinkedList<>();
        for (BankDeclaration bd : _bankDeclarations) {
            loadableBanks.add(createLoadableBank(bd));
        }

        if (stackDepth > 0) {
            BankDeclaration lastBank = bankDeclarations[bankDeclarations.length - 1];
            loadableBanks.add(createRCSBank(lastBank._bankLevel,
                                            lastBank._bankDescriptorIndex + 1,
                                            8 * stackDepth, bankDeclarations[0]._accessInfo));
        }

        LoadableBank[] lbArray = loadableBanks.toArray(new LoadableBank[0]);
        EntryPointInfo epInfo = new EntryPointInfo(lbArray);
        Modes modes = determineModes(epInfo._foundEntryPoint ? epInfo._entryPointRelocatableModule : null);
        AbsoluteModule module = null;
        if (_errors == 0) {
            //  Create the module
            try {
                module = new AbsoluteModule.Builder().setName(moduleName)
                                                     .setLoadableBanks(lbArray)
                                                     .setEntryPointAddress(epInfo._foundEntryPoint ? epInfo._entryPointAddress : null)
                                                     .setEntryPointBank(epInfo._foundEntryPoint ? epInfo._entryPointBank : null)
                                                     .setQuarter(modes._quarterWordMode)
                                                     .setThird(modes._thirdWordMode)
                                                     .setAFCMSet(modes._afCompatibilityMode)
                                                     .setAFCMClear(modes._afNonInterruptMode)
                                                     .setAllowNoEntryPoint(_options._noEntryPoint)
                                                     .build();
                display(module);
            } catch (InvalidParameterException ex) {
                raise(ex.getMessage());
            }
        }

        System.out.println(String.format("Linking Ends Errors=%d -------------------------------------------------------",
                                         _errors));
        return module;
    }
}
