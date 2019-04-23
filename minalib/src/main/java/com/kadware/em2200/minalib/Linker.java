/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.Word36Array;
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
        final int _bankDescriptorIndex;
        final int _bankLevel;
        final String _bankName;
        final Integer _initialBaseRegister;
        final boolean _isExtended;
        final LCPoolSpecification[] _poolSpecifications;
        final int _startingAddress;

        public BankDeclaration(
            final String bankName,
            final int bankDescriptorIndex,
            final int bankLevel,
            final int startingAddress,
            final boolean isExtended,
            final LCPoolSpecification[] poolSpecifications
        ) {
            _bankDescriptorIndex = bankDescriptorIndex;
            _bankLevel = bankLevel;
            _bankName = bankName;
            _initialBaseRegister = null;
            _isExtended = isExtended;
            _poolSpecifications = poolSpecifications;
            _startingAddress = startingAddress;
        }

        public BankDeclaration(
            final String bankName,
            final int bankDescriptorIndex,
            final int bankLevel,
            final int startingAddress,
            final boolean isExtended,
            final LCPoolSpecification[] poolSpecifications,
            final int initialBaseRegister
        ) {
            _bankDescriptorIndex = bankDescriptorIndex;
            _bankLevel = bankLevel;
            _bankName = bankName;
            _initialBaseRegister = initialBaseRegister;
            _isExtended = isExtended;
            _poolSpecifications = poolSpecifications;
            _startingAddress = startingAddress;
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
                for (RelocatableWord36 rw36 : lcps._module.getLocationCounterPool(lcps._lcIndex)._storage) {
                    //TODO
                }
            }

            return new LoadableBank(bankDeclaration._bankDescriptorIndex,
                                    bankDeclaration._startingAddress,
                                    wArray);
        } catch (InvalidParameterException ex) {
            //  can't happen
            return null;
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
                if (_dictionary.containsKey(label)) {
                    raise("Duplicate label:" + label);
                } else {
                    long value = entry.getValue().getValue();
                    for (IntegerValue.UndefinedReference ur : entry.getValue().getUndefinedReferences()) {
                        if (_dictionary.containsKey(ur._reference)) {
                            value += (ur._isNegative ? -1 : 1) * _dictionary.get(ur._reference);
                        } else {
                            raise(String.format("Module %s contains an external label with an undefined reference:%s",
                                                module._name,
                                                ur._reference));
                        }
                    }
                    _dictionary.put(label, value);
                }
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
                    String label = String.format("%s_LC$BASE_%d", lcps._module._name, lcps._lcIndex);
                    if (_dictionary.containsKey(label)) {
                        raise("Duplicate label:" + label);
                    } else {
                        _dictionary.put(label, (long) address);
                    }
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

    public AbsoluteModule link(
        final String moduleName
    ) {
        mapLCPools();
        extractLabels();

        LoadableBank[] loadableBanks = new LoadableBank[_bankDeclarations.length];
        for (int bx = 0; bx < _bankDeclarations.length; ++bx) {
            loadableBanks[bx = createLoadableBank(bd);
        }

        try {
            if (_errors == 0) {
                return new AbsoluteModule(moduleName, loadableBanks);
            }
        } catch (InvalidParameterException ex) {
            raise(ex.getMessage());
        } finally {
            return null;
        }
    }
}
