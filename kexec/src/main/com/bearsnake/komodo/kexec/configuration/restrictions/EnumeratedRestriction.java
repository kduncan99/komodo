/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.EnumeratedException;
import com.bearsnake.komodo.kexec.configuration.values.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EnumeratedRestriction implements Restriction {

    private final Set<Value> _values = new HashSet<>();

    public EnumeratedRestriction(
        final Value[] values
    ) {
        Collections.addAll(_values, values);
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (!_values.contains(value)) {
            throw new EnumeratedException(value, this);
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (Object value : _values) {
            sb.append(value).append(" ");
        }
        return sb.toString();
    }
}
