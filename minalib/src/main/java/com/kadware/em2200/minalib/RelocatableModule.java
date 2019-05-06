/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.TreeMap;
import java.util.Map;

/**
 * Represents the assembly of a particular set of lines of source code,
 * generally accepted as representing a symbolic element or file.
 */
public class RelocatableModule {

    public final String _name;
    public final Map<Integer, LocationCounterPool> _storage = new TreeMap<>();
    public final Map<String, IntegerValue> _externalLabels = new TreeMap<>();
    public final boolean _requiresQuarterWordMode;
    public final boolean _requiresThirdWordMode;
    public final boolean _requiresArithmeticFaultCompatibilityMode;
    public final boolean _requiresArithmeticFaultNonInterruptMode;

    /**
     * Constructor
     * @param name name of the module
     */
    private RelocatableModule(
        final String name,
        final Map<Integer, LocationCounterPool> storage,
        final Map<String, IntegerValue> externalLabels,
        final boolean quarterWord,
        final boolean thirdWord,
        final boolean arithmeticCompatibility,
        final boolean arithmeticNonInterrupt
    ) {
        _name = name;
        if (storage != null) {
            _storage.putAll(storage);
        }
        if (externalLabels != null) {
            _externalLabels.putAll(externalLabels);
        }
        _requiresQuarterWordMode = quarterWord;
        _requiresThirdWordMode = thirdWord;
        _requiresArithmeticFaultCompatibilityMode = arithmeticCompatibility;
        _requiresArithmeticFaultNonInterruptMode = arithmeticNonInterrupt;
    }

    /**
     * Retrieves the LocationCounterPool associated with the given index.
     * If one does not exist, it is created and returned.
     * @param index lc index
     * @return reference to LocationCounterPool
     * @throws InvalidParameterException if the index is out of range
     */
    public LocationCounterPool getLocationCounterPool(
        final int index
    ) throws InvalidParameterException {
        if ( (index < 0) || (index > 63) ) {
            throw new InvalidParameterException(String.format("Location Counter Index %d is out of range", index));
        }

        LocationCounterPool lcp = _storage.get(index);
        if (lcp == null) {
            lcp = new LocationCounterPool( new RelocatableWord36[0] );
            _storage.put( index, lcp );
        }

        return lcp;
    }

    public static class Builder {
        private String _name = null;
        private Map<Integer, LocationCounterPool> _storage = null;
        private Map<String, IntegerValue> _externalLabels = null;
        private boolean _requiresQuarterWordMode = false;
        private boolean _requiresThirdWordMode = false;
        private boolean _requiresArithmeticFaultCompatibilityMode = false;
        private boolean _requiresArithmeticFaultNonInterruptMode = false;

        public Builder setExternalLabels(
            final Map<String, IntegerValue> externalLabelse
        ) {
            _externalLabels = externalLabelse;
            return this;
        }

        public Builder setName(
            final String name
        ) {
            _name = name;
            return this;
        }

        public Builder setArithmeticFaultCompatibilityMode(
            final boolean flag
        ) {
            _requiresArithmeticFaultCompatibilityMode = flag;
            return this;
        }

        public Builder setArithmeticFaultNonInterruptMode(
            final boolean flag
        ) {
            _requiresArithmeticFaultNonInterruptMode = flag;
            return this;
        }

        public Builder setRequiresQuarterWordMode(
            final boolean flag
        ) {
            _requiresQuarterWordMode = flag;
            return this;
        }

        public Builder setRequiresThirdWordMode(
            final boolean flag
        ) {
            _requiresThirdWordMode = flag;
            return this;
        }

        public Builder setStorage(
            final Map<Integer, LocationCounterPool> storage
        ) {
            _storage = storage;
            return this;
        }

        public RelocatableModule build() {
            if (_name == null) {
                throw new RuntimeException("No name specified for relocatable module");
            }
            if ((_requiresQuarterWordMode && _requiresThirdWordMode)
                || (_requiresArithmeticFaultCompatibilityMode && _requiresArithmeticFaultNonInterruptMode)) {
                throw new RuntimeException("Incompatible mode configuration for relocatable module");
            }

            return new RelocatableModule(_name,
                                         _storage,
                                         _externalLabels,
                                         _requiresQuarterWordMode,
                                         _requiresThirdWordMode,
                                         _requiresArithmeticFaultCompatibilityMode,
                                         _requiresArithmeticFaultCompatibilityMode);
        }
    }
}
