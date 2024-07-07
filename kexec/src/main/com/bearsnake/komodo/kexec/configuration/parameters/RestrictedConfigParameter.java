/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.restrictions.Restriction;

public class RestrictedConfigParameter extends SettableConfigParameter {

    private final Restriction _restriction;

    public RestrictedConfigParameter(
        final Tag tag,
        final Object defaultValue,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description,
        final Restriction restriction
    ) {
        super(tag, defaultValue, isProtected, isRebootRequired, description);
        _restriction = restriction;
    }

    public Restriction getRestriction() { return _restriction; }

    public void checkValue(
        final Object value
    ) throws ConfigurationException {
        _restriction.checkValue(value);
    }
}
