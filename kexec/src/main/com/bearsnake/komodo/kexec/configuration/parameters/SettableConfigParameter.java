/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class SettableConfigParameter extends Parameter {

    private final boolean _isProtected; // needs SSCONFIGMGR privilege to change setting dynamically
    private final boolean _isRebootRequired;
    private Value _specifiedValue = null;

    public SettableConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description
    ) {
        this(tag, valueType, null, isProtected, isRebootRequired, description);
    }

    public SettableConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description
    ) {
        super(tag, valueType, defaultValue, description);
        _isProtected = isProtected;
        _isRebootRequired = isRebootRequired;
    }

    public boolean isProtected() { return _isProtected; }
    public boolean isRebootRequired() { return _isRebootRequired; }
    public void setSpecifiedValue(final Value value) { _specifiedValue = value; }

    @Override
    public Value getEffectiveValue() {
        return _specifiedValue != null ? _specifiedValue : super.getEffectiveValue();
    }

    @Override
    public boolean hasEffectiveValue() {
        return (_specifiedValue != null) || (super.hasEffectiveValue());
    }

    @Override
    public void setValue(
        final Value value
    ) throws ConfigurationException {
        _specifiedValue = value;
        checkValue();
    }
}
