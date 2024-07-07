/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public interface ParameterRestriction {

    public boolean isValueAcceptable(final Object value);
}
