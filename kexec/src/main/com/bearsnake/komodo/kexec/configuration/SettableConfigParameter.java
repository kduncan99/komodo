/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class SettableConfigParameter extends ConfigParameter {

    private final boolean _isProtected; // needs SSCONFIGMGR privilege to change setting dynamically
    private final boolean _isRebootRequired;
    private Object _specifiedValue = null;

    public SettableConfigParameter(
        final ConfigParameterTag tag,
        final String dynamicName,
        final Object defaultValue,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description
    ) {
        super(tag, dynamicName, defaultValue, description);
        _isProtected = isProtected;
        _isRebootRequired = isRebootRequired;
    }

    @Override
    public Object getEffectiveValue() {
        return _specifiedValue != null ? _specifiedValue : super.getEffectiveValue();
    }

    @Override
    public boolean hasEffectiveValue() {
        return (_specifiedValue != null) || (super.hasEffectiveValue());
    }

    public boolean isProtected() { return _isProtected; }
    public boolean isRebootRequired() { return _isRebootRequired; }
    public void setSpecifiedValue(final Object value) { _specifiedValue = value; }
}
