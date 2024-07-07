/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class RestrictedConfigParameter extends SettableConfigParameter {

    private final ParameterRestriction _restriction;

    public RestrictedConfigParameter(
        final ConfigParameterTag tag,
        final String dynamicName,
        final Object defaultValue,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description,
        final ParameterRestriction restriction
    ) {
        super(tag, dynamicName, defaultValue, isProtected, isRebootRequired, description);
        _restriction = restriction;
    }

    public ParameterRestriction getRestriction() { return _restriction; }
    public boolean isValueAcceptable(final Object value) { return _restriction.isValueAcceptable(value); }
}
