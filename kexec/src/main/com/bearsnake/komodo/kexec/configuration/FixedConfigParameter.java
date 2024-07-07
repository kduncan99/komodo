/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class FixedConfigParameter extends ConfigParameter {

    public FixedConfigParameter(
        final ConfigParameterTag tag,
        final String dynamicName,
        final Object defaultValue,
        final String description
    ) {
        super(tag, dynamicName, defaultValue, description);
    }
}
