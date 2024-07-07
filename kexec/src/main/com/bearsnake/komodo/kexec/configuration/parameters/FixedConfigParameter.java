/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

public class FixedConfigParameter extends Parameter {

    public FixedConfigParameter(
        final Tag tag,
        final Object defaultValue,
        final String description
    ) {
        super(tag, defaultValue, description);
    }
}
