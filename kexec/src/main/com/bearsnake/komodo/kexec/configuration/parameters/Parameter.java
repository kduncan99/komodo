/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public abstract class Parameter {

    private final Tag _tag;
    private final ValueType _valueType;
    private final Value _defaultValue;
    private final String _description;

    // c'tor for parameters without default values
    public Parameter(
        final Tag tag,
        final ValueType valueType,
        final String description
    ) {
        this(tag, valueType, null, description);
    }

    // c'tor for parameters with default values
    public Parameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final String description
    ) {
        _tag = tag;
        _valueType = valueType;
        _defaultValue = defaultValue;
        _description = description;
    }

    public Value getEffectiveValue() { return _defaultValue; }
    public Tag getTag() { return _tag; }
    public String getStaticName() { return _tag.name(); }
    public Object getDefaultValue() { return _defaultValue; }
    public String getDescription() { return _description; }
    public ValueType getValueType() { return _valueType; }
    public boolean hasDefaultValue() { return _defaultValue != null; }
    public boolean hasEffectiveValue() { return _defaultValue != null; }
    public abstract void setValue(Value value) throws ConfigurationException;
}
