/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.exceptions.InvalidParameterException;
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

    public static class Builder {
        private String _name = null;
        private Map<Integer, LocationCounterPool> _storage = null;
        private Map<String, IntegerValue> _externalLabels = null;
        private boolean _requiresQuarterWordMode = false;
        private boolean _requiresThirdWordMode = false;
        private boolean _requiresArithmeticFaultCompatibilityMode = false;
        private boolean _requiresArithmeticFaultNonInterruptMode = false;

        public Builder setExternalLabels(final Map<String, IntegerValue> value) { _externalLabels = value; return this; }
        public Builder setName(final String value) { _name = value; return this; }
        public Builder setArithmeticFaultCompatibilityMode(final boolean value) { _requiresArithmeticFaultCompatibilityMode = value; return this; }
        public Builder setArithmeticFaultNonInterruptMode(final boolean value) { _requiresArithmeticFaultNonInterruptMode = value; return this; }
        public Builder setRequiresQuarterWordMode(final boolean value) { _requiresQuarterWordMode = value; return this; }
        public Builder setRequiresThirdWordMode(final boolean value) { _requiresThirdWordMode = value; return this; }
        public Builder setStorage(final Map<Integer, LocationCounterPool> storage) { _storage = storage; return this; }

        public RelocatableModule build(
        ) throws InvalidParameterException {
            if (_name == null) {
                throw new InvalidParameterException("No name specified");
            }

            if (_storage == null) {
                throw new InvalidParameterException("No storage provided");
            }

            if ((_requiresQuarterWordMode && _requiresThirdWordMode)
                || (_requiresArithmeticFaultCompatibilityMode && _requiresArithmeticFaultNonInterruptMode)) {
                throw new InvalidParameterException("Incompatible mode configuration");
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
