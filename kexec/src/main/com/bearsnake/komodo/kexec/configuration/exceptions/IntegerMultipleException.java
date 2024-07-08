/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.restrictions.IntegerMultipleRestriction;

public class IntegerMultipleException extends ConfigurationException {

    public IntegerMultipleException(
        final Object value,
        final IntegerMultipleRestriction restriction
    ) {
        super(String.format("Value %s is not a multiple of %s", value, restriction));
    }
}
