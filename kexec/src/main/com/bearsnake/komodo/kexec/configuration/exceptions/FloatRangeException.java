/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.restrictions.FloatRangeRestriction;

public class FloatRangeException extends ConfigurationException {

    public FloatRangeException(
        final Object value,
        final FloatRangeRestriction restriction
    ) {
        super(String.format("Value %s is out of range %s", value, restriction));
    }
}
