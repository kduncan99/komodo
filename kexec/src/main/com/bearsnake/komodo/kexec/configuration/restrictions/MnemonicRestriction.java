/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.MnemonicInfo;
import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.MnemonicException;
import com.bearsnake.komodo.kexec.configuration.exceptions.TypeException;

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
        final Object value
    ) throws ConfigurationException {
        if (value instanceof String sv) {
            if (!_mnemonicTable.containsKey(sv)) {
                throw new MnemonicException(value);
            }
        } else {
            throw new TypeException(value, String.class);
        }
    }
}
