/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class ConfigParameter {

    private final ConfigParameterTag _tag;
    private final String _dynamicName;
    private final Object _defaultValue;
    private final boolean _isProtected; // needs SSCONFIGMGR privilege to change setting dynamically
    private final boolean _isRebootRequired;
    private final String _description;

    public ConfigParameter(
        final ConfigParameterTag tag,
        final String dynamicName,
        final Object defaultValue,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description
    ) {
        _tag = tag;
        _dynamicName = dynamicName;
        _defaultValue = defaultValue;
        _isProtected = isProtected;
        _isRebootRequired = isRebootRequired;
        _description = description;
    }

    public ConfigParameterTag getTag() { return _tag; }
    public String getStaticName() { return _tag.name(); }
    public String getDynamicName() { return _dynamicName; }
    public Object getDefaultValue() { return _defaultValue; }
    public String getDescription() { return _description; }
    public boolean isProtected() { return _isProtected; }
    public boolean isRebootRequired() { return _isRebootRequired; }
}
