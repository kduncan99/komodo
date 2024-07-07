/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.restrictions.IntegerRangeRestriction;

public class RangeException extends ConfigurationException {

    public RangeException(
        final Object value,
        final IntegerRangeRestriction restriction
    ) {
        super(String.format("Value %s is out of range %s", value, restriction));
    }
}
