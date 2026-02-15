/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.UserIdException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.StringValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class UserIdRestriction implements Restriction {

    public UserIdRestriction() {}

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof StringValue sv) {
            if (!Parser.isValidUserId(sv.getValue())) {
                throw new UserIdException(value);
            }
        } else {
            throw new ValueTypeException(value, ValueType.STRING);
        }
    }
}
