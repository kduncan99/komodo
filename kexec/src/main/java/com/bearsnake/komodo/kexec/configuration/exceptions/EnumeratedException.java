/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.restrictions.EnumeratedRestriction;

public class EnumeratedException extends ConfigurationException {

    public EnumeratedException(
        final Object value,
        final EnumeratedRestriction restriction
    ) {
        super(String.format("Value %s is not a member of set %s", value, restriction));
    }
}
