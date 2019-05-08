/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Links one or more RelocatableModule objects into an AbsoluteModule object
 */
@SuppressWarnings("Duplicates")
public class Linker {

    public static enum Option {
        OPTION_NO_ENTRY_POINT,
    }

    //TODO make this and the next, options
    public static enum PartialWordMode {
        NONE,
        QUARTER_WORD,
        THIRD_WORD,
    }

    public static enum ArithmeticFaultMode {
        NONE,
        COMPATIBILITY,
        NON_INTERRUPT,
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
        private final LCPoolSpecification[] _poolSpecifications;
        private final AccessPermissions _specialAccessPermissions;
        private final int _startingAddress;

        //  optional stuff
        private final boolean _needsExtendedMode;
        private final boolean _quarterWordMode;
        private final boolean _thirdWordMode;
        private final boolean _afcmMode;
        private final boolean _nonAfcmMode;

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
         * @param quarterWordMode true to override pool settings (if any) and require QW mode
         * @param thirdWordMode true to override pool settings (if any) and require TW mode
         * @param afcmMode true to override pool settings (if any) and require AFCM mode
         * @param nonAfcmMode true to override pool settings (if any) and require non-AFCM mode
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
            final boolean needsExtendedMode,
            final boolean quarterWordMode,
            final boolean thirdWordMode,
            final boolean afcmMode,
            final boolean nonAfcmMode
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
            _quarterWordMode = quarterWordMode;
            _thirdWordMode = thirdWordMode;
            _afcmMode = afcmMode;
            _nonAfcmMode = nonAfcmMode;
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
            private boolean _quarterWordMode = false;
            private boolean _thirdWordMode = false;
            private boolean _afcmMode = false;
            private boolean _nonAfcmMode = false;

            public Builder setAccessInfo(final AccessInfo value) { _accessInfo = value; return this; }
            public Builder setAFCMMode(final boolean value) { _afcmMode = value; return this; }
            public Builder setBankDescriptorIndex(final int value) { _bankDescriptorIndex = value; return this; }
            public Builder setBankLevel(final int value) { _bankLevel = value; return this; }
            public Builder setBankName(final String value) { _bankName = value; return this; }
            public Builder setGeneralAccessPermissions(final AccessPermissions value) { _generalAccessPermissions = value; return this; }
            public Builder setInitialBaseRegister(final Integer value) { _initialBaseRegister = value; return this; }
            public Builder setNeedsExtendedMode(final boolean value) { _needsExtendedMode = value; return this; }
            public Builder setNonAFCMMode(final boolean value) { _nonAfcmMode = value; return this; }
            public Builder setPoolSpecifications(final LCPoolSpecification[] values) { _poolSpecifications = values; return this; }
            public Builder setQuarterWordMode(final boolean value) { _quarterWordMode = value; return this; }
            public Builder setSpecialAccessPermissions(final AccessPermissions value) { _specialAccessPermissions = value; return this; }
            public Builder setStartingAddress(final int value) { _startingAddress = value; return this; }
            public Builder setThirdWordMode(final boolean value) { _thirdWordMode = value; return this; }

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
                                           _needsExtendedMode,
                                           _quarterWordMode,
                                           _thirdWordMode,
                                           _afcmMode,
                                           _nonAfcmMode);
            }
        }
    }

    private final BankDeclaration[] _bankDeclarations;

    private final Map<String, Long> _dictionary = new HashMap<>();
    private int _errors = 0;

    //  Maps LCPools to their virtual addresses within the bank which will contain them
    private final Map<LCPoolSpecification, Integer> _lcPoolMap = new HashMap<>();

    //  Unique set of the relocatable modules we're taking as input
    private final Set<RelocatableModule> _modules = new HashSet<>();

    //  Entry point (program start address)
    private Integer _entryPointAddress = null;

    //  Options
    private boolean _noEntryPoint = false;

    //  Modes discovered within the relocatable modules / pools
    private boolean _relQuarterWordMode = false;
    private boolean _relThirdWordMode = false;
    private boolean _relAFCM = false;
    private boolean _relNonAFCM = false;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Runs through the list of source modules and sets the _rel* data accordingly.
     * _modules must be built prior to invoking
     */
    private void checkModeSettings(
    ) {
        for (RelocatableModule module : _modules) {
            if (module._requiresQuarterWordMode) {
                _relQuarterWordMode = true;
            }
            if (module._requiresThirdWordMode) {
                _relThirdWordMode = true;
            }
            if (module._requiresArithmeticFaultCompatibilityMode) {
                _relAFCM = true;
            }
            if (module._requiresArithmeticFaultNonInterruptMode) {
                _relNonAFCM = true;
            }
        }
    }

    /**
     * Extracts code from the Relocatable Modules, satisfies any lingering undefined references,
     * and generates the storage for the various banks.
     */
    private LoadableBank createLoadableBank(
        final BankDeclaration bankDeclaration
    ) {
        boolean needsExtended = false;

        int bankSize = 0;
        for (LCPoolSpecification lcps : bankDeclaration._poolSpecifications) {
            bankSize += lcps._module._storage.get(lcps._lcIndex)._storage.length;
        }

        Word36Array wArray = new Word36Array(bankSize);

        for (LCPoolSpecification lcps : bankDeclaration._poolSpecifications) {
            //  Does the pool need extended mode?
            LocationCounterPool lcp = lcps._module._storage.get(lcps._lcIndex);
            if (lcp._needsExtendedMode) {
                needsExtended = true;
            }

            loadPool(bankDeclaration, lcps, wArray);
        }

        return new LoadableBank.Builder().setBankDescriptorIndex(bankDeclaration._bankDescriptorIndex)
                                         .setBankLevel(bankDeclaration._bankLevel)
                                         .setBankName(bankDeclaration._bankName)
                                         .setContent(wArray)
                                         .setIsExtendedMode(needsExtended)
                                         .setStartingAddress(bankDeclaration._startingAddress)
                                         .setInitialBaseRegister(bankDeclaration._initialBaseRegister)
                                         .setAccessInfo(bankDeclaration._accessInfo)
                                         .setGeneralPermissions(bankDeclaration._generalAccessPermissions)
                                         .setSpecialPermissions(bankDeclaration._specialAccessPermissions)
                                         .build();
    }

    /**
     * Displays summary information
     * @param module AbsoluteModule to be displayed
     */
    private void display(
        final AbsoluteModule module
    ) {
        //TODO code

        System.out.println("Dictionary:");
        for (Map.Entry<String, Long> entry : _dictionary.entrySet()) {
            System.out.println(String.format("  %s  %012o", entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Establishes a label while checking for duplicates
     * @param label label
     * @param value value
     */
    private void establishLabel(
        final String label,
        final long value
    ) {
        if (_dictionary.containsKey(label)) {
            raise("Duplicate label:" + label);
        } else {
            _dictionary.put(label, (long) value);
        }
    }

    /**
     * Extracts externalized labels from the modules and puts them into our dictionary.
     */
    private void extractLabels(
    ) {
        for (RelocatableModule module : _modules) {
            for (Map.Entry<String, IntegerValue> entry : module._externalLabels.entrySet()) {
                String label = entry.getKey();
                long value = entry.getValue()._value;
                for (UndefinedReference ur : entry.getValue()._undefinedReferences) {
                    value += resolveUndefinedReference(ur, module._name);
                }

                establishLabel(label, value);
            }
        }
    }

    /**
     * Loads a pool for createLoadableBank()
     */
    private void loadPool(
        final BankDeclaration bankDeclaration,
        final LCPoolSpecification poolSpec,
        final Word36Array wArray
    ) {
        //  For each source word in the relocatable element's location counter pool,
        //  get the discrete integer value, update it if appropriate, and move it
        //  to the Word36Array which eventually becomes the storage for the bank.
        LocationCounterPool lcp = poolSpec._module._storage.get(poolSpec._lcIndex);
        for (int rwx = 0, wax = _lcPoolMap.get(poolSpec) - bankDeclaration._startingAddress;
             rwx < lcp._storage.length;
             ++rwx, ++wax) {
            RelocatableWord36 rw36 = lcp._storage[rwx];

            //  Check for null - this happens due to $RES in the assembler
            if (rw36 != null) {
                long discreteValue = rw36.getW();

                //  If there are any undefined references in the source word from the relocatable module,
                //  iterate over them.  For each undefined reference, lookup the value for the reference,
                //  slice out the particular field of the discrete value, add the reference value thereto,
                //  check for truncation, and splice the resulting value back into the discrete value.
                if (rw36._undefinedReferences.length > 0) {
                    for (UndefinedReference ur : rw36._undefinedReferences) {
                        FieldDescriptor fd = ur._fieldDescriptor;
                        long refValue = resolveUndefinedReference(ur, poolSpec._module._name);
                        long mask = (1 << fd._fieldSize) - 1;
                        long msbMask = 1 << (fd._fieldSize - 1);
                        long notMask = mask ^ 0_777777_777777L;
                        int shift = 36 - (fd._fieldSize + fd._startingBit);

                        //  A special note - we recognize that the source word is in ones-complement.
                        //  The reference value *might* be negative - if that is the case, we have a bit of a dilemma,
                        //  as we don't know whether the field we slice out is signed or unsigned.
                        //  As it turns out, it doesn't matter.  We treat it as signed, sign-extend it if it is
                        //  negative, convert to twos-complement, add or subtract the reference, then convert it
                        //  back to ones-complement.  This works regardless, via magic.
                        long tempValue = (discreteValue & mask) >> shift;
                        if ((tempValue & msbMask) != 0) {
                            //  original field value is negative...  sign-extend it.
                            tempValue |= notMask;
                        }

                        OnesComplement.OnesComplement36Result ocr = new OnesComplement.OnesComplement36Result();
                        OnesComplement.getOnesComplement36(tempValue, ocr);
                        ocr._result += (ur._isNegative ? -1 : 1) * refValue;
                        tempValue = OnesComplement.getNative36(ocr._result);

                        //  Check for field overflow...
                        boolean trunc;
                        if (OnesComplement.isPositive36(tempValue)) {
                            trunc = (tempValue & notMask) != 0;
                        } else {
                            trunc = (tempValue | mask) != 0_777777_777777L;
                        }
                        if (trunc) {
                            raise(String.format("Truncation resolving %s LC(%d) offset %d for %s",
                                                poolSpec._module._name,
                                                poolSpec._lcIndex,
                                                rwx,
                                                ur.toString()));
                        }

                        //  splice it back into the discrete value
                        tempValue = tempValue & mask;
                        long shiftedNotMask = (mask << shift) ^ 0_777777_777777L;
                        discreteValue = (discreteValue & shiftedNotMask) | (tempValue << shift);
                    }
                }

                wArray.setValue(wax, discreteValue);
            }
        }
    }

    /**
     * Generates a map which assigns base addresses to all the LC pools.
     * Also generates a set of unique references to the relocatable modules.
     * Finally, we create labels to represent the location counter base addresses.
     */
    private void mapLCPools(
    ) {
        for (BankDeclaration bd : _bankDeclarations) {
            int address = bd._startingAddress;
            for (LCPoolSpecification lcps : bd._poolSpecifications) {
                _modules.add(lcps._module);
                _lcPoolMap.put(lcps, address);
                establishLabel(String.format("%s_LC$BDI_%d", lcps._module._name, lcps._lcIndex),
                               bd._bankDescriptorIndex);
                address += lcps._module._storage.get(lcps._lcIndex)._storage.length;
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

    /**
     * Resolves an otherwise undefined reference
     */
    private long resolveUndefinedReference(
        final UndefinedReference reference,
        final String relocatableModuleName
    ) {
        if (reference instanceof UndefinedReferenceToLabel) {
            UndefinedReferenceToLabel lRef = (UndefinedReferenceToLabel) reference;
            if (_dictionary.containsKey(lRef._label)) {
                return _dictionary.get(lRef._label);
            }
        } else if (reference instanceof UndefinedReferenceToLocationCounter) {
            //  I *think* every location counter reference will be local to the containing module.
            //  Let's proceed on that assumption.  Find the LCPoolSpec which corresponds to the LC index,
            //  it has the virtual address we need to resolve the reference.
            UndefinedReferenceToLocationCounter lRef = (UndefinedReferenceToLocationCounter) reference;
            for (Map.Entry<LCPoolSpecification, Integer> pSpecEntry : _lcPoolMap.entrySet()) {
                if (pSpecEntry.getKey()._lcIndex == lRef._locationCounterIndex) {
                    return (lRef._isNegative ? -1 : 1) * pSpecEntry.getValue();
                }
            }
        } else {
            raise("Internal Error unknown type of undefined reference");
            return 0;
        }

        raise(String.format("Value in %s contains undefined reference %s",
                            relocatableModuleName,
                            reference.toString()));
        return 0;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor with no options
     */
    public Linker(
        final BankDeclaration[] bankDeclarations
    ) {
        _bankDeclarations = bankDeclarations;
    }

    /**
     * Constructor with options
     */
    public Linker(
        final BankDeclaration[] bankDeclarations,
        final Option[] options
    ) {
        _bankDeclarations = bankDeclarations;
        for (Option opt : options) {
            switch (opt) {
                case OPTION_NO_ENTRY_POINT:
                    _noEntryPoint = true;
                    break;
            }
        }
    }

    /**
     * Performs the linkage
     * @param moduleName name of the absolute module to be created
     * @param partialWordMode indicates partial word mode override
     * @param arithmeticFaultMode indicates arithmetic fault mode override
     * @param display true to display linkage detailed output
     * @return newly created AbsoluteModule
     */
    public AbsoluteModule link(
        final String moduleName,
        final PartialWordMode partialWordMode,
        final ArithmeticFaultMode arithmeticFaultMode,
        final boolean display
    ) {
        System.out.println(String.format("Linking module %s -----------------------------------", moduleName));
        mapLCPools();
        extractLabels();

        //  Create LoadableBank objects to contain the code from the relocable module(s)
        LoadableBank[] loadableBanks = new LoadableBank[_bankDeclarations.length];
        for (int bx = 0; bx < _bankDeclarations.length; ++bx) {
            loadableBanks[bx] = createLoadableBank(_bankDeclarations[bx]);
        }

        //  Find entry point address - need bank and offset
        if (_dictionary.containsKey("START$")) {
            _entryPointAddress = (int)(long) _dictionary.get("START$");
        }

        AbsoluteModule module = null;
        if (_errors == 0) {
            //  Determine mode settings
            checkModeSettings();
            boolean quarterWordMode = false;
            boolean thirdWordMode = false;
            boolean afcmClear = false;
            boolean afcmSet = false;

            if (partialWordMode == PartialWordMode.NONE) {
                if (_relThirdWordMode && _relQuarterWordMode) {
                    //TODO
                } else {
                    quarterWordMode = _relQuarterWordMode;
                    thirdWordMode = _relThirdWordMode;
                }
            } else {
                quarterWordMode = partialWordMode == PartialWordMode.QUARTER_WORD;
                thirdWordMode = partialWordMode == PartialWordMode.THIRD_WORD;
            }

            if (arithmeticFaultMode == ArithmeticFaultMode.NONE) {
                if (_relAFCM && _relNonAFCM) {
                    //TODO
                } else {
                    afcmClear = _relNonAFCM;
                    afcmSet = _relAFCM;
                }
            } else {
                afcmClear = arithmeticFaultMode == ArithmeticFaultMode.NON_INTERRUPT;
                afcmSet = arithmeticFaultMode == ArithmeticFaultMode.COMPATIBILITY;
            }

            //  Create the module
            try {
                module = new AbsoluteModule.Builder().setName(moduleName)
                                                     .setLoadableBanks(loadableBanks)
                                                     .setEntryPointAddress(_entryPointAddress)
                                                     .setQuarter(quarterWordMode)
                                                     .setThird(thirdWordMode)
                                                     .setAFCMSet(afcmSet)
                                                     .setAFCMClear(afcmClear)
                                                     .setAllowNoEntryPoint(_noEntryPoint)
                                                     .build();
                if (display) {
                    display(module);
                }
            } catch (InvalidParameterException ex) {
                raise(ex.getMessage());
            }
        }
        System.out.println(String.format("Linking Ends Errors=%d -------------------------------------------------------",
                                         _errors));
        return module;
    }
}
