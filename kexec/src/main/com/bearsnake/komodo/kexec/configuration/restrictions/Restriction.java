/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;

public interface Restriction {

    void checkValue(final Object value) throws ConfigurationException;
}
