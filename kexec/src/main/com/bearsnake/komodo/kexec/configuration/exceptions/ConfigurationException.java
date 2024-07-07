/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.exceptions.KExecException;

public abstract class ConfigurationException extends KExecException {

    protected ConfigurationException(String message) {
        super(message);
    }
}
