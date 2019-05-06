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

    /**
     * Specifies a particular lc pool within a relocatable module -
     * one or more of these are encapsulated into the BankDeclaration object
     */
    public static class LCPoolSpecification {
        public final RelocatableModule _module;
        public final int _lcIndex;

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
        private final boolean _isExtended;
        private final LCPoolSpecification[] _poolSpecifications;
        private final AccessPermissions _specialAccessPermissions;
        private final int _startingAddress;

        private BankDeclaration(
            final String bankName,
            final int bankDescriptorIndex,
            final int bankLevel,
            final AccessInfo accessInfo,
            final AccessPermissions generalAccessPermissions,
            final AccessPermissions specialAccessPermissions,
            final int startingAddress,
            final boolean isExtended,
            final Integer initialBaseRegister,
            final LCPoolSpecification[] poolSpecifications
        ) {
            _accessInfo = accessInfo;
            _bankDescriptorIndex = bankDescriptorIndex;
            _bankLevel = bankLevel;
            _bankName = bankName;
            _generalAccessPermissions = generalAccessPermissions;
            _initialBaseRegister = initialBaseRegister;
            _isExtended = isExtended;
            _poolSpecifications = poolSpecifications;
            _specialAccessPermissions = specialAccessPermissions;
            _startingAddress = startingAddress;
        }

        public static class Builder{
            private AccessInfo _accessInfo = new AccessInfo((byte) 0,(short) 0);
            private int _bankDescriptorIndex;
            private int _bankLevel;
            private String _bankName = null;
            private AccessPermissions _generalAccessPermissions = new AccessPermissions();
            private Integer _initialBaseRegister = null;
            private boolean _isExtended = false;
            private LCPoolSpecification[] _poolSpecifications = null;
            private AccessPermissions _specialAccessPermissions = new AccessPermissions();
            private int _startingAddress;

            public Builder setAccessInfo(final AccessInfo value) { _accessInfo = value; return this; }
            public Builder setBankDescriptorIndex(final int value) { _bankDescriptorIndex = value; return this; }
            public Builder setBankLevel(final int value) { _bankLevel = value; return this; }
            public Builder setBankName(final String value) { _bankName = value; return this; }
            public Builder setGeneralAccessPermissions(final AccessPermissions value) { _generalAccessPermissions = value; return this; }
            public Builder setInitialBaseRegister(final Integer value) { _initialBaseRegister = value; return this; }
            public Builder setIsExtended(final boolean value) { _isExtended = value; return this; }
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
                                           _isExtended,
                                           _initialBaseRegister,
                                           _poolSpecifications);
            }
        }
    }

    private final BankDeclaration[] _bankDeclarations;

    private final Map<String, Long> _dictionary = new HashMap<>();
    private int _errors = 0;
    private final Map<LCPoolSpecification, Integer> _lcPoolMap = new HashMap<>();
    private final Set<RelocatableModule> _modules = new HashSet<RelocatableModule>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Extracts code from the Relocatable Modules, satisfies any lingering undefined references,
     * and generates the storage for the various banks.
     */
    private LoadableBank createLoadableBank(
        final BankDeclaration bankDeclaration
    ) {
        try {
            int bankSize = 0;
            for (LCPoolSpecification lcps : bankDeclaration._poolSpecifications) {
                bankSize += lcps._module.getLocationCounterPool(lcps._lcIndex)._storage.length;
            }

            Word36Array wArray = new Word36Array(bankSize);

            for (LCPoolSpecification lcps : bankDeclaration._poolSpecifications) {
                //  For each source word in the relocatable element's location counter pool,
                //  get the discrete integer value, update it if appropriate, and move it
                //  to the Word36Array which eventually becomes the storage for the bank.
                LocationCounterPool lcp = lcps._module.getLocationCounterPool(lcps._lcIndex);
                for (int rwx = 0, wax = _lcPoolMap.get(lcps) - bankDeclaration._startingAddress;
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
                            for (RelocatableWord36.UndefinedReference ur : rw36._undefinedReferences) {
                                FieldDescriptor fd = ur._fieldDescriptor;
                                if (_dictionary.containsKey(ur._reference)) {
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
                                    ocr._result += (ur._isNegative ? -1 : 1) * _dictionary.get(ur._reference);
                                    tempValue = OnesComplement.getNative36(ocr._result);

                                    //  Check for field overflow...
                                    boolean trunc = false;
                                    if (OnesComplement.isPositive36(tempValue)) {
                                        trunc = (tempValue & notMask) != 0;
                                    } else {
                                        trunc = (tempValue | mask) != 0_777777_777777L;
                                    }
                                    if (trunc) {
                                        raise(String.format("Truncation resolving %s LC(%d) offset %d reference %s, field %s",
                                                            lcps._module._name,
                                                            lcps._lcIndex,
                                                            rwx,
                                                            ur._reference,
                                                            fd.toString()));
                                    }

                                    //  splice it back into the discrete value
                                    tempValue = tempValue & mask;
                                    long shiftedNotMask = (mask << shift) ^ 0_777777_777777L;
                                    discreteValue = (discreteValue & shiftedNotMask) | (tempValue << shift);
                                } else {
                                    raise(String.format("Value in %s LC(%d) offset %d - undefined reference %s in field %s",
                                                        lcps._module._name,
                                                        lcps._lcIndex,
                                                        rwx,
                                                        ur._reference,
                                                        fd.toString()));
                                }
                            }
                        }

                        wArray.setValue(wax, discreteValue);
                    }
                }
            }

            return new LoadableBank.Builder().setBankDescriptorIndex(bankDeclaration._bankDescriptorIndex)
                                             .setBankName(bankDeclaration._bankName)
                                             .setContent(wArray)
                                             .setIsExtendedMode(bankDeclaration._isExtended)
                                             .setStartingAddress(bankDeclaration._startingAddress)
                                             .setInitialBaseRegister(bankDeclaration._initialBaseRegister)
                                             .setAccessInfo(bankDeclaration._accessInfo)
                                             .setGeneralPermissions(bankDeclaration._generalAccessPermissions)
                                             .setSpecialPermissions(bankDeclaration._specialAccessPermissions)
                                             .build();
        } catch (InvalidParameterException ex) {
            //  can't happen
            return null;
        }
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
                for (IntegerValue.UndefinedReference ur : entry.getValue()._undefinedReferences) {
                    if (_dictionary.containsKey(ur._reference)) {
                        value += (ur._isNegative ? -1 : 1) * _dictionary.get(ur._reference);
                    } else {
                        raise(String.format("Module %s contains an external label with an undefined reference:%s",
                                            module._name,
                                            ur._reference));
                    }
                }
                establishLabel(label, value);
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
                try {
                    _lcPoolMap.put( lcps, address );
                    establishLabel(String.format("%s_LC$BASE_%d", lcps._module._name, lcps._lcIndex),
                                   (long) address);
                    establishLabel(String.format("%s_LC$BDI_%d", lcps._module._name, lcps._lcIndex),
                                   bd._bankDescriptorIndex);
                    address += lcps._module.getLocationCounterPool(lcps._lcIndex)._storage.length;
                } catch ( InvalidParameterException ex ) {
                    //  I don't think this can happen...
                }
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
     * Constructor
     */
    public Linker(
        final BankDeclaration[] bankDeclarations
    ) {
        _bankDeclarations = bankDeclarations;
    }

    /**
     * Performs the linkage
     * @param moduleName name of the absolute module to be created
     * @param display true to display linkage detailed output
     * @return newly created AbsoluteModule
     */
    public AbsoluteModule link(
        final String moduleName,
        final boolean display
    ) {
        System.out.println(String.format("Linking module %s -----------------------------------", moduleName));
        mapLCPools();
        extractLabels();

        LoadableBank[] loadableBanks = new LoadableBank[_bankDeclarations.length];
        for (int bx = 0; bx < _bankDeclarations.length; ++bx) {
            loadableBanks[bx] = createLoadableBank(_bankDeclarations[bx]);
        }

        try {
            if (_errors == 0) {
                AbsoluteModule module = new AbsoluteModule(moduleName, loadableBanks, loadableBanks[0]._startingAddress);
                if (display) {
                    display(module);
                }
                System.out.println(String.format("Linking Ends Errors=%d -------------------------------------------------------",
                                                 _errors));
                return module;
            }
        } catch (InvalidParameterException ex) {
            raise(ex.getMessage());
        }

        return null;
    }
}
