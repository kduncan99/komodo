/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.restrictions.Restriction;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class RestrictedConfigParameter extends SettableConfigParameter {

    private final Restriction[] _restrictions;

    public RestrictedConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description,
        final Restriction ... restriction
    ) {
        super(tag, valueType, isProtected, isRebootRequired, description);
        _restrictions = restriction;
    }

    public RestrictedConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final boolean isProtected,
        final boolean isRebootRequired,
        final String description,
        final Restriction ... restriction
    ) {
        super(tag, valueType, defaultValue, isProtected, isRebootRequired, description);
        _restrictions = restriction;
    }

    public Restriction[] getRestrictions() { return _restrictions; }

    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        for (var r : _restrictions) {
            r.checkValue(value);
        }
    }

    @Override
    public void setValue(
        final Value value
    ) throws ConfigurationException {
        super.setValue(value);
        checkValue(value);
    }
}
