/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

public class FixedParameterException extends ConfigurationException {

    public FixedParameterException() {
        super("Cannot update parameter value");
    }
}
