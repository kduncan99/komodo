/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;

public class ValueMissingException extends ConfigurationException {

    public ValueMissingException(
        final Tag tag
    ) {
        super(String.format("Value not specified for parameter %s", tag.name()));
    }
}
