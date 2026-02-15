/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.MnemonicInfo;
import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.MnemonicException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.StringValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

import java.util.Map;

public class MnemonicRestriction implements Restriction {

    private final Map<String, MnemonicInfo> _mnemonicTable;

    public MnemonicRestriction(
        final Map<String, MnemonicInfo> mnemonicTable
    ) {
        _mnemonicTable = mnemonicTable;
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof StringValue sv) {
            if (!_mnemonicTable.containsKey(sv.getValue())) {
                throw new MnemonicException(value);
            }
        } else {
            throw new ValueTypeException(value, ValueType.STRING);
        }
    }
}
