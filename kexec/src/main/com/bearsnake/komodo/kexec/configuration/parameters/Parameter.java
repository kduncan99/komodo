/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

public abstract class Parameter {

    private final Tag _tag;
    private final Object _defaultValue;
    private final String _description;

    public Parameter(
        final Tag tag,
        final Object defaultValue,
        final String description
    ) {
        _tag = tag;
        _defaultValue = defaultValue;
        _description = description;
    }

    public Object getEffectiveValue() { return _defaultValue; }
    public Tag getTag() { return _tag; }
    public String getStaticName() { return _tag.name(); }
    public Object getDefaultValue() { return _defaultValue; }
    public String getDescription() { return _description; }
    public boolean hasEffectiveValue() { return _defaultValue != null; }
}
