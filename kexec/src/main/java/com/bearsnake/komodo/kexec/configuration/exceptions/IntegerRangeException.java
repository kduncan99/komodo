/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.restrictions.IntegerRangeRestriction;

public class IntegerRangeException extends ConfigurationException {

    public IntegerRangeException(
        final Object value,
        final IntegerRangeRestriction restriction
    ) {
        super(String.format("Value %s is out of range %s", value, restriction));
    }
}
