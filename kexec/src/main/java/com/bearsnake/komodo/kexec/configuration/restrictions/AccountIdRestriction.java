/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.exceptions.AccountIdException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.StringValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class AccountIdRestriction implements Restriction {

    public AccountIdRestriction() {}

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof StringValue sv) {
            if (!Parser.isValidAccountId(sv.getValue())) {
                throw new AccountIdException(value);
            }
        } else {
            throw new ValueTypeException(value, ValueType.STRING);
        }
    }
}
