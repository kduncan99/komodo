/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ElementNameException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.StringValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class ElementNameRestriction implements Restriction {

    private boolean _allowBlank;

    public ElementNameRestriction(
        final boolean allowBlank
    ) {
        _allowBlank = allowBlank;
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof StringValue sv) {
            var str = sv.getValue();
            if (_allowBlank && str.isEmpty()) {
                return;
            }
            if (!Parser.isValidElementName(sv.getValue())) {
                throw new ElementNameException(value);
            }
        } else {
            throw new ValueTypeException(value, ValueType.STRING);
        }
    }
}
